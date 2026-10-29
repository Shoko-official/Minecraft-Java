package dev.shoko.minecraft.item.stack;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.NumericTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.core.nbt.snbt.SnbtWriter;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.FindingException;
import dev.shoko.minecraft.item.component.ComponentChange;
import dev.shoko.minecraft.item.component.ComponentCodec;
import dev.shoko.minecraft.item.component.ComponentPatch;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.registry.ItemType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.StringJoiner;

/**
 * Reads and writes stacks in the three forms of the game (IC-R24 to IC-R29): the NBT of
 * inventories, the JSON of data files and the text of commands. The item registry decides which
 * identifiers are known; component values go through {@code ComponentCodec}.
 */
public final class StackCodec {

  private static final String ID = "id";
  private static final String COUNT = "count";
  private static final String COMPONENTS = "components";

  private final ItemCatalog items;
  private final ComponentCodec components;

  private StackCodec(ItemCatalog items) {
    this.items = items;
    this.components = ComponentCodec.of(items.components());
  }

  /**
   * Creates a codec.
   *
   * @param items the loaded items
   * @return the codec
   */
  public static StackCodec of(ItemCatalog items) {
    return new StackCodec(items);
  }

  /**
   * Reads a stack from NBT (IC-R24, IC-R26): {@code {id, count?, components?}}.
   *
   * @param tag the compound; extra keys such as {@code Slot} are ignored
   * @return the stack. A missing {@code count} is 1; a count above 99 is a {@code VALUE_RANGE}
   *     failure, as in the JSON form (IC-R16). An unknown {@code id} gives the empty stack with an
   *     {@code UNKNOWN_ITEM} warning, and a count of 0 or less the empty stack with {@code
   *     NON_POSITIVE_COUNT} (provisional, probe PI6). A missing {@code id} or a wrongly typed field
   *     is a failure; so is an error in {@code components}
   */
  public Decoding<ItemStack> fromNbt(CompoundTag tag) {
    try {
      return readNbt(tag);
    } catch (FindingException refusal) {
      return new Decoding.Failure<>(refusal.findings());
    }
  }

  private Decoding<ItemStack> readNbt(CompoundTag tag) {
    Identifier id = itemId(nbtField(tag, ID, "$.id"), "$.id");
    int count = nbtCount(tag);
    CompoundTag patchTag = nbtComponents(tag);
    List<Finding> warnings = new ArrayList<>();
    Optional<ItemType> type = items.find(id);
    if (type.isEmpty()) {
      warnings.add(new Finding(FindingCode.UNKNOWN_ITEM, "$.id", "no item " + id));
    }
    if (count <= 0) {
      warnings.add(new Finding(FindingCode.NON_POSITIVE_COUNT, "$.count", "count " + count));
    }
    if (!warnings.isEmpty()) {
      return new Decoding.Success<>(ItemStack.empty(), warnings);
    }
    ComponentPatch patch = decodePatch(components.patchFromNbt(patchTag));
    return new Decoding.Success<>(ItemStack.of(type.get(), count, patch), List.of());
  }

  private static NbtTag nbtField(CompoundTag tag, String key, String path) {
    return tag.get(key)
        .orElseThrow(
            () -> new FindingException(FindingCode.MISSING_FIELD, path, "missing field " + key));
  }

  private static int nbtCount(CompoundTag tag) {
    Optional<NbtTag> count = tag.get(COUNT);
    if (count.isEmpty()) {
      return 1;
    }
    if (!(count.get() instanceof NumericTag number) || !isIntegral(number)) {
      throw new FindingException(FindingCode.TYPE_MISMATCH, "$.count", "expected an integer");
    }
    long value = number.longValue();
    if (value > ItemStack.MAX_COUNT) {
      throw new FindingException(
          FindingCode.VALUE_RANGE, "$.count", "count above " + ItemStack.MAX_COUNT);
    }
    return (int) Math.max(value, Integer.MIN_VALUE);
  }

  private static boolean isIntegral(NumericTag number) {
    return switch (number.type()) {
      case BYTE, SHORT, INT, LONG -> true;
      default -> false;
    };
  }

  private static CompoundTag nbtComponents(CompoundTag tag) {
    Optional<NbtTag> value = tag.get(COMPONENTS);
    if (value.isEmpty()) {
      return CompoundTag.empty();
    }
    if (value.get() instanceof CompoundTag compound) {
      return compound;
    }
    throw new FindingException(FindingCode.TYPE_MISMATCH, "$.components", "expected a compound");
  }

  private static Identifier itemId(NbtTag tag, String path) {
    if (!(tag instanceof StringTag text)) {
      throw new FindingException(FindingCode.TYPE_MISMATCH, path, "expected a string");
    }
    return parseId(text.value(), path);
  }

  private static Identifier parseId(String text, String path) {
    return Identifier.tryParse(text)
        .orElseThrow(
            () ->
                new FindingException(
                    FindingCode.INVALID_IDENTIFIER, path, "not an identifier: " + text));
  }

  private static ComponentPatch decodePatch(Decoding<ComponentPatch> decoding) {
    if (decoding instanceof Decoding.Success<ComponentPatch> success) {
      return success.value();
    }
    List<Finding> prefixed = new ArrayList<>();
    for (Finding finding : decoding.findings()) {
      prefixed.add(
          new Finding(
              finding.code(), "$.components" + finding.path().substring(1), finding.message()));
    }
    throw new FindingException(prefixed);
  }

  /**
   * Writes a stack to NBT (IC-R24, IC-R27).
   *
   * @param stack a non-empty stack; the empty stack is written as an absence by the caller
   * @return {@code {id, count, components?}} in this key order, {@code count} an Int, {@code
   *     components} only when the normalized patch is not empty
   * @throws IllegalArgumentException when the stack is empty
   */
  public CompoundTag toNbt(ItemStack stack) {
    requireNonEmpty(stack);
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    entries.put(ID, new StringTag(stack.type().id().toString()));
    entries.put(COUNT, new IntTag(stack.count()));
    if (!stack.patch().isEmpty()) {
      entries.put(COMPONENTS, components.patchToNbt(stack.patch()));
    }
    return CompoundTag.of(entries);
  }

  private static void requireNonEmpty(ItemStack stack) {
    if (stack.isEmpty()) {
      throw new IllegalArgumentException("the empty stack has no written form");
    }
  }

  /**
   * Reads a stack from JSON (IC-R28): {@code {id, count?, components?}}.
   *
   * @param node the JSON object; a missing {@code count} is 1. The empty object is not accepted
   *     here: recipes handle the derived result of IC-R28 themselves
   * @return the stack, or the failure
   */
  public Decoding<ItemStack> fromJson(JsonNode node) {
    return Decoding.attempt(() -> readJson(node));
  }

  private ItemStack readJson(JsonNode node) {
    if (!(node instanceof JsonObject object)) {
      throw new FindingException(FindingCode.TYPE_MISMATCH, "$", "expected an object");
    }
    JsonNode idNode =
        object
            .get(ID)
            .orElseThrow(
                () -> new FindingException(FindingCode.MISSING_FIELD, "$.id", "missing field id"));
    if (!(idNode instanceof JsonString text)) {
      throw new FindingException(FindingCode.TYPE_MISMATCH, "$.id", "expected a string");
    }
    Identifier id = parseId(text.value(), "$.id");
    ItemType type =
        items
            .find(id)
            .filter(found -> !found.isAir())
            .orElseThrow(
                () ->
                    new FindingException(
                        FindingCode.UNRESOLVED_ITEM, "$.id", "no item stack of " + id));
    int count = object.get(COUNT).map(StackCodec::jsonCount).orElse(1);
    ComponentPatch patch =
        object
            .get(COMPONENTS)
            .map(c -> decodePatch(components.patchFromJson(c)))
            .orElse(ComponentPatch.empty());
    return ItemStack.of(type, count, patch);
  }

  private static int jsonCount(JsonNode node) {
    if (!(node instanceof JsonNumber number) || !number.isInteger()) {
      throw new FindingException(FindingCode.TYPE_MISMATCH, "$.count", "expected an integer");
    }
    long value = number.longValue();
    if (value < 1 || value > ItemStack.MAX_COUNT) {
      throw new FindingException(
          FindingCode.VALUE_RANGE, "$.count", "count outside 1 to " + ItemStack.MAX_COUNT);
    }
    return (int) value;
  }

  /**
   * Writes a stack as compact JSON text that the JSON reader of {@code data} reads back to an equal
   * stack.
   *
   * @param stack a non-empty stack
   * @return the JSON text, {@code count} always written
   * @throws IllegalArgumentException when the stack is empty or a component holds a number that
   *     JSON cannot write (NaN or an infinity, which only NBT input produces)
   */
  public String toJson(ItemStack stack) {
    requireNonEmpty(stack);
    StringBuilder out = new StringBuilder("{\"id\":\"").append(stack.type().id()).append('"');
    out.append(",\"count\":").append(stack.count());
    if (!stack.patch().isEmpty()) {
      out.append(",\"components\":").append(componentsJson(stack.patch()));
    }
    return out.append('}').toString();
  }

  private String componentsJson(ComponentPatch patch) {
    StringJoiner joiner = new StringJoiner(",", "{", "}");
    patch
        .changes()
        .forEach(
            (id, change) ->
                joiner.add(
                    switch (change) {
                      case ComponentChange.Set set ->
                          "\"" + id + "\":" + components.valueToJson(id, set.value());
                      case ComponentChange.Removed _ -> "\"!" + id + "\":{}";
                    }));
    return joiner.toString();
  }

  /**
   * Reads the text form of the {@code minecraft:item_stack} argument of commands (IC-R29): {@code
   * <id>[<component>=<value>, !<component>, ...]} with SNBT values. The count is not part of this
   * form.
   *
   * @param text the argument text
   * @return the stack with a count of 1, or the failure with {@code TEXT_SYNTAX} or the finding of
   *     the component
   */
  public Decoding<ItemStack> fromCommandText(String text) {
    return Decoding.attempt(
        () -> {
          CommandText.Parts parts = CommandText.split(text);
          Identifier id = parseId(parts.item(), "$");
          ItemType type =
              items
                  .find(id)
                  .filter(found -> !found.isAir())
                  .orElseThrow(
                      () ->
                          new FindingException(
                              FindingCode.UNRESOLVED_ITEM, "$", "no item stack of " + id));
          CompoundTag entries = CompoundTag.of(parts.components());
          return ItemStack.of(type, 1, decodePatch(components.patchFromNbt(entries)));
        });
  }

  /**
   * Writes the text form of commands.
   *
   * @param stack a non-empty stack
   * @return the identifier followed by the bracketed modifications when the patch is not empty
   * @throws IllegalArgumentException when the stack is empty
   */
  public String toCommandText(ItemStack stack) {
    requireNonEmpty(stack);
    String id = stack.type().id().toString();
    if (stack.patch().isEmpty()) {
      return id;
    }
    StringJoiner joiner = new StringJoiner(",", id + "[", "]");
    stack
        .patch()
        .changes()
        .forEach(
            (key, change) ->
                joiner.add(
                    switch (change) {
                      case ComponentChange.Set set -> key + "=" + snbt(set.value().tag());
                      case ComponentChange.Removed _ -> "!" + key;
                    }));
    return joiner.toString();
  }

  private static String snbt(NbtTag tag) {
    try {
      return SnbtWriter.write(tag);
    } catch (NbtException e) {
      throw new IllegalArgumentException("value too deep for SNBT", e);
    }
  }
}

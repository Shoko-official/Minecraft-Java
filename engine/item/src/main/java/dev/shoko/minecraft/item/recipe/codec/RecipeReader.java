package dev.shoko.minecraft.item.recipe.codec;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonBoolean;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.data.registry.Holder;
import dev.shoko.minecraft.data.registry.HolderSet;
import dev.shoko.minecraft.data.registry.TagKey;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.FindingException;
import dev.shoko.minecraft.item.recipe.RecipeCategory;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.stack.StackCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The common readers of the fields of a recipe file: strings, numbers, ingredients (RC-R9) and
 * result stacks (RC-R11a, RC-R24). A problem is thrown as a {@link FindingException} and caught by
 * {@link RecipeCodec}, so that each reader reads straight through.
 */
final class RecipeReader {

  private final ItemCatalog items;
  private final StackCodec stacks;
  private final ItemAccess access;

  RecipeReader(ItemCatalog items, StackCodec stacks, ItemAccess access) {
    this.items = items;
    this.stacks = stacks;
    this.access = access;
  }

  static String at(String path, String field) {
    return path + "." + field;
  }

  static JsonObject object(JsonNode node, String path) {
    if (node instanceof JsonObject object) {
      return object;
    }
    throw new FindingException(FindingCode.TYPE_MISMATCH, path, "expected an object");
  }

  static JsonNode required(JsonObject object, String field, String path) {
    return object
        .get(field)
        .orElseThrow(
            () ->
                new FindingException(
                    FindingCode.MISSING_FIELD, at(path, field), "missing field " + field));
  }

  static String string(JsonNode node, String path) {
    if (node instanceof JsonString string) {
      return string.value();
    }
    throw new FindingException(FindingCode.TYPE_MISMATCH, path, "expected a string");
  }

  static Identifier identifier(JsonNode node, String path) {
    String text = string(node, path);
    return Identifier.tryParse(text)
        .orElseThrow(
            () ->
                new FindingException(
                    FindingCode.INVALID_IDENTIFIER, path, "not an identifier: " + text));
  }

  static String group(JsonObject object, String path) {
    return object.get("group").map(node -> string(node, at(path, "group"))).orElse("");
  }

  static RecipeCategory category(JsonObject object, String path) {
    Optional<JsonNode> node = object.get("category");
    if (node.isEmpty()) {
      return RecipeCategory.MISC;
    }
    String field = at(path, "category");
    String name = string(node.get(), field).toUpperCase(Locale.ROOT);
    for (RecipeCategory category : RecipeCategory.values()) {
      if (category.name().equals(name)) {
        return category;
      }
    }
    throw new FindingException(FindingCode.VALUE_RANGE, field, "unknown category " + name);
  }

  static int integer(JsonNode node, String path) {
    if (!(node instanceof JsonNumber number) || !number.isInteger()) {
      throw new FindingException(FindingCode.TYPE_MISMATCH, path, "expected an integer");
    }
    long value = number.longValue();
    if (value != (int) value) {
      throw new FindingException(FindingCode.VALUE_RANGE, path, "integer out of range");
    }
    return (int) value;
  }

  static float decimal(JsonNode node, String path) {
    if (!(node instanceof JsonNumber number)) {
      throw new FindingException(FindingCode.TYPE_MISMATCH, path, "expected a number");
    }
    float value = (float) number.doubleValue();
    if (Float.isInfinite(value)) {
      throw new FindingException(FindingCode.VALUE_RANGE, path, "number out of range");
    }
    return value;
  }

  static boolean bool(JsonNode node, String path) {
    if (node instanceof JsonBoolean bool) {
      return bool.value();
    }
    throw new FindingException(FindingCode.TYPE_MISMATCH, path, "expected a boolean");
  }

  /** Reads an ingredient: an item, a tag prefixed by {@code #}, or a list of items (RC-R9). */
  Ingredient ingredient(JsonNode node, String path) {
    return switch (node) {
      case JsonString string -> new Ingredient(single(string.value(), path));
      case JsonArray array -> new Ingredient(list(array, path));
      default ->
          throw new FindingException(
              FindingCode.TYPE_MISMATCH, path, "expected an item, a tag or a list of items");
    };
  }

  private HolderSet<ItemType> single(String text, String path) {
    if (!text.startsWith("#")) {
      return HolderSet.list(List.of(item(text, path)));
    }
    Identifier id =
        Identifier.tryParse(text.substring(1))
            .orElseThrow(
                () ->
                    new FindingException(
                        FindingCode.INVALID_IDENTIFIER, path, "not a tag identifier: " + text));
    TagKey<ItemType> tag = new TagKey<>(ItemCatalog.REGISTRY, id);
    if (access.tagMembers(tag).isEmpty()) {
      throw new FindingException(FindingCode.UNRESOLVED_TAG, path, "tag #" + id + " is not loaded");
    }
    return HolderSet.tag(tag, access);
  }

  private HolderSet<ItemType> list(JsonArray array, String path) {
    if (array.elements().isEmpty()) {
      throw new FindingException(FindingCode.EMPTY_INGREDIENT, path, "an empty list of items");
    }
    List<Holder<ItemType>> holders = new ArrayList<>();
    for (int i = 0; i < array.elements().size(); i++) {
      String at = path + "[" + i + "]";
      holders.add(item(string(array.elements().get(i), at), at));
    }
    return HolderSet.list(holders);
  }

  private Holder<ItemType> item(String text, String path) {
    Identifier id =
        Identifier.tryParse(text)
            .orElseThrow(
                () ->
                    new FindingException(
                        FindingCode.INVALID_IDENTIFIER, path, "not an identifier: " + text));
    return items
        .registry()
        .holder(id)
        .orElseThrow(
            () -> new FindingException(FindingCode.UNRESOLVED_ITEM, path, "no item " + id));
  }

  /** Reads a stack of the file whose count does not exceed the stack size of its item. */
  ItemStack stack(JsonNode node, String path) {
    Decoding<ItemStack> decoded = stacks.fromJson(node);
    if (!(decoded instanceof Decoding.Success<ItemStack> success)) {
      List<Finding> prefixed = new ArrayList<>();
      for (Finding finding : decoded.findings()) {
        prefixed.add(
            new Finding(finding.code(), path + finding.path().substring(1), finding.message()));
      }
      throw new FindingException(prefixed);
    }
    ItemStack stack = success.value();
    if (stack.count() > stack.maxStackSize()) {
      throw new FindingException(
          FindingCode.RESULT_EXCEEDS_STACK,
          path,
          stack.count() + " exceeds the stack size " + stack.maxStackSize());
    }
    return stack;
  }

  /**
   * Reads the {@code result} of a crafting recipe: a stack, or the empty object and an absent field
   * for a result derived from the grid (RC-R12).
   *
   * @param root the recipe object
   * @param path the path of the recipe object
   * @return the result
   */
  RecipeResult derivableResult(JsonObject root, String path) {
    Optional<JsonNode> node = root.get("result");
    if (node.isEmpty() || (node.get() instanceof JsonObject o && o.members().isEmpty())) {
      return new RecipeResult.Derived();
    }
    return new RecipeResult.Fixed(stack(node.get(), at(path, "result")));
  }
}

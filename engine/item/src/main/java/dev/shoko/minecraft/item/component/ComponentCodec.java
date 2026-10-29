package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.FindingException;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * Decodes and encodes component values and tables (IC-R9 to IC-R13, IC-R27). The registry is the
 * acceptance list: an identifier it does not contain makes the whole table fail with {@code
 * UNKNOWN_COMPONENT} (IC-R10). The shape of each value follows IC-R12 for the 69 types seen in the
 * prototypes and IC-R9b (provisional) for the 53 others; a field outside the shape is {@code
 * UNKNOWN_FIELD} and a missing required one {@code MISSING_FIELD} (IC-R12, probe PI3). Floating
 * point numbers are read as double, then converted to the type of the field (IC-R13).
 */
public final class ComponentCodec {

  private static final String REMOVAL_PREFIX = "!";

  private final ComponentCatalog catalog;
  private final ShapeReader<JsonNode> jsonReader = new ShapeReader<>(JsonInput.INSTANCE);
  private final ShapeReader<NbtTag> nbtReader = new ShapeReader<>(NbtInput.INSTANCE);

  private ComponentCodec(ComponentCatalog catalog) {
    this.catalog = catalog;
  }

  /**
   * Creates a codec.
   *
   * @param catalog the registered component types
   * @return the codec
   */
  public static ComponentCodec of(ComponentCatalog catalog) {
    return new ComponentCodec(catalog);
  }

  /**
   * Decodes one value from JSON.
   *
   * @param type the component type
   * @param node the JSON value
   * @return the value, or the failure
   */
  public Decoding<ComponentValue> valueFromJson(Identifier type, JsonNode node) {
    return Decoding.attempt(() -> jsonValue(type, node, "$"));
  }

  /**
   * Decodes one value from NBT.
   *
   * @param type the component type
   * @param tag the NBT value
   * @return the value, or the failure
   */
  public Decoding<ComponentValue> valueFromNbt(Identifier type, NbtTag tag) {
    return Decoding.attempt(() -> nbtValue(type, tag, "$"));
  }

  /**
   * Decodes the full component table of a prototype (IC-R2): an object of component identifiers to
   * values, with no key prefixed by an exclamation mark.
   *
   * @param table the JSON object
   * @return the components in file order, or the failure
   */
  public Decoding<ComponentMap> tableFromJson(JsonNode table) {
    return Decoding.attempt(
        () -> {
          SequencedMap<Identifier, ComponentValue> entries = new LinkedHashMap<>();
          jsonMembers(table)
              .forEach(
                  (key, node) -> {
                    String path = "$." + key;
                    if (key.startsWith(REMOVAL_PREFIX)) {
                      throw new FindingException(
                          FindingCode.INVALID_IDENTIFIER, path, "a removal is not part of a table");
                    }
                    Identifier id = componentId(key, path);
                    entries.put(id, jsonValue(id, node, path));
                  });
          return new ComponentMap(entries);
        });
  }

  /**
   * Decodes the {@code components} object of a stack in JSON (IC-R28): a key prefixed by an
   * exclamation mark removes a component of the prototype.
   *
   * @param components the JSON object
   * @return the patch, or the failure
   */
  public Decoding<ComponentPatch> patchFromJson(JsonNode components) {
    return Decoding.attempt(
        () -> {
          SequencedMap<Identifier, ComponentChange> changes = new LinkedHashMap<>();
          jsonMembers(components)
              .forEach(
                  (key, node) -> {
                    String path = "$." + key;
                    boolean removal = key.startsWith(REMOVAL_PREFIX);
                    Identifier id = componentId(removal ? key.substring(1) : key, path);
                    changes.put(
                        id, removal ? requireKnown(id, path) : set(jsonValue(id, node, path)));
                  });
          return new ComponentPatch(changes);
        });
  }

  /**
   * Decodes the {@code components} compound of a stack in NBT (IC-R24, IC-R27).
   *
   * @param components the compound; a key prefixed by an exclamation mark removes a component
   * @return the patch, or the failure
   */
  public Decoding<ComponentPatch> patchFromNbt(CompoundTag components) {
    return Decoding.attempt(
        () -> {
          SequencedMap<Identifier, ComponentChange> changes = new LinkedHashMap<>();
          components
              .entries()
              .forEach(
                  (key, tag) -> {
                    String path = "$." + key;
                    boolean removal = key.startsWith(REMOVAL_PREFIX);
                    Identifier id = componentId(removal ? key.substring(1) : key, path);
                    changes.put(
                        id, removal ? requireKnown(id, path) : set(nbtValue(id, tag, path)));
                  });
          return new ComponentPatch(changes);
        });
  }

  /**
   * Encodes a patch in NBT (IC-R27, provisional): a set is written under its identifier, a removal
   * under the identifier prefixed by an exclamation mark with an empty compound, in the order of
   * the patch.
   *
   * @param patch the patch
   * @return the compound, empty for an empty patch
   */
  public CompoundTag patchToNbt(ComponentPatch patch) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    patch
        .changes()
        .forEach(
            (id, change) -> {
              switch (change) {
                case ComponentChange.Set set -> entries.put(id.toString(), set.value().tag());
                case ComponentChange.Removed _ ->
                    entries.put(REMOVAL_PREFIX + id, CompoundTag.empty());
              }
            });
    return CompoundTag.of(entries);
  }

  /**
   * Encodes one value as compact JSON text that the JSON reader of {@code data} reads back to the
   * same value. Floats keep every bit (IC-R12, {@code -2.799999952316284}).
   *
   * @param type the component type, which gives the JSON shape of the value
   * @param value the value
   * @return the JSON text
   * @throws IllegalArgumentException when the value holds a number that JSON cannot write (NaN or
   *     an infinity, which only NBT input can produce)
   */
  public String valueToJson(Identifier type, ComponentValue value) {
    return TagJson.write(value.tag());
  }

  private static ComponentChange set(ComponentValue value) {
    return new ComponentChange.Set(value);
  }

  private ComponentChange requireKnown(Identifier id, String path) {
    knownType(id, path);
    return new ComponentChange.Removed();
  }

  private static SequencedMap<String, JsonNode> jsonMembers(JsonNode node) {
    if (node instanceof JsonObject object) {
      return object.members();
    }
    throw new FindingException(FindingCode.TYPE_MISMATCH, "$", "expected an object");
  }

  private static Identifier componentId(String text, String path) {
    Optional<Identifier> id = Identifier.tryParse(text);
    if (id.isEmpty() || text.isEmpty()) {
      throw new FindingException(
          FindingCode.INVALID_IDENTIFIER, path, "not a component identifier: " + text);
    }
    return id.get();
  }

  private void knownType(Identifier type, String path) {
    if (catalog.find(type).isEmpty()) {
      throw new FindingException(FindingCode.UNKNOWN_COMPONENT, path, "no component type " + type);
    }
  }

  private ComponentValue jsonValue(Identifier type, JsonNode node, String path) {
    knownType(type, path);
    return new ComponentValue(jsonReader.read(ShapeTable.of(type), node, path));
  }

  private ComponentValue nbtValue(Identifier type, NbtTag tag, String path) {
    knownType(type, path);
    return new ComponentValue(nbtReader.read(ShapeTable.of(type), tag, path));
  }
}

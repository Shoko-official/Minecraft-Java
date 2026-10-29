package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.item.Decoding;

/**
 * Decodes and encodes component values and tables (IC-R9 to IC-R13, IC-R27). The registry is the
 * acceptance list: an identifier it does not contain makes the whole table fail with {@code
 * UNKNOWN_COMPONENT} (IC-R10). The shape of each value follows IC-R12 for the 69 types seen in the
 * prototypes and IC-R9b (provisional) for the 53 others; a field outside the shape is {@code
 * UNKNOWN_FIELD} and a missing required one {@code MISSING_FIELD} (IC-R12, probe PI3). Floating
 * point numbers are read as double, then converted to the type of the field (IC-R13).
 */
public final class ComponentCodec {

  private ComponentCodec() {}

  /**
   * Creates a codec.
   *
   * @param catalog the registered component types
   * @return the codec
   */
  public static ComponentCodec of(ComponentCatalog catalog) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes one value from JSON.
   *
   * @param type the component type
   * @param node the JSON value
   * @return the value, or the failure
   */
  public Decoding<ComponentValue> valueFromJson(Identifier type, JsonNode node) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes one value from NBT.
   *
   * @param type the component type
   * @param tag the NBT value
   * @return the value, or the failure
   */
  public Decoding<ComponentValue> valueFromNbt(Identifier type, NbtTag tag) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes the full component table of a prototype (IC-R2): an object of component identifiers to
   * values, with no key prefixed by an exclamation mark.
   *
   * @param table the JSON object
   * @return the components in file order, or the failure
   */
  public Decoding<ComponentMap> tableFromJson(JsonNode table) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes the {@code components} object of a stack in JSON (IC-R28): a key prefixed by an
   * exclamation mark removes a component of the prototype.
   *
   * @param components the JSON object
   * @return the patch, or the failure
   */
  public Decoding<ComponentPatch> patchFromJson(JsonNode components) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes the {@code components} compound of a stack in NBT (IC-R24, IC-R27).
   *
   * @param components the compound; a key prefixed by an exclamation mark removes a component
   * @return the patch, or the failure
   */
  public Decoding<ComponentPatch> patchFromNbt(CompoundTag components) {
    return Skeleton.throwNotImplemented();
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
    return Skeleton.throwNotImplemented();
  }

  /**
   * Encodes one value as compact JSON text that the JSON reader of {@code data} reads back to the
   * same value. Floats keep every bit (IC-R12, {@code -2.799999952316284}).
   *
   * @param type the component type, which gives the JSON shape of the value
   * @param value the value
   * @return the JSON text
   */
  public String valueToJson(Identifier type, ComponentValue value) {
    return Skeleton.throwNotImplemented();
  }
}

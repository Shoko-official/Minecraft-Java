package dev.shoko.minecraft.loot.number;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.loader.ElementRegistryData;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.LootDecoding;
import java.util.List;

/**
 * Decodes number providers from their JSON form (LT-R50). A number is a constant, an object has a
 * {@code type} among the registered types, a string is a reference to a named provider. Findings
 * carry the JSON path.
 */
public final class NumberProviderCodec {

  private NumberProviderCodec() {}

  /**
   * Decodes one integer provider.
   *
   * @param node the JSON value
   * @param access the loaded registries and tags
   * @return the provider; {@code UNKNOWN_TYPE} for a type outside the 23, {@code TYPE_MISMATCH} or
   *     {@code MISSING_FIELD} for a wrong shape. A reference is not checked here: the loader checks
   *     it once every provider is read ({@code UNRESOLVED_REFERENCE})
   */
  public static LootDecoding<IntProvider> decodeInt(JsonNode node, RegistryAccess access) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes one float provider.
   *
   * @param node the JSON value
   * @param access the loaded registries and tags
   * @return the provider; failures as in {@link #decodeInt}, with the 28 float types
   */
  public static LootDecoding<FloatProvider> decodeFloat(JsonNode node, RegistryAccess access) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes the two registries of named providers.
   *
   * @param ints the {@code context_int_provider} elements
   * @param floats the {@code context_float_provider} elements
   * @param access the loaded registries and tags
   * @return the set; a provider that fails to decode is absent and a reference to it is {@code
   *     UNRESOLVED_REFERENCE}. Findings are listed per element, in identifier order
   */
  public static LootDecoding<NumberProviderSet> decodeSet(
      ElementRegistryData ints, ElementRegistryData floats, RegistryAccess access) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Lists the identifiers of the registered types of one family.
   *
   * @param floats {@code true} for the 28 float types, {@code false} for the 23 integer types
   * @return the type identifiers in registry order
   */
  public static List<Identifier> registeredTypes(boolean floats) {
    return Skeleton.throwNotImplemented();
  }
}

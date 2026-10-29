package dev.shoko.minecraft.loot.enchant;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.loader.ElementRegistryData;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.LootDecoding;

/**
 * Decodes the enchantment registries (EN-R1, EN-R2, EN-R34). Findings carry the JSON path inside
 * the file.
 */
public final class EnchantmentCodec {

  private EnchantmentCodec() {}

  /**
   * Decodes one definition.
   *
   * @param id the identifier of the file
   * @param node the file content
   * @param access the loaded registries and tags
   * @return the definition; {@code MISSING_FIELD} for a required field of EN-R2 that is absent,
   *     {@code VALUE_RANGE} for a non-positive {@code max_level}, {@code UNKNOWN_TYPE} for an
   *     effect component outside the 31
   */
  public static LootDecoding<EnchantmentDefinition> decode(
      Identifier id, JsonNode node, RegistryAccess access) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes one enchantment provider.
   *
   * @param node the file content
   * @param access the loaded registries and tags
   * @return the provider; {@code UNKNOWN_TYPE} for a type outside {@code single}, {@code by_cost}
   *     and {@code by_cost_with_difficulty}
   */
  public static LootDecoding<EnchantmentProvider> decodeProvider(
      JsonNode node, RegistryAccess access) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes both registries.
   *
   * @param enchantments the elements of the enchantment registry
   * @param providers the {@code enchantment_provider} elements
   * @param access the loaded registries and tags
   * @return the catalog; an element that fails is absent and listed in the findings, in identifier
   *     order
   */
  public static LootDecoding<EnchantmentCatalog> decodeCatalog(
      ElementRegistryData enchantments, ElementRegistryData providers, RegistryAccess access) {
    return Skeleton.throwNotImplemented();
  }
}

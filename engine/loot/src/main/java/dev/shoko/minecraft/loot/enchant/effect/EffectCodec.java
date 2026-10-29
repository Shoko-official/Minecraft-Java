package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.LootDecoding;

/**
 * Decodes the effect data of enchantments from JSON (EN-R11 to EN-R20, EN-R28). The 31 effect
 * components, 6 value effects and 6 level-based types are the registered ones; any other {@code
 * type} or component is {@code UNKNOWN_TYPE}.
 */
public final class EffectCodec {

  private EffectCodec() {}

  /**
   * Decodes a level-based value: a bare number is a constant, an object has a {@code type}. The
   * cost shape {@code {base, per_level_above_first}} without type is read as {@code linear} by
   * {@code EnchantmentCodec}, not here.
   *
   * @param node the JSON value
   * @return the value
   */
  public static LootDecoding<LevelBasedValue> levelBased(JsonNode node) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes a value effect.
   *
   * @param node the JSON object
   * @return the effect; an object with no {@code type} that is empty reads as {@code Empty}
   */
  public static LootDecoding<ValueEffect> valueEffect(JsonNode node) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes the {@code effects} table of an enchantment.
   *
   * @param node the JSON object, component to value
   * @param access the loaded registries and tags, for the conditions of the requirements
   * @return the components in file order
   */
  public static LootDecoding<EffectComponents> components(JsonNode node, RegistryAccess access) {
    return Skeleton.throwNotImplemented();
  }
}

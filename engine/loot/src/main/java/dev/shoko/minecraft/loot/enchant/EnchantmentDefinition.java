package dev.shoko.minecraft.loot.enchant;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.loot.enchant.effect.EffectComponents;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import java.util.List;

/**
 * A decoded enchantment (EN-R1, EN-R2, EN-R4).
 *
 * @param id the identifier, such as {@code minecraft:sharpness}
 * @param description the text component as read
 * @param maxLevel the highest level, 1 to 5 in the data
 * @param costs the weight, anvil cost and cost by level
 * @param slots the slot groups where the enchantment is active, at least one
 * @param items the items it accepts
 * @param exclusiveSet the enchantments it excludes, empty when the key is absent; the relation is
 *     made symmetric by {@link EnchantmentCatalog#areCompatible}
 * @param effects the effect components, empty for {@code fortune}
 */
public record EnchantmentDefinition(
    Identifier id,
    JsonNode description,
    int maxLevel,
    EnchantmentCosts costs,
    List<SlotGroup> slots,
    EnchantmentItems items,
    IdOrTagSet exclusiveSet,
    EffectComponents effects) {

  /**
   * Evaluates the minimum cost at a level (EN-R5): {@code base + per_level_above_first * (level -
   * 1)}, the form of {@code linear}.
   *
   * @param level 1 to {@code maxLevel}
   * @return the cost, truncated to an integer
   */
  public int minCost(int level) {
    return (int) costs.minCost().calculate(level);
  }

  /**
   * Evaluates the maximum cost at a level (EN-R5).
   *
   * @param level 1 to {@code maxLevel}
   * @return the cost, truncated to an integer
   */
  public int maxCost(int level) {
    return (int) costs.maxCost().calculate(level);
  }
}

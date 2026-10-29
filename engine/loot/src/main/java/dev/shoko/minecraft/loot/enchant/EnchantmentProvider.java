package dev.shoko.minecraft.loot.enchant;

import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.component.EnchantmentLevels;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;

/**
 * An enchantment provider (registry {@code enchantment_provider}, EN-R34): how a mob's equipment
 * gets its enchantments.
 */
public sealed interface EnchantmentProvider
    permits EnchantmentProvider.Single,
        EnchantmentProvider.ByCost,
        EnchantmentProvider.ByCostWithDifficulty {

  /**
   * Selects the enchantments for a stack (EN-R34, provisional): {@code single} gives its
   * enchantment at its level; the cost providers draw a cost uniformly in {@code [min_cost,
   * min_cost + max_cost_span]} and pass it to the selection of EN-R38 and EN-R39 over the
   * candidates of the tag. The dependence of {@code by_cost_with_difficulty} on the difficulty is
   * open.
   *
   * @param stack the stack to enchant
   * @param catalog the loaded enchantments
   * @param access the loaded registries and tags
   * @param random the source consumed by the draws
   * @param difficulty the effective difficulty, used by {@code by_cost_with_difficulty} only
   * @return the levels to set on the stack, empty when nothing is selected
   */
  default EnchantmentLevels select(
      ItemStack stack,
      EnchantmentCatalog catalog,
      RegistryAccess access,
      RandomSource random,
      double difficulty) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * {@code single}: one enchantment at one level.
   *
   * @param enchantment the enchantment identifier
   * @param level the level
   */
  record Single(Identifier enchantment, int level) implements EnchantmentProvider {}

  /**
   * {@code by_cost}: no vanilla usage.
   *
   * @param enchantments the candidates
   * @param minCost the lowest cost
   * @param maxCostSpan the span added to the lowest cost
   */
  record ByCost(IdOrTagSet enchantments, int minCost, int maxCostSpan)
      implements EnchantmentProvider {}

  /**
   * {@code by_cost_with_difficulty}: used by {@code mob_spawn_equipment}.
   *
   * @param enchantments the candidates
   * @param minCost the lowest cost
   * @param maxCostSpan the span added to the lowest cost
   */
  record ByCostWithDifficulty(IdOrTagSet enchantments, int minCost, int maxCostSpan)
      implements EnchantmentProvider {}
}

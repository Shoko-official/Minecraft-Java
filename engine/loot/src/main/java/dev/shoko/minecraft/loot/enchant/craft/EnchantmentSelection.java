package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import java.util.List;

/**
 * The selection of enchantments for a cost (EN-R35 to EN-R39), shared by the enchanting table and
 * the modifiers {@code enchant_with_levels} and {@code enchant_randomly}. Every draw is reported to
 * the observer of the context or, for the methods that take a bare source, not reported.
 */
public final class EnchantmentSelection {

  private EnchantmentSelection() {}

  /**
   * Lists the candidates for an effective cost (EN-R38): the enchantments of {@code options} that
   * the item accepts ({@code primary_items} when {@code primaryOnly}, {@code supported_items}
   * otherwise; a book accepts all), each at the highest level whose {@code min_cost <= cost <=
   * max_cost}; a candidate with no valid level is dropped. Draws nothing.
   *
   * @param stack the item to enchant
   * @param cost the effective cost
   * @param options the candidate enchantments
   * @param primaryOnly whether the table's {@code primary_items} restriction applies
   * @param data the loaded data
   * @return the candidates in registry order
   */
  public static List<EnchantmentInstance> possible(
      ItemStack stack, int cost, IdOrTagSet options, boolean primaryOnly, LootData data) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Chooses enchantments among candidates (EN-R39): a weighted draw by {@code weight}; the choice
   * is added and the incompatible candidates are removed; with candidates left, continue with the
   * probability {@code (cost + 1) / 50} (to be confirmed by probe PE1), halving the cost ({@code
   * floor(cost / 2)}) before the next weighted draw.
   *
   * @param candidates the result of {@link #possible}
   * @param cost the effective cost, halved at each continuation
   * @param random the source consumed
   * @param data the loaded data, for the incompatibilities
   * @return the chosen enchantments in order, empty when there is no candidate
   */
  public static List<EnchantmentInstance> choose(
      List<EnchantmentInstance> candidates, int cost, RandomSource random, LootData data) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Applies {@code enchant_with_levels} (EN-R36, LT-R45): the enchantability comes from the {@code
   * enchantable} component (a book has 1 and becomes {@code enchanted_book} with {@code
   * stored_enchantments}); the effective cost is derived from {@code levels} as in EN-R38, then
   * {@link #possible} and {@link #choose} run. A stack without {@code enchantable} is returned
   * unchanged.
   *
   * @param stack the item
   * @param levels the cost in levels, already drawn
   * @param options the candidates, unrestricted for all enchantments
   * @param context the context; its source is consumed
   * @return the enchanted stack
   */
  public static ItemStack enchantWithLevels(
      ItemStack stack, int levels, IdOrTagSet options, LootContext context) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Applies {@code enchant_randomly} (EN-R35): one enchantment among the compatible candidates of
   * {@code options}, equally likely whatever their weight, then a level from 1 to {@code max_level}
   * equally likely (provisional). A book becomes {@code enchanted_book}.
   *
   * @param stack the item
   * @param options the candidates
   * @param onlyCompatible whether candidates that the item or its present enchantments exclude are
   *     removed
   * @param context the context; its source is consumed
   * @return the enchanted stack, unchanged when there is no candidate
   */
  public static ItemStack enchantRandomly(
      ItemStack stack, IdOrTagSet options, boolean onlyCompatible, LootContext context) {
    return Skeleton.throwNotImplemented();
  }
}

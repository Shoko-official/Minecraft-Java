package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.item.component.EnchantmentLevels;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.enchant.EnchantmentCatalog;
import dev.shoko.minecraft.loot.enchant.EnchantmentTags;
import dev.shoko.minecraft.loot.enchant.ItemEnchantments;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The enchanting table (EN-R37 to EN-R40). With a fixed source the offers are the same (EN-R40,
 * PE9). How the player's {@code XpSeed} becomes a {@code RandomSource}, and how it is renewed, is
 * the caller's job (open point): the functions here only consume the source they are given.
 */
public final class EnchantingTable {

  private static final int MAX_SHELVES = 15;

  private EnchantingTable() {}

  /**
   * Draws the costs of the three lines (EN-R37): {@code base = 1 + R(7) + floor(min(15, b) / 2) +
   * R(min(15, b))} where {@code R(n)} is uniform in [0, n]. Three draws in this order, the first
   * two only when needed.
   *
   * @param shelves the number of bookshelves around the table, 0 to 15 counted (more are capped)
   * @param random the source consumed
   * @return the three costs
   */
  public static LineCosts lineCosts(int shelves, RandomSource random) {
    int counted = Math.max(0, Math.min(MAX_SHELVES, shelves));
    int base = 1 + random.nextInt(8) + counted / 2 + random.nextInt(counted + 1);
    return new LineCosts(Math.max(base / 3, 1), base * 2 / 3 + 1, Math.max(base, counted * 2));
  }

  /**
   * Computes the offer of one line (EN-R38, EN-R39): the effective cost is {@code round((cost + 1 +
   * R(k) + R(k)) * (1 + T))} with {@code k = floor(enchantability / 4)} and {@code T} triangular in
   * [-0.15, 0.15[; then the candidates of the {@code in_enchanting_table} tag are listed with the
   * {@code primary_items} restriction and chosen. For a book with more than one enchantment, one is
   * removed at random.
   *
   * @param stack the item, with {@code enchantable} and no enchantment
   * @param lineCost the cost of the line
   * @param lineIndex 0 for the top line, 1 for the middle, 2 for the bottom
   * @param random the source consumed
   * @param data the loaded data
   * @return the offer, with no enchantment when none is possible
   */
  public static EnchantOffer offer(
      ItemStack stack, int lineCost, int lineIndex, RandomSource random, LootData data) {
    EnchantmentLevels none = new EnchantmentLevels(new LinkedHashMap<>());
    int lapis = lineIndex + 1;
    if (!StackEnchants.isEnchantable(stack)) {
      return new EnchantOffer(lineCost, lapis, none);
    }
    int enchantability = StackEnchants.enchantability(stack);
    int cost = EnchantmentSelection.effectiveCost(lineCost, enchantability, random);
    IdOrTagSet options =
        new IdOrTagSet(
            EnchantmentCatalog.REGISTRY.name(),
            List.of(),
            List.of(EnchantmentTags.IN_ENCHANTING_TABLE),
            false);
    List<EnchantmentInstance> candidates =
        EnchantmentSelection.possible(stack, cost, options, true, data);
    List<EnchantmentInstance> chosen =
        new ArrayList<>(EnchantmentSelection.choose(candidates, cost, random, data));
    if (StackEnchants.isBook(stack) && chosen.size() > 1) {
      // EN-R45: one enchantment is removed, chosen uniformly (H-livre-tirage), after the loop.
      chosen.remove(random.nextInt(chosen.size()));
    }
    EnchantmentLevels levels = none;
    for (EnchantmentInstance instance : chosen) {
      levels = levels.with(instance.enchantment().id(), instance.level());
    }
    return new EnchantOffer(lineCost, lapis, levels);
  }

  /**
   * Computes the three offers for an item (EN-R40): {@link #lineCosts} then one {@link #offer} per
   * line.
   *
   * @param stack the item to offer
   * @param shelves the number of bookshelves
   * @param random the source consumed
   * @param data the loaded data
   * @return three offers, or an empty list when the item has no {@code enchantable} component or
   *     already has enchantments
   */
  public static List<EnchantOffer> offers(
      ItemStack stack, int shelves, RandomSource random, LootData data) {
    boolean enchanted = !ItemEnchantments.of(stack).levels().isEmpty();
    if (!StackEnchants.isEnchantable(stack) || enchanted) {
      return List.of();
    }
    LineCosts costs = lineCosts(shelves, random);
    return List.of(
        offer(stack, costs.top(), 0, random, data),
        offer(stack, costs.middle(), 1, random, data),
        offer(stack, costs.bottom(), 2, random, data));
  }
}

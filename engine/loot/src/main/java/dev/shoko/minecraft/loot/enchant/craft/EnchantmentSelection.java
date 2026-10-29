package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.component.EnchantmentLevels;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.EnchantmentCatalog;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import dev.shoko.minecraft.loot.random.DrawReason;
import dev.shoko.minecraft.loot.random.Draws;
import dev.shoko.minecraft.loot.random.Weights;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.OptionalInt;

/**
 * The selection of enchantments for a cost (EN-R35 to EN-R39), shared by the enchanting table and
 * the modifiers {@code enchant_with_levels} and {@code enchant_randomly}. Every draw is reported to
 * the observer of the context or, for the methods that take a bare source, not reported.
 */
public final class EnchantmentSelection {

  private static final int CONTINUATION_BOUND = 50;
  private static final float COST_SPREAD = 0.15f;
  private static final int ENCHANTABILITY_DIVISOR = 4;

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
    return possible(stack, cost, options, primaryOnly, data.enchantments(), data.registries());
  }

  /**
   * Lists the candidates as {@link #possible(ItemStack, int, IdOrTagSet, boolean, LootData)}, with
   * the enchantments and the registries given apart.
   *
   * @param stack the item to enchant
   * @param cost the effective cost
   * @param options the candidate enchantments
   * @param primaryOnly whether the table's {@code primary_items} restriction applies
   * @param catalog the loaded enchantments
   * @param access the loaded registries and tags
   * @return the candidates in registry order
   */
  public static List<EnchantmentInstance> possible(
      ItemStack stack,
      int cost,
      IdOrTagSet options,
      boolean primaryOnly,
      EnchantmentCatalog catalog,
      RegistryAccess access) {
    List<EnchantmentInstance> candidates = new ArrayList<>();
    for (EnchantmentDefinition definition : catalog.members(options, access)) {
      IdOrTagSet accepted =
          primaryOnly ? definition.items().primary() : definition.items().supported();
      if (!StackEnchants.isBook(stack) && !accepted.contains(stack.type().id(), access)) {
        continue;
      }
      for (int level = definition.maxLevel(); level >= 1; level--) {
        if (definition.minCost(level) <= cost && cost <= definition.maxCost(level)) {
          candidates.add(new EnchantmentInstance(definition, level));
          break;
        }
      }
    }
    return candidates;
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
    return choose(candidates, cost, random, data.enchantments(), data.registries());
  }

  /**
   * Chooses enchantments as {@link #choose(List, int, RandomSource, LootData)}, with the
   * enchantments and the registries given apart.
   *
   * @param candidates the result of {@link #possible}
   * @param cost the effective cost, halved at each continuation
   * @param random the source consumed
   * @param catalog the loaded enchantments, for the incompatibilities
   * @param access the loaded registries and tags
   * @return the chosen enchantments in order, empty when there is no candidate
   */
  public static List<EnchantmentInstance> choose(
      List<EnchantmentInstance> candidates,
      int cost,
      RandomSource random,
      EnchantmentCatalog catalog,
      RegistryAccess access) {
    List<EnchantmentInstance> remaining = new ArrayList<>(candidates);
    List<EnchantmentInstance> chosen = new ArrayList<>();
    int current = cost;
    while (!remaining.isEmpty()) {
      int[] weights = remaining.stream().mapToInt(i -> i.enchantment().costs().weight()).toArray();
      OptionalInt picked = Weights.pick(weights, random);
      if (picked.isEmpty()) {
        break;
      }
      EnchantmentInstance pick = remaining.get(picked.getAsInt());
      chosen.add(pick);
      Identifier id = pick.enchantment().id();
      remaining.removeIf(other -> !catalog.areCompatible(id, other.enchantment().id(), access));
      remaining.remove(pick);
      if (remaining.isEmpty() || random.nextInt(CONTINUATION_BOUND) >= current + 1) {
        break;
      }
      current /= 2;
    }
    return chosen;
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
    if (!StackEnchants.isEnchantable(stack)) {
      return stack;
    }
    EnchantmentCatalog catalog = context.data().enchantments();
    RegistryAccess access = context.data().registries();
    EnchantmentLevels chosen =
        Draws.observe(
            context,
            DrawReason.ENCHANTMENT_SELECTION,
            "enchant_with_levels",
            () -> select(stack, levels, options, context.random(), catalog, access));
    if (chosen.levels().isEmpty()) {
      return stack;
    }
    EnchantmentLevels merged = StackEnchants.levels(stack);
    for (var entry : chosen.levels().entrySet()) {
      merged = merged.with(entry.getKey(), entry.getValue());
    }
    return StackEnchants.withLevels(stack, merged, context.data().items());
  }

  /**
   * Selects the enchantments of an item for a cost of levels: the effective cost of EN-R38, the
   * candidates of the item, and the weighted choice of EN-R39.
   *
   * @param stack the item
   * @param levels the cost in levels
   * @param options the candidate enchantments
   * @param random the source consumed
   * @param catalog the loaded enchantments
   * @param access the loaded registries and tags
   * @return the chosen levels in order, empty when none is possible
   */
  public static EnchantmentLevels select(
      ItemStack stack,
      int levels,
      IdOrTagSet options,
      RandomSource random,
      EnchantmentCatalog catalog,
      RegistryAccess access) {
    int enchantability = StackEnchants.enchantability(stack);
    int cost = effectiveCost(levels, enchantability, random);
    List<EnchantmentInstance> candidates = possible(stack, cost, options, true, catalog, access);
    EnchantmentLevels result = new EnchantmentLevels(new LinkedHashMap<>());
    for (EnchantmentInstance instance : choose(candidates, cost, random, catalog, access)) {
      result = result.with(instance.enchantment().id(), instance.level());
    }
    return result;
  }

  /** The effective cost of EN-R38: {@code round((c + 1 + R(k) + R(k)) * (1 + T))}, at least 1. */
  static int effectiveCost(int levels, int enchantability, RandomSource random) {
    int spread = enchantability / ENCHANTABILITY_DIVISOR + 1;
    long base = (long) levels + 1 + random.nextInt(spread) + random.nextInt(spread);
    float tilt = (random.nextFloat() + random.nextFloat() - 1.0f) * COST_SPREAD;
    return Math.max(1, Math.round(base * (1.0f + tilt)));
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
    EnchantmentCatalog catalog = context.data().enchantments();
    RegistryAccess access = context.data().registries();
    EnchantmentLevels present = StackEnchants.levels(stack);
    List<EnchantmentDefinition> pool = new ArrayList<>();
    for (EnchantmentDefinition definition : catalog.members(options, access)) {
      if (!onlyCompatible || accepts(definition, stack, present, catalog, access)) {
        pool.add(definition);
      }
    }
    if (pool.isEmpty()) {
      return stack;
    }
    EnchantmentInstance chosen =
        Draws.observe(
            context,
            DrawReason.ENCHANTMENT_SELECTION,
            "enchant_randomly",
            () -> {
              RandomSource random = context.random();
              // The full 32-bit draw: a bounded draw on a power of two reads the top bit of the
              // generator,
              // which is strongly correlated between neighbouring seeds.
              EnchantmentDefinition definition =
                  pool.get(Math.floorMod(random.nextInt(), pool.size()));
              return new EnchantmentInstance(
                  definition, random.nextIntBetweenInclusive(1, definition.maxLevel()));
            });
    EnchantmentLevels merged = present.with(chosen.enchantment().id(), chosen.level());
    return StackEnchants.withLevels(stack, merged, context.data().items());
  }

  private static boolean accepts(
      EnchantmentDefinition definition,
      ItemStack stack,
      EnchantmentLevels present,
      EnchantmentCatalog catalog,
      RegistryAccess access) {
    if (!StackEnchants.isBook(stack)
        && !definition.items().supported().contains(stack.type().id(), access)) {
      return false;
    }
    for (Identifier other : present.levels().keySet()) {
      if (!catalog.areCompatible(definition.id(), other, access)) {
        return false;
      }
    }
    return true;
  }
}

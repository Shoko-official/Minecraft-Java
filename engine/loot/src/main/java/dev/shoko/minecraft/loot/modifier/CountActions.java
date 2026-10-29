package dev.shoko.minecraft.loot.modifier;

import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.LootBudgetException;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.ItemEnchantments;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import java.util.OptionalDouble;

/** The modifiers that change the quantity of a stack (LT-R39 to LT-R43). */
final class CountActions {

  /** Bound on the draws of one modifier, far above what any stack asks for. */
  private static final int MAX_DRAWS = 100_000;

  /** Bound on the units one explosion_decay draws for; a larger pile fails the draw (LT-R74f). */
  private static final int MAX_DECAY_UNITS = 10_000_000;

  private CountActions() {}

  static ItemStack setCount(ModifierAction.SetCount action, ItemStack stack, LootContext context) {
    int value = action.count().sample(context);
    long count = action.add() ? (long) stack.count() + value : value;
    return stack.withCount(clamp(count));
  }

  static ItemStack limitCount(
      ModifierAction.LimitCount action, ItemStack stack, LootContext context) {
    int min = action.min().sample(context);
    int max = action.max().sample(context);
    return stack.withCount(Math.min(Math.max(stack.count(), min), max));
  }

  /**
   * Each unit is kept when its draw is below 1 / radius, one draw per unit (LT-R41, H-decay-b: the
   * same sense as {@code survives_explosion}).
   */
  static ItemStack explosionDecay(ItemStack stack, LootContext context) {
    OptionalDouble radius = context.explosionRadius();
    if (radius.isEmpty() || radius.getAsDouble() <= 0) {
      return stack;
    }
    if (stack.count() > MAX_DECAY_UNITS) {
      throw new LootBudgetException("explosion_decay on " + stack.count() + " units");
    }
    float probability = 1.0f / (float) radius.getAsDouble();
    RandomSource random = context.random();
    int kept = 0;
    for (int unit = 0; unit < stack.count(); unit++) {
      if (random.nextFloat() < probability) {
        kept++;
      }
    }
    return stack.withCount(kept);
  }

  static ItemStack applyBonus(
      ModifierAction.ApplyBonus action, ItemStack stack, LootContext context) {
    int level =
        context.tool().map(tool -> ItemEnchantments.levelOf(tool, action.enchantment())).orElse(0);
    RandomSource random = context.random();
    long count = stack.count();
    switch (action.formula()) {
      case BonusFormula.OreDrops ore ->
          // H-bonus0-a (LT-R42): the formula is evaluated at level 0 too, one bounded draw.
          count *= Math.max(1, random.nextInt(level + 2));
      case BonusFormula.UniformBonusCount uniform ->
          count += random.nextInt(Math.max(1, level * uniform.bonusMultiplier() + 1));
      case BonusFormula.BinomialWithBonusCount binomial -> {
        long trials = (long) level + binomial.extra();
        if (trials > MAX_DRAWS) {
          throw new LootBudgetException("apply_bonus binomial with " + trials + " trials");
        }
        for (int i = 0; i < trials; i++) {
          if (random.nextFloat() < binomial.probability()) {
            count++;
          }
        }
      }
    }
    return stack.withCount(clamp(count));
  }

  static ItemStack enchantedCountIncrease(
      ModifierAction.EnchantedCountIncrease action, ItemStack stack, LootContext context) {
    int level =
        context
            .entity(EntityTarget.ATTACKER)
            .map(attacker -> attacker.equipment(EquipmentSlot.MAINHAND))
            .map(weapon -> ItemEnchantments.levelOf(weapon, action.enchantment()))
            .orElse(0);
    if (level <= 0) {
      return stack;
    }
    float increase = action.count().sample(context) * level;
    long count = (long) stack.count() + (long) Math.floor(increase);
    if (action.limit() > 0) {
      count = Math.min(count, action.limit());
    }
    return stack.withCount(clamp(count));
  }

  private static int clamp(long count) {
    return (int) Math.max(0, Math.min(Integer.MAX_VALUE, count));
  }
}

package dev.shoko.minecraft.loot.modifier;

import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.ItemEnchantments;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import java.util.OptionalDouble;

/** The modifiers that change the quantity of a stack (LT-R39 to LT-R43). */
final class CountActions {

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

  /** Each unit is lost with the probability 1 / radius, one draw per unit (LT-R41). */
  static ItemStack explosionDecay(ItemStack stack, LootContext context) {
    OptionalDouble radius = context.explosionRadius();
    if (radius.isEmpty() || radius.getAsDouble() <= 0) {
      return stack;
    }
    float probability = 1.0f / (float) radius.getAsDouble();
    RandomSource random = context.random();
    int kept = 0;
    for (int unit = 0; unit < stack.count(); unit++) {
      if (!(random.nextFloat() < probability)) {
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
      case BonusFormula.OreDrops ore -> {
        if (level > 0) {
          count *= Math.max(1, random.nextInt(level + 2));
        }
      }
      case BonusFormula.UniformBonusCount uniform ->
          count += random.nextInt(Math.max(1, level * uniform.bonusMultiplier() + 1));
      case BonusFormula.BinomialWithBonusCount binomial -> {
        int trials = level + binomial.extra();
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

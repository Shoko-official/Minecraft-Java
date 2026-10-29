package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.context.NodeBudget;

/** Evaluation of {@link LevelBasedValue}s and {@link ValueEffect}s (EN-R18, EN-R21). */
final class LevelMath {

  private LevelMath() {}

  static float calculate(LevelBasedValue value, int level, NodeBudget budget) {
    budget.node();
    return switch (value) {
      case LevelBasedValue.Constant constant -> constant.value();
      case LevelBasedValue.Linear linear ->
          linear.base() + linear.perLevelAboveFirst() * (level - 1);
      case LevelBasedValue.LevelsSquared squared -> (float) level * level + squared.added();
      case LevelBasedValue.Fraction fraction ->
          calculate(fraction.numerator(), level, budget)
              / calculate(fraction.denominator(), level, budget);
      case LevelBasedValue.Clamped clamped ->
          Math.max(
              clamped.min(), Math.min(clamped.max(), calculate(clamped.value(), level, budget)));
      case LevelBasedValue.Lookup lookup ->
          level - 1 >= 0 && level - 1 < lookup.values().size()
              ? lookup.values().get(level - 1)
              : calculate(lookup.fallback(), level, budget);
      case LevelBasedValue.Exponent exponent ->
          (float)
              Math.pow(
                  calculate(exponent.base(), level, budget),
                  calculate(exponent.power(), level, budget));
    };
  }

  static float apply(ValueEffect effect, float current, int level, LootContext context) {
    return switch (effect) {
      case ValueEffect.Add add -> current + add.value().calculate(level, context.budget());
      case ValueEffect.Set set -> set.value().calculate(level, context.budget());
      case ValueEffect.Multiply multiply ->
          current * multiply.factor().calculate(level, context.budget());
      case ValueEffect.Exponential exponential ->
          current * exponential.factor().calculate(level, context.budget());
      case ValueEffect.RemoveBinomial remove ->
          context.random().nextFloat() < remove.chance().calculate(level, context.budget())
              ? current - 1
              : current;
      case ValueEffect.AllOf all -> {
        float value = current;
        for (ValueEffect inner : all.effects()) {
          value = apply(inner, value, level, context);
        }
        yield value;
      }
      case ValueEffect.Empty empty -> current;
    };
  }
}

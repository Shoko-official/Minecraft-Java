package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.loot.context.LootContext;

/** Evaluation of {@link LevelBasedValue}s and {@link ValueEffect}s (EN-R18, EN-R21). */
final class LevelMath {

  private LevelMath() {}

  static float calculate(LevelBasedValue value, int level) {
    return switch (value) {
      case LevelBasedValue.Constant constant -> constant.value();
      case LevelBasedValue.Linear linear ->
          linear.base() + linear.perLevelAboveFirst() * (level - 1);
      case LevelBasedValue.LevelsSquared squared -> (float) level * level + squared.added();
      case LevelBasedValue.Fraction fraction ->
          fraction.numerator().calculate(level) / fraction.denominator().calculate(level);
      case LevelBasedValue.Clamped clamped ->
          Math.max(clamped.min(), Math.min(clamped.max(), clamped.value().calculate(level)));
      case LevelBasedValue.Lookup lookup ->
          level - 1 >= 0 && level - 1 < lookup.values().size()
              ? lookup.values().get(level - 1)
              : lookup.fallback().calculate(level);
      case LevelBasedValue.Exponent exponent ->
          (float) Math.pow(exponent.base().calculate(level), exponent.power().calculate(level));
    };
  }

  static float apply(ValueEffect effect, float current, int level, LootContext context) {
    return switch (effect) {
      case ValueEffect.Add add -> current + add.value().calculate(level);
      case ValueEffect.Set set -> set.value().calculate(level);
      case ValueEffect.Multiply multiply -> current * multiply.factor().calculate(level);
      case ValueEffect.Exponential exponential -> current * exponential.factor().calculate(level);
      case ValueEffect.RemoveBinomial remove ->
          context.random().nextFloat() < remove.chance().calculate(level) ? current - 1 : current;
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

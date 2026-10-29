package dev.shoko.minecraft.loot.enchant.effect;

import java.util.List;

/**
 * A value that depends on the level of an enchantment (registry {@code
 * enchantment_level_based_value_type}, EN-R20). A bare number in the data is a {@link Constant}.
 * The costs {@code min_cost} and {@code max_cost} have the shape of {@link Linear} without {@code
 * type} (EN-R25).
 */
public sealed interface LevelBasedValue
    permits LevelBasedValue.Constant,
        LevelBasedValue.Linear,
        LevelBasedValue.LevelsSquared,
        LevelBasedValue.Fraction,
        LevelBasedValue.Clamped,
        LevelBasedValue.Lookup,
        LevelBasedValue.Exponent {

  /**
   * Evaluates the value at a level (EN-R21, provisional): {@code linear} = {@code base +
   * per_level_above_first * (level - 1)}; {@code levels_squared} = {@code level * level + added};
   * {@code fraction} = {@code numerator(level) / denominator(level)}, float division; {@code
   * clamped} bounds {@code value(level)} to {@code [min, max]}; {@code lookup} = {@code
   * values[level - 1]} when in range, else {@code fallback(level)}; {@code exponent} = {@code
   * base(level)} to the power {@code power(level)}. Sums and products are made in 32-bit floats.
   *
   * @param level the level, 1 to 255
   * @return the value
   */
  default float calculate(int level) {
    return LevelMath.calculate(this, level);
  }

  /**
   * A constant.
   *
   * @param value the value
   */
  record Constant(float value) implements LevelBasedValue {}

  /**
   * {@code linear}.
   *
   * @param base the value at level 1
   * @param perLevelAboveFirst the increase per level above the first
   */
  record Linear(float base, float perLevelAboveFirst) implements LevelBasedValue {}

  /**
   * {@code levels_squared}.
   *
   * @param added the value added to the square of the level
   */
  record LevelsSquared(float added) implements LevelBasedValue {}

  /**
   * {@code fraction}.
   *
   * @param numerator the numerator
   * @param denominator the denominator
   */
  record Fraction(LevelBasedValue numerator, LevelBasedValue denominator)
      implements LevelBasedValue {}

  /**
   * {@code clamped}.
   *
   * @param value the value to bound
   * @param min the lower bound
   * @param max the upper bound
   */
  record Clamped(LevelBasedValue value, float min, float max) implements LevelBasedValue {}

  /**
   * {@code lookup}: the first values by level, then a fallback (EN-R20; {@code wind_burst}).
   *
   * @param values the values for levels 1, 2, 3...
   * @param fallback the value for the levels beyond the list
   */
  record Lookup(List<Float> values, LevelBasedValue fallback) implements LevelBasedValue {}

  /**
   * {@code exponent}: no vanilla usage, the semantics is uncertain (EN spec, open questions).
   *
   * @param base the base
   * @param power the power
   */
  record Exponent(LevelBasedValue base, LevelBasedValue power) implements LevelBasedValue {}
}

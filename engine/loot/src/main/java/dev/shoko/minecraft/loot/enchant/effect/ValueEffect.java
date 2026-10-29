package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.loot.context.LootContext;
import java.util.List;

/**
 * A value effect (registry {@code enchantment_value_effect_type}, EN-R17): a transformation of a
 * number, such as a damage or a durability loss.
 */
public sealed interface ValueEffect
    permits ValueEffect.Add,
        ValueEffect.Set,
        ValueEffect.Multiply,
        ValueEffect.RemoveBinomial,
        ValueEffect.AllOf,
        ValueEffect.Exponential,
        ValueEffect.Empty {

  /**
   * Applies the effect to a value (EN-R18, provisional): {@code add} adds {@code value(level)};
   * {@code set} replaces; {@code multiply} multiplies by {@code factor(level)}; {@code
   * remove_binomial} draws one float and removes 1 when it is below {@code chance(level)}; {@code
   * all_of} applies its effects in series; {@code Empty} returns the value unchanged. Rounding and
   * clamping belong to the consumer of the value.
   *
   * @param current the value so far
   * @param level the level of the enchantment
   * @param context the context; its source is consumed by {@code remove_binomial}
   * @return the new value
   */
  default float apply(float current, int level, LootContext context) {
    return LevelMath.apply(this, current, level, context);
  }

  /**
   * {@code add}.
   *
   * @param value the amount added
   */
  record Add(LevelBasedValue value) implements ValueEffect {}

  /**
   * {@code set}.
   *
   * @param value the new value
   */
  record Set(LevelBasedValue value) implements ValueEffect {}

  /**
   * {@code multiply}.
   *
   * @param factor the factor
   */
  record Multiply(LevelBasedValue factor) implements ValueEffect {}

  /**
   * {@code remove_binomial}.
   *
   * @param chance the probability of removing 1
   */
  record RemoveBinomial(LevelBasedValue chance) implements ValueEffect {}

  /**
   * {@code all_of}.
   *
   * @param effects the effects in series
   */
  record AllOf(List<ValueEffect> effects) implements ValueEffect {}

  /**
   * {@code exponential}: no vanilla usage, uncertain semantics.
   *
   * @param factor the factor
   */
  record Exponential(LevelBasedValue factor) implements ValueEffect {}

  /** The empty object {@code {}} used as the effect of {@code damage_immunity} (EN-R17). */
  record Empty() implements ValueEffect {}
}

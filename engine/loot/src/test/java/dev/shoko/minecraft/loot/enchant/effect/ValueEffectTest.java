package dev.shoko.minecraft.loot.enchant.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: EN-R17 (value effects), EN-R18 (semantics, provisional); the value of departure of each
// component (EN-R19) and the rounding belong to the consumer. exponential is not tested
// (uncertain).
class ValueEffectTest {

  private static LootContext context(ScriptedRandom random) {
    return Ctx.context(random);
  }

  private static LootContext plain() {
    return context(ScriptedRandom.of());
  }

  private static LevelBasedValue.Constant c(float value) {
    return new LevelBasedValue.Constant(value);
  }

  @Test
  void provisionalAddAddsTheValueOfTheLevel() {
    // spec: EN-R18 (add ajoute value(niveau))
    ValueEffect add = new ValueEffect.Add(new LevelBasedValue.Linear(1.0f, 0.5f));
    assertThat(add.apply(10f, 1, plain())).isCloseTo(11f, within(1e-5f));
    assertThat(add.apply(10f, 3, plain())).isCloseTo(12f, within(1e-5f));
  }

  @Test
  void provisionalSetReplacesTheCurrentValue() {
    // spec: EN-R18
    assertThat(new ValueEffect.Set(c(5f)).apply(100f, 1, plain())).isEqualTo(5f);
  }

  @Test
  void provisionalMultiplyMultipliesByTheFactorOfTheLevel() {
    // spec: EN-R18, EN-R44b (repair_with_xp multiply 2.0)
    assertThat(new ValueEffect.Multiply(c(2f)).apply(1f, 1, plain())).isEqualTo(2f);
    ValueEffect perLevel = new ValueEffect.Multiply(new LevelBasedValue.Linear(1f, 1f));
    assertThat(perLevel.apply(3f, 4, plain())).isCloseTo(12f, within(1e-5f));
  }

  @Test
  void provisionalAllOfAppliesItsEffectsInSeries() {
    // spec: EN-R17, EN-R18 (all_of execute ses sous-effets en serie)
    ValueEffect addThenMultiply =
        new ValueEffect.AllOf(List.of(new ValueEffect.Add(c(2f)), new ValueEffect.Multiply(c(3f))));
    ValueEffect multiplyThenAdd =
        new ValueEffect.AllOf(List.of(new ValueEffect.Multiply(c(3f)), new ValueEffect.Add(c(2f))));
    assertThat(addThenMultiply.apply(4f, 1, plain())).isEqualTo(18f);
    assertThat(multiplyThenAdd.apply(4f, 1, plain())).isEqualTo(14f);
  }

  @Test
  void provisionalAnEmptyEffectReturnsTheValueUnchanged() {
    // spec: ValueEffect.Empty contract, EN-R17 (damage_immunity)
    assertThat(new ValueEffect.Empty().apply(7.5f, 3, plain())).isEqualTo(7.5f);
  }

  @Test
  void provisionalRemoveBinomialRemovesOneWhenTheDrawIsBelowTheChance() {
    // spec: EN-R18 (un essai de probabilite chance(niveau), retire 1 en cas de succes)
    ValueEffect remove = new ValueEffect.RemoveBinomial(c(0.5f));
    ScriptedRandom success = ScriptedRandom.of(0.2);
    ScriptedRandom failure = ScriptedRandom.of(0.8);
    assertThat(remove.apply(5f, 1, context(success))).isEqualTo(4f);
    assertThat(remove.apply(5f, 1, context(failure))).isEqualTo(5f);
    assertThat(success.steps()).isEqualTo(1);
    assertThat(failure.steps()).isEqualTo(1);
  }

  @Test
  void provisionalRemoveBinomialDrawEqualToTheChanceIsNotASuccess() {
    // spec: EN-R18, LT-R30 (strictly below)
    ScriptedRandom random = ScriptedRandom.of(0.5);
    assertThat(new ValueEffect.RemoveBinomial(c(0.5f)).apply(5f, 1, context(random))).isEqualTo(5f);
  }

  @Test
  void provisionalRemoveBinomialReadsTheChanceAtTheLevel() {
    // spec: EN-R18, EN-R23 (unbreaking: L / (L + 1))
    LevelBasedValue chance =
        new LevelBasedValue.Fraction(
            new LevelBasedValue.Linear(1f, 1f), new LevelBasedValue.Linear(2f, 1f));
    ValueEffect remove = new ValueEffect.RemoveBinomial(chance);
    assertThat(remove.apply(5f, 3, context(ScriptedRandom.of(0.7)))).isEqualTo(4f);
    assertThat(remove.apply(5f, 3, context(ScriptedRandom.of(0.8)))).isEqualTo(5f);
    assertThat(remove.apply(5f, 1, context(ScriptedRandom.of(0.6)))).isEqualTo(5f);
  }

  @Test
  void provisionalRemoveBinomialWithChanceOneAlwaysRemovesAndZeroNeverDoes() {
    // spec: EN-R18
    assertThat(
            new ValueEffect.RemoveBinomial(c(1f))
                .apply(2f, 1, context(ScriptedRandom.of(0.999999))))
        .isEqualTo(1f);
    assertThat(new ValueEffect.RemoveBinomial(c(0f)).apply(2f, 1, context(ScriptedRandom.of(0.0))))
        .isEqualTo(2f);
  }

  @Test
  void provisionalAnEffectOtherThanRemoveBinomialDrawsNothing() {
    // spec: EN-R18
    ScriptedRandom random = ScriptedRandom.of();
    new ValueEffect.Add(c(1f)).apply(1f, 1, context(random));
    new ValueEffect.Set(c(1f)).apply(1f, 1, context(random));
    new ValueEffect.Multiply(c(2f)).apply(1f, 1, context(random));
    assertThat(random.steps()).isZero();
  }
}

package dev.shoko.minecraft.loot.enchant.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

// spec: EN-R20 (types), EN-R21 (evaluation at level L, provisional), EN-R5 (costs), EN-R22 to
// EN-R24 (vectors V-E5, V-E6, V-E15, V-E16, V-E17). The values are S until probe PE10.
class LevelBasedValueTest {

  private static final float EPS = 1e-5f;

  private static LevelBasedValue.Constant c(float value) {
    return new LevelBasedValue.Constant(value);
  }

  private static LevelBasedValue.Linear linear(float base, float perLevel) {
    return new LevelBasedValue.Linear(base, perLevel);
  }

  private static void assertValues(LevelBasedValue value, float... expected) {
    for (int i = 0; i < expected.length; i++) {
      assertThat(value.calculate(i + 1)).as("level %d", i + 1).isCloseTo(expected[i], within(EPS));
    }
  }

  @Test
  void provisionalAConstantIgnoresTheLevel() {
    // spec: EN-R20 (a bare number is a constant), EN-R21
    assertValues(c(0.15f), 0.15f, 0.15f, 0.15f);
    assertThat(c(2f).calculate(255)).isEqualTo(2f);
  }

  @Test
  void provisionalLinearAddsThePerLevelIncreaseAboveTheFirstLevel() {
    // spec: EN-R21 (linear), V-E15 (sharpness damage 1, 1.5, 2, 2.5, 3)
    assertValues(linear(1.0f, 0.5f), 1f, 1.5f, 2f, 2.5f, 3f);
  }

  @Test
  void provisionalLinearGivesTheProtectionOfVectorE16() {
    // spec: V-E16 (protection 1, 2, 3, 4)
    assertValues(linear(1.0f, 1.0f), 1f, 2f, 3f, 4f);
  }

  @Test
  void provisionalLinearGivesTheCostsOfSharpness() {
    // spec: EN-R5, V-E15 (min 1, 12, 23, 34, 45; max 21, 32, 43, 54, 65)
    assertValues(linear(1f, 11f), 1f, 12f, 23f, 34f, 45f);
    assertValues(linear(21f, 11f), 21f, 32f, 43f, 54f, 65f);
  }

  @Test
  void provisionalLinearGivesTheCostsOfFortuneAndMending() {
    // spec: EN-R5 (fortune min 15, 24, 33 and max 65, 74, 83; mending level 1: 25 and 75)
    assertValues(linear(15f, 9f), 15f, 24f, 33f);
    assertValues(linear(65f, 9f), 65f, 74f, 83f);
    assertThat(linear(25f, 25f).calculate(1)).isEqualTo(25f);
    assertThat(linear(75f, 25f).calculate(1)).isEqualTo(75f);
  }

  @Test
  void provisionalLevelsSquaredAddsToTheSquareOfTheLevel() {
    // spec: EN-R21, EN-R24, V-E6 (efficiency 2, 5, 10, 17, 26)
    assertValues(new LevelBasedValue.LevelsSquared(1.0f), 2f, 5f, 10f, 17f, 26f);
  }

  @Test
  void provisionalFractionDividesTwoLevelBasedValuesInFloats() {
    // spec: EN-R21, EN-R24 (sweeping edge: L / (L + 1))
    LevelBasedValue sweeping = new LevelBasedValue.Fraction(linear(1f, 1f), linear(2f, 1f));
    assertValues(sweeping, 0.5f, 2f / 3f, 0.75f);
  }

  @Test
  void provisionalUnbreakingProbabilitiesOfAnArmorPiece() {
    // spec: EN-R23, V-E5 ((2 + 2(L-1)) / (10 + 5(L-1)): 0.2, 0.2667, 0.3)
    LevelBasedValue armor = new LevelBasedValue.Fraction(linear(2f, 2f), linear(10f, 5f));
    assertValues(armor, 0.2f, 4f / 15f, 0.3f);
  }

  @Test
  void provisionalUnbreakingProbabilitiesOfOtherItems() {
    // spec: EN-R23, V-E5 (L / (L + 1): 0.5, 0.6667, 0.75)
    LevelBasedValue other = new LevelBasedValue.Fraction(linear(1f, 1f), linear(2f, 1f));
    assertValues(other, 0.5f, 2f / 3f, 0.75f);
  }

  @Test
  void provisionalLookupReadsTheListThenTheFallback() {
    // spec: EN-R20, EN-R21, V-E17 (wind_burst: 1.2, 1.75, 2.2 then 1.5 + 0.35 x 3 = 2.55)
    LevelBasedValue windBurst =
        new LevelBasedValue.Lookup(List.of(1.2f, 1.75f, 2.2f), linear(1.5f, 0.35f));
    assertValues(windBurst, 1.2f, 1.75f, 2.2f, 2.55f);
  }

  @Test
  void provisionalLookupBeyondTheListFollowsTheFallbackLinearly() {
    // spec: EN-R20, cas limite
    LevelBasedValue windBurst =
        new LevelBasedValue.Lookup(List.of(1.2f, 1.75f, 2.2f), linear(1.5f, 0.35f));
    assertThat(windBurst.calculate(5)).isCloseTo(1.5f + 0.35f * 4f, within(EPS));
  }

  @Test
  void provisionalClampedBoundsTheInnerValue() {
    // spec: EN-R21 (clamped)
    LevelBasedValue clamped = new LevelBasedValue.Clamped(linear(0f, 1f), 2f, 4f);
    assertThat(clamped.calculate(1)).isEqualTo(2f);
    assertThat(clamped.calculate(4)).isEqualTo(3f);
    assertThat(clamped.calculate(9)).isEqualTo(4f);
  }

  @Test
  void provisionalExponentRaisesTheBaseToThePower() {
    // spec: EN-R21 (exponent: base(level) puissance power(level))
    LevelBasedValue exponent = new LevelBasedValue.Exponent(c(2f), c(3f));
    assertThat(exponent.calculate(1)).isCloseTo(8f, within(EPS));
  }

  @Test
  void provisionalTheTopLevelDoesNotOverflow() {
    // spec: EN-R21 (level 1 to 255)
    assertThat(linear(1f, 11f).calculate(255)).isCloseTo(1f + 11f * 254f, within(EPS));
  }
}

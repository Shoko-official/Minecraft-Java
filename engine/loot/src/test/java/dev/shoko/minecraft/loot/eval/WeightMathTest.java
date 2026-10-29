package dev.shoko.minecraft.loot.eval;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// spec: LT-R25 (effective weight = weight + floor(quality x luck), bounded below by 0), LT-R27
// (fishing values). The numeric precision of the product is open (PL1, PL2) and is not tested.
class WeightMathTest {

  @Test
  void provisionalWithoutLuckTheWeightIsTheWeight() {
    // spec: LT-R25
    assertThat(WeightMath.effectiveWeight(10, -2, 0f)).isEqualTo(10);
    assertThat(WeightMath.effectiveWeight(85, -1, 0f)).isEqualTo(85);
  }

  @Test
  void provisionalWithoutQualityLuckChangesNothing() {
    // spec: LT-R25
    assertThat(WeightMath.effectiveWeight(7, 0, 5f)).isEqualTo(7);
    assertThat(WeightMath.effectiveWeight(7, 0, -5f)).isEqualTo(7);
  }

  @Test
  void provisionalFishingWeightsOfOneLuck() {
    // spec: LT-R27 (L=1: 8, 7, 84)
    assertThat(WeightMath.effectiveWeight(10, -2, 1f)).isEqualTo(8);
    assertThat(WeightMath.effectiveWeight(5, 2, 1f)).isEqualTo(7);
    assertThat(WeightMath.effectiveWeight(85, -1, 1f)).isEqualTo(84);
  }

  @Test
  void provisionalFishingWeightsOfTwoLuck() {
    // spec: LT-R27 (L=2: 6, 9, 83)
    assertThat(WeightMath.effectiveWeight(10, -2, 2f)).isEqualTo(6);
    assertThat(WeightMath.effectiveWeight(5, 2, 2f)).isEqualTo(9);
    assertThat(WeightMath.effectiveWeight(85, -1, 2f)).isEqualTo(83);
  }

  @Test
  void provisionalFishingWeightsOfThreeLuck() {
    // spec: LT-R27 (L=3: 4, 11, 82)
    assertThat(WeightMath.effectiveWeight(10, -2, 3f)).isEqualTo(4);
    assertThat(WeightMath.effectiveWeight(5, 2, 3f)).isEqualTo(11);
    assertThat(WeightMath.effectiveWeight(85, -1, 3f)).isEqualTo(82);
  }

  @Test
  void provisionalTheWeightIsBoundedBelowByZero() {
    // spec: LT-R25 (planchers a 0)
    assertThat(WeightMath.effectiveWeight(10, -2, 5f)).isZero();
    assertThat(WeightMath.effectiveWeight(10, -2, 6f)).isZero();
    assertThat(WeightMath.effectiveWeight(10, -2, 7f)).isZero();
    assertThat(WeightMath.effectiveWeight(1, -1, 100f)).isZero();
  }

  @Test
  void provisionalAFractionalLuckIsFlooredAfterTheProduct() {
    // spec: LT-R25
    assertThat(WeightMath.effectiveWeight(1, 1, 0.5f)).isEqualTo(1);
    assertThat(WeightMath.effectiveWeight(1, 2, 0.5f)).isEqualTo(2);
    assertThat(WeightMath.effectiveWeight(5, -1, 0.5f)).isEqualTo(4);
  }

  @Test
  void provisionalANegativeLuckReversesTheEffectOfTheQuality() {
    // spec: LT-R25
    assertThat(WeightMath.effectiveWeight(10, -2, -1f)).isEqualTo(12);
    assertThat(WeightMath.effectiveWeight(5, 2, -3f)).isZero();
  }
}

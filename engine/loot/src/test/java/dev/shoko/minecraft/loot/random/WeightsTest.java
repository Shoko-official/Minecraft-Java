package dev.shoko.minecraft.loot.random;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

// spec: LT-R26 (probability proportional to the weight, no candidate gives nothing, a single
// candidate is chosen without a draw: H-single), LT-R74c (the sum of capped weights can pass the
// int range and must neither overflow nor throw).
class WeightsTest {

  @Test
  void provisionalNoPositiveWeightGivesNoIndexAndNoDraw() {
    // spec: LT-R26 (sans candidat, rien n'est produit), LT-R25 (poids 0 jamais tire)
    ScriptedRandom random = ScriptedRandom.of();
    assertThat(Weights.pick(new int[] {}, random)).isEmpty();
    assertThat(Weights.pick(new int[] {0, 0, -3}, random)).isEmpty();
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalASinglePositiveWeightIsChosenWithoutADraw() {
    // spec: LT-R26 (H-single)
    ScriptedRandom random = ScriptedRandom.of();
    assertThat(Weights.pick(new int[] {0, 7, 0}, random)).hasValue(1);
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalTheWalkFollowsTheWeightsInOrderWithOneBoundedDraw() {
    // spec: LT-R26: weights 3 and 1, total 4, one nextInt(4): 0 to 2 pick the first, 3 the second
    assertThat(Weights.pick(new int[] {3, 1}, ScriptedRandom.of(0.0))).hasValue(0);
    assertThat(Weights.pick(new int[] {3, 1}, ScriptedRandom.of(0.74))).hasValue(0);
    assertThat(Weights.pick(new int[] {3, 1}, ScriptedRandom.of(0.76))).hasValue(1);
    ScriptedRandom one = ScriptedRandom.of(0.5);
    Weights.pick(new int[] {3, 1}, one);
    assertThat(one.steps()).isEqualTo(1);
  }

  @Test
  void provisionalAnEntryOfWeightZeroIsSkippedByTheWalk() {
    // spec: LT-R25, LT-R26: the weights 0 and 5 are never counted
    assertThat(Weights.pick(new int[] {0, 5, 5}, ScriptedRandom.of(0.0))).hasValue(1);
    assertThat(Weights.pick(new int[] {0, 5, 5}, ScriptedRandom.of(0.99))).hasValue(2);
  }

  @Test
  void provisionalASumAboveTheIntRangeIsReducedModuloOnOneLongDraw() {
    // spec: LT-R74c (nextLong modulo the long sum): two weights of 2^30 make 2^31
    long total = 1L << 31;
    for (double fraction : new double[] {0.1, 0.3, 0.5, 0.7, 0.9}) {
      long drawn = (long) (fraction * 9.0e18);
      int expected = Math.floorMod(drawn, total) < (1L << 30) ? 0 : 1;
      ScriptedRandom random = ScriptedRandom.of(fraction);
      OptionalInt picked = Weights.pick(new int[] {1 << 30, 1 << 30}, random);
      assertThat(picked).as("fraction %s", fraction).hasValue(expected);
      assertThat(random.steps()).isEqualTo(1);
    }
  }

  @Test
  void provisionalWeightsOfIntegerMaxKeepTheirProportionsAndNeverThrow() {
    // spec: LT-R74c: weights 3 x 2^30 and 2^30 give a quarter to the second entry
    Lcg48Random random = Lcg48Random.ofSeed(99);
    int second = 0;
    int samples = 6000;
    for (int i = 0; i < samples; i++) {
      second += Weights.pick(new int[] {3 << 28, 1 << 28, 3 << 28, 1 << 28}, random).getAsInt() % 2;
    }
    assertThat(second / (double) samples).isBetween(0.22, 0.28);
    for (int i = 0; i < 100; i++) {
      assertThat(
              Weights.pick(
                      new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}, random)
                  .getAsInt())
          .isBetween(0, 2);
    }
  }
}

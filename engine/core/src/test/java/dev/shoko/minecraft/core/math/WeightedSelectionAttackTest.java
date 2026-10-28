package dev.shoko.minecraft.core.math;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.OptionalInt;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class WeightedSelectionAttackTest {

  /** Independent reference: cumulative sums in long, first index whose prefix sum exceeds r. */
  private static OptionalInt reference(int[] weights, int r) {
    long cumulative = 0;
    for (int i = 0; i < weights.length; i++) {
      if (weights[i] > 0) {
        cumulative += weights[i];
        if (r < cumulative) {
          return OptionalInt.of(i);
        }
      }
    }
    return OptionalInt.empty();
  }

  @Test
  @Timeout(20)
  void denseRandomListsMatchTheReferenceWalk() {
    SplittableRandom rnd = new SplittableRandom(23);
    for (int trial = 0; trial < 100_000; trial++) {
      int[] weights = new int[rnd.nextInt(0, 8)];
      long total = 0;
      int positives = 0;
      for (int i = 0; i < weights.length; i++) {
        weights[i] = rnd.nextInt(-3, 6);
        if (weights[i] > 0) {
          total += weights[i];
          positives++;
        }
      }
      int r = total == 0 ? 0 : rnd.nextInt((int) total);
      AtomicInteger draws = new AtomicInteger();
      OptionalInt picked =
          WeightedSelection.weightedPick(
              weights.clone(),
              bound -> {
                draws.incrementAndGet();
                return r;
              });
      if (positives == 0) {
        assertThat(picked).isEmpty();
      } else if (positives == 1) {
        assertThat(picked).isPresent();
        assertThat(weights[picked.getAsInt()]).isPositive();
      } else {
        assertThat(picked).isEqualTo(reference(weights, r));
      }
      assertThat(draws.get()).isEqualTo(positives >= 2 ? 1 : 0);
    }
  }

  // Overflow of W and draws outside [0, W) are caller errors (Javadoc of weightedPick); the
  // exception types pinned here are not specified by core-math and may be revisited.
  @Test
  void extremeWeights() {
    int max = Integer.MAX_VALUE;
    assertThat(WeightedSelection.weightedPick(new int[] {0, max, Integer.MIN_VALUE}, b -> 0))
        .hasValue(1);
    assertThat(WeightedSelection.weightedPick(new int[] {max - 1, 1}, b -> max - 1)).hasValue(1);
    assertThat(WeightedSelection.weightedPick(new int[] {max - 1, 1}, b -> max - 2)).hasValue(0);
    assertThatThrownBy(() -> WeightedSelection.weightedPick(new int[] {max, 1}, b -> 0))
        .isInstanceOf(ArithmeticException.class);
    assertThatThrownBy(() -> WeightedSelection.weightedPick(new int[] {1, 1}, b -> 2))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> WeightedSelection.weightedPick(new int[] {1, 1}, b -> -1))
        .isInstanceOf(IllegalStateException.class);
  }

  // spec: core-math R23 and V17 "L'entree de poids 0 n'est jamais retenue". The weights array is
  // read twice, before and after the draw; the draw callback (or any other holder of the array)
  // can change it in between, and the walk then picks an entry whose weight was 0 when W was
  // computed.
  @Test
  void zeroWeightEntryIsNeverPickedEvenIfTheArrayChangesDuringTheDraw() {
    int[] weights = {1, 0, 1};
    OptionalInt picked =
        WeightedSelection.weightedPick(
            weights,
            bound -> {
              weights[1] = 5;
              return 1;
            });
    assertThat(picked).isNotEqualTo(OptionalInt.of(1));
    assertThat(picked).hasValue(2);
  }
}

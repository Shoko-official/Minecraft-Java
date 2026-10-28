package dev.shoko.minecraft.core.math;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.IntUnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// spec: core-math R23 is S: the walk below is provisional for every weighted list.
class WeightedSelectionTest {

  /** Bounded draw stub: returns a fixed value and records every bound it receives. */
  private static final class RecordingDraw implements IntUnaryOperator {
    private final int result;
    private final List<Integer> bounds = new ArrayList<>();

    RecordingDraw(int result) {
      this.result = result;
    }

    @Override
    public int applyAsInt(int bound) {
      bounds.add(bound);
      return result;
    }
  }

  // spec: core-math R23 (S), vector V17: weights [3, 0, 5, 2]
  @ParameterizedTest(name = "provisional r = {0} picks index {1}")
  @CsvSource({"0, 0", "1, 0", "2, 0", "3, 2", "4, 2", "5, 2", "6, 2", "7, 2", "8, 3", "9, 3"})
  void provisionalPickWalksWeightsInListOrder(int r, int expectedIndex) {
    RecordingDraw draw = new RecordingDraw(r);
    OptionalInt picked = WeightedSelection.weightedPick(new int[] {3, 0, 5, 2}, draw);
    assertThat(picked).hasValue(expectedIndex);
    assertThat(draw.bounds).containsExactly(10);
  }

  // spec: core-math R23 (S), V17: an entry of weight 0 is never picked
  @Test
  void provisionalZeroWeightIsNeverPicked() {
    for (int r = 0; r < 10; r++) {
      OptionalInt picked =
          WeightedSelection.weightedPick(new int[] {3, 0, 5, 2}, new RecordingDraw(r));
      assertThat(picked).isPresent();
      assertThat(picked.getAsInt()).isNotEqualTo(1);
    }
  }

  // spec: core-math R23 (S): negative weights are excluded from the total and the walk
  @ParameterizedTest(name = "provisional r = {0} picks index {1} with a negative weight")
  @CsvSource({"0, 1", "3, 1", "4, 3", "5, 3"})
  void provisionalNegativeWeightIsExcluded(int r, int expectedIndex) {
    RecordingDraw draw = new RecordingDraw(r);
    OptionalInt picked = WeightedSelection.weightedPick(new int[] {-3, 4, 0, 2}, draw);
    assertThat(picked).hasValue(expectedIndex);
    assertThat(draw.bounds).containsExactly(6);
  }

  // spec: core-math R23 (S): equal weights, the earlier entry takes the lower draws
  @Test
  void provisionalEarlierEntryTakesLowerDraws() {
    assertThat(WeightedSelection.weightedPick(new int[] {2, 2}, new RecordingDraw(1))).hasValue(0);
    assertThat(WeightedSelection.weightedPick(new int[] {2, 2}, new RecordingDraw(2))).hasValue(1);
  }

  // spec: core-math R23 (S), V17: a single remaining entry is returned without any draw
  @Test
  void provisionalSingleEntryIsPickedWithoutDraw() {
    RecordingDraw draw = new RecordingDraw(0);
    assertThat(WeightedSelection.weightedPick(new int[] {7}, draw)).hasValue(0);
    assertThat(WeightedSelection.weightedPick(new int[] {0, -1, 7, 0}, draw)).hasValue(2);
    assertThat(draw.bounds).isEmpty();
  }

  // spec: core-math R23 (S), V17: no positive weight gives no result and no draw
  @Test
  void provisionalNoPositiveWeightGivesEmptyWithoutDraw() {
    RecordingDraw draw = new RecordingDraw(0);
    assertThat(WeightedSelection.weightedPick(new int[] {0, 0}, draw)).isEmpty();
    assertThat(WeightedSelection.weightedPick(new int[] {}, draw)).isEmpty();
    assertThat(WeightedSelection.weightedPick(new int[] {-5, 0}, draw)).isEmpty();
    assertThat(draw.bounds).isEmpty();
  }

  // spec: core-math R23 (S): the weights are read, not modified
  @Test
  void provisionalWeightsAreNotModified() {
    int[] weights = {3, 0, 5, 2};
    WeightedSelection.weightedPick(weights, new RecordingDraw(9));
    assertThat(weights).containsExactly(3, 0, 5, 2);
  }
}

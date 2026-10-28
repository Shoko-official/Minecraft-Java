package dev.shoko.minecraft.core.math;

import java.util.OptionalInt;
import java.util.function.IntUnaryOperator;

/**
 * Selection of an entry in an ordered list of integer weights (R23, provisional for every weighted
 * list; each consuming spec confirms it with its own probe).
 *
 * <p>The bounded draw itself belongs to the {@code core-random} spec (R22). This class receives it
 * as a function, so it never depends on a generator and the number of draws stays observable.
 */
public final class WeightedSelection {

  private WeightedSelection() {}

  /**
   * Picks the index of an entry (R23, provisional). Entries of weight 0 or less are excluded; let
   * {@code W} be the sum of the remaining weights. With no remaining entry: empty result, no draw.
   * With exactly one: its index, no draw. Otherwise {@code r = nextIntBelow.applyAsInt(W)} is drawn
   * once, then the remaining entries are walked in their original order, subtracting each weight
   * from {@code r}; the first entry for which {@code r} becomes negative is picked. Example:
   * weights {@code [3, 0, 5, 2]} give index 0 for {@code r} in 0..2, 2 for 3..7, 3 for 8..9.
   *
   * @param weights weights in list order, read but not modified; their positive sum must fit an
   *     {@code int}
   * @param nextIntBelow bounded draw of {@code core-random}: given {@code W > 0}, returns an
   *     integer in {@code [0, W)}; called at most once
   * @return the index of the picked entry in {@code weights}, or empty if no weight is positive
   * @throws ArithmeticException if the positive weights add up beyond {@code Integer.MAX_VALUE}
   * @throws IllegalStateException if {@code nextIntBelow} returns a value outside {@code [0, W)}
   */
  public static OptionalInt weightedPick(int[] weights, IntUnaryOperator nextIntBelow) {
    int total = 0;
    int candidates = 0;
    int lastCandidate = -1;
    for (int i = 0; i < weights.length; i++) {
      if (weights[i] > 0) {
        total = Math.addExact(total, weights[i]);
        candidates++;
        lastCandidate = i;
      }
    }
    if (candidates == 0) {
      return OptionalInt.empty();
    }
    if (candidates == 1) {
      return OptionalInt.of(lastCandidate);
    }
    int r = nextIntBelow.applyAsInt(total);
    if (r < 0 || r >= total) {
      throw new IllegalStateException("bounded draw " + r + " outside [0, " + total + ")");
    }
    for (int i = 0; i < weights.length; i++) {
      if (weights[i] > 0) {
        r -= weights[i];
        if (r < 0) {
          return OptionalInt.of(i);
        }
      }
    }
    throw new IllegalStateException("weighted walk ended without a pick");
  }
}

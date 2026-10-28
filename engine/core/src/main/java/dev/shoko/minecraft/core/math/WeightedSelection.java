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
   */
  public static OptionalInt weightedPick(int[] weights, IntUnaryOperator nextIntBelow) {
    return Skeleton.throwNotImplemented();
  }
}

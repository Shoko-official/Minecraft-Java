package dev.shoko.minecraft.loot.random;

import dev.shoko.minecraft.core.random.RandomSource;
import java.util.OptionalInt;

/**
 * The weighted choice of a list of weights (LT-R26): same walk as {@code
 * core.math.WeightedSelection}, with the sum in a long so that capped weights can add up beyond the
 * int range (LT-R74c). Entries of weight 0 or less are never chosen; no candidate gives an empty
 * result without a draw, a single candidate is chosen without a draw (H-single).
 */
public final class Weights {

  private Weights() {}

  /**
   * Picks the index of an entry.
   *
   * @param weights the weights in list order
   * @param random the source; one draw when two or more entries have a positive weight
   * @return the index, empty when no weight is positive
   */
  public static OptionalInt pick(int[] weights, RandomSource random) {
    long total = 0;
    int candidates = 0;
    int last = -1;
    for (int i = 0; i < weights.length; i++) {
      if (weights[i] > 0) {
        total += weights[i];
        candidates++;
        last = i;
      }
    }
    if (candidates == 0) {
      return OptionalInt.empty();
    }
    if (candidates == 1) {
      return OptionalInt.of(last);
    }
    long remaining =
        total <= Integer.MAX_VALUE
            ? random.nextInt((int) total)
            : Math.floorMod(random.nextLong(), total);
    for (int i = 0; i < weights.length; i++) {
      if (weights[i] > 0) {
        remaining -= weights[i];
        if (remaining < 0) {
          return OptionalInt.of(i);
        }
      }
    }
    return OptionalInt.of(last);
  }
}

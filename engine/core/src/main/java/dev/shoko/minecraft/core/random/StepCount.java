package dev.shoko.minecraft.core.random;

/**
 * Saturating arithmetic of the step counter of both generator families: the count stops at {@link
 * Long#MAX_VALUE} instead of overflowing (spec, interface requirements).
 */
final class StepCount {

  private StepCount() {}

  /**
   * Returns {@code count + n}, or {@link Long#MAX_VALUE} when the sum would overflow.
   *
   * @param count the current count, 0 or more
   * @param n the steps to add, 0 or more
   */
  static long add(long count, long n) {
    return count > Long.MAX_VALUE - n ? Long.MAX_VALUE : count + n;
  }
}

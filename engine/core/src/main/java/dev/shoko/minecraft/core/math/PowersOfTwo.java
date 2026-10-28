package dev.shoko.minecraft.core.math;

/**
 * Powers of two and bit counts (R21).
 *
 * <p>Both methods are defined for {@code 1 <= v <= 2^30} only. Other arguments are forbidden and
 * their result is unspecified.
 */
public final class PowersOfTwo {

  private PowersOfTwo() {}

  /**
   * Returns the smallest power of two greater than or equal to {@code v} (R21).
   *
   * @param v value in {@code [1, 2^30]}
   * @return the power of two, in {@code [1, 2^30]}
   */
  public static int ceilingPowerOfTwo(int v) {
    return 1 << ceilingLog2(v);
  }

  /**
   * Returns {@code ceil(log2(v))}: the number of bits needed to encode {@code v} distinct values
   * (R21). Examples: 1 gives 0, 2 gives 1, 3 gives 2, 17 gives 5, 33 gives 6.
   *
   * @param v value in {@code [1, 2^30]}
   * @return the number of bits, in {@code [0, 30]}
   */
  public static int ceilingLog2(int v) {
    // v - 1 has exactly ceil(log2(v)) significant bits; for v = 1 it is 0, which has none.
    return Integer.SIZE - Integer.numberOfLeadingZeros(v - 1);
  }
}

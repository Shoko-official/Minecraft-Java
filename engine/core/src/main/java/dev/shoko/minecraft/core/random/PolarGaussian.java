package dev.shoko.minecraft.core.random;

/**
 * Marsaglia's polar method with one cached value (R7, R9, R20), shared by both generator families.
 * Mutable, owned by a single generator.
 */
final class PolarGaussian {

  private double cached;
  private boolean hasCached;

  /** Returns the cached value without drawing, or draws a new pair from {@code source}. */
  double next(RandomSource source) {
    if (hasCached) {
      hasCached = false;
      return cached;
    }
    double v1;
    double v2;
    double s;
    do {
      v1 = 2 * source.nextDouble() - 1;
      v2 = 2 * source.nextDouble() - 1;
      s = v1 * v1 + v2 * v2;
    } while (s >= 1 || s == 0);
    // StrictMath, not Math: provisional choice of R9 (spec Q3), the only platform-independent one.
    double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
    cached = v2 * multiplier;
    hasCached = true;
    return v1 * multiplier;
  }

  /** Drops the cached value, as a reinitialization does (R2, R25). */
  void clear() {
    hasCached = false;
  }
}

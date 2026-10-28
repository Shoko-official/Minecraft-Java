package dev.shoko.minecraft.core.math;

/**
 * Rounding of {@code float} and {@code double} values to integers (R4 to R7).
 *
 * <p>Domain: the contract holds for finite arguments whose result fits the target type. Outside of
 * it (infinities, magnitudes at or above 2^31 for {@code int}) the game's behavior is unknown;
 * provisional: mathematical floor or ceiling, then saturating conversion, NaN gives 0 (candidates
 * F1 and C1 of the spec). Callers bound their inputs and never rely on that case (R4, Q4).
 *
 * <p>The {@code float} and {@code double} overloads are not interchangeable: the {@code float}
 * variant applies to the value already rounded to {@code float} (R5). The consuming spec chooses.
 */
public final class Rounding {

  private Rounding() {}

  /**
   * Returns the largest {@code int} lower than or equal to {@code x} (R4). {@code -0.0} gives 0,
   * {@code -1e-300} and {@code -0.5} give -1. Out of domain, provisional: saturates, NaN gives 0.
   *
   * @param x value to round down
   * @return the floor of {@code x} as an {@code int}
   */
  public static int floorToInt(double x) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the largest {@code int} lower than or equal to {@code x}, computed on the {@code float}
   * value (R4, R5). Out of domain, provisional: saturates, NaN gives 0.
   *
   * @param x value to round down
   * @return the floor of {@code x} as an {@code int}
   */
  public static int floorToInt(float x) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the smallest {@code int} greater than or equal to {@code x} (R6). {@code -0.5} gives 0,
   * {@code 15.999999999999998} gives 16. Out of domain, provisional: saturates, NaN gives 0.
   *
   * @param x value to round up
   * @return the ceiling of {@code x} as an {@code int}
   */
  public static int ceilToInt(double x) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the smallest {@code int} greater than or equal to {@code x}, computed on the {@code
   * float} value (R5, R6). Out of domain, provisional: saturates, NaN gives 0.
   *
   * @param x value to round up
   * @return the ceiling of {@code x} as an {@code int}
   */
  public static int ceilToInt(float x) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the largest {@code long} lower than or equal to {@code x} (R7, domain and provisional
   * behavior of R4 transposed to {@code long}).
   *
   * @param x value to round down
   * @return the floor of {@code x} as a {@code long}
   */
  public static long floorToLong(double x) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@code x - floorToLong(x)} computed in {@code double} (R7). The result is not always
   * strictly below 1: {@code fractionalPart(-1e-20)} is exactly {@code 1.0} after rounding.
   *
   * @param x value to split
   * @return the fractional part of {@code x}, in {@code [0, 1]}
   */
  public static double fractionalPart(double x) {
    return Skeleton.throwNotImplemented();
  }
}

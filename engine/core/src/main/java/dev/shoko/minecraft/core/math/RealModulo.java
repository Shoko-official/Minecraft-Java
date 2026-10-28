package dev.shoko.minecraft.core.math;

/**
 * Two distinct real modulo functions, an internal contract of the engine (R10, provisional).
 *
 * <p>Neither form is imposed: each consuming spec picks one, and whether the game has a real modulo
 * at all is left to the probes of those specs (Q10). The forms differ on small negative values: for
 * {@code (-1e-20, 360)} {@link #realModFloor(double, double)} gives {@code 360.0} and {@link
 * #realModWrap(double, double)} gives {@code 0.0}. Each overload computes in the precision of its
 * arguments.
 */
public final class RealModulo {

  private RealModulo() {}

  /**
   * Returns {@code a - floor(a / b) * b} in {@code double} (R10, provisional).
   *
   * @param a dividend
   * @param b divisor
   * @return the floored remainder, which may equal {@code b} after rounding
   */
  public static double realModFloor(double a, double b) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@code a - floor(a / b) * b} in {@code float} (R10, provisional).
   *
   * @param a dividend
   * @param b divisor
   * @return the floored remainder, which may equal {@code b} after rounding
   */
  public static float realModFloor(float a, float b) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@code ((a % b) + b) % b} in {@code double} (R10, provisional).
   *
   * @param a dividend
   * @param b divisor
   * @return the wrapped remainder
   */
  public static double realModWrap(double a, double b) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@code ((a % b) + b) % b} in {@code float} (R10, provisional).
   *
   * @param a dividend
   * @param b divisor
   * @return the wrapped remainder
   */
  public static float realModWrap(float a, float b) {
    return Skeleton.throwNotImplemented();
  }
}

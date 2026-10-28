package dev.shoko.minecraft.core.math;

/**
 * Clamping of a value between two bounds (R15).
 *
 * <p>For {@code lo <= hi} and a non-NaN value, every overload returns {@code lo} if {@code v < lo},
 * {@code hi} if {@code v > hi}, {@code v} otherwise. Provisional form, chosen without proof: {@code
 * v < lo ? lo : min(v, hi)} (form MIX of the spec). Consequences of that form: {@code clamp(-0.0,
 * 0.0, 1.0)} is {@code -0.0}; a NaN {@code hi} propagates, a NaN {@code lo} is ignored; a NaN value
 * gives NaN; with {@code lo > hi}, {@code lo} if {@code v < lo} and {@code hi} otherwise. Unlike
 * {@link java.lang.Math#clamp(double, double, double)} nothing throws, but the engine never calls
 * these methods with {@code lo > hi}.
 */
public final class Clamping {

  private Clamping() {}

  /**
   * Clamps an {@code int} (R15, provisional form MIX).
   *
   * @param v value to clamp
   * @param lo lower bound
   * @param hi upper bound, not lower than {@code lo}
   * @return the clamped value
   */
  public static int clamp(int v, int lo, int hi) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Clamps a {@code long} (R15, provisional form MIX).
   *
   * @param v value to clamp
   * @param lo lower bound
   * @param hi upper bound, not lower than {@code lo}
   * @return the clamped value
   */
  public static long clamp(long v, long lo, long hi) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Clamps a {@code float} (R15, provisional form MIX, including its handling of signed zeros and
   * NaN described on the class).
   *
   * @param v value to clamp
   * @param lo lower bound
   * @param hi upper bound, not lower than {@code lo}
   * @return the clamped value
   */
  public static float clamp(float v, float lo, float hi) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Clamps a {@code double} (R15, provisional form MIX, including its handling of signed zeros and
   * NaN described on the class).
   *
   * @param v value to clamp
   * @param lo lower bound
   * @param hi upper bound, not lower than {@code lo}
   * @return the clamped value
   */
  public static double clamp(double v, double lo, double hi) {
    return Skeleton.throwNotImplemented();
  }
}

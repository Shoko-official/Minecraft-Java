package dev.shoko.minecraft.core.math;

/**
 * Arc tangent and normalization of angles in degrees (R18, R20).
 *
 * <p>{@code normalizeDegrees} brings an angle into {@code [-180, 180)}, provisional formula: {@code
 * r = v % 360; if r >= 180 then r -= 360; if r < -180 then r += 360}, in the precision of the
 * argument. Examples: {@code 180 -> -180}, {@code 540 -> -180}, {@code -190 -> 170}, {@code -0.0 ->
 * -0.0}, NaN stays NaN. Whether and how the game normalizes yaws is left to probe P10.
 */
public final class Angles {

  private Angles() {}

  /**
   * Returns the angle of the point {@code (x, y)} in radians (R18, provisional: {@link
   * java.lang.StrictMath#atan2(double, double)}; probe P18 measures the game's deviation from it).
   *
   * @param y ordinate
   * @param x abscissa
   * @return the angle in {@code [-pi, pi]}
   */
  public static double atan2(double y, double x) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Normalizes an angle in degrees into {@code [-180, 180)}, in {@code int} (R20, provisional).
   *
   * @param degrees angle in degrees
   * @return the normalized angle
   */
  public static int normalizeDegrees(int degrees) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Normalizes an angle in degrees into {@code [-180, 180)}, in {@code float} (R20, provisional).
   *
   * @param degrees angle in degrees
   * @return the normalized angle, NaN for NaN
   */
  public static float normalizeDegrees(float degrees) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Normalizes an angle in degrees into {@code [-180, 180)}, in {@code double} (R20, provisional).
   *
   * @param degrees angle in degrees
   * @return the normalized angle, NaN for NaN
   */
  public static double normalizeDegrees(double degrees) {
    return Skeleton.throwNotImplemented();
  }
}

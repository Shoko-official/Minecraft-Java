package dev.shoko.minecraft.core.math;

/**
 * Linear interpolation and its compositions, plus the quintic fade curve (R11 to R14, R16).
 *
 * <p>No method clamps its parameter unless its name says so: {@code t} outside of {@code [0, 1]}
 * extrapolates. The {@code float} and {@code double} overloads of {@code interpolate} are not
 * interchangeable (R11); every other method computes in {@code double}.
 */
public final class Interpolation {

  private Interpolation() {}

  /**
   * The eight corner values of a unit cube, named {@code v<x><y><z>} (R12).
   *
   * @param v000 value at {@code (0, 0, 0)}
   * @param v100 value at {@code (1, 0, 0)}
   * @param v010 value at {@code (0, 1, 0)}
   * @param v110 value at {@code (1, 1, 0)}
   * @param v001 value at {@code (0, 0, 1)}
   * @param v101 value at {@code (1, 0, 1)}
   * @param v011 value at {@code (0, 1, 1)}
   * @param v111 value at {@code (1, 1, 1)}
   */
  public record CubeCorners(
      double v000,
      double v100,
      double v010,
      double v110,
      double v001,
      double v101,
      double v011,
      double v111) {}

  /**
   * Returns {@code a + t * (b - a)} in {@code double} (R11, provisional: form A). This form does
   * not always return exactly {@code b} for {@code t = 1}: {@code interpolate(1, 1e20, 1)} is 0.
   *
   * @param t interpolation parameter, not clamped
   * @param a value at {@code t = 0}
   * @param b value at {@code t = 1}
   * @return the interpolated value
   */
  public static double interpolate(double t, double a, double b) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@code a + t * (b - a)} in {@code float} (R11, provisional: form A).
   *
   * @param t interpolation parameter, not clamped
   * @param a value at {@code t = 0}
   * @param b value at {@code t = 1}
   * @return the interpolated value
   */
  public static float interpolate(float t, float a, float b) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Interpolates on a unit square, corners named {@code v<x><y>} (R12, provisional): along x first
   * ({@code v00} to {@code v10}, then {@code v01} to {@code v11}), then along y, each step by
   * {@link #interpolate(double, double, double)}.
   *
   * @param tx parameter along x
   * @param ty parameter along y
   * @param v00 value at {@code (0, 0)}
   * @param v10 value at {@code (1, 0)}
   * @param v01 value at {@code (0, 1)}
   * @param v11 value at {@code (1, 1)}
   * @return the interpolated value
   */
  public static double bilinear(
      double tx, double ty, double v00, double v10, double v01, double v11) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Interpolates on a unit cube (R12, provisional): along x first, then y, then z, each step by
   * {@link #interpolate(double, double, double)}.
   *
   * @param tx parameter along x
   * @param ty parameter along y
   * @param tz parameter along z
   * @param corners the eight corner values
   * @return the interpolated value
   */
  public static double trilinear(double tx, double ty, double tz, CubeCorners corners) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@code (v - a) / (b - a)}, without clamping or guard (R13, provisional). For {@code a
   * == b}: NaN if {@code v == a}, a signed infinity otherwise.
   *
   * @param v value to locate
   * @param a value mapped to 0
   * @param b value mapped to 1
   * @return the parameter of {@code v} between {@code a} and {@code b}
   */
  public static double inverseInterpolate(double v, double a, double b) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@code interpolate(inverseInterpolate(v, a0, a1), b0, b1)}, without clamping (R14,
   * provisional).
   *
   * @param v value in the source range
   * @param a0 source value mapped to {@code b0}
   * @param a1 source value mapped to {@code b1}
   * @param b0 target value for {@code a0}
   * @param b1 target value for {@code a1}
   * @return the remapped value
   */
  public static double remap(double v, double a0, double a1, double b0, double b1) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@link #remap(double, double, double, double, double)} clamped by {@link
   * Clamping#clamp(double, double, double)} between {@code min(b0, b1)} and {@code max(b0, b1)}
   * (R14, provisional).
   *
   * @param v value in the source range
   * @param a0 source value mapped to {@code b0}
   * @param a1 source value mapped to {@code b1}
   * @param b0 target value for {@code a0}
   * @param b1 target value for {@code a1}
   * @return the remapped value, within the target range
   */
  public static double remapClamped(double v, double a0, double a1, double b0, double b1) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@code a} if {@code t < 0}, {@code b} if {@code t > 1}, {@link #interpolate(double,
   * double, double)} otherwise (R14, provisional).
   *
   * @param t interpolation parameter
   * @param a value at {@code t <= 0}
   * @param b value at {@code t >= 1}
   * @return the interpolated value
   */
  public static double clampedInterpolate(double t, double a, double b) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@code 6t^5 - 15t^4 + 10t^3} in {@code double} (R16, provisional: Horner form {@code t
   * * t * t * (t * (t * 6 - 15) + 10)}, evaluated left to right). Exact values: 0 at 0, 0.5 at 0.5,
   * 1 at 1; {@code t} is not clamped.
   *
   * @param t curve parameter
   * @return the faded value
   */
  public static double quinticFade(double t) {
    return Skeleton.throwNotImplemented();
  }
}

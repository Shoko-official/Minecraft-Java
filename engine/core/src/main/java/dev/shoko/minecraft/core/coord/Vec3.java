package dev.shoko.minecraft.core.coord;

/**
 * Three-dimensional vector of {@code double} components (R39). Operations are component-wise and
 * keep the operation order of their formula (R2). Equality is the record's exact equality of
 * components; the game's handling of {@code -0.0} and NaN is not observable and left free (R39).
 *
 * @param x x component
 * @param y y component
 * @param z z component
 */
public record Vec3(double x, double y, double z) {

  /**
   * Returns {@code this + other}, component-wise (R39).
   *
   * @param other vector to add
   * @return the sum
   */
  public Vec3 add(Vec3 other) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@code this - other}, component-wise (R39).
   *
   * @param other vector to subtract
   * @return the difference
   */
  public Vec3 subtract(Vec3 other) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns this vector with each component multiplied by {@code factor} (R39).
   *
   * @param factor scalar factor
   * @return the scaled vector
   */
  public Vec3 scale(double factor) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the squared length {@code (x * x + y * y) + z * z}, in that order (R39, order
   * provisional: the other grouping differs in 23.8% of cases).
   *
   * @return the squared length
   */
  public double lengthSquared() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the square root of {@link #lengthSquared()} (R39).
   *
   * @return the length
   */
  public double length() {
    return Skeleton.throwNotImplemented();
  }
}

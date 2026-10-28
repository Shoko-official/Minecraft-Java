package dev.shoko.minecraft.core.coord;

/**
 * Axis-aligned box {@code [minX, maxX] x [minY, maxY] x [minZ, maxZ]} in {@code double} (R40 to
 * R42, provisional until probe P41).
 *
 * <p>The canonical constructor stores its arguments as given, without normalization: a box
 * contracted past zero by {@link #inflate(double)} keeps {@code min > max} (R42). Use {@link
 * #ofCorners(double, double, double, double, double, double)} to build a box from two arbitrary
 * corners.
 *
 * @param minX lowest x
 * @param minY lowest y
 * @param minZ lowest z
 * @param maxX highest x
 * @param maxY highest y
 * @param maxZ highest z
 */
public record Aabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

  /**
   * Returns the box spanned by two arbitrary corners, normalized with the minimum and maximum on
   * each axis (R40, provisional).
   *
   * @param x1 x of the first corner
   * @param y1 y of the first corner
   * @param z1 z of the first corner
   * @param x2 x of the second corner
   * @param y2 y of the second corner
   * @param z2 z of the second corner
   * @return the normalized box
   */
  public static Aabb ofCorners(double x1, double y1, double z1, double x2, double y2, double z2) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the box of a full block, {@code [x, x + 1] x [y, y + 1] x [z, z + 1]} (R40).
   *
   * @param pos block position
   * @return the block's box
   */
  public static Aabb ofBlock(BlockPos pos) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether this box intersects another: on each of the three axes, {@code this.min <
   * other.max} and {@code this.max > other.min} (R41, provisional). The inequalities are strict:
   * two boxes sharing only a face or an edge do not intersect.
   *
   * @param other the other box
   * @return {@code true} if the interiors overlap
   */
  public boolean intersects(Aabb other) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether a point is inside this box, half-open: {@code min <= p < max} on each axis (R41,
   * provisional).
   *
   * @param point the point
   * @return {@code true} if the point is inside
   */
  public boolean contains(Vec3 point) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns this box with {@code offset} added to both corners (R42).
   *
   * @param offset displacement
   * @return the moved box
   */
  public Aabb move(Vec3 offset) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns this box grown by {@code amount} on every side: {@code min - amount}, {@code max +
   * amount}; a negative amount contracts, without renormalization (R42, provisional).
   *
   * @param amount growth on each side
   * @return the inflated box
   */
  public Aabb inflate(double amount) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns this box extended towards {@code direction}: on each axis, a negative component lowers
   * the minimum by its magnitude, a positive one raises the maximum (R42, provisional).
   *
   * @param direction extension vector
   * @return the extended box
   */
  public Aabb expandTowards(Vec3 direction) {
    return Skeleton.throwNotImplemented();
  }
}

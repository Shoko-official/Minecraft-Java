package dev.shoko.minecraft.core.coord;

import java.util.Optional;

/**
 * The six directions, declared in canonical identifier order (R35, provisional until probe P35).
 * Compass: +X is east, +Z is south. Horizontal directions also have a 2D identifier and a yaw
 * (R36). This order is not the order of block state properties (R37).
 */
public enum Direction {
  /** Identifier 0, vector {@code (0, -1, 0)} (R35). */
  DOWN,
  /** Identifier 1, vector {@code (0, 1, 0)} (R35). */
  UP,
  /** Identifier 2, vector {@code (0, 0, -1)}; 2D identifier 2, yaw 180 (R35, R36). */
  NORTH,
  /** Identifier 3, vector {@code (0, 0, 1)}; 2D identifier 0, yaw 0 (R35, R36). */
  SOUTH,
  /** Identifier 4, vector {@code (-1, 0, 0)}; 2D identifier 1, yaw 90 (R35, R36). */
  WEST,
  /** Identifier 5, vector {@code (1, 0, 0)}; 2D identifier 3, yaw -90 (R35, R36). */
  EAST;

  /**
   * Returns the canonical identifier, from 0 for {@link #DOWN} to 5 for {@link #EAST} (R35).
   *
   * @return the identifier
   */
  public int id() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the direction with this canonical identifier (R35).
   *
   * @param id identifier as read, possibly out of range
   * @return the direction, or empty outside {@code [0, 5]}
   */
  public static Optional<Direction> byId(int id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the opposite direction, of identifier {@code id() ^ 1} (R35).
   *
   * @return the opposite direction
   */
  public Direction opposite() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the x component of the unit vector (R35).
   *
   * @return -1, 0 or 1
   */
  public int stepX() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the y component of the unit vector (R35).
   *
   * @return -1, 0 or 1
   */
  public int stepY() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the z component of the unit vector (R35).
   *
   * @return -1, 0 or 1
   */
  public int stepZ() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the axis of the unit vector (R34, R35).
   *
   * @return the axis
   */
  public Axis axis() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the name of this direction in the data: {@code down}, {@code up}, {@code north}, {@code
   * south}, {@code west} or {@code east} (R35).
   *
   * @return the lowercase name
   */
  public String serializedName() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the 2D identifier of a horizontal direction: 0 south, 1 west, 2 north, 3 east (R36,
   * provisional until probe P35).
   *
   * @return the 2D identifier
   * @throws IllegalStateException for {@link #UP} and {@link #DOWN}
   */
  public int horizontalId() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the horizontal direction with this 2D identifier (R36).
   *
   * @param horizontalId 2D identifier as read, possibly out of range
   * @return the direction, or empty outside {@code [0, 3]}
   */
  public static Optional<Direction> byHorizontalId(int horizontalId) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the yaw of a horizontal direction in degrees: south 0, west 90, north 180, east -90
   * (R36).
   *
   * @return the yaw
   * @throws IllegalStateException for {@link #UP} and {@link #DOWN}
   */
  public double yaw() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the horizontal direction faced at a yaw, of 2D identifier {@code floorToInt(yaw / 90 +
   * 0.5) & 3} computed in {@code double} (R36, provisional until probe P36). The boundary at 45
   * falls to west, the one at -45 to south.
   *
   * @param yaw yaw in degrees, any value
   * @return the horizontal direction
   */
  public static Direction fromYaw(double yaw) {
    return Skeleton.throwNotImplemented();
  }
}

package dev.shoko.minecraft.core.coord;

import dev.shoko.minecraft.core.math.Rounding;
import java.util.List;
import java.util.Optional;

/**
 * The six directions, declared in canonical identifier order (R35, provisional until probe P35).
 * Compass: +X is east, +Z is south. Horizontal directions also have a 2D identifier and a yaw
 * (R36). This order is not the order of block state properties (R37).
 */
public enum Direction {
  /** Identifier 0, vector {@code (0, -1, 0)} (R35). */
  DOWN("down", 0, 0, -1, 0),
  /** Identifier 1, vector {@code (0, 1, 0)} (R35). */
  UP("up", 1, 0, 1, 0),
  /** Identifier 2, vector {@code (0, 0, -1)}; 2D identifier 2, yaw 180 (R35, R36). */
  NORTH("north", 2, 0, 0, -1),
  /** Identifier 3, vector {@code (0, 0, 1)}; 2D identifier 0, yaw 0 (R35, R36). */
  SOUTH("south", 3, 0, 0, 1),
  /** Identifier 4, vector {@code (-1, 0, 0)}; 2D identifier 1, yaw 90 (R35, R36). */
  WEST("west", 4, -1, 0, 0),
  /** Identifier 5, vector {@code (1, 0, 0)}; 2D identifier 3, yaw -90 (R35, R36). */
  EAST("east", 5, 1, 0, 0);

  // Constants are declared in identifier order, so the position in values() is the identifier.
  private static final List<Direction> BY_ID = List.of(values());

  /** Horizontal directions in 2D identifier order (R36). */
  private static final List<Direction> BY_HORIZONTAL_ID = List.of(SOUTH, WEST, NORTH, EAST);

  /** One 2D identifier step is a quarter turn (R36). */
  private static final double DEGREES_PER_HORIZONTAL_ID = 90.0;

  private final String serializedName;
  private final int id;
  private final int stepX;
  private final int stepY;
  private final int stepZ;

  Direction(String serializedName, int id, int stepX, int stepY, int stepZ) {
    this.serializedName = serializedName;
    this.id = id;
    this.stepX = stepX;
    this.stepY = stepY;
    this.stepZ = stepZ;
  }

  /**
   * Returns the canonical identifier, from 0 for {@link #DOWN} to 5 for {@link #EAST} (R35).
   *
   * @return the identifier
   */
  public int id() {
    return id;
  }

  /**
   * Returns the direction with this canonical identifier (R35).
   *
   * @param id identifier as read, possibly out of range
   * @return the direction, or empty outside {@code [0, 5]}
   */
  public static Optional<Direction> byId(int id) {
    return id >= 0 && id < BY_ID.size() ? Optional.of(BY_ID.get(id)) : Optional.empty();
  }

  /**
   * Returns the opposite direction, of identifier {@code id() ^ 1} (R35).
   *
   * @return the opposite direction
   */
  public Direction opposite() {
    return BY_ID.get(id() ^ 1);
  }

  /**
   * Returns the x component of the unit vector (R35).
   *
   * @return -1, 0 or 1
   */
  public int stepX() {
    return stepX;
  }

  /**
   * Returns the y component of the unit vector (R35).
   *
   * @return -1, 0 or 1
   */
  public int stepY() {
    return stepY;
  }

  /**
   * Returns the z component of the unit vector (R35).
   *
   * @return -1, 0 or 1
   */
  public int stepZ() {
    return stepZ;
  }

  /**
   * Returns the axis of the unit vector (R34, R35).
   *
   * @return the axis
   */
  public Axis axis() {
    if (stepX != 0) {
      return Axis.X;
    }
    return stepY != 0 ? Axis.Y : Axis.Z;
  }

  /**
   * Returns the name of this direction in the data: {@code down}, {@code up}, {@code north}, {@code
   * south}, {@code west} or {@code east} (R35).
   *
   * @return the lowercase name
   */
  public String serializedName() {
    return serializedName;
  }

  /**
   * Returns the 2D identifier of a horizontal direction: 0 south, 1 west, 2 north, 3 east (R36,
   * provisional until probe P35).
   *
   * @return the 2D identifier
   * @throws IllegalStateException for {@link #UP} and {@link #DOWN}
   */
  public int horizontalId() {
    int horizontalId = BY_HORIZONTAL_ID.indexOf(this);
    if (horizontalId < 0) {
      throw new IllegalStateException(this + " is not horizontal");
    }
    return horizontalId;
  }

  /**
   * Returns the horizontal direction with this 2D identifier (R36).
   *
   * @param horizontalId 2D identifier as read, possibly out of range
   * @return the direction, or empty outside {@code [0, 3]}
   */
  public static Optional<Direction> byHorizontalId(int horizontalId) {
    return horizontalId >= 0 && horizontalId < BY_HORIZONTAL_ID.size()
        ? Optional.of(BY_HORIZONTAL_ID.get(horizontalId))
        : Optional.empty();
  }

  /**
   * Returns the yaw of a horizontal direction in degrees: south 0, west 90, north 180, east -90
   * (R36).
   *
   * @return the yaw
   * @throws IllegalStateException for {@link #UP} and {@link #DOWN}
   */
  public double yaw() {
    // East is -90 rather than 270: R36 gives yaws in [-180, 180], north staying at 180.
    return this == EAST ? -DEGREES_PER_HORIZONTAL_ID : horizontalId() * DEGREES_PER_HORIZONTAL_ID;
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
    int quarterTurns = Rounding.floorToInt(yaw / DEGREES_PER_HORIZONTAL_ID + 0.5);
    return BY_HORIZONTAL_ID.get(quarterTurns & (BY_HORIZONTAL_ID.size() - 1));
  }
}

package dev.shoko.minecraft.core.coord;

/** Rotations of a structure template about the vertical axis (R38, provisional until probe P38). */
public enum Rotation {
  /** Name {@code none}: identity (R38). */
  NONE("none", 0),
  /**
   * Name {@code clockwise_90}: north to east, east to south, south to west, west to north (R38).
   */
  CLOCKWISE_90("clockwise_90", 1),
  /** Name {@code 180}: each horizontal direction to its opposite (R38). */
  CLOCKWISE_180("180", 2),
  /** Name {@code counterclockwise_90}: inverse of {@link #CLOCKWISE_90} (R38). */
  COUNTERCLOCKWISE_90("counterclockwise_90", 3);

  /** Quarter turns in a full turn. */
  private static final int QUARTERS_MASK = 3;

  /** Steps of the {@code rotation} block property in a quarter turn: 16 per full turn (R38). */
  private static final int SIXTEENTHS_PER_QUARTER = 4;

  private static final int SIXTEENTHS_MASK = 15;

  private final String serializedName;
  private final int clockwiseQuarters;

  Rotation(String serializedName, int clockwiseQuarters) {
    this.serializedName = serializedName;
    this.clockwiseQuarters = clockwiseQuarters;
  }

  /**
   * Returns the name of this rotation in commands: {@code none}, {@code clockwise_90}, {@code 180}
   * or {@code counterclockwise_90} (R38).
   *
   * @return the name
   */
  public String serializedName() {
    return serializedName;
  }

  /**
   * Rotates a direction; {@link Direction#UP} and {@link Direction#DOWN} are invariant (R38,
   * provisional).
   *
   * @param direction direction to rotate
   * @return the rotated direction
   */
  public Direction rotate(Direction direction) {
    if (!direction.axis().isHorizontal()) {
      return direction;
    }
    // 2D identifiers go south, west, north, east: one clockwise quarter turn adds 1 (R36, R38).
    int rotated = (direction.horizontalId() + clockwiseQuarters) & QUARTERS_MASK;
    return Direction.byHorizontalId(rotated).orElseThrow();
  }

  /**
   * Rotates a value of the 16-step {@code rotation} block property: a clockwise quarter turn adds 4
   * modulo 16, so this returns {@code floorMod(value + 4 * k, 16)} with {@code k} the number of
   * clockwise quarter turns, 0 to 3 (R38, provisional).
   *
   * @param value property value, normally in {@code [0, 15]}
   * @return the rotated value, in {@code [0, 15]}
   */
  public int rotateSixteenths(int value) {
    return (value + SIXTEENTHS_PER_QUARTER * clockwiseQuarters) & SIXTEENTHS_MASK;
  }
}

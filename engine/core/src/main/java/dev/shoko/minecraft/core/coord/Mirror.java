package dev.shoko.minecraft.core.coord;

/** Mirrors of a structure template (R38, provisional until probe P38). */
public enum Mirror {
  /** Name {@code none}: identity (R38). */
  NONE("none"),
  /** Name {@code left_right}: swaps north and south (R38). */
  LEFT_RIGHT("left_right"),
  /** Name {@code front_back}: swaps east and west (R38). */
  FRONT_BACK("front_back");

  private final String serializedName;

  Mirror(String serializedName) {
    this.serializedName = serializedName;
  }

  /**
   * Returns the name of this mirror in commands: {@code none}, {@code left_right} or {@code
   * front_back} (R38).
   *
   * @return the name
   */
  public String serializedName() {
    return serializedName;
  }

  /**
   * Mirrors a direction (R38, provisional). {@link Direction#UP} and {@link Direction#DOWN} are
   * invariant; {@link #LEFT_RIGHT} leaves east and west unchanged, {@link #FRONT_BACK} leaves north
   * and south unchanged.
   *
   * @param direction direction to mirror
   * @return the mirrored direction
   */
  public Direction mirror(Direction direction) {
    return switch (this) {
      case NONE -> direction;
      case LEFT_RIGHT -> direction.axis() == Axis.Z ? direction.opposite() : direction;
      case FRONT_BACK -> direction.axis() == Axis.X ? direction.opposite() : direction;
    };
  }
}

package dev.shoko.minecraft.core.coord;

/** Rotations of a structure template about the vertical axis (R38, provisional until probe P38). */
public enum Rotation {
  /** Name {@code none}: identity (R38). */
  NONE,
  /**
   * Name {@code clockwise_90}: north to east, east to south, south to west, west to north (R38).
   */
  CLOCKWISE_90,
  /** Name {@code 180}: each horizontal direction to its opposite (R38). */
  CLOCKWISE_180,
  /** Name {@code counterclockwise_90}: inverse of {@link #CLOCKWISE_90} (R38). */
  COUNTERCLOCKWISE_90;

  /**
   * Returns the name of this rotation in commands: {@code none}, {@code clockwise_90}, {@code 180}
   * or {@code counterclockwise_90} (R38).
   *
   * @return the name
   */
  public String serializedName() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Rotates a direction; {@link Direction#UP} and {@link Direction#DOWN} are invariant (R38,
   * provisional).
   *
   * @param direction direction to rotate
   * @return the rotated direction
   */
  public Direction rotate(Direction direction) {
    return Skeleton.throwNotImplemented();
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
    return Skeleton.throwNotImplemented();
  }
}

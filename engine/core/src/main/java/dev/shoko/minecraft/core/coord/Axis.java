package dev.shoko.minecraft.core.coord;

/** The three axes, named {@code x}, {@code y}, {@code z} in the data (R34). */
public enum Axis {
  /** East-west axis, horizontal (R34). */
  X("x"),
  /** Vertical axis (R34). */
  Y("y"),
  /** North-south axis, horizontal (R34). */
  Z("z");

  private final String serializedName;

  Axis(String serializedName) {
    this.serializedName = serializedName;
  }

  /**
   * Tells whether this axis is horizontal: {@code x} and {@code z} (R34).
   *
   * @return {@code true} for {@link #X} and {@link #Z}
   */
  public boolean isHorizontal() {
    return this != Y;
  }

  /**
   * Returns the name of this axis in the data: {@code x}, {@code y} or {@code z} (R34).
   *
   * @return the lowercase name
   */
  public String serializedName() {
    return serializedName;
  }
}

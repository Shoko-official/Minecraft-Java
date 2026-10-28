package dev.shoko.minecraft.data.pack;

/** Where a pack comes from, as the game shows it (R40, provisional). */
public enum PackOrigin {
  /** The vanilla pack, shown as {@code built-in}. */
  BUILT_IN,
  /** A pack shipped with the game that enables a feature flag, shown as {@code feature}. */
  FEATURE,
  /** A pack of the world {@code datapacks} folder, shown as {@code world}. */
  WORLD;

  /**
   * Returns the text the game shows for this origin.
   *
   * @return {@code built-in}, {@code feature} or {@code world}
   */
  public String label() {
    return switch (this) {
      case BUILT_IN -> "built-in";
      case FEATURE -> "feature";
      case WORLD -> "world";
    };
  }
}

package dev.shoko.minecraft.data.pack.meta;

/** Whether a pack suits the target game version (R5). */
public enum PackCompatibility {
  /** The target version lies inside the range of the pack. */
  COMPATIBLE,
  /** The pack was made for an older version: {@code max_format} is below the target. */
  TOO_OLD,
  /** The pack was made for a newer version: {@code min_format} is above the target. */
  TOO_NEW,
  /**
   * The range cannot be used: {@code min_format} above {@code max_format}, a missing or mistyped
   * field, or only {@code pack_format} (provisional, R5). The game says "broken or incompatible".
   */
  BROKEN
}

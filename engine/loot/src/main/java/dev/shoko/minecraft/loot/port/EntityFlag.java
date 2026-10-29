package dev.shoko.minecraft.loot.port;

/**
 * Entity flags tested by the {@code flags} predicate. Only the three flags met in the data are
 * listed (LT-R57a); the full list of flags is an open point.
 */
public enum EntityFlag {
  /** {@code is_on_fire}. */
  ON_FIRE,
  /** {@code is_on_ground}. */
  ON_GROUND,
  /** {@code is_flying}. */
  FLYING
}

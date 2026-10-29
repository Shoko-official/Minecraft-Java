package dev.shoko.minecraft.loot.port;

import java.util.Locale;
import java.util.Optional;

/**
 * Entity flags tested by the {@code flags} predicate. The first three are met in the loot tables
 * (LT-R57a), the others in the enchantments and the entity tables of the data; the JSON name of a
 * flag is {@code is_} followed by its lower-case name.
 */
public enum EntityFlag {
  /** {@code is_on_fire}. */
  ON_FIRE,
  /** {@code is_on_ground}. */
  ON_GROUND,
  /** {@code is_flying}. */
  FLYING,
  /** {@code is_fall_flying}: gliding with an elytra. */
  FALL_FLYING,
  /** {@code is_in_water}. */
  IN_WATER,
  /** {@code is_baby}. */
  BABY,
  /** {@code is_sneaking}. */
  SNEAKING,
  /** {@code is_sprinting}. */
  SPRINTING,
  /** {@code is_swimming}. */
  SWIMMING;

  /**
   * Returns the name used in the JSON.
   *
   * @return the name, such as {@code is_on_fire}
   */
  public String jsonName() {
    return "is_" + name().toLowerCase(Locale.ROOT);
  }

  /**
   * Finds a flag by its JSON name.
   *
   * @param name a name such as {@code is_on_fire}
   * @return the flag, empty when the name is not one of the flags
   */
  public static Optional<EntityFlag> ofJsonName(String name) {
    for (EntityFlag flag : values()) {
      if (flag.jsonName().equals(name)) {
        return Optional.of(flag);
      }
    }
    return Optional.empty();
  }
}

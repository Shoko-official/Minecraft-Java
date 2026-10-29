package dev.shoko.minecraft.loot.context;

/**
 * The named parameters of a loot context (LT-R12; names from the documentation and the data, so
 * provisional).
 */
public enum ContextKey {
  /** The position where the loot happens. */
  ORIGIN("origin"),
  /** The entity concerned: the killed mob, the player who opens, the floating hook. */
  THIS_ENTITY("this_entity"),
  /** The entity at the origin of the damage. */
  ATTACKER("attacker"),
  /** The entity that struck the blow, such as a projectile. */
  DIRECT_ATTACKER("direct_attacker"),
  /** The player credited with the kill. */
  ATTACKING_PLAYER("attacking_player"),
  /** The damage source. */
  DAMAGE_SOURCE("damage_source"),
  /** The tool stack. */
  TOOL("tool"),
  /** The state of the block. */
  BLOCK_STATE("block_state"),
  /** The block entity. */
  BLOCK_ENTITY("block_entity"),
  /** The radius of the explosion. */
  EXPLOSION_RADIUS("explosion_radius"),
  /** The level of the enchantment being evaluated. */
  ENCHANTMENT_LEVEL("enchantment_level"),
  /** Whether the enchantment being evaluated is active. */
  ENCHANTMENT_ACTIVE("enchantment_active"),
  /** The entity targeted by an interaction. */
  TARGET_ENTITY("target_entity"),
  /** The entity that interacts. */
  INTERACTING_ENTITY("interacting_entity");

  private final String key;

  ContextKey(String key) {
    this.key = key;
  }

  /**
   * Returns the name used in the documentation.
   *
   * @return the name, such as {@code this_entity}
   */
  public String key() {
    return key;
  }
}

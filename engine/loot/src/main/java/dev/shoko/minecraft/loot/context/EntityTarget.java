package dev.shoko.minecraft.loot.context;

/**
 * Which entity of the context a condition looks at: the {@code entity} field of {@code
 * entity_properties} (LT-R12, LT-R35). The first three are seen in the data, the others come from
 * the documentation.
 */
public enum EntityTarget {
  /** {@link ContextKey#THIS_ENTITY}. */
  THIS("this"),
  /** {@link ContextKey#ATTACKER}. */
  ATTACKER("attacker"),
  /** {@link ContextKey#DIRECT_ATTACKER}. */
  DIRECT_ATTACKER("direct_attacker"),
  /** {@link ContextKey#ATTACKING_PLAYER}. */
  ATTACKING_PLAYER("attacking_player"),
  /** {@link ContextKey#TARGET_ENTITY}. */
  TARGET_ENTITY("target_entity"),
  /** {@link ContextKey#INTERACTING_ENTITY}. */
  INTERACTING_ENTITY("interacting_entity");

  private final String key;

  EntityTarget(String key) {
    this.key = key;
  }

  /**
   * Returns the context parameter this target reads.
   *
   * @return the key, such as {@code THIS_ENTITY} for {@code THIS}
   */
  public ContextKey contextKey() {
    return switch (this) {
      case THIS -> ContextKey.THIS_ENTITY;
      case ATTACKER -> ContextKey.ATTACKER;
      case DIRECT_ATTACKER -> ContextKey.DIRECT_ATTACKER;
      case ATTACKING_PLAYER -> ContextKey.ATTACKING_PLAYER;
      case TARGET_ENTITY -> ContextKey.TARGET_ENTITY;
      case INTERACTING_ENTITY -> ContextKey.INTERACTING_ENTITY;
    };
  }

  /**
   * Returns the name used in the JSON.
   *
   * @return the name, such as {@code direct_attacker}
   */
  public String key() {
    return key;
  }
}

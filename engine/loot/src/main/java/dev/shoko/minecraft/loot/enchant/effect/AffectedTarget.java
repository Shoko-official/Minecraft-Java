package dev.shoko.minecraft.loot.enchant.effect;

/** Who suffers an entity effect: the {@code affected} key (EN-R14). */
public enum AffectedTarget {
  /** The key is absent; the default is open. */
  UNSET,
  /** {@code attacker}. */
  ATTACKER,
  /** {@code victim}. */
  VICTIM,
  /** {@code damaging_entity}. */
  DAMAGING_ENTITY
}

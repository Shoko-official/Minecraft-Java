package dev.shoko.minecraft.loot.enchant.effect;

/**
 * Who must carry the enchantment for an entity effect to run: the {@code enchanted} key (EN-R14).
 */
public enum EnchantedTarget {
  /** The key is absent; the default is open. */
  UNSET,
  /** {@code attacker}. */
  ATTACKER,
  /** {@code victim}. */
  VICTIM,
  /** {@code this}. */
  THIS
}

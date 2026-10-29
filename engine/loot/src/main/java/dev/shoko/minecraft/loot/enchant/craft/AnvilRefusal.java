package dev.shoko.minecraft.loot.enchant.craft;

/** Why the anvil gives no output. */
public enum AnvilRefusal {
  /** The target is empty, or the operation changes nothing. */
  NOTHING_TO_DO,
  /** The total cost is above 39 levels and the player is not in creative mode (EN-R41d). */
  TOO_EXPENSIVE
}

package dev.shoko.minecraft.item.menu;

/** What a slot of a menu is for, which decides what it accepts and how clicks treat it. */
public enum SlotRole {
  /** Accepts any stack: chest, crafting grid, hopper (IC-R37b). */
  STORAGE,
  /** Accepts what the machine admits, decided by {@link MachineRules} (IC-R38b). */
  INPUT,
  /** Takes nothing: crafting, furnace, anvil, trade outputs (IC-R38a). */
  RESULT,
  /** Head slot of the player window; accepts items whose {@code equippable.slot} is head. */
  ARMOR_HEAD,
  /** Chest slot of the player window. */
  ARMOR_CHEST,
  /** Legs slot of the player window. */
  ARMOR_LEGS,
  /** Feet slot of the player window. */
  ARMOR_FEET,
  /** Off hand slot of the player window. */
  OFFHAND,
  /** One of the 27 slots of the main inventory (IC-R37a). */
  PLAYER_MAIN,
  /** One of the 9 slots of the hotbar (IC-R37a). */
  PLAYER_HOTBAR
}

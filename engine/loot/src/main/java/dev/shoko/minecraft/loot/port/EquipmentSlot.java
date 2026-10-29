package dev.shoko.minecraft.loot.port;

/** A concrete equipment slot of an entity, as read by the {@code equipment} predicate (LT-R57a). */
public enum EquipmentSlot {
  /** Main hand. */
  MAINHAND,
  /** Off hand. */
  OFFHAND,
  /** Helmet slot. */
  HEAD,
  /** Chestplate slot. */
  CHEST,
  /** Leggings slot. */
  LEGS,
  /** Boots slot. */
  FEET,
  /** Body slot (EN-R6, documentation). */
  BODY
}

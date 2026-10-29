package dev.shoko.minecraft.loot.enchant;

import dev.shoko.minecraft.loot.port.EquipmentSlot;

/**
 * A group of equipment slots where an enchantment is active (EN-R6): the values of the {@code
 * slots} list of a definition.
 */
public enum SlotGroup {
  /** {@code mainhand}. */
  MAINHAND,
  /** {@code offhand}. */
  OFFHAND,
  /** {@code hand}: main or off hand. */
  HAND,
  /** {@code head}. */
  HEAD,
  /** {@code chest}. */
  CHEST,
  /** {@code legs}. */
  LEGS,
  /** {@code feet}. */
  FEET,
  /** {@code armor}: the four armor slots. */
  ARMOR,
  /** {@code any}: every slot. */
  ANY,
  /** {@code body} (documentation). */
  BODY;

  /**
   * Tells whether a concrete slot belongs to the group.
   *
   * @param slot an equipment slot
   * @return {@code true} when the group covers it
   */
  public boolean contains(EquipmentSlot slot) {
    return switch (this) {
      case MAINHAND -> slot == EquipmentSlot.MAINHAND;
      case OFFHAND -> slot == EquipmentSlot.OFFHAND;
      case HAND -> slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
      case HEAD -> slot == EquipmentSlot.HEAD;
      case CHEST -> slot == EquipmentSlot.CHEST;
      case LEGS -> slot == EquipmentSlot.LEGS;
      case FEET -> slot == EquipmentSlot.FEET;
      case ARMOR ->
          slot == EquipmentSlot.HEAD
              || slot == EquipmentSlot.CHEST
              || slot == EquipmentSlot.LEGS
              || slot == EquipmentSlot.FEET;
      case ANY -> true;
      case BODY -> slot == EquipmentSlot.BODY;
    };
  }
}

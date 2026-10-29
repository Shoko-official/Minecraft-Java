package dev.shoko.minecraft.item.container;

import java.util.OptionalInt;

/**
 * Conversion between the {@code Slot} Byte of the player's {@code Inventory} NBT and the index of
 * the player window (IC-R34, IC-R36). Under hypothesis H-inv-a: 0 to 8 is the hotbar, 9 to 35 the
 * main inventory, 100 to 103 the feet, legs, chest and head, -106 the off hand. Hypothesis H-inv-b
 * (equipment in a separate compound) is undecided until probe PI7; this class follows H-inv-a.
 */
public final class PlayerSlotSpace {

  private PlayerSlotSpace() {}

  /**
   * Converts an NBT slot to a window index.
   *
   * @param nbtSlot the {@code Slot} value
   * @return the index in the player window (IC-R36), empty for a value that names no slot
   */
  public static OptionalInt windowIndexOf(int nbtSlot) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Converts a window index to an NBT slot.
   *
   * @param windowIndex an index of the player window, from 0 to 45
   * @return the {@code Slot} value, empty for the result slot and the crafting grid, which are not
   *     stored in {@code Inventory}
   */
  public static OptionalInt nbtSlotOf(int windowIndex) {
    return Skeleton.throwNotImplemented();
  }
}

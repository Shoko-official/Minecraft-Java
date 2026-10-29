package dev.shoko.minecraft.item.container;

import java.util.OptionalInt;

/**
 * Conversion between the {@code Slot} Byte of the player's {@code Inventory} NBT and the index of
 * the player window (IC-R34, IC-R36). Under hypothesis H-inv-a: 0 to 8 is the hotbar, 9 to 35 the
 * main inventory, 100 to 103 the feet, legs, chest and head, -106 the off hand. Hypothesis H-inv-b
 * (equipment in a separate compound) is undecided until probe PI7; this class follows H-inv-a.
 */
public final class PlayerSlotSpace {

  private static final int HOTBAR_SIZE = 9;
  private static final int MAIN_END = 35;
  private static final int HOTBAR_WINDOW_START = 36;
  private static final int HOTBAR_WINDOW_END = 44;
  private static final int HEAD_WINDOW = 5;
  private static final int FEET_WINDOW = 8;
  private static final int OFFHAND_WINDOW = 45;
  private static final int FEET_NBT = 100;
  private static final int HEAD_NBT = 103;
  private static final int OFFHAND_NBT = -106;

  private PlayerSlotSpace() {}

  /**
   * Converts an NBT slot to a window index.
   *
   * @param nbtSlot the {@code Slot} value
   * @return the index in the player window (IC-R36), empty for a value that names no slot
   */
  public static OptionalInt windowIndexOf(int nbtSlot) {
    if (nbtSlot >= 0 && nbtSlot < HOTBAR_SIZE) {
      return OptionalInt.of(HOTBAR_WINDOW_START + nbtSlot);
    }
    if (nbtSlot >= HOTBAR_SIZE && nbtSlot <= MAIN_END) {
      return OptionalInt.of(nbtSlot);
    }
    if (nbtSlot >= FEET_NBT && nbtSlot <= HEAD_NBT) {
      return OptionalInt.of(FEET_WINDOW - (nbtSlot - FEET_NBT));
    }
    return nbtSlot == OFFHAND_NBT ? OptionalInt.of(OFFHAND_WINDOW) : OptionalInt.empty();
  }

  /**
   * Converts a window index to an NBT slot.
   *
   * @param windowIndex an index of the player window, from 0 to 45
   * @return the {@code Slot} value, empty for the result slot and the crafting grid, which are not
   *     stored in {@code Inventory}
   */
  public static OptionalInt nbtSlotOf(int windowIndex) {
    if (windowIndex >= HOTBAR_SIZE && windowIndex <= MAIN_END) {
      return OptionalInt.of(windowIndex);
    }
    if (windowIndex >= HOTBAR_WINDOW_START && windowIndex <= HOTBAR_WINDOW_END) {
      return OptionalInt.of(windowIndex - HOTBAR_WINDOW_START);
    }
    if (windowIndex >= HEAD_WINDOW && windowIndex <= FEET_WINDOW) {
      return OptionalInt.of(FEET_NBT + (FEET_WINDOW - windowIndex));
    }
    return windowIndex == OFFHAND_WINDOW ? OptionalInt.of(OFFHAND_NBT) : OptionalInt.empty();
  }
}

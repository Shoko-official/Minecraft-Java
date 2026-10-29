package dev.shoko.minecraft.item.menu.click;

import java.util.List;

/**
 * One operation of the player on a menu, independent of packet numbers (IC-R40a to IC-R47b). A slot
 * index outside the menu makes the operation a no-op ({@link ClickEffect#NO_CHANGE}).
 */
public sealed interface ClickOperation
    permits ClickOperation.Pickup,
        ClickOperation.QuickMove,
        ClickOperation.SwapWithHotbar,
        ClickOperation.SwapWithOffhand,
        ClickOperation.Throw,
        ClickOperation.ClickOutside,
        ClickOperation.Drag,
        ClickOperation.DoubleClick {

  /**
   * Plain click on a slot: take, put, swap or merge with the cursor (IC-R40a to IC-R41b).
   *
   * @param slot the slot index
   * @param button {@code LEFT} or {@code RIGHT}
   */
  record Pickup(int slot, MouseButton button) implements ClickOperation {}

  /**
   * Shift click: move the stack to the other part of the menu (IC-R42a to IC-R42d).
   *
   * @param slot the slot index
   */
  record QuickMove(int slot) implements ClickOperation {}

  /**
   * Number key: swap the slot with a hotbar slot (IC-R43a).
   *
   * @param slot the slot index
   * @param hotbarIndex the hotbar slot, from 0 to 8
   */
  record SwapWithHotbar(int slot, int hotbarIndex) implements ClickOperation {}

  /**
   * Off hand key: swap the slot with the off hand (IC-R43b).
   *
   * @param slot the slot index
   */
  record SwapWithOffhand(int slot) implements ClickOperation {}

  /**
   * Drop key: throw one unit or the whole stack of the hovered slot (IC-R44a, IC-R44b).
   *
   * @param slot the slot index
   * @param wholeStack {@code true} for the modified key
   */
  record Throw(int slot, boolean wholeStack) implements ClickOperation {}

  /**
   * Click outside the window with a stack on the cursor: throw it, or one unit with the right
   * button (IC-R44c).
   *
   * @param button {@code LEFT} or {@code RIGHT}
   */
  record ClickOutside(MouseButton button) implements ClickOperation {}

  /**
   * Drag over several slots with the cursor stack (IC-R45a to IC-R45d).
   *
   * @param button {@code LEFT} for equal shares, {@code RIGHT} for one unit each, {@code MIDDLE}
   *     for full stacks
   * @param slots the slots crossed, in order, without duplicates
   */
  record Drag(MouseButton button, List<Integer> slots) implements ClickOperation {

    /**
     * Copies the slots.
     *
     * @param button the button
     * @param slots the slots crossed
     */
    public Drag {
      slots = List.copyOf(slots);
    }
  }

  /**
   * Double click: gather the stacks of the same content onto the cursor (IC-R46).
   *
   * @param slot the slot index
   */
  record DoubleClick(int slot) implements ClickOperation {}
}

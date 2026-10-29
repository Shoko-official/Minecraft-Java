package dev.shoko.minecraft.item.menu;

import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * Decides whether a stack may be placed in a slot (IC-R38a to IC-R38c, provisional). The answer is:
 * never for {@link SlotRole#RESULT}; for an armor slot only when {@code equippable.slot} of the
 * stack names that slot and for no other armor slot; for {@link SlotRole#INPUT} what the {@link
 * MachineRules} say; always for the other roles. The empty stack is always accepted.
 */
public final class PlacementRules {

  private PlacementRules() {}

  /**
   * Tells whether a stack may be placed in a slot.
   *
   * @param layout the menu layout
   * @param slot the slot index
   * @param stack the stack to place
   * @param machine what the machine inputs accept
   * @return {@code true} when the placement is allowed
   * @throws IndexOutOfBoundsException when the slot is outside the layout
   */
  public static boolean mayPlace(
      MenuLayout layout, int slot, ItemStack stack, MachineRules machine) {
    return Skeleton.throwNotImplemented();
  }
}

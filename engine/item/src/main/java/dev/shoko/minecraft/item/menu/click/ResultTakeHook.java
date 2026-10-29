package dev.shoko.minecraft.item.menu.click;

import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * Port: what happens in the menu when a result stack is taken (IC-R47a). {@code game} implements it
 * with the recipes of {@code item}: consume the ingredients, give back the remainders, and
 * recompute the result slot for the new ingredients. The click algorithm only moves the stack.
 */
@FunctionalInterface
public interface ResultTakeHook {

  /**
   * Called after a result has been moved out of its slot.
   *
   * @param state the state with the result slot already emptied
   * @param resultSlot the slot index the stack came from
   * @param taken the stack that was taken
   * @return the state with the ingredients consumed and the result slot recomputed
   */
  MenuState afterResultTaken(MenuState state, int resultSlot, ItemStack taken);
}

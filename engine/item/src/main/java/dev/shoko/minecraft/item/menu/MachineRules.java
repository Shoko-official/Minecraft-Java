package dev.shoko.minecraft.item.menu;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * Port: what the {@link SlotRole#INPUT} slots of machines accept. {@code game} implements it from
 * the recipes, the fuels (evaluated by {@code loot}) and the state of the machine; this module
 * cannot, because the answer depends on providers it does not evaluate (ADR 0004, amendment 1).
 */
@FunctionalInterface
public interface MachineRules {

  /**
   * Tells whether an input slot accepts a stack (IC-R38b).
   *
   * @param menuType the menu type
   * @param slot the slot index in the layout of the menu
   * @param stack a non-empty stack
   * @return {@code true} when the stack may be placed
   */
  boolean acceptsInput(Identifier menuType, int slot, ItemStack stack);
}

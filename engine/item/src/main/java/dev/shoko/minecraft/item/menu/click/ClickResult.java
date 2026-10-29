package dev.shoko.minecraft.item.menu.click;

import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.List;

/**
 * The outcome of a click: the new state, what is thrown to the ground, and the effect. Units are
 * conserved: the total of the old slots and cursor equals the total of the new slots, cursor and
 * thrown stacks, except for results taken from a result slot (IC-R47a).
 *
 * @param state the state after the click; the unchanged state when {@code effect} is not {@code
 *     APPLIED}
 * @param thrown the stacks thrown to the ground, in order, empty when none
 * @param effect what the click did
 */
public record ClickResult(MenuState state, List<ItemStack> thrown, ClickEffect effect) {

  /**
   * Copies the thrown stacks.
   *
   * @param state the state after the click
   * @param thrown the stacks thrown to the ground
   * @param effect what the click did
   */
  public ClickResult {
    thrown = List.copyOf(thrown);
  }
}

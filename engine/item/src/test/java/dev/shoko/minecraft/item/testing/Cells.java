package dev.shoko.minecraft.item.testing;

import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * Stacks written as short texts: {@code "-"} is empty, {@code "stone"} one unit, {@code "stone:5"}
 * five.
 */
public final class Cells {

  private Cells() {}

  public static ItemStack stack(String cell) {
    if (cell.isEmpty() || cell.equals("-")) {
      return ItemStack.empty();
    }
    int colon = cell.indexOf(':');
    if (colon < 0) {
      return TestItems.stack(cell, 1);
    }
    return TestItems.stack(cell.substring(0, colon), Integer.parseInt(cell.substring(colon + 1)));
  }

  public static List<ItemStack> stacks(String... cells) {
    List<ItemStack> stacks = new ArrayList<>();
    for (String cell : cells) {
      stacks.add(stack(cell));
    }
    return stacks;
  }

  /** The cell text of a stack, the inverse of {@link #stack}, for readable failure messages. */
  public static String text(ItemStack stack) {
    return stack.isEmpty() ? "-" : stack.type().id().path() + ":" + stack.count();
  }
}

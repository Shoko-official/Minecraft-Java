package dev.shoko.minecraft.item.menu.click;

import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Drag over several slots with the cursor stack (IC-R45a to IC-R45d). Only the slots that can take
 * the cursor count: not a result, not a slot that refuses the stack, empty or of the same content
 * with room. What a slot cannot take of its share stays on the cursor.
 */
final class DragClick {

  private DragClick() {}

  static ClickEffect run(Workspace ws, MouseButton button, List<Integer> crossed) {
    ItemStack cursor = ws.cursor();
    if (cursor.isEmpty()) {
      return ClickEffect.NO_CHANGE;
    }
    List<Integer> slots = admissible(ws, cursor, crossed);
    if (slots.isEmpty()) {
      return ClickEffect.NO_CHANGE;
    }
    int deposited =
        switch (button) {
          case LEFT -> deposit(ws, slots, cursor, cursor.count() / slots.size());
          case RIGHT -> depositOneEach(ws, slots, cursor);
          case MIDDLE -> fillAll(ws, slots, cursor);
        };
    if (deposited == 0) {
      return ClickEffect.NO_CHANGE;
    }
    if (button != MouseButton.MIDDLE) {
      ws.setCursor(cursor.withCount(cursor.count() - deposited));
    }
    return ClickEffect.APPLIED;
  }

  private static List<Integer> admissible(Workspace ws, ItemStack cursor, List<Integer> crossed) {
    Set<Integer> unique = new LinkedHashSet<>(crossed);
    return unique.stream()
        .filter(ws::valid)
        .filter(slot -> !ws.isResult(slot))
        .filter(slot -> accepts(ws, slot, cursor))
        .toList();
  }

  private static boolean accepts(Workspace ws, int slot, ItemStack cursor) {
    ItemStack held = ws.get(slot);
    boolean joins =
        held.isEmpty() || (held.sameContent(cursor) && held.count() < cursor.maxStackSize());
    return joins && ws.mayPlace(slot, cursor);
  }

  private static int deposit(Workspace ws, List<Integer> slots, ItemStack cursor, int share) {
    int deposited = 0;
    for (int slot : slots) {
      deposited += add(ws, slot, cursor, share);
    }
    return deposited;
  }

  private static int depositOneEach(Workspace ws, List<Integer> slots, ItemStack cursor) {
    int deposited = 0;
    for (int slot : slots) {
      if (deposited < cursor.count()) {
        deposited += add(ws, slot, cursor, 1);
      }
    }
    return deposited;
  }

  private static int fillAll(Workspace ws, List<Integer> slots, ItemStack cursor) {
    int filled = 0;
    for (int slot : slots) {
      ws.set(slot, cursor.withCount(cursor.maxStackSize()));
      filled++;
    }
    return filled;
  }

  /** Adds up to {@code wanted} units of the cursor content to a slot and returns the amount. */
  private static int add(Workspace ws, int slot, ItemStack cursor, int wanted) {
    ItemStack held = ws.get(slot);
    int moved = Math.min(wanted, cursor.maxStackSize() - held.count());
    if (moved <= 0) {
      return 0;
    }
    ws.set(slot, cursor.withCount(held.count() + moved));
    return moved;
  }
}

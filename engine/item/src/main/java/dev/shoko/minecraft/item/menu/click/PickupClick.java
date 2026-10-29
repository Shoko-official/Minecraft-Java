package dev.shoko.minecraft.item.menu.click;

import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * Plain click on a slot: take, put, swap or merge with the cursor (IC-R40a to IC-R41b) and the
 * taking of a result (IC-R47a).
 */
final class PickupClick {

  private PickupClick() {}

  static ClickEffect run(Workspace ws, int slot, MouseButton button) {
    if (!ws.valid(slot) || button == MouseButton.MIDDLE) {
      return ClickEffect.NO_CHANGE;
    }
    if (ws.isResult(slot)) {
      return takeResult(ws, slot);
    }
    boolean left = button == MouseButton.LEFT;
    return ws.cursor().isEmpty() ? take(ws, slot, left) : put(ws, slot, left);
  }

  /** R40a, R41a: the cursor takes the whole stack, or its upper half. */
  private static ClickEffect take(Workspace ws, int slot, boolean left) {
    ItemStack held = ws.get(slot);
    if (held.isEmpty()) {
      return ClickEffect.NO_CHANGE;
    }
    int taken = left ? held.count() : (held.count() + 1) / 2;
    ws.setCursor(held.withCount(taken));
    ws.set(slot, held.withCount(held.count() - taken));
    return ClickEffect.APPLIED;
  }

  /** R40b to R40d, R41b: the cursor deposits, merges or swaps. */
  private static ClickEffect put(Workspace ws, int slot, boolean left) {
    ItemStack held = ws.get(slot);
    ItemStack cursor = ws.cursor();
    if (!held.isEmpty() && !held.sameContent(cursor)) {
      return swap(ws, slot);
    }
    if (held.isEmpty() && !ws.mayPlace(slot, cursor)) {
      return ClickEffect.NO_CHANGE;
    }
    int room = Math.max(0, cursor.maxStackSize() - held.count());
    int moved = Math.min(left ? cursor.count() : 1, room);
    if (moved == 0) {
      return ClickEffect.NO_CHANGE;
    }
    ws.set(slot, cursor.withCount(held.count() + moved));
    ws.setCursor(cursor.withCount(cursor.count() - moved));
    return ClickEffect.APPLIED;
  }

  private static ClickEffect swap(Workspace ws, int slot) {
    if (!ws.mayPlace(slot, ws.cursor())) {
      return ClickEffect.NO_CHANGE;
    }
    ItemStack held = ws.get(slot);
    ws.set(slot, ws.cursor());
    ws.setCursor(held);
    return ClickEffect.APPLIED;
  }

  /** The result goes whole to an empty cursor or onto a cursor of the same content with room. */
  private static ClickEffect takeResult(Workspace ws, int slot) {
    ItemStack result = ws.get(slot);
    ItemStack cursor = ws.cursor();
    if (result.isEmpty()) {
      return ClickEffect.NO_CHANGE;
    }
    if (!cursor.isEmpty() && !cursor.sameContent(result)) {
      return ClickEffect.NO_CHANGE;
    }
    int total = cursor.count() + result.count();
    if (total > result.maxStackSize()) {
      return ClickEffect.REFUSED;
    }
    ws.setCursor(result.withCount(total));
    ws.resultTaken(slot, result);
    return ClickEffect.APPLIED;
  }
}

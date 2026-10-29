package dev.shoko.minecraft.item.menu.click;

import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * Throwing to the ground: the drop key on a slot (IC-R44a, IC-R44b) and a click outside the window
 * with a stack on the cursor (IC-R44c). A result slot is not thrown from: its ingredients would
 * have to be consumed (IC-R47a), which only a result click does.
 */
final class ThrowClick {

  private ThrowClick() {}

  static ClickEffect fromSlot(Workspace ws, int slot, boolean wholeStack) {
    if (!ws.valid(slot) || ws.isResult(slot) || ws.get(slot).isEmpty()) {
      return ClickEffect.NO_CHANGE;
    }
    ItemStack held = ws.get(slot);
    int amount = wholeStack ? held.count() : 1;
    ws.throwAway(held.withCount(amount));
    ws.set(slot, held.withCount(held.count() - amount));
    return ClickEffect.APPLIED;
  }

  static ClickEffect fromCursor(Workspace ws, MouseButton button) {
    ItemStack cursor = ws.cursor();
    if (cursor.isEmpty() || button == MouseButton.MIDDLE) {
      return ClickEffect.NO_CHANGE;
    }
    int amount = button == MouseButton.LEFT ? cursor.count() : 1;
    ws.throwAway(cursor.withCount(amount));
    ws.setCursor(cursor.withCount(cursor.count() - amount));
    return ClickEffect.APPLIED;
  }
}

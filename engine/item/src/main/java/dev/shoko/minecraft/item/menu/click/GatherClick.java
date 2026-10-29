package dev.shoko.minecraft.item.menu.click;

import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * Double click (IC-R46, provisional): the stacks of the content of the cursor are gathered onto it
 * up to the stack size. The slots are visited in ascending order, the stacks that are not full
 * first; result slots are never touched.
 */
final class GatherClick {

  private GatherClick() {}

  static ClickEffect run(Workspace ws, int slot) {
    ItemStack start = ws.cursor();
    if (!ws.valid(slot) || start.isEmpty()) {
      return ClickEffect.NO_CHANGE;
    }
    int max = start.maxStackSize();
    int gathered = start.count();
    for (boolean full : new boolean[] {false, true}) {
      for (int visited = 0; visited < ws.size() && gathered < max; visited++) {
        gathered += take(ws, visited, start, max - gathered, full);
      }
    }
    if (gathered == start.count()) {
      return ClickEffect.NO_CHANGE;
    }
    ws.setCursor(start.withCount(gathered));
    return ClickEffect.APPLIED;
  }

  /** Takes up to {@code wanted} units from a slot of the same content, full stacks or not. */
  private static int take(Workspace ws, int slot, ItemStack model, int wanted, boolean full) {
    ItemStack held = ws.get(slot);
    boolean candidate =
        !ws.isResult(slot)
            && held.sameContent(model)
            && !held.isEmpty()
            && (held.count() >= held.maxStackSize()) == full;
    if (!candidate) {
      return 0;
    }
    int taken = Math.min(wanted, held.count());
    ws.set(slot, held.withCount(held.count() - taken));
    return taken;
  }
}

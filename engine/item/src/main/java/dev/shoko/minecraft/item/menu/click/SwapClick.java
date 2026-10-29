package dev.shoko.minecraft.item.menu.click;

import dev.shoko.minecraft.item.menu.SlotRole;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.List;

/**
 * Number key and off hand key: swap the hovered slot with a hotbar slot or the off hand (IC-R43a,
 * IC-R43b). When one of the two is empty the stack moves. A result slot is not swapped.
 */
final class SwapClick {

  private static final int HOTBAR_SIZE = 9;

  private SwapClick() {}

  static ClickEffect withHotbar(Workspace ws, int slot, int hotbarIndex) {
    List<Integer> hotbar = ws.indicesOf(SlotRole.PLAYER_HOTBAR);
    if (hotbarIndex < 0 || hotbarIndex >= HOTBAR_SIZE || hotbarIndex >= hotbar.size()) {
      return ClickEffect.NO_CHANGE;
    }
    return swap(ws, slot, hotbar.get(hotbarIndex));
  }

  static ClickEffect withOffhand(Workspace ws, int slot) {
    List<Integer> offhand = ws.indicesOf(SlotRole.OFFHAND);
    return offhand.isEmpty() ? ClickEffect.NO_CHANGE : swap(ws, slot, offhand.getFirst());
  }

  private static ClickEffect swap(Workspace ws, int slot, int other) {
    if (!ws.valid(slot) || slot == other || ws.isResult(slot)) {
      return ClickEffect.NO_CHANGE;
    }
    ItemStack here = ws.get(slot);
    ItemStack there = ws.get(other);
    if (here.isEmpty() && there.isEmpty()) {
      return ClickEffect.NO_CHANGE;
    }
    if (!ws.mayPlace(slot, there) || !ws.mayPlace(other, here)) {
      return ClickEffect.NO_CHANGE;
    }
    ws.set(slot, there);
    ws.set(other, here);
    return ClickEffect.APPLIED;
  }
}

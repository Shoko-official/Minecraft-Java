package dev.shoko.minecraft.item.menu.click;

import dev.shoko.minecraft.item.menu.MenuLayouts;
import dev.shoko.minecraft.item.menu.SlotRole;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shift click (IC-R42a to IC-R42d): the stack goes to the other part of the menu, first onto the
 * stacks it can join, then into empty slots. The per-menu tables of IC-R42e are not specified and
 * are not applied. A result moves whole or not at all (IC-R47b).
 */
final class QuickMove {

  private QuickMove() {}

  static ClickEffect run(Workspace ws, int slot) {
    if (!ws.valid(slot) || ws.get(slot).isEmpty()) {
      return ClickEffect.NO_CHANGE;
    }
    List<Integer> targets = targets(ws, slot);
    ItemStack source = ws.get(slot);
    if (ws.isResult(slot)) {
      return moveResult(ws, slot, targets);
    }
    ItemStack rest = insert(ws, targets, source);
    if (rest.count() == source.count()) {
      return ClickEffect.NO_CHANGE;
    }
    ws.set(slot, rest);
    return ClickEffect.APPLIED;
  }

  /** The slots that may receive the stack of a slot, in the order of filling. */
  private static List<Integer> targets(Workspace ws, int slot) {
    SlotRole role = ws.role(slot);
    boolean player = role == SlotRole.PLAYER_MAIN || role == SlotRole.PLAYER_HOTBAR;
    if (!player) {
      List<Integer> inventory = ws.indicesOf(SlotRole.PLAYER_MAIN, SlotRole.PLAYER_HOTBAR);
      Collections.reverse(inventory);
      return inventory;
    }
    if (isPlayerWindow(ws)) {
      return ws.indicesOf(
          role == SlotRole.PLAYER_MAIN ? SlotRole.PLAYER_HOTBAR : SlotRole.PLAYER_MAIN);
    }
    return ws.indicesOf(SlotRole.STORAGE, SlotRole.INPUT);
  }

  private static boolean isPlayerWindow(Workspace ws) {
    return ws.layout().menuType().equals(MenuLayouts.playerWindow().menuType());
  }

  private static ClickEffect moveResult(Workspace ws, int slot, List<Integer> targets) {
    ItemStack result = ws.get(slot);
    Workspace trial = ws.copy();
    if (!insert(trial, targets, result).isEmpty()) {
      return ClickEffect.REFUSED;
    }
    for (int target : targets) {
      ws.set(target, trial.get(target));
    }
    ws.resultTaken(slot, result);
    return ClickEffect.APPLIED;
  }

  /** Fills the targets in order with a stack and returns what did not fit. */
  private static ItemStack insert(Workspace ws, List<Integer> targets, ItemStack source) {
    ItemStack rest = source;
    List<Integer> empties = new ArrayList<>();
    for (int target : targets) {
      ItemStack held = ws.get(target);
      if (held.isEmpty()) {
        empties.add(target);
      } else if (held.sameContent(rest)) {
        rest = fill(ws, target, rest);
      }
    }
    for (int target : empties) {
      if (!rest.isEmpty() && ws.mayPlace(target, rest)) {
        rest = fill(ws, target, rest);
      }
    }
    return rest;
  }

  private static ItemStack fill(Workspace ws, int target, ItemStack source) {
    ItemStack held = ws.get(target);
    int moved = Math.min(source.count(), Math.max(0, source.maxStackSize() - held.count()));
    if (moved == 0) {
      return source;
    }
    ws.set(target, source.withCount(held.count() + moved));
    return source.withCount(source.count() - moved);
  }
}

package dev.shoko.minecraft.item.menu.click;

import dev.shoko.minecraft.item.menu.MenuLayout;
import dev.shoko.minecraft.item.menu.PlacementRules;
import dev.shoko.minecraft.item.menu.SlotRole;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * The mutable copy of a {@link MenuState} that one click works on: the slots, the cursor and the
 * stacks thrown so far. It lives for one call of {@link MenuEngine#click} and is never shared.
 */
final class Workspace {

  private final MenuLayout layout;
  private final MenuPorts ports;
  private final List<ItemStack> slots;
  private final List<ItemStack> thrown = new ArrayList<>();
  private ItemStack cursor;

  Workspace(MenuLayout layout, MenuPorts ports, MenuState state) {
    this.layout = layout;
    this.ports = ports;
    this.slots = new ArrayList<>(state.slots());
    this.cursor = state.cursor();
  }

  MenuLayout layout() {
    return layout;
  }

  int size() {
    return slots.size();
  }

  boolean valid(int slot) {
    return slot >= 0 && slot < slots.size();
  }

  ItemStack get(int slot) {
    return slots.get(slot);
  }

  void set(int slot, ItemStack stack) {
    slots.set(slot, stack);
  }

  ItemStack cursor() {
    return cursor;
  }

  void setCursor(ItemStack stack) {
    cursor = stack;
  }

  SlotRole role(int slot) {
    return layout.roles().get(slot);
  }

  boolean isResult(int slot) {
    return role(slot) == SlotRole.RESULT;
  }

  void throwAway(ItemStack stack) {
    thrown.add(stack);
  }

  /** Whether a stack may be placed in a slot (IC-R38a to IC-R38c). */
  boolean mayPlace(int slot, ItemStack stack) {
    return PlacementRules.mayPlace(layout, slot, stack, ports.machine());
  }

  /** The indices of the slots with a role, ascending. */
  List<Integer> indicesOf(SlotRole... roles) {
    List<Integer> found = new ArrayList<>();
    for (int slot = 0; slot < slots.size(); slot++) {
      for (SlotRole role : roles) {
        if (role(slot) == role) {
          found.add(slot);
        }
      }
    }
    return found;
  }

  /**
   * Lets the game consume the ingredients after a result left its slot (IC-R47a): the result slot
   * is emptied first, the hook returns the state to continue from.
   */
  void resultTaken(int slot, ItemStack taken) {
    slots.set(slot, ItemStack.empty());
    MenuState after = ports.onResultTaken().afterResultTaken(toState(), slot, taken);
    slots.clear();
    slots.addAll(after.slots());
    cursor = after.cursor();
  }

  Workspace copy() {
    return new Workspace(layout, ports, toState());
  }

  MenuState toState() {
    return new MenuState(slots, cursor);
  }

  List<ItemStack> thrown() {
    return thrown;
  }
}

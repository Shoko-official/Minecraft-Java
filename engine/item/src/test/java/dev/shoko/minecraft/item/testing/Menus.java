package dev.shoko.minecraft.item.testing;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.menu.MachineRules;
import dev.shoko.minecraft.item.menu.MenuLayout;
import dev.shoko.minecraft.item.menu.MenuLayouts;
import dev.shoko.minecraft.item.menu.click.ClickResult;
import dev.shoko.minecraft.item.menu.click.MenuEngine;
import dev.shoko.minecraft.item.menu.click.MenuPorts;
import dev.shoko.minecraft.item.menu.click.MenuState;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Helpers for the menu tests: layouts, states, engines and conservation counts. */
public final class Menus {

  /** Machine inputs accept everything. */
  public static final MachineRules ACCEPT_ALL = (menu, slot, stack) -> true;

  /** Machine inputs accept nothing. */
  public static final MachineRules ACCEPT_NONE = (menu, slot, stack) -> false;

  private Menus() {}

  public static MenuLayout layout(String menu) {
    return MenuLayouts.of(Identifier.minecraft(menu)).orElseThrow();
  }

  /** An engine whose machines accept everything and whose result hook changes nothing. */
  public static MenuEngine engine(MenuLayout layout) {
    return MenuEngine.of(layout, new MenuPorts(ACCEPT_ALL, (state, slot, taken) -> state));
  }

  public static MenuEngine engine(MenuLayout layout, MachineRules machine) {
    return MenuEngine.of(layout, new MenuPorts(machine, (state, slot, taken) -> state));
  }

  /** A state with an empty cursor; cells are {@link Cells} texts. */
  public static MenuState state(String... cells) {
    return new MenuState(Cells.stacks(cells), ItemStack.empty());
  }

  public static MenuState stateWithCursor(String cursor, String... cells) {
    return new MenuState(Cells.stacks(cells), Cells.stack(cursor));
  }

  /** A state with {@code size} slots: the given cells first, then empty slots. */
  public static MenuState padded(int size, String cursor, String... first) {
    List<ItemStack> slots = new ArrayList<>(Cells.stacks(first));
    while (slots.size() < size) {
      slots.add(ItemStack.empty());
    }
    return new MenuState(slots, Cells.stack(cursor));
  }

  public static MenuState withSlot(MenuState state, int slot, String cell) {
    List<ItemStack> slots = new ArrayList<>(state.slots());
    slots.set(slot, Cells.stack(cell));
    return new MenuState(slots, state.cursor());
  }

  /** The units of each item over slots, cursor and thrown stacks, by item path. */
  public static Map<String, Integer> totals(MenuState state, List<ItemStack> thrown) {
    Map<String, Integer> totals = new TreeMap<>();
    List<ItemStack> all = new ArrayList<>(state.slots());
    all.add(state.cursor());
    all.addAll(thrown);
    for (ItemStack stack : all) {
      if (!stack.isEmpty()) {
        totals.merge(stack.type().id().path(), stack.count(), Integer::sum);
      }
    }
    return totals;
  }

  public static Map<String, Integer> totals(ClickResult result) {
    return totals(result.state(), result.thrown());
  }

  public static Map<String, Integer> totals(MenuState state) {
    return totals(state, List.of());
  }

  /** The slots as {@link Cells} texts, for readable assertions. */
  public static List<String> texts(MenuState state) {
    return state.slots().stream().map(Cells::text).toList();
  }
}

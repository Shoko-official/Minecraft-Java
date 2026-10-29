package dev.shoko.minecraft.item.menu.click;

import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.Menus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Shared fixtures of the click tests. The chest layout is {@code generic_9x3}: own slots 0 to 26,
 * main inventory 27 to 53, hotbar 54 to 62 (IC-R37a, IC-R37b).
 */
final class ClickCases {

  static final int CHEST_SIZE = 63;
  static final int CHEST_MAIN = 27;
  static final int CHEST_HOTBAR = 54;

  private ClickCases() {}

  /** A state of {@code size} slots filled with {@code fill}, then the overrides written over. */
  static MenuState filled(int size, String cursor, String fill, Map<Integer, String> overrides) {
    String[] cells = new String[size];
    Arrays.fill(cells, fill);
    overrides.forEach((slot, cell) -> cells[slot] = cell);
    return Menus.stateWithCursor(cursor, cells);
  }

  /** A state of {@code size} slots, empty except for the given cells. */
  static MenuState sparse(int size, String cursor, Map<Integer, String> cells) {
    return filled(size, cursor, "-", cells);
  }

  static MenuState chest(String cursor, Map<Integer, String> cells) {
    return sparse(CHEST_SIZE, cursor, cells);
  }

  static MenuEngine chestEngine() {
    return Menus.engine(Menus.layout("generic_9x3"));
  }

  static String at(MenuState state, int slot) {
    return Cells.text(state.slots().get(slot));
  }

  static String cursor(MenuState state) {
    return Cells.text(state.cursor());
  }

  static List<String> thrown(ClickResult result) {
    return result.thrown().stream().map(Cells::text).toList();
  }

  /** The slot texts of {@code before} with some cells replaced. */
  static List<String> expected(MenuState before, Map<Integer, String> changes) {
    List<String> texts = new ArrayList<>(Menus.texts(before));
    changes.forEach(texts::set);
    return texts;
  }

  static ClickOperation left(int slot) {
    return new ClickOperation.Pickup(slot, MouseButton.LEFT);
  }

  static ClickOperation right(int slot) {
    return new ClickOperation.Pickup(slot, MouseButton.RIGHT);
  }

  static ClickOperation drag(MouseButton button, Integer... slots) {
    return new ClickOperation.Drag(button, List.of(slots));
  }
}

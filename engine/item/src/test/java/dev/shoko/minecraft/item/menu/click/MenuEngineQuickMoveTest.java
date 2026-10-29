package dev.shoko.minecraft.item.menu.click;

import static dev.shoko.minecraft.item.menu.click.ClickCases.at;
import static dev.shoko.minecraft.item.menu.click.ClickCases.chest;
import static dev.shoko.minecraft.item.menu.click.ClickCases.chestEngine;
import static dev.shoko.minecraft.item.menu.click.ClickCases.expected;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.menu.MenuLayouts;
import dev.shoko.minecraft.item.testing.Menus;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R42a to IC-R42d (level S: provisional). The chest is
// generic_9x3: own 0..26, main 27..53, hotbar 54..62; the player window has the main inventory at
// 9..35 and the hotbar at 36..44. Not asserted: IC-R42e (special menus have their own tables).
class MenuEngineQuickMoveTest {

  private static ClickResult quickMove(MenuState state, int slot) {
    return chestEngine().click(state, new ClickOperation.QuickMove(slot));
  }

  private static ClickResult windowQuickMove(MenuState state, int slot) {
    MenuEngine engine = Menus.engine(MenuLayouts.playerWindow());
    return engine.click(state, new ClickOperation.QuickMove(slot));
  }

  private static MenuState window(Map<Integer, String> cells) {
    return ClickCases.sparse(46, "-", cells);
  }

  @Test
  void provisionalMainInventoryMovesToTheFirstEmptyContainerSlot() {
    // spec: IC-R42a
    MenuState before = chest("-", Map.of(27, "stone:5"));

    ClickResult result = quickMove(before, 27);

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(0, "stone:5", 27, "-")));
    assertThat(result.thrown()).isEmpty();
  }

  @Test
  void provisionalHotbarAlsoMovesToTheContainer() {
    // spec: IC-R42a
    MenuState before = chest("-", Map.of(58, "stone:5", 0, "dirt:64"));

    ClickResult result = quickMove(before, 58);

    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(1, "stone:5", 58, "-")));
  }

  @Test
  void provisionalExistingStacksAreFilledBeforeEmptySlots() {
    // spec: IC-R42a (merge first into existing piles)
    MenuState before = chest("-", Map.of(5, "stone:10", 30, "stone:5"));

    ClickResult result = quickMove(before, 30);

    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(5, "stone:15", 30, "-")));
  }

  @Test
  void provisionalPartialStacksFillInAscendingOrderThenTheFirstEmptySlot() {
    // spec: IC-R42a
    MenuState before = chest("-", Map.of(3, "stone:60", 9, "stone:60", 30, "stone:10"));

    ClickResult result = quickMove(before, 30);

    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(3, "stone:64", 9, "stone:64", 0, "stone:2", 30, "-")));
  }

  @Test
  void provisionalEmptySlotsFillInAscendingOrder() {
    // spec: IC-R42a
    MenuState before = chest("-", Map.of(0, "cobblestone:64", 1, "cobblestone:64", 30, "stone:5"));

    ClickResult result = quickMove(before, 30);

    assertThat(at(result.state(), 2)).isEqualTo("stone:5");
    assertThat(at(result.state(), 30)).isEqualTo("-");
  }

  @Test
  void provisionalContainerMovesToTheLastHotbarSlotFirst() {
    // spec: IC-R42b (player range in descending order, hotbar first, right to left)
    MenuState before = chest("-", Map.of(0, "stone:5"));

    ClickResult result = quickMove(before, 0);

    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(0, "-", 62, "stone:5")));
  }

  @Test
  void provisionalContainerMergesIntoExistingPlayerStacksFirst() {
    // spec: IC-R42b (merge first)
    MenuState before = chest("-", Map.of(0, "stone:5", 40, "stone:10"));

    ClickResult result = quickMove(before, 0);

    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(0, "-", 40, "stone:15")));
  }

  @Test
  void provisionalPlayerRangeFillsDownwardsFromTheHotbarThroughTheMainInventory() {
    // spec: IC-R42b
    MenuState oneUsed = chest("-", Map.of(0, "stone:5", 62, "dirt:64"));
    assertThat(at(quickMove(oneUsed, 0).state(), 61)).isEqualTo("stone:5");

    Map<Integer, String> hotbarFull = new HashMap<>();
    for (int slot = ClickCases.CHEST_HOTBAR; slot < ClickCases.CHEST_SIZE; slot++) {
      hotbarFull.put(slot, "dirt:64");
    }
    hotbarFull.put(0, "stone:5");
    ClickResult result = quickMove(chest("-", hotbarFull), 0);
    assertThat(at(result.state(), 53)).isEqualTo("stone:5");
  }

  @Test
  void provisionalWhatDoesNotFitStaysInTheSourceSlot() {
    // spec: IC-R42d
    Map<Integer, String> cells = new HashMap<>();
    for (int slot = 0; slot < 27; slot++) {
      cells.put(slot, "dirt:64");
    }
    cells.put(2, "stone:60");
    cells.put(27, "stone:10");
    MenuState before = chest("-", cells);

    ClickResult result = quickMove(before, 27);

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(at(result.state(), 2)).isEqualTo("stone:64");
    assertThat(at(result.state(), 27)).isEqualTo("stone:6");
    assertThat(Menus.totals(result)).isEqualTo(Menus.totals(before));
  }

  @Test
  void provisionalNothingMovesWhenNothingFits() {
    // spec: IC-R42d
    Map<Integer, String> cells = new HashMap<>();
    for (int slot = 0; slot < 27; slot++) {
      cells.put(slot, "dirt:64");
    }
    cells.put(27, "stone:10");
    MenuState before = chest("-", cells);

    ClickResult result = quickMove(before, 27);

    assertThat(result.state()).isEqualTo(before);
    assertThat(result.effect()).isIn(ClickEffect.NO_CHANGE, ClickEffect.REFUSED);
    assertThat(result.thrown()).isEmpty();
  }

  @Test
  void provisionalQuickMovingAnEmptySlotChangesNothing() {
    // spec: IC-R42a
    MenuState before = chest("-", Map.of(4, "dirt:3"));

    ClickResult result = quickMove(before, 27);

    assertThat(result.effect()).isEqualTo(ClickEffect.NO_CHANGE);
    assertThat(result.state()).isEqualTo(before);
  }

  @Test
  void provisionalAResultSlotNeverReceivesQuickMovedStacks() {
    // spec: IC-R38a, IC-R42a (the furnace result is the third slot)
    MenuEngine engine = Menus.engine(Menus.layout("furnace"));
    MenuState before = ClickCases.sparse(39, "-", Map.of(0, "dirt:64", 1, "dirt:64", 3, "stone:5"));

    ClickResult result = engine.click(before, new ClickOperation.QuickMove(3));

    assertThat(at(result.state(), 2)).isEqualTo("-");
    assertThat(Menus.totals(result)).isEqualTo(Menus.totals(before));
  }

  @Test
  void provisionalPlayerWindowHotbarMovesToTheMainInventoryInAscendingOrder() {
    // spec: IC-R42c
    MenuState before = window(Map.of(36, "stone:5", 9, "dirt:64"));

    ClickResult result = windowQuickMove(before, 36);

    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(36, "-", 10, "stone:5")));
  }

  @Test
  void provisionalPlayerWindowMainInventoryMovesToTheHotbarInAscendingOrder() {
    // spec: IC-R42c
    MenuState before = window(Map.of(10, "stone:5", 36, "dirt:64"));

    ClickResult result = windowQuickMove(before, 10);

    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(10, "-", 37, "stone:5")));
  }

  @Test
  void provisionalPlayerWindowMergesIntoExistingStacksFirst() {
    // spec: IC-R42c, IC-R42a (merge first)
    MenuState before = window(Map.of(10, "stone:5", 40, "stone:3"));

    ClickResult result = windowQuickMove(before, 10);

    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(10, "-", 40, "stone:8")));
  }

  @Test
  void provisionalPlayerWindowKeepsTheRestInTheSourceWhenItDoesNotFit() {
    // spec: IC-R42c, IC-R42d
    Map<Integer, String> cells = new HashMap<>();
    for (int slot = 36; slot <= 44; slot++) {
      cells.put(slot, "dirt:64");
    }
    cells.put(40, "stone:62");
    cells.put(10, "stone:5");
    MenuState before = window(cells);

    ClickResult result = windowQuickMove(before, 10);

    assertThat(at(result.state(), 40)).isEqualTo("stone:64");
    assertThat(at(result.state(), 10)).isEqualTo("stone:3");
  }
}

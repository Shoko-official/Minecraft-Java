package dev.shoko.minecraft.item.menu.click;

import static dev.shoko.minecraft.item.menu.click.ClickCases.at;
import static dev.shoko.minecraft.item.menu.click.ClickCases.chest;
import static dev.shoko.minecraft.item.menu.click.ClickCases.chestEngine;
import static dev.shoko.minecraft.item.menu.click.ClickCases.cursor;
import static dev.shoko.minecraft.item.menu.click.ClickCases.expected;
import static dev.shoko.minecraft.item.menu.click.ClickCases.left;
import static dev.shoko.minecraft.item.menu.click.ClickCases.right;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.Menus;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R40a to IC-R41b, V-I10, V-I13 (level S: provisional).
// Not asserted: the right click with a cursor on a slot of different content (R41b only lists the
// empty slot and the same content).
class MenuEnginePickupTest {

  @Test
  void provisionalLeftClickWithEmptyCursorTakesTheWholeStack() {
    // spec: IC-R40a
    MenuState before = chest("-", Map.of(3, "stone:7", 4, "dirt:2"));

    ClickResult result = chestEngine().click(before, left(3));

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(cursor(result.state())).isEqualTo("stone:7");
    assertThat(Menus.texts(result.state())).isEqualTo(expected(before, Map.of(3, "-")));
    assertThat(result.thrown()).isEmpty();
  }

  @Test
  void provisionalLeftClickOnAnEmptySlotWithEmptyCursorChangesNothing() {
    // spec: IC-R40a
    MenuState before = chest("-", Map.of(4, "dirt:2"));

    ClickResult result = chestEngine().click(before, left(3));

    assertThat(result.effect()).isEqualTo(ClickEffect.NO_CHANGE);
    assertThat(result.state()).isEqualTo(before);
  }

  @Test
  void provisionalLeftClickPlacesTheWholeCursorInAnEmptySlot() {
    // spec: IC-R40b
    MenuState before = chest("stone:5", Map.of(4, "dirt:2"));

    ClickResult result = chestEngine().click(before, left(3));

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(at(result.state(), 3)).isEqualTo("stone:5");
    assertThat(cursor(result.state())).isEqualTo("-");
    assertThat(at(result.state(), 4)).isEqualTo("dirt:2");
  }

  @Test
  void provisionalLeftClickMergesAndTheRestStaysOnTheCursor() {
    // spec: IC-R40c, IC-R19, V-I13
    MenuState before = chest("stone:64", Map.of(3, "stone:60"));

    ClickResult result = chestEngine().click(before, left(3));

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(at(result.state(), 3)).isEqualTo("stone:64");
    assertThat(cursor(result.state())).isEqualTo("stone:60");
  }

  @Test
  void provisionalLeftClickMergesCompletelyWhenItFits() {
    // spec: IC-R40c
    MenuState before = chest("stone:3", Map.of(3, "stone:10"));

    ClickResult result = chestEngine().click(before, left(3));

    assertThat(at(result.state(), 3)).isEqualTo("stone:13");
    assertThat(cursor(result.state())).isEqualTo("-");
  }

  @Test
  void provisionalMergeStopsAtTheStackSizeOfTheItem() {
    // spec: IC-R40c, IC-R19 (ender pearls stack to 16)
    MenuState before = chest("ender_pearl:16", Map.of(3, "ender_pearl:10"));

    ClickResult result = chestEngine().click(before, left(3));

    assertThat(at(result.state(), 3)).isEqualTo("ender_pearl:16");
    assertThat(cursor(result.state())).isEqualTo("ender_pearl:10");
  }

  @Test
  void provisionalLeftClickOnAFullSlotOfTheSameContentChangesNothing() {
    // spec: IC-R40c, IC-R19
    MenuState before = chest("stone:5", Map.of(3, "stone:64"));

    ClickResult result = chestEngine().click(before, left(3));

    assertThat(result.state()).isEqualTo(before);
    assertThat(result.effect()).isNotEqualTo(ClickEffect.APPLIED);
  }

  @Test
  void provisionalLeftClickSwapsDifferentContents() {
    // spec: IC-R40d
    MenuState before = chest("dirt:5", Map.of(3, "stone:7"));

    ClickResult result = chestEngine().click(before, left(3));

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(at(result.state(), 3)).isEqualTo("dirt:5");
    assertThat(cursor(result.state())).isEqualTo("stone:7");
  }

  @Test
  void provisionalStacksThatDifferOnlyByComponentsSwapInsteadOfMerging() {
    // spec: IC-R40d, IC-R19 (a different rarity is a different content)
    ItemStack epic = TestItems.stack("stone", 5, "rarity", "'epic'");
    MenuState base = chest("-", Map.of(3, "stone:10"));
    MenuState before = new MenuState(base.slots(), epic);

    ClickResult result = chestEngine().click(before, left(3));

    assertThat(result.state().slots().get(3)).isEqualTo(epic);
    assertThat(result.state().cursor()).isEqualTo(Cells.stack("stone:10"));
  }

  @Test
  void provisionalRightClickWithEmptyCursorTakesTheUpperHalf() {
    // spec: IC-R41a, V-I10
    MenuState before = chest("-", Map.of(3, "stone:7"));

    ClickResult result = chestEngine().click(before, right(3));

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(cursor(result.state())).isEqualTo("stone:4");
    assertThat(at(result.state(), 3)).isEqualTo("stone:3");
  }

  @Test
  void provisionalRightClickHalvesStacksOfEvenAndUnitSize() {
    // spec: IC-R41a (upper half)
    List<String> cases = List.of("stone:64", "stone:2", "stone:1", "stone:9");
    List<Integer> taken = List.of(32, 1, 1, 5);
    for (int i = 0; i < cases.size(); i++) {
      MenuState before = chest("-", Map.of(3, cases.get(i)));
      ClickResult result = chestEngine().click(before, right(3));
      int expectedTaken = taken.get(i);
      int total = Cells.stack(cases.get(i)).count();
      assertThat(result.state().cursor().count()).as(cases.get(i)).isEqualTo(expectedTaken);
      assertThat(result.state().slots().get(3).count())
          .as(cases.get(i))
          .isEqualTo(total - expectedTaken);
    }
  }

  @Test
  void provisionalRightClickDepositsOneUnitInAnEmptySlot() {
    // spec: IC-R41b
    MenuState before = chest("stone:5", Map.of());

    ClickResult result = chestEngine().click(before, right(3));

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(at(result.state(), 3)).isEqualTo("stone:1");
    assertThat(cursor(result.state())).isEqualTo("stone:4");
  }

  @Test
  void provisionalRightClickAddsOneUnitToTheSameContent() {
    // spec: IC-R41b
    MenuState before = chest("stone:5", Map.of(3, "stone:10"));

    ClickResult result = chestEngine().click(before, right(3));

    assertThat(at(result.state(), 3)).isEqualTo("stone:11");
    assertThat(cursor(result.state())).isEqualTo("stone:4");
  }

  @Test
  void provisionalRightClickEmptiesTheCursorWithItsLastUnit() {
    // spec: IC-R41b
    MenuState before = chest("stone:1", Map.of());

    ClickResult result = chestEngine().click(before, right(3));

    assertThat(at(result.state(), 3)).isEqualTo("stone:1");
    assertThat(cursor(result.state())).isEqualTo("-");
  }

  @Test
  void provisionalRightClickOnAFullSlotOfTheSameContentChangesNothing() {
    // spec: IC-R41b ("with room")
    MenuState before = chest("stone:5", Map.of(3, "stone:64"));

    ClickResult result = chestEngine().click(before, right(3));

    assertThat(result.state()).isEqualTo(before);
    assertThat(result.effect()).isNotEqualTo(ClickEffect.APPLIED);
  }

  @Test
  void provisionalARefusingInputSlotKeepsItsStackWhenTheCursorCannotBePlaced() {
    // spec: IC-R38b, engine contract (a refused placement leaves the slot as it was)
    MenuEngine engine = Menus.engine(Menus.layout("furnace"), Menus.ACCEPT_NONE);
    MenuState before = ClickCases.sparse(39, "stone:5", Map.of(0, "dirt:2"));

    ClickResult result = engine.click(before, left(0));

    assertThat(at(result.state(), 0)).isEqualTo("dirt:2");
    assertThat(cursor(result.state())).isEqualTo("stone:5");
    assertThat(result.effect()).isNotEqualTo(ClickEffect.APPLIED);
  }

  @Test
  void provisionalAnInputSlotAcceptsWhatTheMachineAdmits() {
    // spec: IC-R38b (port MachineRules, here: only stone)
    MenuEngine engine =
        Menus.engine(
            Menus.layout("furnace"),
            (menu, slot, stack) -> stack.type().id().path().equals("stone"));
    MenuState stoneCursor = ClickCases.sparse(39, "stone:5", Map.of());
    MenuState dirtCursor = ClickCases.sparse(39, "dirt:5", Map.of());

    ClickResult accepted = engine.click(stoneCursor, left(0));
    ClickResult refused = engine.click(dirtCursor, left(0));

    assertThat(at(accepted.state(), 0)).isEqualTo("stone:5");
    assertThat(refused.state()).isEqualTo(dirtCursor);
  }

  @Test
  void provisionalAClickLeavesEveryOtherSlotAlone() {
    // spec: IC-R40a to IC-R41b
    Map<Integer, String> cells = new HashMap<>();
    for (int i = 0; i < ClickCases.CHEST_SIZE; i += 5) {
      cells.put(i, "dirt:" + (i + 1));
    }
    cells.put(3, "stone:7");
    MenuState before = chest("-", cells);

    ClickResult result = chestEngine().click(before, left(3));

    assertThat(Menus.texts(result.state())).isEqualTo(expected(before, Map.of(3, "-")));
    assertThat(Menus.totals(result)).isEqualTo(Menus.totals(before));
  }
}

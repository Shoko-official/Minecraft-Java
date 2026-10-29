package dev.shoko.minecraft.item.menu.click;

import static dev.shoko.minecraft.item.menu.click.ClickCases.at;
import static dev.shoko.minecraft.item.menu.click.ClickCases.chest;
import static dev.shoko.minecraft.item.menu.click.ClickCases.chestEngine;
import static dev.shoko.minecraft.item.menu.click.ClickCases.cursor;
import static dev.shoko.minecraft.item.menu.click.ClickCases.drag;
import static dev.shoko.minecraft.item.menu.click.ClickCases.expected;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.testing.Menus;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R45a to IC-R45d, V-I11, V-I12 (level S: provisional).
// Not asserted: which slots receive a unit when a right drag crosses more slots than the cursor
// has units, and what a drag does when it lists a slot outside the menu.
class MenuEngineDragTest {

  @Test
  void provisionalLeftDragSharesTheCursorInEqualParts() {
    // spec: IC-R45a, V-I11
    MenuState before = chest("stone:10", Map.of());

    ClickResult result = chestEngine().click(before, drag(MouseButton.LEFT, 0, 1, 2));

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(0, "stone:3", 1, "stone:3", 2, "stone:3")));
    assertThat(cursor(result.state())).isEqualTo("stone:1");
    assertThat(result.thrown()).isEmpty();
  }

  @Test
  void provisionalLeftDragOnOneSlotDepositsTheWholeStack() {
    // spec: IC-R45a (n = 1 is a full deposit)
    MenuState before = chest("stone:10", Map.of());

    ClickResult result = chestEngine().click(before, drag(MouseButton.LEFT, 4));

    assertThat(at(result.state(), 4)).isEqualTo("stone:10");
    assertThat(cursor(result.state())).isEqualTo("-");
  }

  @Test
  void provisionalLeftDragWithAnExactMultipleEmptiesTheCursor() {
    // spec: IC-R45a
    MenuState before = chest("stone:12", Map.of());

    ClickResult result = chestEngine().click(before, drag(MouseButton.LEFT, 0, 1, 2, 3));

    for (int slot = 0; slot < 4; slot++) {
      assertThat(at(result.state(), slot)).isEqualTo("stone:3");
    }
    assertThat(cursor(result.state())).isEqualTo("-");
  }

  @Test
  void provisionalLeftDragAddsToSlotsOfTheSameContent() {
    // spec: IC-R45a (empty slots or same content with room)
    MenuState before = chest("stone:10", Map.of(0, "stone:20"));

    ClickResult result = chestEngine().click(before, drag(MouseButton.LEFT, 0, 1));

    assertThat(at(result.state(), 0)).isEqualTo("stone:25");
    assertThat(at(result.state(), 1)).isEqualTo("stone:5");
    assertThat(cursor(result.state())).isEqualTo("-");
  }

  @Test
  void provisionalLeftDragSkipsSlotsOfOtherContent() {
    // spec: IC-R45a (only admissible slots count)
    MenuState before = chest("stone:10", Map.of(0, "dirt:3"));

    ClickResult result = chestEngine().click(before, drag(MouseButton.LEFT, 0, 1));

    assertThat(at(result.state(), 0)).isEqualTo("dirt:3");
    assertThat(at(result.state(), 1)).isEqualTo("stone:10");
    assertThat(cursor(result.state())).isEqualTo("-");
  }

  @Test
  void provisionalLeftDragWithSmallerSharesThanSlotsDepositsNothing() {
    // spec: IC-R45a (floor(2 / 3) = 0 per slot, the rest stays on the cursor)
    MenuState before = chest("stone:2", Map.of());

    ClickResult result = chestEngine().click(before, drag(MouseButton.LEFT, 0, 1, 2));

    assertThat(cursor(result.state())).isEqualTo("stone:2");
    assertThat(Menus.totals(result)).isEqualTo(Menus.totals(before));
  }

  @Test
  void provisionalLeftDragKeepsOverflowOfAFullishSlotOnTheCursor() {
    // spec: IC-R45a. Hypothesis named here: what a slot cannot take of its share stays on the
    // cursor (the spec says the rest of the division stays; capacities differ per slot).
    MenuState before = chest("stone:10", Map.of(0, "stone:62"));

    ClickResult result = chestEngine().click(before, drag(MouseButton.LEFT, 0, 1));

    assertThat(result.state().slots().get(0).count()).isLessThanOrEqualTo(64);
    assertThat(at(result.state(), 1)).isEqualTo("stone:5");
    assertThat(Menus.totals(result)).isEqualTo(Menus.totals(before));
  }

  @Test
  void provisionalRightDragDepositsOneUnitPerSlot() {
    // spec: IC-R45b, V-I12
    MenuState before = chest("stone:10", Map.of());

    ClickResult result = chestEngine().click(before, drag(MouseButton.RIGHT, 0, 1, 2));

    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(0, "stone:1", 1, "stone:1", 2, "stone:1")));
    assertThat(cursor(result.state())).isEqualTo("stone:7");
  }

  @Test
  void provisionalRightDragAddsOneUnitToSlotsOfTheSameContent() {
    // spec: IC-R45b
    MenuState before = chest("stone:10", Map.of(0, "stone:20"));

    ClickResult result = chestEngine().click(before, drag(MouseButton.RIGHT, 0, 1));

    assertThat(at(result.state(), 0)).isEqualTo("stone:21");
    assertThat(at(result.state(), 1)).isEqualTo("stone:1");
    assertThat(cursor(result.state())).isEqualTo("stone:8");
  }

  @Test
  void provisionalRightDragIgnoresFullSlots() {
    // spec: IC-R45b (an admissible slot has room)
    MenuState before = chest("stone:10", Map.of(0, "stone:64"));

    ClickResult result = chestEngine().click(before, drag(MouseButton.RIGHT, 0, 1));

    assertThat(at(result.state(), 0)).isEqualTo("stone:64");
    assertThat(at(result.state(), 1)).isEqualTo("stone:1");
    assertThat(cursor(result.state())).isEqualTo("stone:9");
  }

  @Test
  void provisionalRightDragNeverCreatesUnits() {
    // spec: IC-R45b (more slots than units)
    MenuState before = chest("stone:2", Map.of());

    ClickResult result = chestEngine().click(before, drag(MouseButton.RIGHT, 0, 1, 2, 3));

    assertThat(Menus.totals(result)).isEqualTo(Menus.totals(before));
  }

  @Test
  void provisionalMiddleDragPutsAFullStackInEachAdmissibleSlot() {
    // spec: IC-R45c (creative). The cursor after the drag is not specified: not asserted.
    MenuState before = chest("stone:1", Map.of(2, "dirt:5"));

    ClickResult result = chestEngine().click(before, drag(MouseButton.MIDDLE, 0, 1, 2));

    assertThat(at(result.state(), 0)).isEqualTo("stone:64");
    assertThat(at(result.state(), 1)).isEqualTo("stone:64");
    assertThat(at(result.state(), 2)).isEqualTo("dirt:5");
  }

  @Test
  void provisionalADragIgnoresAResultSlot() {
    // spec: IC-R45d, IC-R38a (furnace: inputs 0 and 1, result 2)
    MenuEngine engine = Menus.engine(Menus.layout("furnace"));
    MenuState before = ClickCases.sparse(39, "stone:9", Map.of());

    ClickResult result = engine.click(before, drag(MouseButton.LEFT, 0, 2, 1));

    assertThat(at(result.state(), 0)).isEqualTo("stone:4");
    assertThat(at(result.state(), 1)).isEqualTo("stone:4");
    assertThat(at(result.state(), 2)).isEqualTo("-");
    assertThat(cursor(result.state())).isEqualTo("stone:1");
  }

  @Test
  void provisionalADragIgnoresSlotsTheMachineRefuses() {
    // spec: IC-R45d (filtered slot)
    MenuEngine engine = Menus.engine(Menus.layout("furnace"), (menu, slot, stack) -> slot == 1);
    MenuState before = ClickCases.sparse(39, "stone:9", Map.of());

    ClickResult result = engine.click(before, drag(MouseButton.LEFT, 0, 1));

    assertThat(at(result.state(), 0)).isEqualTo("-");
    assertThat(at(result.state(), 1)).isEqualTo("stone:9");
    assertThat(cursor(result.state())).isEqualTo("-");
  }

  @Test
  void provisionalADragOnlyOverRefusingSlotsChangesNothing() {
    // spec: IC-R45d
    MenuEngine engine = Menus.engine(Menus.layout("furnace"), Menus.ACCEPT_NONE);
    MenuState before = ClickCases.sparse(39, "stone:9", Map.of());

    for (MouseButton button : MouseButton.values()) {
      ClickResult result = engine.click(before, drag(button, 0, 1, 2));
      assertThat(result.state()).isEqualTo(before);
      assertThat(result.effect()).isEqualTo(ClickEffect.NO_CHANGE);
    }
  }

  @Test
  void provisionalADragWithAnEmptyCursorChangesNothing() {
    // spec: IC-R45a, IC-R45b
    MenuState before = chest("-", Map.of(0, "stone:5"));

    for (MouseButton button : new MouseButton[] {MouseButton.LEFT, MouseButton.RIGHT}) {
      ClickResult result = chestEngine().click(before, drag(button, 0, 1));
      assertThat(result.state()).isEqualTo(before);
      assertThat(result.effect()).isEqualTo(ClickEffect.NO_CHANGE);
    }
  }

  @Test
  void provisionalLeftDragRespectsTheStackSizeOfTheItem() {
    // spec: IC-R45a, IC-R19 (ender pearls stack to 16)
    MenuState before = chest("ender_pearl:16", Map.of(0, "ender_pearl:15"));

    ClickResult result = chestEngine().click(before, drag(MouseButton.LEFT, 0, 1));

    assertThat(result.state().slots().get(0).count()).isLessThanOrEqualTo(16);
    assertThat(Menus.totals(result)).isEqualTo(Menus.totals(before));
  }
}

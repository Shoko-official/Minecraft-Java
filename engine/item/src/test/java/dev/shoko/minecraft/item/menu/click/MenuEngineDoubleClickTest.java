package dev.shoko.minecraft.item.menu.click;

import static dev.shoko.minecraft.item.menu.click.ClickCases.at;
import static dev.shoko.minecraft.item.menu.click.ClickCases.chest;
import static dev.shoko.minecraft.item.menu.click.ClickCases.chestEngine;
import static dev.shoko.minecraft.item.menu.click.ClickCases.cursor;
import static dev.shoko.minecraft.item.menu.click.ClickCases.expected;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.testing.Menus;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R46 (level S: provisional). The provisional rule: the slots of
// the menu are visited in ascending order, the stacks that are not full first, the result slots
// are never touched, and the cursor stops at the stack size of its item. The cursor holds the
// stack picked up by the first click of the double click; the clicked slot is an empty one here.
class MenuEngineDoubleClickTest {

  private static ClickResult doubleClick(MenuState state) {
    return chestEngine().click(state, new ClickOperation.DoubleClick(0));
  }

  @Test
  void provisionalGathersStacksOfTheSameContentOntoTheCursor() {
    // spec: IC-R46
    MenuState before = chest("stone:10", Map.of(3, "stone:20", 7, "stone:5", 8, "dirt:30"));

    ClickResult result = doubleClick(before);

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(cursor(result.state())).isEqualTo("stone:35");
    assertThat(Menus.texts(result.state())).isEqualTo(expected(before, Map.of(3, "-", 7, "-")));
    assertThat(result.thrown()).isEmpty();
  }

  @Test
  void provisionalGatheringStopsAtTheStackSizeAndVisitsSlotsInAscendingOrder() {
    // spec: IC-R46
    MenuState before = chest("stone:60", Map.of(1, "stone:3", 2, "stone:3"));

    ClickResult result = doubleClick(before);

    assertThat(cursor(result.state())).isEqualTo("stone:64");
    assertThat(at(result.state(), 1)).isEqualTo("-");
    assertThat(at(result.state(), 2)).isEqualTo("stone:2");
  }

  @Test
  void provisionalStacksThatAreNotFullAreVisitedBeforeFullOnes() {
    // spec: IC-R46 (non full stacks first)
    MenuState before = chest("stone:60", Map.of(1, "stone:64", 2, "stone:2"));

    ClickResult result = doubleClick(before);

    assertThat(cursor(result.state())).isEqualTo("stone:64");
    assertThat(at(result.state(), 2)).isEqualTo("-");
    assertThat(at(result.state(), 1)).isEqualTo("stone:62");
  }

  @Test
  void provisionalGatheringWorksAcrossTheOwnSlotsAndThePlayerInventory() {
    // spec: IC-R46 (the whole menu)
    MenuState before = chest("stone:1", Map.of(3, "stone:2", 30, "stone:4", 60, "stone:8"));

    ClickResult result = doubleClick(before);

    assertThat(cursor(result.state())).isEqualTo("stone:15");
    assertThat(Menus.totals(result.state())).isEqualTo(Menus.totals(before));
  }

  @Test
  void provisionalAResultSlotIsNeverEmptiedByAGather() {
    // spec: IC-R46, IC-R38a (furnace result at slot 2)
    MenuEngine engine = Menus.engine(Menus.layout("furnace"));
    MenuState before = ClickCases.sparse(39, "stone:10", Map.of(0, "stone:4", 2, "stone:5"));

    ClickResult result = engine.click(before, new ClickOperation.DoubleClick(1));

    assertThat(cursor(result.state())).isEqualTo("stone:14");
    assertThat(at(result.state(), 0)).isEqualTo("-");
    assertThat(at(result.state(), 2)).isEqualTo("stone:5");
  }

  @Test
  void provisionalAFullCursorGathersNothing() {
    // spec: IC-R46
    MenuState before = chest("stone:64", Map.of(3, "stone:20"));

    ClickResult result = doubleClick(before);

    assertThat(result.state()).isEqualTo(before);
    assertThat(result.effect()).isEqualTo(ClickEffect.NO_CHANGE);
  }

  @Test
  void provisionalNothingToGatherChangesNothing() {
    // spec: IC-R46
    MenuState before = chest("stone:10", Map.of(3, "dirt:20"));

    ClickResult result = doubleClick(before);

    assertThat(result.state()).isEqualTo(before);
    assertThat(result.effect()).isEqualTo(ClickEffect.NO_CHANGE);
  }

  @Test
  void provisionalGatheringRespectsTheStackSizeOfSmallStacks() {
    // spec: IC-R46, IC-R19 (ender pearls stack to 16)
    MenuState before = chest("ender_pearl:10", Map.of(3, "ender_pearl:10", 4, "ender_pearl:10"));

    ClickResult result = doubleClick(before);

    assertThat(cursor(result.state())).isEqualTo("ender_pearl:16");
    assertThat(at(result.state(), 3)).isEqualTo("ender_pearl:4");
    assertThat(at(result.state(), 4)).isEqualTo("ender_pearl:10");
  }
}

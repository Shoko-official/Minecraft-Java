package dev.shoko.minecraft.item.menu.click;

import static dev.shoko.minecraft.item.menu.click.ClickCases.chest;
import static dev.shoko.minecraft.item.menu.click.ClickCases.chestEngine;
import static dev.shoko.minecraft.item.menu.click.ClickCases.left;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.item.testing.Menus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R39a (only the open menu gets clicks, a slot outside the menu
// is a no-op) and the contract of MenuEngine (state size, no mutation, effects).
class MenuEngineEdgeTest {

  private static List<ClickOperation> operationsOn(int slot) {
    return List.of(
        left(slot),
        ClickCases.right(slot),
        new ClickOperation.QuickMove(slot),
        new ClickOperation.SwapWithHotbar(slot, 0),
        new ClickOperation.SwapWithOffhand(slot),
        new ClickOperation.Throw(slot, false),
        new ClickOperation.Throw(slot, true),
        new ClickOperation.DoubleClick(slot));
  }

  @Test
  void provisionalASlotOutsideTheMenuMakesEveryOperationANoOp() {
    // spec: IC-R39a, ClickOperation contract
    MenuState before = chest("dirt:3", Map.of(0, "stone:5", 30, "stone:2", 60, "bread:1"));

    for (int slot : new int[] {-1, ClickCases.CHEST_SIZE, ClickCases.CHEST_SIZE + 40, 1_000_000}) {
      for (ClickOperation operation : operationsOn(slot)) {
        ClickResult result = chestEngine().click(before, operation);
        assertThat(result.effect()).as("%s", operation).isEqualTo(ClickEffect.NO_CHANGE);
        assertThat(result.state()).as("%s", operation).isEqualTo(before);
        assertThat(result.thrown()).as("%s", operation).isEmpty();
      }
    }
  }

  @Test
  void aStateWithTooFewSlotsIsRefusedWithIllegalArgument() {
    MenuState tooSmall = Menus.padded(ClickCases.CHEST_SIZE - 1, "-");
    MenuEngine engine = chestEngine();
    assertThatThrownBy(() -> engine.click(tooSmall, left(0)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aStateWithTooManySlotsIsRefusedWithIllegalArgument() {
    MenuState tooBig = Menus.padded(ClickCases.CHEST_SIZE + 1, "-");
    MenuEngine engine = chestEngine();
    assertThatThrownBy(() -> engine.click(tooBig, left(0)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void theStateGivenIsNeverModified() {
    MenuState before = chest("stone:5", Map.of(0, "stone:60", 1, "dirt:2", 30, "stone:9"));
    MenuState copy = chest("stone:5", Map.of(0, "stone:60", 1, "dirt:2", 30, "stone:9"));

    for (ClickOperation operation :
        List.of(
            left(0),
            ClickCases.right(1),
            new ClickOperation.QuickMove(30),
            new ClickOperation.Throw(0, true),
            new ClickOperation.DoubleClick(5),
            ClickCases.drag(MouseButton.LEFT, 2, 3, 4))) {
      chestEngine().click(before, operation);
      assertThat(before).as("%s", operation).isEqualTo(copy);
    }
  }

  @Test
  void aClickIsDeterministic() {
    MenuState before = chest("stone:5", Map.of(0, "stone:60", 1, "dirt:2", 30, "stone:9"));

    ClickResult first = chestEngine().click(before, new ClickOperation.QuickMove(30));
    ClickResult second = chestEngine().click(before, new ClickOperation.QuickMove(30));

    assertThat(second).isEqualTo(first);
  }

  @Test
  void aClickThatDoesNotApplyReturnsTheStateItWasGiven() {
    MenuState before = chest("-", Map.of(4, "dirt:2"));

    for (ClickOperation operation :
        List.of(
            left(3),
            new ClickOperation.QuickMove(3),
            new ClickOperation.Throw(3, false),
            new ClickOperation.ClickOutside(MouseButton.LEFT))) {
      ClickResult result = chestEngine().click(before, operation);
      assertThat(result.effect()).as("%s", operation).isNotEqualTo(ClickEffect.APPLIED);
      assertThat(result.state()).as("%s", operation).isEqualTo(before);
      assertThat(result.thrown()).as("%s", operation).isEmpty();
    }
  }

  @Test
  void provisionalOperationsThatThrowNothingReportAnEmptyThrownList() {
    // spec: IC-R40a to IC-R43b
    MenuState before = chest("stone:5", Map.of(0, "dirt:2", 30, "bread:9", 57, "stone:3"));

    for (ClickOperation operation :
        List.of(
            left(0),
            ClickCases.right(0),
            new ClickOperation.QuickMove(30),
            new ClickOperation.SwapWithHotbar(0, 3),
            ClickCases.drag(MouseButton.RIGHT, 1, 2),
            new ClickOperation.DoubleClick(5))) {
      assertThat(chestEngine().click(before, operation).thrown()).as("%s", operation).isEmpty();
    }
  }

  @Test
  void anEmptyStateInEveryOperationNeverFails() {
    MenuState empty = chest("-", Map.of());

    for (ClickOperation operation : operationsOn(3)) {
      ClickResult result = chestEngine().click(empty, operation);
      assertThat(result.state()).as("%s", operation).isEqualTo(empty);
      assertThat(result.effect()).as("%s", operation).isNotEqualTo(ClickEffect.APPLIED);
    }
  }
}

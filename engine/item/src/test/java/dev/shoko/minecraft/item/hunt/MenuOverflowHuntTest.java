package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.menu.MenuLayout;
import dev.shoko.minecraft.item.menu.click.ClickEffect;
import dev.shoko.minecraft.item.menu.click.ClickOperation;
import dev.shoko.minecraft.item.menu.click.ClickResult;
import dev.shoko.minecraft.item.menu.click.MenuEngine;
import dev.shoko.minecraft.item.menu.click.MenuState;
import dev.shoko.minecraft.item.menu.click.MouseButton;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Menus;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R40 to IC-R47 (units are conserved, provisional): stacks whose
// count is far above the stack size (a count is not clamped, IC-R16) must not overflow an int in
// the click algorithms (defect B4: a right click on 2^31-1 units destroyed them).
class MenuOverflowHuntTest {

  private static final MenuLayout CHEST = Menus.layout("generic_9x3");

  private static ItemStack stone(int count) {
    return ItemStack.of(TestItems.type("stone"), count);
  }

  private static MenuState state(MenuLayout layout, ItemStack cursor, ItemStack... first) {
    List<ItemStack> slots = new ArrayList<>(List.of(first));
    while (slots.size() < layout.slotCount()) {
      slots.add(ItemStack.empty());
    }
    return new MenuState(slots, cursor);
  }

  private static long units(MenuState state, List<ItemStack> thrown) {
    long total = state.cursor().count();
    for (ItemStack stack : state.slots()) {
      total += stack.count();
    }
    for (ItemStack stack : thrown) {
      total += stack.count();
    }
    return total;
  }

  private static void assertConserved(MenuState before, ClickResult result, String label) {
    assertThat(units(result.state(), result.thrown()))
        .as(label)
        .isEqualTo(units(before, List.of()));
    assertThat(result.state().cursor().count()).as(label).isGreaterThanOrEqualTo(0);
    result.state().slots().forEach(stack -> assertThat(stack.count()).as(label).isNotNegative());
    result.thrown().forEach(stack -> assertThat(stack.count()).as(label).isPositive());
  }

  private static final List<Integer> HUGE =
      List.of(Integer.MAX_VALUE, Integer.MAX_VALUE - 1, 1_500_000_001, 1_073_741_825, 200);

  @Test
  void provisionalARightClickOnAHugeStackConservesItsUnits() {
    // spec: IC-R41a ((count + 1) / 2 overflowed for 2^31 - 1)
    MenuEngine engine = Menus.engine(CHEST);
    for (int count : HUGE) {
      MenuState before = state(CHEST, ItemStack.empty(), stone(count));
      ClickResult result = engine.click(before, new ClickOperation.Pickup(0, MouseButton.RIGHT));
      assertConserved(before, result, "right click on " + count);
    }
  }

  @Test
  void provisionalALeftClickMergeWithAHugeStackConservesItsUnits() {
    // spec: IC-R40c
    MenuEngine engine = Menus.engine(CHEST);
    for (int count : HUGE) {
      MenuState onSlot = state(CHEST, stone(5), stone(count));
      assertConserved(
          onSlot,
          engine.click(onSlot, new ClickOperation.Pickup(0, MouseButton.LEFT)),
          "cursor 5 on " + count);
      MenuState onCursor = state(CHEST, stone(count), stone(7));
      assertConserved(
          onCursor,
          engine.click(onCursor, new ClickOperation.Pickup(0, MouseButton.LEFT)),
          "cursor " + count + " on 7");
      MenuState both = state(CHEST, stone(count), stone(count));
      assertConserved(
          both,
          engine.click(both, new ClickOperation.Pickup(0, MouseButton.LEFT)),
          "both " + count);
    }
  }

  @Test
  void provisionalTheOtherOperationsOnAHugeStackConserveItsUnits() {
    // spec: IC-R42 to IC-R46
    MenuEngine engine = Menus.engine(CHEST);
    for (int count : HUGE) {
      MenuState slot = state(CHEST, ItemStack.empty(), stone(count));
      List<ClickOperation> operations =
          List.of(
              new ClickOperation.QuickMove(0),
              new ClickOperation.Throw(0, false),
              new ClickOperation.Throw(0, true),
              new ClickOperation.DoubleClick(0),
              new ClickOperation.SwapWithHotbar(0, 4),
              new ClickOperation.SwapWithOffhand(0));
      for (ClickOperation operation : operations) {
        assertConserved(slot, engine.click(slot, operation), operation + " on " + count);
      }
      MenuState cursor = state(CHEST, stone(count));
      List<ClickOperation> withCursor =
          List.of(
              new ClickOperation.Drag(MouseButton.LEFT, List.of(1, 2, 3)),
              new ClickOperation.Drag(MouseButton.RIGHT, List.of(1, 2, 3)),
              new ClickOperation.ClickOutside(MouseButton.LEFT),
              new ClickOperation.ClickOutside(MouseButton.RIGHT),
              new ClickOperation.Pickup(1, MouseButton.LEFT),
              new ClickOperation.Pickup(1, MouseButton.RIGHT));
      for (ClickOperation operation : withCursor) {
        assertConserved(
            cursor, engine.click(cursor, operation), operation + " with cursor " + count);
      }
    }
  }

  @Test
  void provisionalTakingAResultOntoAHugeCursorConservesItsUnits() {
    // spec: IC-R47a (cursor.count() + result.count() overflowed)
    MenuLayout crafting = Menus.layout("crafting");
    MenuEngine engine = Menus.engine(crafting);
    for (int cursorCount : HUGE) {
      MenuState before = state(crafting, stone(cursorCount), stone(10));
      ClickResult result = engine.click(before, new ClickOperation.Pickup(0, MouseButton.LEFT));
      assertConserved(before, result, "result 10 onto cursor " + cursorCount);
      if (result.effect() != ClickEffect.APPLIED) {
        assertThat(result.state()).isEqualTo(before);
      }
    }
  }

  @Test
  void provisionalTakingAHugeResultOntoACursorConservesItsUnits() {
    // spec: IC-R47a
    MenuLayout crafting = Menus.layout("crafting");
    MenuEngine engine = Menus.engine(crafting);
    for (int resultCount : HUGE) {
      MenuState before = state(crafting, stone(5), stone(resultCount));
      assertConserved(
          before,
          engine.click(before, new ClickOperation.Pickup(0, MouseButton.LEFT)),
          "result " + resultCount);
      assertConserved(
          before,
          engine.click(before, new ClickOperation.QuickMove(0)),
          "quick move " + resultCount);
    }
  }
}

package dev.shoko.minecraft.item.menu.click;

import static dev.shoko.minecraft.item.menu.click.ClickCases.at;
import static dev.shoko.minecraft.item.menu.click.ClickCases.cursor;
import static dev.shoko.minecraft.item.menu.click.ClickCases.left;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.menu.MenuLayout;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.Menus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R38a, IC-R47a, IC-R47b (level S: provisional). The crafting
// menu is the 46-slot layout: result 0, grid 1..9, main inventory 10..36, hotbar 37..45. The
// hook plays the part of game: it consumes one unit of each grid slot and recomputes nothing.
class MenuEngineResultTest {

  private static final int SIZE = 46;

  /** Records every call and consumes one unit from each grid slot. */
  private static final class ConsumingHook implements ResultTakeHook {
    private final List<Integer> resultSlots = new ArrayList<>();
    private final List<String> taken = new ArrayList<>();
    private final List<String> resultSlotContent = new ArrayList<>();

    @Override
    public MenuState afterResultTaken(MenuState state, int resultSlot, ItemStack stack) {
      resultSlots.add(resultSlot);
      taken.add(Cells.text(stack));
      resultSlotContent.add(Cells.text(state.slots().get(resultSlot)));
      List<ItemStack> slots = new ArrayList<>(state.slots());
      for (int grid = 1; grid <= 9; grid++) {
        slots.set(grid, slots.get(grid).withCount(slots.get(grid).count() - 1));
      }
      return new MenuState(slots, state.cursor());
    }
  }

  private static MenuEngine engine(ResultTakeHook hook) {
    MenuLayout layout = Menus.layout("crafting");
    return MenuEngine.of(layout, new MenuPorts(Menus.ACCEPT_ALL, hook));
  }

  private static MenuState crafting(String cursor, Map<Integer, String> cells) {
    return ClickCases.sparse(SIZE, cursor, cells);
  }

  @Test
  void provisionalTakingTheResultMovesItToTheCursorAndCallsTheHookOnce() {
    // spec: IC-R47a
    ConsumingHook hook = new ConsumingHook();
    MenuState before = crafting("-", Map.of(0, "oak_planks:4", 1, "oak_log:2", 5, "oak_log:1"));

    ClickResult result = engine(hook).click(before, left(0));

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(cursor(result.state())).isEqualTo("oak_planks:4");
    assertThat(at(result.state(), 0)).isEqualTo("-");
    assertThat(at(result.state(), 1)).isEqualTo("oak_log:1");
    assertThat(at(result.state(), 5)).isEqualTo("-");
    assertThat(hook.resultSlots).containsExactly(0);
    assertThat(hook.taken).containsExactly("oak_planks:4");
    assertThat(hook.resultSlotContent).containsExactly("-");
  }

  @Test
  void provisionalTheStateReturnedByTheHookIsTheStateOfTheClick() {
    // spec: IC-R47a (the hook consumes the ingredients and recomputes the result slot)
    int[] calls = {0};
    ResultTakeHook hook =
        (state, resultSlot, stack) -> {
          calls[0]++;
          List<ItemStack> slots = new ArrayList<>(state.slots());
          slots.set(resultSlot, stack);
          slots.set(9, Cells.stack("dirt:1"));
          return new MenuState(slots, state.cursor());
        };
    MenuState before = crafting("-", Map.of(0, "oak_planks:4"));

    ClickResult result = engine(hook).click(before, left(0));

    assertThat(calls[0]).isEqualTo(1);
    assertThat(at(result.state(), 0)).isEqualTo("oak_planks:4");
    assertThat(at(result.state(), 9)).isEqualTo("dirt:1");
    assertThat(cursor(result.state())).isEqualTo("oak_planks:4");
  }

  @Test
  void provisionalAResultSlotAcceptsNoDeposit() {
    // spec: IC-R38a
    ConsumingHook hook = new ConsumingHook();
    MenuState withResult = crafting("stone:5", Map.of(0, "oak_planks:4"));
    MenuState emptyResult = crafting("stone:5", Map.of());

    ClickResult swap = engine(hook).click(withResult, left(0));
    ClickResult deposit = engine(hook).click(emptyResult, left(0));

    assertThat(swap.state()).isEqualTo(withResult);
    assertThat(deposit.state()).isEqualTo(emptyResult);
    assertThat(swap.effect()).isNotEqualTo(ClickEffect.APPLIED);
    assertThat(deposit.effect()).isNotEqualTo(ClickEffect.APPLIED);
    assertThat(hook.resultSlots).isEmpty();
  }

  @Test
  void provisionalAnEmptyResultSlotWithAnEmptyCursorDoesNothing() {
    // spec: IC-R47a
    ConsumingHook hook = new ConsumingHook();
    MenuState before = crafting("-", Map.of(1, "oak_log:2"));

    ClickResult result = engine(hook).click(before, left(0));

    assertThat(result.effect()).isEqualTo(ClickEffect.NO_CHANGE);
    assertThat(result.state()).isEqualTo(before);
    assertThat(hook.resultSlots).isEmpty();
  }

  @Test
  void provisionalTheResultMergesIntoACursorOfTheSameContentWhenItFits() {
    // spec: IC-R47a, IC-R40c
    ConsumingHook hook = new ConsumingHook();
    MenuState before = crafting("oak_planks:60", Map.of(0, "oak_planks:4", 1, "oak_log:1"));

    ClickResult result = engine(hook).click(before, left(0));

    assertThat(cursor(result.state())).isEqualTo("oak_planks:64");
    assertThat(at(result.state(), 0)).isEqualTo("-");
    assertThat(hook.resultSlots).containsExactly(0);
  }

  @Test
  void provisionalTheCursorNeverExceedsTheStackSizeWhenTheResultDoesNotFit() {
    // spec: IC-R47b (no duplication, no loss), IC-R19
    ConsumingHook hook = new ConsumingHook();
    MenuState before = crafting("oak_planks:62", Map.of(0, "oak_planks:4", 1, "oak_log:1"));

    ClickResult result = engine(hook).click(before, left(0));

    assertThat(result.state().cursor().count()).isLessThanOrEqualTo(64);
    assertThat(Menus.totals(result).get("oak_planks")).isEqualTo(66);
    if (result.effect() != ClickEffect.APPLIED) {
      assertThat(result.state()).isEqualTo(before);
      assertThat(hook.resultSlots).isEmpty();
    }
  }

  @Test
  void provisionalACursorOfOtherContentCannotTakeTheResult() {
    // spec: IC-R38a, IC-R40d
    ConsumingHook hook = new ConsumingHook();
    MenuState before = crafting("stone:2", Map.of(0, "oak_planks:4", 1, "oak_log:1"));

    ClickResult result = engine(hook).click(before, left(0));

    assertThat(result.state()).isEqualTo(before);
    assertThat(hook.resultSlots).isEmpty();
  }

  @Test
  void provisionalQuickMovingTheResultPutsItInThePlayerInventory() {
    // spec: IC-R47a, IC-R42b
    ConsumingHook hook = new ConsumingHook();
    MenuState before = crafting("-", Map.of(0, "oak_planks:4", 1, "oak_log:1"));

    ClickResult result = engine(hook).click(before, new ClickOperation.QuickMove(0));

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(at(result.state(), 0)).isEqualTo("-");
    int planksInPlayerRange = 0;
    for (int slot = 10; slot < SIZE; slot++) {
      ItemStack stack = result.state().slots().get(slot);
      planksInPlayerRange += stack.isEmpty() ? 0 : stack.count();
    }
    assertThat(planksInPlayerRange).isEqualTo(4);
    assertThat(hook.resultSlots).containsExactly(0);
    assertThat(hook.taken).containsExactly("oak_planks:4");
    assertThat(result.thrown()).isEmpty();
  }

  @Test
  void provisionalQuickMovingTheResultIsRefusedWhenThePlayerInventoryIsFull() {
    // spec: IC-R47b (refused or partial, never a duplication or a loss)
    ConsumingHook hook = new ConsumingHook();
    Map<Integer, String> cells = new HashMap<>();
    for (int slot = 10; slot < SIZE; slot++) {
      cells.put(slot, "dirt:64");
    }
    cells.put(0, "oak_planks:4");
    cells.put(1, "oak_log:1");
    MenuState before = crafting("-", cells);

    ClickResult result = engine(hook).click(before, new ClickOperation.QuickMove(0));

    assertThat(result.effect()).isEqualTo(ClickEffect.REFUSED);
    assertThat(result.state()).isEqualTo(before);
    assertThat(hook.resultSlots).isEmpty();
    assertThat(result.thrown()).isEmpty();
  }

  @Test
  void provisionalQuickMovingAResultThatPartlyFitsConservesTheUnits() {
    // spec: IC-R47b (the room is 2 units: refusing and partial moves are both allowed)
    ConsumingHook hook = new ConsumingHook();
    Map<Integer, String> cells = new HashMap<>();
    for (int slot = 10; slot < SIZE; slot++) {
      cells.put(slot, "dirt:64");
    }
    cells.put(20, "oak_planks:62");
    cells.put(0, "oak_planks:4");
    cells.put(1, "oak_log:1");
    MenuState before = crafting("-", cells);

    ClickResult result = engine(hook).click(before, new ClickOperation.QuickMove(0));

    assertThat(Menus.totals(result).get("oak_planks")).isEqualTo(66);
    assertThat(result.state().slots())
        .allSatisfy(s -> assertThat(s.count()).isLessThanOrEqualTo(64));
    if (result.effect() != ClickEffect.APPLIED) {
      assertThat(result.state()).isEqualTo(before);
    }
  }
}

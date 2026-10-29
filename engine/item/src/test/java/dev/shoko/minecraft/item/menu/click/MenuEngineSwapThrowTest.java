package dev.shoko.minecraft.item.menu.click;

import static dev.shoko.minecraft.item.menu.click.ClickCases.at;
import static dev.shoko.minecraft.item.menu.click.ClickCases.chest;
import static dev.shoko.minecraft.item.menu.click.ClickCases.chestEngine;
import static dev.shoko.minecraft.item.menu.click.ClickCases.cursor;
import static dev.shoko.minecraft.item.menu.click.ClickCases.expected;
import static dev.shoko.minecraft.item.menu.click.ClickCases.thrown;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.menu.MenuLayouts;
import dev.shoko.minecraft.item.testing.Menus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R43a, IC-R43b, IC-R44a, IC-R44b, IC-R44c (level S:
// provisional). The chest is generic_9x3 (hotbar 54..62); the player window has its hotbar at
// 36..44 and its off hand at 45.
class MenuEngineSwapThrowTest {

  private static MenuEngine windowEngine() {
    return Menus.engine(MenuLayouts.playerWindow());
  }

  @Test
  void provisionalNumberKeySwapsTheSlotWithTheHotbarSlot() {
    // spec: IC-R43a
    MenuState before = chest("-", Map.of(5, "stone:3", 56, "dirt:4"));

    ClickResult result = chestEngine().click(before, new ClickOperation.SwapWithHotbar(5, 2));

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(5, "dirt:4", 56, "stone:3")));
    assertThat(result.thrown()).isEmpty();
  }

  @Test
  void provisionalNumberKeyMovesTheSlotWhenTheHotbarSlotIsEmpty() {
    // spec: IC-R43a (if one is empty, a move)
    MenuState before = chest("-", Map.of(5, "stone:3"));

    ClickResult result = chestEngine().click(before, new ClickOperation.SwapWithHotbar(5, 0));

    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(5, "-", 54, "stone:3")));
  }

  @Test
  void provisionalNumberKeyMovesTheHotbarStackWhenTheSlotIsEmpty() {
    // spec: IC-R43a
    MenuState before = chest("-", Map.of(62, "dirt:9"));

    ClickResult result = chestEngine().click(before, new ClickOperation.SwapWithHotbar(5, 8));

    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(5, "dirt:9", 62, "-")));
  }

  @Test
  void provisionalNumberKeyWithTwoEmptySlotsChangesNothing() {
    // spec: IC-R43a
    MenuState before = chest("-", Map.of(10, "dirt:9"));

    ClickResult result = chestEngine().click(before, new ClickOperation.SwapWithHotbar(5, 3));

    assertThat(result.effect()).isEqualTo(ClickEffect.NO_CHANGE);
    assertThat(result.state()).isEqualTo(before);
  }

  @Test
  void provisionalNumberKeyUsesTheHotbarOfThePlayerWindow() {
    // spec: IC-R43a, IC-R36 (hotbar at 36..44)
    MenuState before = ClickCases.sparse(46, "-", Map.of(10, "stone:3", 39, "dirt:4"));

    ClickResult result = windowEngine().click(before, new ClickOperation.SwapWithHotbar(10, 3));

    assertThat(at(result.state(), 10)).isEqualTo("dirt:4");
    assertThat(at(result.state(), 39)).isEqualTo("stone:3");
  }

  @Test
  void provisionalNumberKeyCannotPutAnythingInAResultSlot() {
    // spec: IC-R38a, IC-R43a
    MenuEngine engine = Menus.engine(Menus.layout("furnace"));
    MenuState before = ClickCases.sparse(39, "-", Map.of(30, "stone:3"));

    ClickResult result = engine.click(before, new ClickOperation.SwapWithHotbar(2, 0));

    assertThat(at(result.state(), 2)).isEqualTo("-");
    assertThat(Menus.totals(result)).isEqualTo(Menus.totals(before));
  }

  @Test
  void provisionalOffhandKeySwapsTheSlotWithTheOffhand() {
    // spec: IC-R43b
    MenuState before = ClickCases.sparse(46, "-", Map.of(10, "stone:3", 45, "dirt:2"));

    ClickResult result = windowEngine().click(before, new ClickOperation.SwapWithOffhand(10));

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(at(result.state(), 10)).isEqualTo("dirt:2");
    assertThat(at(result.state(), 45)).isEqualTo("stone:3");
  }

  @Test
  void provisionalOffhandKeyMovesTheSlotWhenTheOffhandIsEmpty() {
    // spec: IC-R43b
    MenuState before = ClickCases.sparse(46, "-", Map.of(10, "stone:3"));

    ClickResult result = windowEngine().click(before, new ClickOperation.SwapWithOffhand(10));

    assertThat(Menus.texts(result.state()))
        .isEqualTo(expected(before, Map.of(10, "-", 45, "stone:3")));
  }

  @Test
  void provisionalDropKeyThrowsOneUnit() {
    // spec: IC-R44a
    MenuState before = chest("-", Map.of(5, "stone:5"));

    ClickResult result = chestEngine().click(before, new ClickOperation.Throw(5, false));

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(at(result.state(), 5)).isEqualTo("stone:4");
    assertThat(thrown(result)).containsExactly("stone:1");
  }

  @Test
  void provisionalDropKeyOnALastUnitEmptiesTheSlot() {
    // spec: IC-R44a
    MenuState before = chest("-", Map.of(5, "stone:1"));

    ClickResult result = chestEngine().click(before, new ClickOperation.Throw(5, false));

    assertThat(at(result.state(), 5)).isEqualTo("-");
    assertThat(thrown(result)).containsExactly("stone:1");
  }

  @Test
  void provisionalDropKeyWithModifierThrowsTheWholeStack() {
    // spec: IC-R44b
    MenuState before = chest("-", Map.of(5, "stone:37"));

    ClickResult result = chestEngine().click(before, new ClickOperation.Throw(5, true));

    assertThat(at(result.state(), 5)).isEqualTo("-");
    assertThat(thrown(result)).containsExactly("stone:37");
    assertThat(Menus.totals(result)).isEqualTo(Menus.totals(before));
  }

  @Test
  void provisionalDropKeyOnAnEmptySlotThrowsNothing() {
    // spec: IC-R44a, IC-R44b
    MenuState before = chest("-", Map.of(6, "dirt:1"));

    for (boolean whole : List.of(false, true)) {
      ClickResult result = chestEngine().click(before, new ClickOperation.Throw(5, whole));
      assertThat(result.effect()).isEqualTo(ClickEffect.NO_CHANGE);
      assertThat(result.state()).isEqualTo(before);
      assertThat(result.thrown()).isEmpty();
    }
  }

  @Test
  void provisionalDropKeyDoesNotTouchTheCursor() {
    // spec: IC-R44a
    MenuState before = chest("dirt:3", Map.of(5, "stone:5"));

    ClickResult result = chestEngine().click(before, new ClickOperation.Throw(5, false));

    assertThat(cursor(result.state())).isEqualTo("dirt:3");
  }

  @Test
  void provisionalLeftClickOutsideThrowsTheCursorStack() {
    // spec: IC-R44c
    MenuState before = chest("stone:5", Map.of(5, "dirt:5"));

    ClickResult result =
        chestEngine().click(before, new ClickOperation.ClickOutside(MouseButton.LEFT));

    assertThat(result.effect()).isEqualTo(ClickEffect.APPLIED);
    assertThat(cursor(result.state())).isEqualTo("-");
    assertThat(thrown(result)).containsExactly("stone:5");
    assertThat(result.state().slots()).isEqualTo(before.slots());
  }

  @Test
  void provisionalRightClickOutsideThrowsOneUnit() {
    // spec: IC-R44c
    MenuState before = chest("stone:5", Map.of());

    ClickResult result =
        chestEngine().click(before, new ClickOperation.ClickOutside(MouseButton.RIGHT));

    assertThat(cursor(result.state())).isEqualTo("stone:4");
    assertThat(thrown(result)).containsExactly("stone:1");
  }

  @Test
  void provisionalClickOutsideWithAnEmptyCursorChangesNothing() {
    // spec: IC-R44c
    MenuState before = chest("-", Map.of(5, "dirt:5"));

    for (MouseButton button : List.of(MouseButton.LEFT, MouseButton.RIGHT)) {
      ClickResult result = chestEngine().click(before, new ClickOperation.ClickOutside(button));
      assertThat(result.effect()).isEqualTo(ClickEffect.NO_CHANGE);
      assertThat(result.state()).isEqualTo(before);
      assertThat(result.thrown()).isEmpty();
    }
  }
}

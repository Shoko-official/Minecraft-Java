package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.menu.MenuLayout;
import dev.shoko.minecraft.item.menu.MenuLayouts;
import dev.shoko.minecraft.item.menu.click.ClickEffect;
import dev.shoko.minecraft.item.menu.click.ClickOperation;
import dev.shoko.minecraft.item.menu.click.ClickResult;
import dev.shoko.minecraft.item.menu.click.MenuEngine;
import dev.shoko.minecraft.item.menu.click.MenuState;
import dev.shoko.minecraft.item.menu.click.MouseButton;
import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.Menus;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R40 to IC-R47 (provisional), boundaries that the mutation
// review found unpinned (M37b, M43, M73, M76, M77).
class MenuGapsHuntTest {

  private static final MenuLayout CHEST = Menus.layout("generic_9x3");
  private static final int CHEST_SIZE = 63;
  private static final int CHEST_HOTBAR = 54;

  private static MenuState chest(String cursor, String... first) {
    return Menus.padded(CHEST_SIZE, cursor, first);
  }

  @Test
  void provisionalSwappingASlotWithItselfChangesNothing() {
    // spec: IC-R43a (M43: the hovered slot is hotbar slot n)
    MenuEngine engine = Menus.engine(CHEST);
    MenuState before = Menus.withSlot(chest("-"), CHEST_HOTBAR + 3, "stone:7");
    ClickResult result =
        engine.click(before, new ClickOperation.SwapWithHotbar(CHEST_HOTBAR + 3, 3));
    assertThat(result.effect()).isEqualTo(ClickEffect.NO_CHANGE);
    assertThat(result.state()).isEqualTo(before);
    assertThat(result.thrown()).isEmpty();
  }

  @Test
  void provisionalSwappingAPlayerWindowHotbarSlotWithItselfChangesNothing() {
    // spec: IC-R43a (M43, player window: hotbar at 36..44)
    MenuLayout window = MenuLayouts.playerWindow();
    MenuState before = Menus.withSlot(Menus.padded(window.slotCount(), "-"), 38, "dirt:5");
    ClickResult result =
        Menus.engine(window).click(before, new ClickOperation.SwapWithHotbar(38, 2));
    assertThat(result.effect()).isEqualTo(ClickEffect.NO_CHANGE);
    assertThat(result.state()).isEqualTo(before);
  }

  @Test
  void provisionalAFullSlotOfTheSameContentDoesNotCountInALeftDragShare() {
    // spec: IC-R45a (M37b: n counts the admissible slots only, a full slot is not admissible)
    MenuEngine engine = Menus.engine(CHEST);
    MenuState before = chest("stone:10", "-", "stone:64", "-");
    ClickResult result =
        engine.click(before, new ClickOperation.Drag(MouseButton.LEFT, List.of(0, 1, 2)));
    assertThat(Cells.text(result.state().slots().get(0))).isEqualTo("stone:5");
    assertThat(Cells.text(result.state().slots().get(1))).isEqualTo("stone:64");
    assertThat(Cells.text(result.state().slots().get(2))).isEqualTo("stone:5");
    assertThat(result.state().cursor().isEmpty()).isTrue();
  }

  @Test
  void provisionalAMiddleDragGivesAFullStackPerSlotAndLeavesTheCursor() {
    // spec: IC-R45c (M73: creative copy, the cursor is unchanged)
    MenuEngine engine = Menus.engine(CHEST);
    MenuState before = chest("diamond:1");
    ClickResult result =
        engine.click(before, new ClickOperation.Drag(MouseButton.MIDDLE, List.of(0, 1, 2)));
    for (int slot = 0; slot < 3; slot++) {
      assertThat(Cells.text(result.state().slots().get(slot))).isEqualTo("diamond:64");
    }
    assertThat(Cells.text(result.state().cursor())).isEqualTo("diamond:1");
  }

  @Test
  void provisionalAMiddleClickOnASlotDoesNothing() {
    // spec: IC-R40 (M77: only LEFT and RIGHT pick up; the middle click is not specified)
    MenuEngine engine = Menus.engine(CHEST);
    MenuState before = Menus.withSlot(chest("-"), 4, "stone:5");
    ClickResult result = engine.click(before, new ClickOperation.Pickup(4, MouseButton.MIDDLE));
    assertThat(result.effect()).isEqualTo(ClickEffect.NO_CHANGE);
    assertThat(result.state()).isEqualTo(before);
  }

  @Test
  void provisionalAResultSlotCannotBeThrownFrom() {
    // spec: IC-R38a, IC-R44a (M76: the result stays where it is, nothing reaches the ground)
    MenuLayout crafting = Menus.layout("crafting");
    MenuState before = Menus.withSlot(Menus.padded(crafting.slotCount(), "-"), 0, "stone:5");
    for (boolean whole : List.of(false, true)) {
      ClickResult result = Menus.engine(crafting).click(before, new ClickOperation.Throw(0, whole));
      assertThat(result.effect()).isNotEqualTo(ClickEffect.APPLIED);
      assertThat(result.thrown()).isEmpty();
      assertThat(result.state()).isEqualTo(before);
    }
  }
}

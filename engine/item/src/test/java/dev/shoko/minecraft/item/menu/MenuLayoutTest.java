package dev.shoko.minecraft.item.menu;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.testing.Menus;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R37a (slot count, player inventory after own slots)
class MenuLayoutTest {

  private static MenuLayout sample() {
    return new MenuLayout(
        Identifier.minecraft("test_menu"),
        List.of(
            SlotRole.STORAGE,
            SlotRole.RESULT,
            SlotRole.STORAGE,
            SlotRole.STORAGE,
            SlotRole.INPUT,
            SlotRole.STORAGE));
  }

  @Test
  void slotCountIsTheNumberOfRoles() {
    assertThat(sample().slotCount()).isEqualTo(6);
  }

  @Test
  void rangesAreTheMaximalRunsOfConsecutiveIndicesInAscendingOrder() {
    assertThat(sample().ranges(SlotRole.STORAGE))
        .containsExactly(new SlotRange(0, 1), new SlotRange(2, 4), new SlotRange(5, 6));
    assertThat(sample().ranges(SlotRole.RESULT)).containsExactly(new SlotRange(1, 2));
    assertThat(sample().ranges(SlotRole.INPUT)).containsExactly(new SlotRange(4, 5));
  }

  @Test
  void aRoleWithNoSlotHasNoRange() {
    assertThat(sample().ranges(SlotRole.OFFHAND)).isEmpty();
  }

  @Test
  void aLayoutOfOneRoleIsOneRange() {
    MenuLayout layout =
        new MenuLayout(
            Identifier.minecraft("test_menu"),
            List.of(SlotRole.STORAGE, SlotRole.STORAGE, SlotRole.STORAGE));
    assertThat(layout.ranges(SlotRole.STORAGE)).containsExactly(new SlotRange(0, 3));
  }

  @Test
  void provisionalPlayerRangesOfAChestAreTheTwentySevenThenNineSlotsAfterItsOwn() {
    // spec: IC-R37a
    MenuLayout chest = Menus.layout("generic_9x3");
    assertThat(chest.ranges(SlotRole.STORAGE)).containsExactly(new SlotRange(0, 27));
    assertThat(chest.ranges(SlotRole.PLAYER_MAIN)).containsExactly(new SlotRange(27, 54));
    assertThat(chest.ranges(SlotRole.PLAYER_HOTBAR)).containsExactly(new SlotRange(54, 63));
  }

  @Test
  void provisionalFurnaceRangesSeparateInputsAndResult() {
    // spec: IC-R37c
    MenuLayout furnace = Menus.layout("furnace");
    assertThat(furnace.ranges(SlotRole.INPUT)).containsExactly(new SlotRange(0, 2));
    assertThat(furnace.ranges(SlotRole.RESULT)).containsExactly(new SlotRange(2, 3));
    assertThat(furnace.ranges(SlotRole.PLAYER_MAIN)).containsExactly(new SlotRange(3, 30));
    assertThat(furnace.ranges(SlotRole.PLAYER_HOTBAR)).containsExactly(new SlotRange(30, 39));
  }

  @Test
  void provisionalOnlyTheLecternLeavesOutThePlayerInventory() {
    // spec: IC-R37a
    for (Identifier type : MenuLayouts.menuTypes()) {
      MenuLayout layout = MenuLayouts.of(type).orElseThrow();
      boolean lectern = type.equals(Identifier.minecraft("lectern"));
      assertThat(layout.includesPlayerInventory()).as(type.toString()).isEqualTo(!lectern);
    }
  }
}

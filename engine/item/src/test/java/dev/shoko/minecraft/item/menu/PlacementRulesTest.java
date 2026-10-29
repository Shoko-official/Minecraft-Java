package dev.shoko.minecraft.item.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.Menus;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R38a, IC-R38b, IC-R38c (level S: provisional)
class PlacementRulesTest {

  /** A machine rule that records its calls and answers a fixed value. */
  private static final class Recorder implements MachineRules {
    private final boolean answer;
    private final List<String> calls = new ArrayList<>();

    Recorder(boolean answer) {
      this.answer = answer;
    }

    @Override
    public boolean acceptsInput(Identifier menuType, int slot, ItemStack stack) {
      calls.add(menuType + "/" + slot + "/" + Cells.text(stack));
      return answer;
    }
  }

  @Test
  void provisionalResultSlotsAcceptNothing() {
    // spec: IC-R38a
    MenuLayout furnace = Menus.layout("furnace");
    MenuLayout crafting = Menus.layout("crafting");
    for (String cell : List.of("stone", "stone:64", "iron_helmet", "bread")) {
      assertThat(PlacementRules.mayPlace(furnace, 2, Cells.stack(cell), Menus.ACCEPT_ALL))
          .as("furnace result %s", cell)
          .isFalse();
      assertThat(PlacementRules.mayPlace(crafting, 0, Cells.stack(cell), Menus.ACCEPT_ALL))
          .as("crafting result %s", cell)
          .isFalse();
    }
  }

  @Test
  void provisionalEveryResultSlotOfEveryMenuRefusesEveryStack() {
    // spec: IC-R38a (metric: for each result slot of R37b to R37d)
    for (Identifier type : MenuLayouts.menuTypes()) {
      MenuLayout layout = MenuLayouts.of(type).orElseThrow();
      for (SlotRange range : layout.ranges(SlotRole.RESULT)) {
        for (int slot = range.start(); slot < range.endExclusive(); slot++) {
          assertThat(PlacementRules.mayPlace(layout, slot, Cells.stack("stone"), Menus.ACCEPT_ALL))
              .as("%s slot %d", type, slot)
              .isFalse();
        }
      }
    }
  }

  @Test
  void provisionalArmorSlotsAcceptOnlyTheirOwnEquipment() {
    // spec: IC-R38c
    MenuLayout window = MenuLayouts.playerWindow();
    List<String> items = List.of("iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots");
    for (int item = 0; item < items.size(); item++) {
      for (int slot = 5; slot <= 8; slot++) {
        boolean expected = slot - 5 == item;
        assertThat(
                PlacementRules.mayPlace(
                    window, slot, Cells.stack(items.get(item)), Menus.ACCEPT_NONE))
            .as("%s in slot %d", items.get(item), slot)
            .isEqualTo(expected);
      }
    }
  }

  @Test
  void provisionalArmorSlotsRefuseItemsWithoutEquippable() {
    // spec: IC-R38c
    MenuLayout window = MenuLayouts.playerWindow();
    for (int slot = 5; slot <= 8; slot++) {
      assertThat(PlacementRules.mayPlace(window, slot, Cells.stack("stone"), Menus.ACCEPT_ALL))
          .as("stone in slot %d", slot)
          .isFalse();
    }
  }

  @Test
  void provisionalInputSlotsAskTheMachineWithMenuSlotAndStack() {
    // spec: IC-R38b (port MachineRules)
    MenuLayout furnace = Menus.layout("furnace");
    Recorder yes = new Recorder(true);
    Recorder no = new Recorder(false);

    assertThat(PlacementRules.mayPlace(furnace, 1, Cells.stack("coal:3"), yes)).isTrue();
    assertThat(PlacementRules.mayPlace(furnace, 0, Cells.stack("stone"), no)).isFalse();

    assertThat(yes.calls).containsExactly("minecraft:furnace/1/coal:3");
    assertThat(no.calls).containsExactly("minecraft:furnace/0/stone:1");
  }

  @Test
  void provisionalOtherRolesAlwaysAcceptAndDoNotAskTheMachine() {
    // spec: IC-R38 (storage, player inventory, off hand accept any stack)
    Recorder none = new Recorder(false);
    MenuLayout chest = Menus.layout("generic_9x3");
    MenuLayout window = MenuLayouts.playerWindow();

    assertThat(PlacementRules.mayPlace(chest, 0, Cells.stack("stone:5"), none)).isTrue();
    assertThat(PlacementRules.mayPlace(chest, 30, Cells.stack("iron_helmet"), none)).isTrue();
    assertThat(PlacementRules.mayPlace(chest, 60, Cells.stack("dirt:9"), none)).isTrue();
    assertThat(PlacementRules.mayPlace(window, 1, Cells.stack("stone"), none)).isTrue();
    assertThat(PlacementRules.mayPlace(window, 45, Cells.stack("stone"), none)).isTrue();
    assertThat(none.calls).isEmpty();
  }

  @Test
  void provisionalTheEmptyStackIsAlwaysAccepted() {
    // spec: IC-R38 (skeleton contract: the empty stack is always accepted)
    MenuLayout window = MenuLayouts.playerWindow();
    MenuLayout furnace = Menus.layout("furnace");

    for (int slot = 5; slot <= 8; slot++) {
      assertThat(PlacementRules.mayPlace(window, slot, ItemStack.empty(), Menus.ACCEPT_NONE))
          .isTrue();
    }
    assertThat(PlacementRules.mayPlace(furnace, 0, ItemStack.empty(), Menus.ACCEPT_NONE)).isTrue();
  }

  @Test
  void aSlotOutsideTheLayoutIsRejectedWithIndexOutOfBounds() {
    MenuLayout furnace = Menus.layout("furnace");
    ItemStack stone = Cells.stack("stone");
    assertThatThrownBy(() -> PlacementRules.mayPlace(furnace, -1, stone, Menus.ACCEPT_ALL))
        .isInstanceOf(IndexOutOfBoundsException.class);
    int size = furnace.slotCount();
    assertThatThrownBy(() -> PlacementRules.mayPlace(furnace, size, stone, Menus.ACCEPT_ALL))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }
}

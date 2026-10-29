package dev.shoko.minecraft.item.menu;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.testing.Menus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R36, IC-R37a to IC-R37d, IC-R38a (all level S: provisional).
// Not asserted: the size and the result position of crafter_3x3 (R37b puts its result at 45,
// "after the player", which does not fit the rule "own slots first"), and the menu type of the
// player window, which is not a registry entry.
class MenuLayoutsTest {

  /** Own slots of each menu (IC-R37b to IC-R37d), crafter_3x3 left out. */
  private static final Map<String, Integer> OWN =
      Map.ofEntries(
          entry("generic_9x1", 9),
          entry("generic_9x2", 18),
          entry("generic_9x3", 27),
          entry("generic_9x4", 36),
          entry("generic_9x5", 45),
          entry("generic_9x6", 54),
          entry("shulker_box", 27),
          entry("generic_3x3", 9),
          entry("crafting", 10),
          entry("furnace", 3),
          entry("blast_furnace", 3),
          entry("smoker", 3),
          entry("brewing_stand", 5),
          entry("enchantment", 2),
          entry("anvil", 3),
          entry("grindstone", 3),
          entry("beacon", 1),
          entry("hopper", 5),
          entry("merchant", 3),
          entry("lectern", 1),
          entry("loom", 4),
          entry("smithing", 4),
          entry("cartography_table", 3),
          entry("stonecutter", 2));

  private static final Map<String, Set<Integer>> RESULTS =
      Map.ofEntries(
          entry("crafting", Set.of(0)),
          entry("furnace", Set.of(2)),
          entry("blast_furnace", Set.of(2)),
          entry("smoker", Set.of(2)),
          entry("anvil", Set.of(2)),
          entry("grindstone", Set.of(2)),
          entry("merchant", Set.of(2)),
          entry("loom", Set.of(3)),
          entry("smithing", Set.of(3)),
          entry("cartography_table", Set.of(2)),
          entry("stonecutter", Set.of(1)));

  private static Set<Integer> resultIndices(MenuLayout layout) {
    Set<Integer> found = new java.util.TreeSet<>();
    for (int i = 0; i < layout.roles().size(); i++) {
      if (layout.roles().get(i) == SlotRole.RESULT) {
        found.add(i);
      }
    }
    return found;
  }

  @Test
  void provisionalMenuTypesAreTheTwentyFiveRegistryEntriesSorted() {
    // spec: IC-R37 (registry menu, 25 entries)
    List<Identifier> expected = new ArrayList<>();
    OWN.keySet().forEach(name -> expected.add(Identifier.minecraft(name)));
    expected.add(Identifier.minecraft("crafter_3x3"));
    expected.sort(null);

    assertThat(expected).hasSize(25);
    assertThat(MenuLayouts.menuTypes()).isEqualTo(expected);
  }

  @Test
  void provisionalEveryMenuTypeHasALayoutNamingItself() {
    // spec: IC-R37a
    for (Identifier type : MenuLayouts.menuTypes()) {
      MenuLayout layout = MenuLayouts.of(type).orElseThrow();
      assertThat(layout.menuType()).as(type.toString()).isEqualTo(type);
    }
  }

  @Test
  void provisionalAnIdentifierOutsideTheTableHasNoLayout() {
    // spec: IC-R37a
    assertThat(MenuLayouts.of(Identifier.minecraft("no_such_menu"))).isEmpty();
    assertThat(MenuLayouts.of(Identifier.of("other", "furnace"))).isEmpty();
  }

  @Test
  void provisionalSlotCountIsOwnSlotsPlusThirtySixExceptForTheLectern() {
    // spec: IC-R37a (metric), IC-R37b to IC-R37d
    for (var own : OWN.entrySet()) {
      int expected = own.getKey().equals("lectern") ? 1 : own.getValue() + 36;
      MenuLayout layout = Menus.layout(own.getKey());
      assertThat(layout.slotCount()).as(own.getKey()).isEqualTo(expected);
      assertThat(layout.roles()).as(own.getKey()).hasSize(expected);
    }
  }

  @Test
  void provisionalPlayerSlotsFollowTheOwnSlotsAsMainThenHotbar() {
    // spec: IC-R37a
    for (var own : OWN.entrySet()) {
      if (own.getKey().equals("lectern")) {
        continue;
      }
      List<SlotRole> roles = Menus.layout(own.getKey()).roles();
      int start = own.getValue();
      for (int i = 0; i < 27; i++) {
        assertThat(roles.get(start + i))
            .as("%s main %d", own.getKey(), i)
            .isEqualTo(SlotRole.PLAYER_MAIN);
      }
      for (int i = 0; i < 9; i++) {
        assertThat(roles.get(start + 27 + i))
            .as("%s hotbar %d", own.getKey(), i)
            .isEqualTo(SlotRole.PLAYER_HOTBAR);
      }
    }
  }

  @Test
  void provisionalTheLecternHasOnlyItsBookSlot() {
    // spec: IC-R37a, IC-R37d
    MenuLayout lectern = Menus.layout("lectern");
    assertThat(lectern.slotCount()).isEqualTo(1);
    assertThat(lectern.roles()).doesNotContain(SlotRole.PLAYER_MAIN, SlotRole.PLAYER_HOTBAR);
  }

  @Test
  void provisionalChestLikeMenusHaveOnlyStorageOwnSlots() {
    // spec: IC-R37b (storage accepts any stack)
    for (String name :
        List.of(
            "generic_9x1",
            "generic_9x2",
            "generic_9x3",
            "generic_9x4",
            "generic_9x5",
            "generic_9x6",
            "shulker_box",
            "generic_3x3",
            "hopper")) {
      MenuLayout layout = Menus.layout(name);
      for (int i = 0; i < OWN.get(name); i++) {
        assertThat(layout.roles().get(i)).as("%s slot %d", name, i).isEqualTo(SlotRole.STORAGE);
      }
    }
  }

  @Test
  void provisionalCrafter3x3StartsWithNineStorageSlots() {
    // spec: IC-R37b
    MenuLayout crafter = Menus.layout("crafter_3x3");
    for (int i = 0; i < 9; i++) {
      assertThat(crafter.roles().get(i)).isEqualTo(SlotRole.STORAGE);
    }
  }

  @Test
  void provisionalCraftingHasAResultThenNineGridSlots() {
    // spec: IC-R37b
    MenuLayout crafting = Menus.layout("crafting");
    assertThat(crafting.roles().get(0)).isEqualTo(SlotRole.RESULT);
    for (int i = 1; i <= 9; i++) {
      assertThat(crafting.roles().get(i)).isEqualTo(SlotRole.STORAGE);
    }
  }

  @Test
  void provisionalFurnacesTakeTwoInputsThenAResult() {
    // spec: IC-R37c, IC-R38b
    for (String name : List.of("furnace", "blast_furnace", "smoker")) {
      List<SlotRole> roles = Menus.layout(name).roles();
      assertThat(roles.subList(0, 3))
          .as(name)
          .containsExactly(SlotRole.INPUT, SlotRole.INPUT, SlotRole.RESULT);
    }
  }

  @Test
  void provisionalResultSlotsAreExactlyWhereTheSpecPutsThem() {
    // spec: IC-R38a (list of result slots), IC-R37b to IC-R37d
    for (String name : OWN.keySet()) {
      Set<Integer> expected = RESULTS.getOrDefault(name, Set.of());
      assertThat(resultIndices(Menus.layout(name))).as(name).isEqualTo(expected);
    }
  }

  @Test
  void provisionalThePlayerWindowFollowsTheDocumentedIndices() {
    // spec: IC-R36
    List<SlotRole> roles = MenuLayouts.playerWindow().roles();
    assertThat(MenuLayouts.playerWindow().slotCount()).isEqualTo(46);
    assertThat(roles).hasSize(46);
    assertThat(roles.get(0)).isEqualTo(SlotRole.RESULT);
    for (int i = 1; i <= 4; i++) {
      assertThat(roles.get(i)).as("grid %d", i).isEqualTo(SlotRole.STORAGE);
    }
    assertThat(roles.subList(5, 9))
        .containsExactly(
            SlotRole.ARMOR_HEAD, SlotRole.ARMOR_CHEST, SlotRole.ARMOR_LEGS, SlotRole.ARMOR_FEET);
    for (int i = 9; i <= 35; i++) {
      assertThat(roles.get(i)).as("main %d", i).isEqualTo(SlotRole.PLAYER_MAIN);
    }
    for (int i = 36; i <= 44; i++) {
      assertThat(roles.get(i)).as("hotbar %d", i).isEqualTo(SlotRole.PLAYER_HOTBAR);
    }
    assertThat(roles.get(45)).isEqualTo(SlotRole.OFFHAND);
  }
}

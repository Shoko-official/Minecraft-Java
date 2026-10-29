package dev.shoko.minecraft.item.menu;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The layouts of the 25 menu types of the {@code menu} registry (IC-R37a to IC-R37d, provisional
 * level S) and of the player window (IC-R36).
 */
public final class MenuLayouts {

  private static final int MAIN_SLOTS = 27;
  private static final int HOTBAR_SLOTS = 9;

  private static final MenuLayout PLAYER_WINDOW = playerWindowLayout();

  private static final Map<Identifier, MenuLayout> BY_TYPE = build();

  private MenuLayouts() {}

  /**
   * Lists the menu types the table covers, so that a test can compare it with the {@code menu}
   * registry of the loaded data.
   *
   * @return the 25 identifiers, sorted
   */
  public static List<Identifier> menuTypes() {
    List<Identifier> types = new ArrayList<>(BY_TYPE.keySet());
    Collections.sort(types);
    return List.copyOf(types);
  }

  /**
   * Returns the layout of a menu type.
   *
   * @param menuType a {@code menu} registry entry
   * @return the layout, or empty for an identifier outside the table
   */
  public static Optional<MenuLayout> of(Identifier menuType) {
    return Optional.ofNullable(BY_TYPE.get(menuType));
  }

  /**
   * Returns the layout of the window of a player with no container open (IC-R36): 0 result, 1 to 4
   * crafting grid, 5 to 8 armor from head to feet, 9 to 35 main inventory, 36 to 44 hotbar, 45 off
   * hand. Its {@code menuType} is {@code minecraft:player}, which is not a registry entry: the
   * registry has no player menu.
   *
   * @return the layout
   */
  public static MenuLayout playerWindow() {
    return PLAYER_WINDOW;
  }

  private static MenuLayout playerWindowLayout() {
    List<SlotRole> roles = new ArrayList<>();
    roles.add(SlotRole.RESULT);
    repeat(roles, SlotRole.STORAGE, 4);
    roles.add(SlotRole.ARMOR_HEAD);
    roles.add(SlotRole.ARMOR_CHEST);
    roles.add(SlotRole.ARMOR_LEGS);
    roles.add(SlotRole.ARMOR_FEET);
    addPlayerInventory(roles);
    roles.add(SlotRole.OFFHAND);
    return new MenuLayout(Identifier.minecraft("player"), roles);
  }

  private static void repeat(List<SlotRole> roles, SlotRole role, int count) {
    for (int i = 0; i < count; i++) {
      roles.add(role);
    }
  }

  private static void addPlayerInventory(List<SlotRole> roles) {
    repeat(roles, SlotRole.PLAYER_MAIN, MAIN_SLOTS);
    repeat(roles, SlotRole.PLAYER_HOTBAR, HOTBAR_SLOTS);
  }

  /** The own slots of a menu, then the player inventory (IC-R37a). */
  private static MenuLayout withPlayer(String name, SlotRole... own) {
    List<SlotRole> roles = new ArrayList<>(List.of(own));
    addPlayerInventory(roles);
    return new MenuLayout(Identifier.minecraft(name), roles);
  }

  private static MenuLayout chest(String name, int slots) {
    List<SlotRole> roles = new ArrayList<>();
    repeat(roles, SlotRole.STORAGE, slots);
    addPlayerInventory(roles);
    return new MenuLayout(Identifier.minecraft(name), roles);
  }

  private static Map<Identifier, MenuLayout> build() {
    SlotRole in = SlotRole.INPUT;
    SlotRole out = SlotRole.RESULT;
    List<MenuLayout> layouts = new ArrayList<>();
    for (int rows = 1; rows <= 6; rows++) {
      layouts.add(chest("generic_9x" + rows, rows * 9));
    }
    layouts.add(chest("shulker_box", MAIN_SLOTS));
    layouts.add(chest("generic_3x3", 9));
    layouts.add(chest("hopper", 5));
    layouts.add(crafter());
    layouts.add(crafting());
    for (String furnace : List.of("furnace", "blast_furnace", "smoker")) {
      layouts.add(withPlayer(furnace, in, in, out));
    }
    layouts.add(withPlayer("brewing_stand", in, in, in, in, in));
    layouts.add(withPlayer("enchantment", in, in));
    layouts.add(withPlayer("anvil", in, in, out));
    layouts.add(withPlayer("grindstone", in, in, out));
    layouts.add(withPlayer("beacon", in));
    layouts.add(withPlayer("merchant", in, in, out));
    layouts.add(new MenuLayout(Identifier.minecraft("lectern"), List.of(in)));
    layouts.add(withPlayer("loom", in, in, in, out));
    layouts.add(withPlayer("smithing", in, in, in, out));
    layouts.add(withPlayer("cartography_table", in, in, out));
    layouts.add(withPlayer("stonecutter", in, out));
    Map<Identifier, MenuLayout> byType = new LinkedHashMap<>();
    layouts.forEach(layout -> byType.put(layout.menuType(), layout));
    return Map.copyOf(byType);
  }

  private static MenuLayout crafting() {
    List<SlotRole> roles = new ArrayList<>();
    roles.add(SlotRole.RESULT);
    repeat(roles, SlotRole.STORAGE, 9);
    addPlayerInventory(roles);
    return new MenuLayout(Identifier.minecraft("crafting"), roles);
  }

  /** IC-R37b literally: nine slots, the player inventory, then the result at index 45. */
  private static MenuLayout crafter() {
    List<SlotRole> roles = new ArrayList<>();
    repeat(roles, SlotRole.STORAGE, 9);
    addPlayerInventory(roles);
    roles.add(SlotRole.RESULT);
    return new MenuLayout(Identifier.minecraft("crafter_3x3"), roles);
  }
}

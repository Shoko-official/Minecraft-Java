package dev.shoko.minecraft.item.menu;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.List;
import java.util.Optional;

/**
 * The layouts of the 25 menu types of the {@code menu} registry (IC-R37a to IC-R37d, provisional
 * level S) and of the player window (IC-R36).
 */
public final class MenuLayouts {

  private MenuLayouts() {}

  /**
   * Lists the menu types the table covers, so that a test can compare it with the {@code menu}
   * registry of the loaded data.
   *
   * @return the 25 identifiers, sorted
   */
  public static List<Identifier> menuTypes() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the layout of a menu type.
   *
   * @param menuType a {@code menu} registry entry
   * @return the layout, or empty for an identifier outside the table
   */
  public static Optional<MenuLayout> of(Identifier menuType) {
    return Skeleton.throwNotImplemented();
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
    return Skeleton.throwNotImplemented();
  }
}

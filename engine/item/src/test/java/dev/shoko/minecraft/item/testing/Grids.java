package dev.shoko.minecraft.item.testing;

import dev.shoko.minecraft.item.recipe.match.CraftingGrid;

/** Crafting grids written with {@link Cells} texts in row-major order. */
public final class Grids {

  private Grids() {}

  public static CraftingGrid of(int width, int height, String... cells) {
    if (cells.length != width * height) {
      throw new IllegalArgumentException("need " + width * height + " cells");
    }
    return new CraftingGrid(width, height, Cells.stacks(cells));
  }

  /** A 3x3 grid. */
  public static CraftingGrid table(String... cells) {
    return of(3, 3, cells);
  }

  /** A 2x2 grid of the player window. */
  public static CraftingGrid player(String... cells) {
    return of(2, 2, cells);
  }
}

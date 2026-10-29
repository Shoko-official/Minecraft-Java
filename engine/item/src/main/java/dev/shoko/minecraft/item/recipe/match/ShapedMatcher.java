package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.ShapedPattern;

/**
 * Recognition of a shaped recipe (RC-R16, RC-R17, provisional level S). The pattern may sit at any
 * whole offset of the grid, and also mirrored left to right, as long as every cell outside the
 * pattern rectangle is empty. A pattern wider or taller than the grid never matches. Only items are
 * tested, never components.
 */
public final class ShapedMatcher {

  private ShapedMatcher() {}

  /**
   * Tells whether a grid satisfies a shaped recipe.
   *
   * @param recipe the recipe
   * @param grid the grid
   * @return {@code true} when recognized
   */
  public static boolean matches(ShapedRecipe recipe, CraftingGrid grid) {
    ShapedPattern pattern = recipe.pattern();
    for (int offsetY = 0; offsetY + pattern.height() <= grid.height(); offsetY++) {
      for (int offsetX = 0; offsetX + pattern.width() <= grid.width(); offsetX++) {
        if (fits(pattern, grid, offsetX, offsetY, false)
            || fits(pattern, grid, offsetX, offsetY, true)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean fits(
      ShapedPattern pattern, CraftingGrid grid, int offsetX, int offsetY, boolean mirrored) {
    for (int y = 0; y < grid.height(); y++) {
      for (int x = 0; x < grid.width(); x++) {
        int px = x - offsetX;
        int py = y - offsetY;
        boolean inside = px >= 0 && px < pattern.width() && py >= 0 && py < pattern.height();
        if (inside) {
          int column = mirrored ? pattern.width() - 1 - px : px;
          if (!pattern.cell(column, py).matches(grid.get(x, y))) {
            return false;
          }
        } else if (!grid.get(x, y).isEmpty()) {
          return false;
        }
      }
    }
    return true;
  }
}

package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.ShapedPattern;

/**
 * Recognition of a shaped recipe (RC-R16, RC-R17, provisional level S). The pattern may sit at any
 * whole offset of the grid, and also mirrored left to right, as long as every cell outside the
 * pattern rectangle is empty. A pattern wider or taller than the grid never matches. Only items are
 * tested, never components.
 *
 * <p>The rectangle of the pattern is the box of its filled cells: blank columns and rows along the
 * edges of a pattern, as in the files of {@code mace} or {@code spyglass}, are ignored (provisional
 * R16, to settle with probe PR1). The pattern itself keeps the dimensions of the file (RC-R14).
 */
public final class ShapedMatcher {

  private ShapedMatcher() {}

  /** The box of the filled cells of a pattern: its first cell and its size. */
  private record Box(int left, int top, int width, int height) {

    static Box of(ShapedPattern pattern) {
      int left = pattern.width();
      int top = pattern.height();
      int right = -1;
      int bottom = -1;
      for (int y = 0; y < pattern.height(); y++) {
        for (int x = 0; x < pattern.width(); x++) {
          if (!pattern.cell(x, y).isNone()) {
            left = Math.min(left, x);
            right = Math.max(right, x);
            top = Math.min(top, y);
            bottom = Math.max(bottom, y);
          }
        }
      }
      return new Box(left, top, right - left + 1, bottom - top + 1);
    }

    Ingredient cell(ShapedPattern pattern, int x, int y, boolean mirrored) {
      int column = mirrored ? width - 1 - x : x;
      return pattern.cell(left + column, top + y);
    }
  }

  /**
   * Tells whether a grid satisfies a shaped recipe.
   *
   * @param recipe the recipe
   * @param grid the grid
   * @return {@code true} when recognized
   */
  public static boolean matches(ShapedRecipe recipe, CraftingGrid grid) {
    ShapedPattern pattern = recipe.pattern();
    Box box = Box.of(pattern);
    if (box.width() <= 0) {
      return false;
    }
    for (int offsetY = 0; offsetY + box.height() <= grid.height(); offsetY++) {
      for (int offsetX = 0; offsetX + box.width() <= grid.width(); offsetX++) {
        if (fits(pattern, box, grid, offsetX, offsetY, false)
            || fits(pattern, box, grid, offsetX, offsetY, true)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean fits(
      ShapedPattern pattern,
      Box box,
      CraftingGrid grid,
      int offsetX,
      int offsetY,
      boolean mirrored) {
    for (int y = 0; y < grid.height(); y++) {
      for (int x = 0; x < grid.width(); x++) {
        int px = x - offsetX;
        int py = y - offsetY;
        boolean inside = px >= 0 && px < box.width() && py >= 0 && py < box.height();
        boolean accepted =
            inside
                ? box.cell(pattern, px, py, mirrored).matches(grid.get(x, y))
                : grid.get(x, y).isEmpty();
        if (!accepted) {
          return false;
        }
      }
    }
    return true;
  }
}

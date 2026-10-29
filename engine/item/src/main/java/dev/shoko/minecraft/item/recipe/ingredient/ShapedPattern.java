package dev.shoko.minecraft.item.recipe.ingredient;

import java.util.List;

/**
 * The pattern of a shaped recipe, with the keys already resolved (RC-R14, RC-R15).
 *
 * @param width the number of columns, from 1 to 3
 * @param height the number of rows, from 1 to 3
 * @param cells the ingredient of each cell in row-major order, {@code x + y * width}; {@link
 *     Ingredient#none()} for a space; at least one cell is not empty
 */
public record ShapedPattern(int width, int height, List<Ingredient> cells) {

  /**
   * Reads one cell.
   *
   * @param x the column, from 0 to {@code width - 1}
   * @param y the row, from 0 to {@code height - 1}
   * @return the ingredient of the cell
   * @throws IndexOutOfBoundsException when the cell is outside the pattern
   */
  public Ingredient cell(int x, int y) {
    return Skeleton.throwNotImplemented();
  }
}

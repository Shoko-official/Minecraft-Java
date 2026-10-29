package dev.shoko.minecraft.item.recipe.ingredient;

import java.util.List;
import java.util.Objects;

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
   * Copies the cells.
   *
   * @param width the number of columns
   * @param height the number of rows
   * @param cells the ingredients in row-major order
   * @throws IllegalArgumentException when a dimension is below 1 or the number of cells is not
   *     {@code width * height}
   */
  public ShapedPattern {
    if (width < 1 || height < 1 || cells.size() != (long) width * height) {
      throw new IllegalArgumentException(
          "a " + width + "x" + height + " pattern needs " + width * height + " cells");
    }
    cells = List.copyOf(cells);
  }

  /**
   * Reads one cell.
   *
   * @param x the column, from 0 to {@code width - 1}
   * @param y the row, from 0 to {@code height - 1}
   * @return the ingredient of the cell
   * @throws IndexOutOfBoundsException when the cell is outside the pattern
   */
  public Ingredient cell(int x, int y) {
    return cells.get(Objects.checkIndex(x, width) + Objects.checkIndex(y, height) * width);
  }
}

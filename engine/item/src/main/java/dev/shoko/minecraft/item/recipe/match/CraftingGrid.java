package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.List;

/**
 * The stacks of a crafting grid, 2x2 for the player window or 3x3 for the crafting table (RC-R16).
 *
 * @param width the number of columns
 * @param height the number of rows
 * @param stacks the cells in row-major order, {@code x + y * width}; empty stacks for empty cells;
 *     the size is {@code width * height}
 */
public record CraftingGrid(int width, int height, List<ItemStack> stacks) {

  /**
   * Reads one cell.
   *
   * @param x the column, from 0 to {@code width - 1}
   * @param y the row, from 0 to {@code height - 1}
   * @return the stack, empty for an empty cell
   * @throws IndexOutOfBoundsException when the cell is outside the grid
   */
  public ItemStack get(int x, int y) {
    return Skeleton.throwNotImplemented();
  }
}

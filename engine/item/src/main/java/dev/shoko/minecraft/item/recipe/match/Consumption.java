package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.List;

/**
 * What taking the result of a recipe does to the grid (RC-R22, RC-R23).
 *
 * @param grid the cells after consumption, in the layout of the grid: one unit removed from every
 *     non-empty cell
 * @param remainders the stack left by each cell, same size and order as {@code grid}, empty where
 *     the item has no {@code use_remainder}; where it goes (the cell, the inventory, the ground) is
 *     decided by {@code game} (RC-R23)
 */
public record Consumption(List<ItemStack> grid, List<ItemStack> remainders) {}

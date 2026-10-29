package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * A recipe that accepts a grid, with the stack it produces.
 *
 * @param recipe the retained recipe
 * @param result the stack produced for this grid, non-empty (RC-R21 for ordinary recipes, RC-R43 to
 *     RC-R49f for the others)
 */
public record CraftingMatch(Recipe recipe, ItemStack result) {}

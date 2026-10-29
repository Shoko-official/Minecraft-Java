package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * A smithing recipe that accepts three stacks, with the stack it produces.
 *
 * @param recipe a {@code SmithingTransformRecipe} or a {@code SmithingTrimRecipe}
 * @param result the stack produced: for a transformation the result item with all the components of
 *     the base (RC-R36, provisional); for a trim the base with the {@code trim} component set to
 *     the material and pattern (RC-R37)
 */
public record SmithingMatch(Recipe recipe, ItemStack result) {}

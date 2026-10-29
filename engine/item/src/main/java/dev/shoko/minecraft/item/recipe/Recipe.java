package dev.shoko.minecraft.item.recipe;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * A recipe loaded from {@code data/<namespace>/recipe/<path>.json} (RC-R1). The identifier is
 * {@code <namespace>:<path>} without the extension, with subfolders kept ({@code
 * minecraft:brewing/potion_awkward_blaze_powder}).
 */
public sealed interface Recipe
    permits ShapedRecipe,
        ShapelessRecipe,
        TransmuteRecipe,
        SpecialRecipe,
        CookingRecipe,
        StonecuttingRecipe,
        SmithingTransformRecipe,
        SmithingTrimRecipe,
        BrewingRecipe {

  /**
   * Returns the identifier of the recipe.
   *
   * @return the identifier
   */
  Identifier id();

  /**
   * Returns the machine that runs the recipe (RC-R4).
   *
   * @return the recipe type
   */
  RecipeType type();
}

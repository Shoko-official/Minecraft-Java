package dev.shoko.minecraft.item.recipe;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * {@code stonecutting} (351 recipes, RC-R38).
 *
 * @param id the recipe identifier
 * @param ingredient the cut item
 * @param result the stack obtained from one unit
 */
public record StonecuttingRecipe(Identifier id, Ingredient ingredient, ItemStack result)
    implements Recipe {

  /**
   * Returns {@link RecipeType#STONECUTTING}.
   *
   * @return the type
   */
  @Override
  public RecipeType type() {
    return Skeleton.throwNotImplemented();
  }
}

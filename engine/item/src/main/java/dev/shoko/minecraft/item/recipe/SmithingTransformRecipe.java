package dev.shoko.minecraft.item.recipe;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * {@code smithing_transform} (12 recipes, RC-R36): the result takes the components of the base.
 *
 * @param id the recipe identifier
 * @param template the smithing template
 * @param base the upgraded item
 * @param addition the material
 * @param result the stack whose item replaces the base item
 */
public record SmithingTransformRecipe(
    Identifier id, Ingredient template, Ingredient base, Ingredient addition, ItemStack result)
    implements Recipe {

  /**
   * Returns {@link RecipeType#SMITHING}.
   *
   * @return the type
   */
  @Override
  public RecipeType type() {
    return Skeleton.throwNotImplemented();
  }
}

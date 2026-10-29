package dev.shoko.minecraft.item.recipe;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;

/**
 * {@code smithing_trim} (18 recipes, RC-R37): the result is the base with the {@code trim}
 * component; it has no result field (RC-R12).
 *
 * @param id the recipe identifier
 * @param template the trim template
 * @param base the trimmable armor
 * @param addition the trim material, whose {@code provides_trim_material} names the material
 * @param pattern the trim pattern identifier
 */
public record SmithingTrimRecipe(
    Identifier id, Ingredient template, Ingredient base, Ingredient addition, Identifier pattern)
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

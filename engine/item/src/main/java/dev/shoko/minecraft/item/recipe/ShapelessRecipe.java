package dev.shoko.minecraft.item.recipe;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import java.util.List;

/**
 * {@code crafting_shapeless} (375 recipes, RC-R18 to RC-R20).
 *
 * @param id the recipe identifier
 * @param group the recipe book group, empty when absent (RC-R6, provisional)
 * @param category the recipe book category (RC-R7)
 * @param ingredients one to nine ingredients, in file order
 * @param result the result
 */
public record ShapelessRecipe(
    Identifier id,
    String group,
    RecipeCategory category,
    List<Ingredient> ingredients,
    RecipeResult result)
    implements Recipe {

  /**
   * Copies the ingredients.
   *
   * @param id the recipe identifier
   * @param group the recipe book group
   * @param category the recipe book category
   * @param ingredients one to nine ingredients
   * @param result the result
   */
  public ShapelessRecipe {
    ingredients = List.copyOf(ingredients);
  }

  /**
   * Returns {@link RecipeType#CRAFTING}.
   *
   * @return the type
   */
  @Override
  public RecipeType type() {
    return RecipeType.CRAFTING;
  }
}

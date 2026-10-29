package dev.shoko.minecraft.item.recipe;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.ingredient.CountRange;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;

/**
 * {@code crafting_transmute} (33 recipes, RC-R49f): the result takes the item of {@code result} and
 * the components of the {@code input} stack.
 *
 * @param id the recipe identifier
 * @param group the recipe book group, empty when absent
 * @param category the recipe book category
 * @param input the stack whose components are kept
 * @param material the other ingredient
 * @param materialCount how many materials the grid holds; 1 to 1 when the file has no field
 *     (provisional)
 * @param addMaterialCountToResult whether the materials add to the count of the result ({@code
 *     map_cloning}), false when absent
 * @param result the result, derived for {@code map_cloning}
 */
public record TransmuteRecipe(
    Identifier id,
    String group,
    RecipeCategory category,
    Ingredient input,
    Ingredient material,
    CountRange materialCount,
    boolean addMaterialCountToResult,
    RecipeResult result)
    implements Recipe {

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

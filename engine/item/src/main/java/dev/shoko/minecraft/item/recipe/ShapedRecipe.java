package dev.shoko.minecraft.item.recipe;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.recipe.ingredient.ShapedPattern;

/**
 * {@code crafting_shaped} (827 recipes, RC-R14 to RC-R17).
 *
 * @param id the recipe identifier
 * @param group the recipe book group, empty when absent (RC-R6, provisional)
 * @param category the recipe book category (RC-R7)
 * @param showNotification whether unlocking the recipe shows a notification, true when absent
 *     (RC-R8, provisional)
 * @param pattern the pattern with its keys resolved
 * @param result the result, always a fixed stack for this serializer
 */
public record ShapedRecipe(
    Identifier id,
    String group,
    RecipeCategory category,
    boolean showNotification,
    ShapedPattern pattern,
    RecipeResult result)
    implements Recipe {

  /**
   * Returns {@link RecipeType#CRAFTING}.
   *
   * @return the type
   */
  @Override
  public RecipeType type() {
    return Skeleton.throwNotImplemented();
  }
}

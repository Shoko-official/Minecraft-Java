package dev.shoko.minecraft.item.recipe;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.ingredient.BrewingInput;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * {@code brewing} (279 recipes, RC-R10, RC-R39 to RC-R41). The brewing time, the fuel and the three
 * bottles at once belong to the machine, in {@code game}.
 *
 * @param id the recipe identifier, such as {@code minecraft:brewing/potion_awkward_blaze_powder}
 * @param input the bottle and the potion it must hold
 * @param reagent the ingredient of the fourth slot
 * @param output the bottle obtained: its item and its components (RC-R40)
 */
public record BrewingRecipe(Identifier id, BrewingInput input, Ingredient reagent, ItemStack output)
    implements Recipe {

  /**
   * Returns {@link RecipeType#BREWING}.
   *
   * @return the type
   */
  @Override
  public RecipeType type() {
    return RecipeType.BREWING;
  }
}

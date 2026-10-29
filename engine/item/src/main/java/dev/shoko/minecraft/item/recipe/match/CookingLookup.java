package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.recipe.CookingRecipe;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Finds the cooking recipe of an input item for a machine (RC-R25, RC-R26a, RC-R31). A recipe of
 * one machine is never read by another: the furnace reads {@code smelting} only, the blast furnace
 * {@code blasting}, the smoker {@code smoking} and the campfire {@code campfire_cooking}. The
 * reference data has no input accepted by two recipes of one machine; for a pack the first recipe
 * in resolution order wins (RC-R54, provisional).
 */
public final class CookingLookup {

  private static final Set<RecipeType> MACHINES =
      EnumSet.of(
          RecipeType.SMELTING,
          RecipeType.BLASTING,
          RecipeType.SMOKING,
          RecipeType.CAMPFIRE_COOKING);

  private final RecipeSet recipes;

  private CookingLookup(RecipeSet recipes) {
    this.recipes = recipes;
  }

  /**
   * Creates a lookup.
   *
   * @param recipes the loaded recipes
   * @return the lookup
   */
  public static CookingLookup of(RecipeSet recipes) {
    return new CookingLookup(recipes);
  }

  /**
   * Finds the recipe for an input.
   *
   * @param machine {@code SMELTING}, {@code BLASTING}, {@code SMOKING} or {@code CAMPFIRE_COOKING}
   * @param input the stack in the input slot; only its item is tested
   * @return the recipe, empty when the machine cannot cook it or the stack is empty
   * @throws IllegalArgumentException when the machine is not a cooking type
   */
  public Optional<CookingRecipe> find(RecipeType machine, ItemStack input) {
    if (!MACHINES.contains(machine)) {
      throw new IllegalArgumentException(machine + " does not cook");
    }
    if (input.isEmpty()) {
      return Optional.empty();
    }
    for (Recipe recipe : recipes.ofType(machine)) {
      if (recipe instanceof CookingRecipe cooking && cooking.ingredient().matches(input)) {
        return Optional.of(cooking);
      }
    }
    return Optional.empty();
  }
}

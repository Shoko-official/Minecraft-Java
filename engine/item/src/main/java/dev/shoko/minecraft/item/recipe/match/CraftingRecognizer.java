package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.recipe.ShapelessRecipe;
import dev.shoko.minecraft.item.recipe.SpecialRecipe;
import dev.shoko.minecraft.item.recipe.TransmuteRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Finds the crafting recipe of a grid among the recipes of type {@code CRAFTING} (RC-R16 to RC-R21,
 * RC-R43 to RC-R49f, RC-R54). The shaped, shapeless, transmute and special recipes are all
 * candidates; the result is assembled for the grid (RC-R21: a copy of the file stack for ordinary
 * recipes, derived for the others). An empty grid matches nothing.
 */
public final class CraftingRecognizer {

  private final List<Recipe> candidates;

  private CraftingRecognizer(List<Recipe> candidates) {
    this.candidates = candidates;
  }

  /**
   * Creates a recognizer.
   *
   * @param recipes the loaded recipes
   * @param items the loaded items; the results are built from the stacks of the grid and of the
   *     recipes, so the catalog is not consulted
   * @return the recognizer
   */
  public static CraftingRecognizer of(RecipeSet recipes, ItemCatalog items) {
    return new CraftingRecognizer(recipes.ofType(RecipeType.CRAFTING));
  }

  /**
   * Finds the recipe to use.
   *
   * @param grid the grid
   * @return the first matching recipe in the resolution order of the {@code RecipeSet} (RC-R54,
   *     provisional H-ord-a), with its result; empty when no recipe matches
   */
  public Optional<CraftingMatch> recognize(CraftingGrid grid) {
    if (GridStacks.nonEmpty(grid).isEmpty()) {
      return Optional.empty();
    }
    for (Recipe recipe : candidates) {
      Optional<CraftingMatch> match = match(recipe, grid);
      if (match.isPresent()) {
        return match;
      }
    }
    return Optional.empty();
  }

  /**
   * Finds every recipe that accepts the grid, to detect conflicts (probe PR12).
   *
   * @param grid the grid
   * @return all matches in resolution order, empty when none
   */
  public List<CraftingMatch> allMatches(CraftingGrid grid) {
    List<CraftingMatch> matches = new ArrayList<>();
    if (GridStacks.nonEmpty(grid).isEmpty()) {
      return matches;
    }
    for (Recipe recipe : candidates) {
      match(recipe, grid).ifPresent(matches::add);
    }
    return matches;
  }

  private static Optional<CraftingMatch> match(Recipe recipe, CraftingGrid grid) {
    Optional<ItemStack> result =
        switch (recipe) {
          case ShapedRecipe shaped ->
              ShapedMatcher.matches(shaped, grid) ? fixed(shaped.result()) : Optional.empty();
          case ShapelessRecipe shapeless ->
              ShapelessMatcher.matches(shapeless, grid)
                  ? fixed(shapeless.result())
                  : Optional.empty();
          case TransmuteRecipe transmute -> TransmuteMatcher.match(transmute, grid);
          case SpecialRecipe special -> SpecialMatcher.match(special, grid);
          default -> Optional.empty();
        };
    return result.filter(stack -> !stack.isEmpty()).map(stack -> new CraftingMatch(recipe, stack));
  }

  private static Optional<ItemStack> fixed(RecipeResult result) {
    return result instanceof RecipeResult.Fixed fixed
        ? Optional.of(fixed.stack())
        : Optional.empty();
  }
}

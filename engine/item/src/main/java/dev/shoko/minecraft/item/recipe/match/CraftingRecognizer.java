package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import java.util.List;
import java.util.Optional;

/**
 * Finds the crafting recipe of a grid among the recipes of type {@code CRAFTING} (RC-R16 to RC-R21,
 * RC-R43 to RC-R49f, RC-R54). The shaped, shapeless, transmute and special recipes are all
 * candidates; the result is assembled for the grid (RC-R21: a copy of the file stack for ordinary
 * recipes, derived for the others). An empty grid matches nothing.
 */
public final class CraftingRecognizer {

  private CraftingRecognizer() {}

  /**
   * Creates a recognizer.
   *
   * @param recipes the loaded recipes
   * @param items the loaded items, to build derived results
   * @return the recognizer
   */
  public static CraftingRecognizer of(RecipeSet recipes, ItemCatalog items) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Finds the recipe to use.
   *
   * @param grid the grid
   * @return the first matching recipe in the resolution order of the {@code RecipeSet} (RC-R54,
   *     provisional H-ord-a), with its result; empty when no recipe matches
   */
  public Optional<CraftingMatch> recognize(CraftingGrid grid) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Finds every recipe that accepts the grid, to detect conflicts (probe PR12).
   *
   * @param grid the grid
   * @return all matches in resolution order, empty when none
   */
  public List<CraftingMatch> allMatches(CraftingGrid grid) {
    return Skeleton.throwNotImplemented();
  }
}

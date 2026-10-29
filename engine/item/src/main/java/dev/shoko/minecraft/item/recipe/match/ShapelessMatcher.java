package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.recipe.ShapelessRecipe;

/**
 * Recognition of a shapeless recipe (RC-R19, RC-R20, provisional level S): the non-empty stacks of
 * the grid and the ingredients pair one to one, in any order and position. When a stack suits
 * several ingredients the recipe matches if a complete pairing exists, so the answer does not
 * depend on the order of trial.
 */
public final class ShapelessMatcher {

  private ShapelessMatcher() {}

  /**
   * Tells whether a grid satisfies a shapeless recipe.
   *
   * @param recipe the recipe
   * @param grid the grid
   * @return {@code true} when recognized; false when the number of non-empty stacks differs from
   *     the number of ingredients
   */
  public static boolean matches(ShapelessRecipe recipe, CraftingGrid grid) {
    return Skeleton.throwNotImplemented();
  }
}

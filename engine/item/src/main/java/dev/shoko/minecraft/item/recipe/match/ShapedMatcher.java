package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.recipe.ShapedRecipe;

/**
 * Recognition of a shaped recipe (RC-R16, RC-R17, provisional level S). The pattern may sit at any
 * whole offset of the grid, and also mirrored left to right, as long as every cell outside the
 * pattern rectangle is empty. A pattern wider or taller than the grid never matches. Only items are
 * tested, never components.
 */
public final class ShapedMatcher {

  private ShapedMatcher() {}

  /**
   * Tells whether a grid satisfies a shaped recipe.
   *
   * @param recipe the recipe
   * @param grid the grid
   * @return {@code true} when recognized
   */
  public static boolean matches(ShapedRecipe recipe, CraftingGrid grid) {
    return Skeleton.throwNotImplemented();
  }
}

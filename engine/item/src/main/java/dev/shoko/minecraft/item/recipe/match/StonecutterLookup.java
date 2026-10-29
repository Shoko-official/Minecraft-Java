package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.StonecuttingRecipe;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.List;

/** Lists the stonecutter recipes offered for an input (RC-R38). Each use consumes one unit. */
public final class StonecutterLookup {

  private StonecutterLookup() {}

  /**
   * Creates a lookup.
   *
   * @param recipes the loaded recipes
   * @return the lookup
   */
  public static StonecutterLookup of(RecipeSet recipes) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Lists the recipes whose ingredient accepts the input.
   *
   * @param input the stack in the input slot
   * @return the recipes in resolution order (the order the game proposes them is not specified:
   *     RC-R38, probe PR5), empty for an empty stack or an input nothing cuts
   */
  public List<StonecuttingRecipe> recipesFor(ItemStack input) {
    return Skeleton.throwNotImplemented();
  }
}

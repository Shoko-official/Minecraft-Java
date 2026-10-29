package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.recipe.BrewingRecipe;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.Optional;

/**
 * Brewing recipes applied to one bottle (RC-R10, RC-R39, RC-R40, provisional level S). The infusion
 * itself (reagent consumed once, 400 ticks, fuel, three bottles at once: RC-R41) is the brewing
 * stand of {@code game}, which calls this per bottle.
 */
public final class BrewingLookup {

  private BrewingLookup() {}

  /**
   * Creates a lookup.
   *
   * @param recipes the loaded recipes
   * @return the lookup
   */
  public static BrewingLookup of(RecipeSet recipes) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Finds the recipe that transforms a bottle with a reagent.
   *
   * @param bottle the stack in a bottle slot; its item and, when the input asks for one, its {@code
   *     potion_contents.potions} are tested
   * @param reagent the stack in the ingredient slot
   * @return the first matching recipe in resolution order, empty when none
   */
  public Optional<BrewingRecipe> find(ItemStack bottle, ItemStack reagent) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Applies a recipe to a bottle: the bottle becomes the output item with the components of the
   * output covering the components of the input (RC-R40, provisional).
   *
   * @param recipe a recipe that {@link #find} returned for this bottle
   * @param bottle the bottle before
   * @return the bottle after, with a count of 1
   */
  public ItemStack brew(BrewingRecipe recipe, ItemStack bottle) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether a stack is the ingredient of at least one recipe, to filter the ingredient slot.
   *
   * @param stack a stack
   * @return {@code true} when some recipe accepts it as reagent
   */
  public boolean isReagent(ItemStack stack) {
    return Skeleton.throwNotImplemented();
  }
}

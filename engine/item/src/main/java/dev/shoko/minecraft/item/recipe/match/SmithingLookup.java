package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.Optional;

/**
 * Finds the smithing recipe of a template, a base and an addition (RC-R36, RC-R37, provisional).
 * The trim material comes from the {@code provides_trim_material} component of the addition.
 */
public final class SmithingLookup {

  private SmithingLookup() {}

  /**
   * Creates a lookup.
   *
   * @param recipes the loaded recipes
   * @param items the loaded items, to build results
   * @return the lookup
   */
  public static SmithingLookup of(RecipeSet recipes, ItemCatalog items) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Finds the recipe for three input stacks.
   *
   * @param template the stack in the template slot
   * @param base the stack in the base slot
   * @param addition the stack in the addition slot
   * @return the first matching recipe in resolution order with its result, empty when none
   */
  public Optional<SmithingMatch> find(ItemStack template, ItemStack base, ItemStack addition) {
    return Skeleton.throwNotImplemented();
  }
}

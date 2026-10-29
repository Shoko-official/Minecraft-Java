package dev.shoko.minecraft.item.recipe.ingredient;

import dev.shoko.minecraft.data.registry.HolderSet;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * The items a recipe slot accepts (RC-R9, RC-R17). Only the item is tested, never the components of
 * the stack, except in special recipes.
 *
 * @param items an identifier, a tag or a non-empty list of identifiers; empty only for {@link
 *     #none()}, the empty cell of a pattern
 */
public record Ingredient(HolderSet<ItemType> items) {

  /**
   * Returns the ingredient of an empty cell of a shaped pattern: it accepts only the empty stack.
   *
   * @return the empty-cell ingredient
   */
  public static Ingredient none() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether this is the empty-cell ingredient.
   *
   * @return {@code true} for {@link #none()}
   */
  public boolean isNone() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tests a stack (RC-R17).
   *
   * @param stack a stack
   * @return {@code true} when the stack is non-empty and its item is accepted, or when this is
   *     {@link #none()} and the stack is empty
   * @throws IllegalStateException when the ingredient is a tag that is not loaded
   */
  public boolean matches(ItemStack stack) {
    return Skeleton.throwNotImplemented();
  }
}

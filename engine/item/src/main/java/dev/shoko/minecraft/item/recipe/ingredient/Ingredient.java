package dev.shoko.minecraft.item.recipe.ingredient;

import dev.shoko.minecraft.data.registry.Holder;
import dev.shoko.minecraft.data.registry.HolderSet;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.List;

/**
 * The items a recipe slot accepts (RC-R9, RC-R17). Only the item is tested, never the components of
 * the stack, except in special recipes.
 *
 * @param items an identifier, a tag or a non-empty list of identifiers; empty only for {@link
 *     #none()}, the empty cell of a pattern
 */
public record Ingredient(HolderSet<ItemType> items) {

  private static final Ingredient NONE = new Ingredient(HolderSet.list(List.of()));

  /**
   * Returns the ingredient of an empty cell of a shaped pattern: it accepts only the empty stack.
   *
   * @return the empty-cell ingredient
   */
  public static Ingredient none() {
    return NONE;
  }

  /**
   * Tells whether this is the empty-cell ingredient.
   *
   * @return {@code true} for {@link #none()}
   */
  public boolean isNone() {
    return items == NONE.items;
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
    if (isNone()) {
      return stack.isEmpty();
    }
    if (stack.isEmpty()) {
      return false;
    }
    for (Holder<ItemType> holder : items.holders()) {
      if (holder.id().orElseGet(() -> holder.value().id()).equals(stack.type().id())) {
        return true;
      }
    }
    return false;
  }
}

package dev.shoko.minecraft.item.recipe.ingredient;

import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * The result of a crafting recipe (RC-R11a, RC-R12, IC-R28): a stack written in the file, or a
 * result derived from the grid.
 */
public sealed interface RecipeResult permits RecipeResult.Fixed, RecipeResult.Derived {

  /**
   * A stack from the file: the prototype components of the item covered by the {@code components}
   * of the file, a missing {@code count} being 1 (RC-R11b, provisional).
   *
   * @param stack the stack, with a count that does not exceed its stack size (RC-R24)
   */
  record Fixed(ItemStack stack) implements RecipeResult {}

  /** The file has the empty object as result: the grid decides ({@code map_cloning}, RC-R12). */
  record Derived() implements RecipeResult {}
}

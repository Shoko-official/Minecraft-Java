package dev.shoko.minecraft.loot.modifier;

import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.LootContext;
import java.util.List;

/**
 * Applies item modifiers (LT-R38 to LT-R49). The draws of an action are reported to the observer of
 * the context with the reason {@code MODIFIER}; those of its conditions with {@code
 * MODIFIER_CONDITION}.
 */
public final class ModifierApplier {

  private ModifierApplier() {}

  /**
   * Applies one modifier to one stack: its conditions in order, then its action. A count above the
   * stack size is kept; the split is done by the table (LT-R54). Every {@code Filtered} branch is
   * applied with this method.
   *
   * @param modifier the modifier
   * @param stack the stack to modify; not changed
   * @param context the context; its source is consumed
   * @return the new stack and the cause
   */
  public static ModifierResult apply(ItemModifier modifier, ItemStack stack, LootContext context) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Applies a list of modifiers to a list of stacks (LT-R48, LT-R49): for each modifier in order,
   * each stack in order. A stack that becomes empty is removed from the list and is not given to
   * the next modifiers. The order of the draws is modifier by modifier, then stack by stack.
   *
   * @param modifiers the modifiers in file order
   * @param stacks the stacks, not changed
   * @param context the context; its source is consumed
   * @return the stacks after all the modifiers, none empty
   */
  public static List<ItemStack> applyAll(
      List<ItemModifier> modifiers, List<ItemStack> stacks, LootContext context) {
    return Skeleton.throwNotImplemented();
  }
}

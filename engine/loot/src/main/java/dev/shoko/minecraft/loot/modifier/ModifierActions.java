package dev.shoko.minecraft.loot.modifier;

import dev.shoko.minecraft.item.recipe.CookingRecipe;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.recipe.match.CookingLookup;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.craft.EnchantmentSelection;
import dev.shoko.minecraft.loot.random.DrawReason;
import dev.shoko.minecraft.loot.random.Draws;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Runs the action of a modifier whose conditions held (LT-R39 to LT-R47). */
final class ModifierActions {

  private ModifierActions() {}

  static ModifierResult run(ModifierAction action, ItemStack stack, LootContext context) {
    return switch (action) {
      case ModifierAction.Deferred deferred ->
          new ModifierResult(stack, ModifierCause.DEFERRED_IDENTITY);
      case ModifierAction.Filtered filtered ->
          new ModifierResult(filtered(filtered, stack, context), ModifierCause.APPLIED);
      case ModifierAction.EnchantRandomly random ->
          new ModifierResult(
              EnchantmentSelection.enchantRandomly(
                  stack, random.options(), random.onlyCompatible(), context),
              ModifierCause.APPLIED);
      case ModifierAction.EnchantWithLevels levels -> enchantWithLevels(levels, stack, context);
      case ModifierAction.CopyState copy ->
          counted(ComponentActions.copyState(copy, stack, context), stack);
      case ModifierAction.CopyComponents copy ->
          counted(ComponentActions.copyComponents(copy, stack, context), stack);
      default -> observed(() -> simple(action, stack, context), context);
    };
  }

  private static ModifierResult counted(Optional<ItemStack> result, ItemStack original) {
    return result
        .map(changed -> new ModifierResult(changed, ModifierCause.APPLIED))
        .orElseGet(() -> new ModifierResult(original, ModifierCause.PARAMETER_ABSENT));
  }

  private static ModifierResult observed(Supplier<ItemStack> action, LootContext context) {
    ItemStack result = Draws.observe(context, DrawReason.MODIFIER, "modifier", action);
    return new ModifierResult(result, ModifierCause.APPLIED);
  }

  private static ModifierResult enchantWithLevels(
      ModifierAction.EnchantWithLevels action, ItemStack stack, LootContext context) {
    int levels =
        Draws.observe(
            context, DrawReason.MODIFIER, "modifier.levels", () -> action.levels().sample(context));
    return new ModifierResult(
        EnchantmentSelection.enchantWithLevels(stack, levels, action.options(), context),
        ModifierCause.APPLIED);
  }

  private static ItemStack simple(ModifierAction action, ItemStack stack, LootContext context) {
    return switch (action) {
      case ModifierAction.SetCount set -> CountActions.setCount(set, stack, context);
      case ModifierAction.LimitCount limit -> CountActions.limitCount(limit, stack, context);
      case ModifierAction.Discard discard -> ItemStack.empty();
      case ModifierAction.ExplosionDecay decay -> CountActions.explosionDecay(stack, context);
      case ModifierAction.ApplyBonus bonus -> CountActions.applyBonus(bonus, stack, context);
      case ModifierAction.EnchantedCountIncrease increase ->
          CountActions.enchantedCountIncrease(increase, stack, context);
      case ModifierAction.FurnaceSmelt smelt -> smelt(stack, context);
      case ModifierAction.ExplorationMap map -> stack;
      default -> ComponentActions.edit(action, stack, context);
    };
  }

  private static ItemStack filtered(
      ModifierAction.Filtered action, ItemStack stack, LootContext context) {
    boolean passes = action.itemFilter().matches(stack, context.data().registries());
    List<ItemModifier> branch = passes ? action.onPass() : action.onFail();
    List<ItemStack> result = ModifierApplier.applyAll(branch, List.of(stack), context);
    return result.isEmpty() ? ItemStack.empty() : result.getFirst();
  }

  private static ItemStack smelt(ItemStack stack, LootContext context) {
    Optional<CookingRecipe> recipe =
        CookingLookup.of(context.data().recipes()).find(RecipeType.SMELTING, stack);
    if (recipe.isEmpty() || recipe.get().result().isEmpty()) {
      return stack;
    }
    ItemStack result = recipe.get().result();
    return ItemStack.of(result.type(), stack.count(), result.patch());
  }
}

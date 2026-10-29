package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.NumericTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentChange;
import dev.shoko.minecraft.item.component.ComponentPatch;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/** Small readers and builders shared by the matchers that look at the content of the stacks. */
final class GridStacks {

  private GridStacks() {}

  /** The non-empty stacks of a grid in row-major order. */
  static List<ItemStack> nonEmpty(CraftingGrid grid) {
    return grid.stacks().stream().filter(stack -> !stack.isEmpty()).toList();
  }

  /** Whether every stack suits the ingredient. */
  static boolean allMatch(List<ItemStack> stacks, Ingredient ingredient) {
    return stacks.stream().allMatch(ingredient::matches);
  }

  /** The stacks without the one at an index. */
  static List<ItemStack> without(List<ItemStack> stacks, int index) {
    return IntStream.range(0, stacks.size()).filter(i -> i != index).mapToObj(stacks::get).toList();
  }

  /**
   * A stack of another item (or the same) that keeps the modifications of a model: the patch of the
   * model covered by the changes that the result itself asks for.
   */
  static ItemStack carry(ItemStack model, ItemType type, int count, ComponentPatch overrides) {
    ComponentPatch patch = model.patch();
    for (var change : overrides.changes().entrySet()) {
      patch =
          switch (change.getValue()) {
            case ComponentChange.Set set -> patch.withSet(change.getKey(), set.value());
            case ComponentChange.Removed _ -> patch.withRemoved(change.getKey());
          };
    }
    return ItemStack.of(type, count, patch);
  }

  /** The number of elements of a list component, 0 when absent or not a list. */
  static int listSize(ItemStack stack, Identifier component) {
    return stack
        .component(component)
        .map(ComponentValue::tag)
        .filter(ListTag.class::isInstance)
        .map(tag -> ((ListTag) tag).elements().size())
        .orElse(0);
  }

  /** An integer field of a compound component (any integer tag), a default otherwise. */
  static int intField(ItemStack stack, Identifier component, String field, int fallback) {
    return stack
        .component(component)
        .map(ComponentValue::tag)
        .filter(CompoundTag.class::isInstance)
        .flatMap(tag -> ((CompoundTag) tag).get(field))
        .filter(NumericTag.class::isInstance)
        .map(NumericTag.class::cast)
        .filter(number -> !(number instanceof FloatTag || number instanceof DoubleTag))
        .map(number -> Math.clamp(number.longValue(), Integer.MIN_VALUE, Integer.MAX_VALUE))
        .orElse(fallback);
  }

  /** The compound of a component, empty when absent or not a compound. */
  static CompoundTag compound(ItemStack stack, Identifier component) {
    return stack
        .component(component)
        .map(ComponentValue::tag)
        .filter(CompoundTag.class::isInstance)
        .map(tag -> (CompoundTag) tag)
        .orElse(CompoundTag.empty());
  }

  /** The tag of a component of a stack, if any. */
  static Optional<NbtTag> tag(ItemStack stack, Identifier component) {
    return stack.component(component).map(ComponentValue::tag);
  }
}

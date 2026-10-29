package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.recipe.SpecialRecipe;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.stack.StackCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Computes the consumption of a crafting grid (RC-R22, provisional level S). Each non-empty stack
 * loses one unit; an item carrying {@code use_remainder} leaves that stack as remainder. The banner
 * duplication and the cloning of a book give back the banner or the book that is copied (RC-R43,
 * RC-R44), one unit in the remainder of its cell. The other special recipes follow the general rule
 * until probe PR1.
 */
public final class CraftingConsumer {

  private final StackCodec stacks;

  private CraftingConsumer(StackCodec stacks) {
    this.stacks = stacks;
  }

  /**
   * Creates a consumer.
   *
   * @param items the loaded items, to decode the remainder stacks
   * @return the consumer
   */
  public static CraftingConsumer of(ItemCatalog items) {
    return new CraftingConsumer(StackCodec.of(items));
  }

  /**
   * Consumes a grid.
   *
   * @param grid the grid before
   * @param match the recognized recipe and result
   * @return the grid after and the remainders
   */
  public Consumption consume(CraftingGrid grid, CraftingMatch match) {
    List<ItemStack> after = new ArrayList<>(grid.stacks().size());
    List<ItemStack> remainders = new ArrayList<>(grid.stacks().size());
    int kept = keptCell(grid, match);
    for (int cell = 0; cell < grid.stacks().size(); cell++) {
      ItemStack stack = grid.stacks().get(cell);
      after.add(stack.withCount(stack.count() - 1));
      boolean given = cell == kept && !stack.isEmpty();
      remainders.add(given ? stack.withCount(1) : remainder(stack));
    }
    return new Consumption(after, remainders);
  }

  /** The cell a special recipe gives back whole, -1 when none. */
  private static int keptCell(CraftingGrid grid, CraftingMatch match) {
    if (match != null && match.recipe() instanceof SpecialRecipe special) {
      return SpecialMatcher.keptCell(special, grid).orElse(-1);
    }
    return -1;
  }

  /** The stack that {@code use_remainder} names: {@code {id, count?, components?}}. */
  private ItemStack remainder(ItemStack stack) {
    if (stack.isEmpty()) {
      return ItemStack.empty();
    }
    Optional<ComponentValue> remainder = stack.component(WellKnownComponents.USE_REMAINDER);
    if (remainder.isPresent()
        && remainder.get().tag() instanceof CompoundTag tag
        && stacks.fromNbt(tag) instanceof Decoding.Success<ItemStack> read) {
      return read.value();
    }
    return ItemStack.empty();
  }
}

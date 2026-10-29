package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.stack.StackCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Computes the consumption of a crafting grid (RC-R22, provisional level S). Each non-empty stack
 * loses one unit; an item carrying {@code use_remainder} leaves that stack as remainder. The
 * special recipes that give back an ingredient (RC-R43, RC-R44) or consume more than one unit are
 * not specified beyond the general rule; the result for them is the general rule until probe PR1.
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
    for (ItemStack stack : grid.stacks()) {
      after.add(stack.withCount(stack.count() - 1));
      remainders.add(stack.isEmpty() ? ItemStack.empty() : remainder(stack));
    }
    return new Consumption(after, remainders);
  }

  /** The stack that {@code use_remainder} names: {@code {id, count?, components?}}. */
  private ItemStack remainder(ItemStack stack) {
    Optional<ComponentValue> remainder = stack.component(WellKnownComponents.USE_REMAINDER);
    if (remainder.isPresent()
        && remainder.get().tag() instanceof CompoundTag tag
        && stacks.fromNbt(tag) instanceof Decoding.Success<ItemStack> read) {
      return read.value();
    }
    return ItemStack.empty();
  }
}

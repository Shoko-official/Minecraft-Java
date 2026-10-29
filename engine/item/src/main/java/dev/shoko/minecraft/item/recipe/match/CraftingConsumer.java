package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.registry.ItemCatalog;

/**
 * Computes the consumption of a crafting grid (RC-R22, provisional level S). Each non-empty stack
 * loses one unit; an item carrying {@code use_remainder} leaves that stack as remainder. The
 * special recipes that give back an ingredient (RC-R43, RC-R44) or consume more than one unit are
 * not specified beyond the general rule; the result for them is the general rule until probe PR1.
 */
public final class CraftingConsumer {

  private CraftingConsumer() {}

  /**
   * Creates a consumer.
   *
   * @param items the loaded items, to decode the remainder stacks
   * @return the consumer
   */
  public static CraftingConsumer of(ItemCatalog items) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Consumes a grid.
   *
   * @param grid the grid before
   * @param match the recognized recipe and result
   * @return the grid after and the remainders
   */
  public Consumption consume(CraftingGrid grid, CraftingMatch match) {
    return Skeleton.throwNotImplemented();
  }
}

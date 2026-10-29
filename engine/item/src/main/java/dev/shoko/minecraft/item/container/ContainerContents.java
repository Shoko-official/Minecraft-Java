package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.List;

/**
 * The inventory part of a chest, trapped chest, barrel, dispenser or dropper (IC-R31, IC-R32):
 * {@code Items} and {@code LootTable} with {@code LootTableSeed}.
 *
 * @param slots every slot in order, empty where nothing is stored, unmodifiable
 * @param loot the table still to unpack
 */
public record ContainerContents(List<ItemStack> slots, LootBinding loot) {

  /**
   * Copies the slots.
   *
   * @param slots every slot in order
   * @param loot the table still to unpack
   */
  public ContainerContents {
    slots = List.copyOf(slots);
  }
}

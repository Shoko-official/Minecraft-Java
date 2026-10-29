package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.stack.StackCodec;
import java.util.List;

/**
 * The sparse {@code Items} list of block entities and containers (IC-R24, IC-R25): only non-empty
 * stacks are written, each with its {@code Slot} Byte, and an empty list is written as is.
 */
public final class ContainerItemsCodec {

  private ContainerItemsCodec() {}

  /**
   * Creates a codec.
   *
   * @param stacks the codec of one stack
   * @return the codec
   */
  public static ContainerItemsCodec of(StackCodec stacks) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads an {@code Items} list.
   *
   * @param items the list of compounds
   * @param size the number of slots of the container
   * @return {@code size} stacks, empty where nothing is written. An entry whose {@code Slot} is
   *     missing, mistyped, outside {@code 0..size-1} or already filled is skipped with an {@code
   *     INVALID_SLOT} warning; the findings of a stack (IC-R26) are kept. The behavior of the game
   *     on these entries is not specified (probe PI6)
   */
  public Decoding<List<ItemStack>> read(ListTag items, int size) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes an {@code Items} list.
   *
   * @param slots the content of every slot, in slot order
   * @return the compounds of the non-empty stacks in slot order, each {@code {count, Slot, id,
   *     components?}} as in the structure templates; an empty list when every slot is empty
   */
  public ListTag write(List<ItemStack> slots) {
    return Skeleton.throwNotImplemented();
  }
}

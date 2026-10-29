package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.List;

/**
 * A fixed-size array of slots, each holding a stack that may be empty (IC-R30). Mutable and
 * confined to the thread that ticks the world.
 *
 * <p>The quantity a slot accepts is the smaller of the stack size of the item and the limit of the
 * container (IC-R30, provisional 64 for chests).
 */
public final class Container {

  private Container() {}

  /**
   * Creates a container of empty slots.
   *
   * @param size the number of slots
   * @param slotLimit the quantity limit of a slot, from 1 to 99
   * @return the container
   */
  public static Container of(int size, int slotLimit) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Creates a container holding stacks.
   *
   * @param stacks the content of the slots, in slot order
   * @param slotLimit the quantity limit of a slot, from 1 to 99
   * @return the container; later changes to the list do not affect it
   */
  public static Container copyOf(List<ItemStack> stacks, int slotLimit) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of slots.
   *
   * @return the size
   */
  public int size() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the quantity limit of a slot.
   *
   * @return the limit of the container
   */
  public int slotLimit() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the largest quantity of a content that one slot accepts (IC-R30).
   *
   * @param stack a stack
   * @return the minimum of its stack size and the limit of the container
   */
  public int maxCountFor(ItemStack stack) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a slot.
   *
   * @param slot the index, from 0 to {@code size() - 1}
   * @return the stack, empty for an empty slot
   * @throws IndexOutOfBoundsException when the index is outside the container
   */
  public ItemStack get(int slot) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Replaces a slot.
   *
   * @param slot the index, from 0 to {@code size() - 1}
   * @param stack the new stack
   * @throws IndexOutOfBoundsException when the index is outside the container
   * @throws IllegalArgumentException when the count exceeds {@link #maxCountFor(ItemStack)}
   */
  public void set(int slot, ItemStack stack) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Takes units out of a slot (IC-R20).
   *
   * @param slot the index
   * @param amount the count wanted, bounded by the count available
   * @return the stack taken, empty when the slot is empty
   * @throws IndexOutOfBoundsException when the index is outside the container
   */
  public ItemStack remove(int slot, int amount) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Inserts a stack the way a transfer does: first into the stacks of the same content with room,
   * in slot order, then into empty slots in slot order (IC-R19, IC-R42a).
   *
   * @param stack the stack to insert
   * @return what did not fit, empty when everything was inserted
   */
  public ItemStack insert(ItemStack stack) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether every slot is empty.
   *
   * @return {@code true} when the container holds nothing
   */
  public boolean isEmpty() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Copies the content.
   *
   * @return the stacks in slot order, unmodifiable
   */
  public List<ItemStack> snapshot() {
    return Skeleton.throwNotImplemented();
  }
}

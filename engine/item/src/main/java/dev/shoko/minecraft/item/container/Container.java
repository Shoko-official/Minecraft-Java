package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A fixed-size array of slots, each holding a stack that may be empty (IC-R30). Mutable and
 * confined to the thread that ticks the world.
 *
 * <p>The quantity a slot accepts is the smaller of the stack size of the item and the limit of the
 * container (IC-R30, provisional 64 for chests).
 */
public final class Container {

  private final ItemStack[] slots;
  private final int slotLimit;

  private Container(ItemStack[] slots, int slotLimit) {
    this.slots = slots;
    this.slotLimit = slotLimit;
  }

  /**
   * Creates a container of empty slots.
   *
   * @param size the number of slots
   * @param slotLimit the quantity limit of a slot, from 1 to 99
   * @return the container
   */
  public static Container of(int size, int slotLimit) {
    ItemStack[] slots = new ItemStack[size];
    Arrays.fill(slots, ItemStack.empty());
    return new Container(slots, checkedLimit(slotLimit));
  }

  /**
   * Creates a container holding stacks.
   *
   * @param stacks the content of the slots, in slot order
   * @param slotLimit the quantity limit of a slot, from 1 to 99
   * @return the container; later changes to the list do not affect it
   */
  public static Container copyOf(List<ItemStack> stacks, int slotLimit) {
    return new Container(stacks.toArray(new ItemStack[0]), checkedLimit(slotLimit));
  }

  private static int checkedLimit(int slotLimit) {
    if (slotLimit < 1 || slotLimit > ItemStack.MAX_COUNT) {
      throw new IllegalArgumentException("slot limit outside 1 to 99: " + slotLimit);
    }
    return slotLimit;
  }

  /**
   * Returns the number of slots.
   *
   * @return the size
   */
  public int size() {
    return slots.length;
  }

  /**
   * Returns the quantity limit of a slot.
   *
   * @return the limit of the container
   */
  public int slotLimit() {
    return slotLimit;
  }

  /**
   * Returns the largest quantity of a content that one slot accepts (IC-R30).
   *
   * @param stack a stack
   * @return the minimum of its stack size and the limit of the container
   */
  public int maxCountFor(ItemStack stack) {
    return Math.min(stack.maxStackSize(), slotLimit);
  }

  /**
   * Reads a slot.
   *
   * @param slot the index, from 0 to {@code size() - 1}
   * @return the stack, empty for an empty slot
   * @throws IndexOutOfBoundsException when the index is outside the container
   */
  public ItemStack get(int slot) {
    return slots[Objects.checkIndex(slot, slots.length)];
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
    Objects.checkIndex(slot, slots.length);
    if (stack.count() > maxCountFor(stack)) {
      throw new IllegalArgumentException(
          "count " + stack.count() + " exceeds the limit " + maxCountFor(stack));
    }
    slots[slot] = stack;
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
    ItemStack held = get(slot);
    var split = held.split(amount);
    slots[slot] = split.remaining();
    return split.taken();
  }

  /**
   * Inserts a stack the way a transfer does: first into the stacks of the same content with room,
   * in slot order, then into empty slots in slot order (IC-R19, IC-R42a).
   *
   * @param stack the stack to insert
   * @return what did not fit, empty when everything was inserted
   */
  public ItemStack insert(ItemStack stack) {
    ItemStack rest = stack;
    for (int slot = 0; slot < slots.length && !rest.isEmpty(); slot++) {
      if (!slots[slot].isEmpty()) {
        rest = fill(slot, rest);
      }
    }
    for (int slot = 0; slot < slots.length && !rest.isEmpty(); slot++) {
      if (slots[slot].isEmpty()) {
        rest = fill(slot, rest);
      }
    }
    return rest;
  }

  /** Moves what fits of a stack into a slot of the same content or an empty one. */
  private ItemStack fill(int slot, ItemStack source) {
    ItemStack held = slots[slot];
    if (!held.isEmpty() && !held.sameContent(source)) {
      return source;
    }
    int limit = maxCountFor(source);
    int moved = Math.min(source.count(), Math.max(0, limit - held.count()));
    if (moved == 0) {
      return source;
    }
    slots[slot] = source.withCount(held.count() + moved);
    return source.withCount(source.count() - moved);
  }

  /**
   * Tells whether every slot is empty.
   *
   * @return {@code true} when the container holds nothing
   */
  public boolean isEmpty() {
    for (ItemStack slot : slots) {
      if (!slot.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Copies the content.
   *
   * @return the stacks in slot order, unmodifiable
   */
  public List<ItemStack> snapshot() {
    return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(slots)));
  }
}

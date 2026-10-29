package dev.shoko.minecraft.item.stack;

/**
 * The outcome of {@link ItemStack#mergeFrom(ItemStack)} (IC-R19): the units moved are the
 * difference of counts. Two stacks of different content do not merge: nothing moves.
 *
 * @param merged the receiving stack after the move
 * @param remainder what is left of the source, empty when everything moved
 */
public record MergeResult(ItemStack merged, ItemStack remainder) {}

package dev.shoko.minecraft.item.menu;

/**
 * A run of consecutive slot indices.
 *
 * @param start the first index
 * @param endExclusive the index after the last one
 */
public record SlotRange(int start, int endExclusive) {}

package dev.shoko.minecraft.item.stack;

/**
 * The outcome of {@link ItemStack#split(int)} (IC-R20): no component is lost.
 *
 * @param taken the units taken, with the content of the original stack
 * @param remaining what stays, empty when everything was taken
 */
public record SplitResult(ItemStack taken, ItemStack remaining) {}

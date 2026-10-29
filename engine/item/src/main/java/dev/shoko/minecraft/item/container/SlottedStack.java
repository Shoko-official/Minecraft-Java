package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * A stack nested in a component, with its slot (IC-R23).
 *
 * @param slot the slot index inside the nested container
 * @param stack the nested stack, non-empty
 */
public record SlottedStack(int slot, ItemStack stack) {}

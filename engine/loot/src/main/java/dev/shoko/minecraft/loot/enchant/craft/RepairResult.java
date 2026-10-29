package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * The outcome of repairing an item with experience (EN-R44b).
 *
 * @param stack the repaired item
 * @param experienceLeft the experience points not used, to give to the player
 */
public record RepairResult(ItemStack stack, int experienceLeft) {}

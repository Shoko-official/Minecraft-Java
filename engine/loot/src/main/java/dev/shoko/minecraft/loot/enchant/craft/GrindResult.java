package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * The output of the grindstone (EN-R44a).
 *
 * @param output the item without its non-curse enchantments and without its work penalty
 * @param experience the experience points the player gets (formula open, probe PE4)
 */
public record GrindResult(ItemStack output, int experience) {}

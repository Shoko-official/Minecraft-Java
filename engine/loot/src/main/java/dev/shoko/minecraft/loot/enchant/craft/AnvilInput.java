package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * What the player puts in the anvil.
 *
 * @param target the left slot: the item to repair, enchant or rename
 * @param sacrifice the right slot: a second item, a book, a repair material, or the empty stack
 * @param requestedName the text of the name field; the empty string when the player does not rename
 *     (EN-R41c, EN-R42c)
 */
public record AnvilInput(ItemStack target, ItemStack sacrifice, String requestedName) {}

package dev.shoko.minecraft.item.menu.click;

import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.List;

/**
 * The slots of a menu and the stack on the cursor. The equality of two states is the equality of
 * their slots and cursor, stack by stack.
 *
 * @param slots every slot of the menu in index order, empty stacks for empty slots, unmodifiable
 * @param cursor the stack held by the player, empty when none
 */
public record MenuState(List<ItemStack> slots, ItemStack cursor) {}

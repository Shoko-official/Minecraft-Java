package dev.shoko.minecraft.loot.modifier;

import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * The outcome of applying one modifier to one stack.
 *
 * @param stack the stack after the modifier; the empty stack when it was removed (LT-R49)
 * @param cause what happened
 */
public record ModifierResult(ItemStack stack, ModifierCause cause) {}

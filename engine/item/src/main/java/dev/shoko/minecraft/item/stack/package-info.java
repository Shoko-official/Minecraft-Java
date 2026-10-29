/**
 * Item stacks and their NBT, JSON and command text forms.
 *
 * <p>Spec: {@code docs/specs/item-components-inventory.md} (IC-R14 to IC-R29). A stack is an item,
 * a count and a patch over the prototype components. Stacks are immutable; the empty stack is a
 * single notion (IC-R15), whatever its item and patch. Equality of content ignores the count
 * (IC-R18): use {@link dev.shoko.minecraft.item.stack.ItemStack#sameContent}.
 */
package dev.shoko.minecraft.item.stack;

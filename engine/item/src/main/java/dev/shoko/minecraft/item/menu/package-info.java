/**
 * Slot layouts of the 25 menus and the rules that say what a slot accepts.
 *
 * <p>Spec: {@code docs/specs/item-components-inventory.md} (IC-R36 to IC-R38c). A layout is pure
 * data: the role of each slot index, in the order of IC-R37a (own slots first, then the 27 slots of
 * the main inventory and the 9 of the hotbar, except the lectern). Menus tied to a block or to a
 * player, their opening and closing (IC-R39) and the block entities belong to {@code game}; the
 * click algorithms are in {@link dev.shoko.minecraft.item.menu.click}.
 *
 * <p>What a machine slot accepts depends on recipes and fuels, which {@code game} knows: it enters
 * through the {@link dev.shoko.minecraft.item.menu.MachineRules} port. No method accepts or returns
 * {@code null}.
 */
package dev.shoko.minecraft.item.menu;

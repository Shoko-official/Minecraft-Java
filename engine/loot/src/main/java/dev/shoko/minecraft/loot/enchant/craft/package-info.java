/**
 * Choosing and combining enchantments: the selection shared by the enchanting table and the loot
 * modifiers, the table offers, the anvil, the grindstone and the repair by experience.
 *
 * <p>Spec: {@code docs/specs/item-enchantments.md} (EN-R35 to EN-R44b, probes PE1 to PE4, PE9; ADR
 * 0004 amendment 1, decision 1). These are pure functions of stacks, loaded definitions and a
 * random source. The menus, slots and clicks are in {@code item} and {@code game}; the counting of
 * bookshelves around a table needs a reader of the level that this module does not have (open
 * point), so the number of shelves is an input.
 */
package dev.shoko.minecraft.loot.enchant.craft;

/**
 * Item modifiers (loot functions): the 42 registered types, applied to the stacks a table produces
 * and to stacks by command.
 *
 * <p>Spec: {@code docs/specs/loot-tables.md} (LT-R38 to LT-R49, LT-R54, probes PL3, PL4, PL6, PL12
 * to PL15, PL21, PL22) and {@code docs/specs/item-enchantments.md} (EN-R35, EN-R36). The 21 types
 * with a behavior are typed; the 21 types no data uses are decoded as {@code Deferred} and applied
 * as the identity (LT-R47).
 */
package dev.shoko.minecraft.loot.modifier;

/**
 * The parts recipes are made of: ingredients, patterns, ranges and results.
 *
 * <p>Spec: {@code docs/specs/item-recipes.md} (RC-R9 to RC-R14, RC-R18, RC-R36 to RC-R42). An
 * ingredient is a set of items given as an identifier, a tag or a list of identifiers, held as a
 * {@code HolderSet} of {@code data} so that tags are resolved when they are used, after reloads
 * (RC-R13). All types are immutable. No method accepts or returns {@code null}.
 */
package dev.shoko.minecraft.item.recipe.ingredient;

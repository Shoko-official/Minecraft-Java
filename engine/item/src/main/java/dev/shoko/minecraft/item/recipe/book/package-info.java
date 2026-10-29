/**
 * The recipe book: categories and the set of recipes a player has unlocked.
 *
 * <p>Spec: {@code docs/specs/item-recipes.md} (RC-R7, RC-R50 to RC-R53). Unlocking by advancement
 * and the {@code /recipe} command are done by {@code game}; this is the data and its rules. The
 * form of the book in the player data is not specified (probe PR11) and is not written here.
 * Immutable. No method accepts or returns {@code null}.
 */
package dev.shoko.minecraft.item.recipe.book;

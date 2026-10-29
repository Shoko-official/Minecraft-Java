/**
 * Decoding of recipe files and loading of the recipe set.
 *
 * <p>Spec: {@code docs/specs/item-recipes.md} (RC-R1 to RC-R15, RC-R24, RC-R54, RC-R55). A recipe
 * that fails to decode is ignored and reported; the others still load (RC-R13, probe PR13). Every
 * problem is a {@link dev.shoko.minecraft.item.Finding} with a closed code. No method accepts or
 * returns {@code null}.
 */
package dev.shoko.minecraft.item.recipe.codec;

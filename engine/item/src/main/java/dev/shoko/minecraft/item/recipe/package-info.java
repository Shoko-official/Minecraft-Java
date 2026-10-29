/**
 * The recipe model: 2042 recipes of 22 serializers in 8 types, and the set that holds them.
 *
 * <p>Spec: {@code docs/specs/item-recipes.md} (RC-R1 to RC-R8, RC-R54, RC-R55). A {@link
 * dev.shoko.minecraft.item.recipe.Recipe} is data, immutable and independent of the world. Decoding
 * is in {@code recipe.codec}, the algorithms that recognize a grid or look up a machine recipe in
 * {@code recipe.match}, the parts (ingredients, results) in {@code recipe.ingredient} and the
 * recipe book in {@code recipe.book}.
 *
 * <p>The twelve serializers whose behavior depends on the content of the stacks (RC-R43 to RC-R49f)
 * share {@link dev.shoko.minecraft.item.recipe.SpecialRecipe}; {@code crafting_transmute} has its
 * own type because it carries a count range. No method accepts or returns {@code null}.
 */
package dev.shoko.minecraft.item.recipe;

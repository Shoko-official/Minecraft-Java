/**
 * Pure recipe algorithms: recognizing a crafting grid, consuming it, and looking up the recipe of a
 * machine from its input.
 *
 * <p>Spec: {@code docs/specs/item-recipes.md} (RC-R16 to RC-R24, RC-R26a, RC-R31, RC-R36 to
 * RC-R49f, RC-R54). Every function takes stacks and recipes and returns stacks: no world, no tick,
 * no random source. The progress of cooking and brewing, the experience and the placement of
 * remainders (RC-R23) belong to {@code game}, which calls these functions. Results are immutable.
 * No method accepts or returns {@code null}.
 */
package dev.shoko.minecraft.item.recipe.match;

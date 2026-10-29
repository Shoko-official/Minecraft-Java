package dev.shoko.minecraft.item.recipe.codec;

import dev.shoko.minecraft.item.recipe.RecipeSet;
import java.util.List;

/**
 * What loading the recipes produced.
 *
 * @param recipes the recipes that decoded, without those that failed
 * @param findings the problems of every file, in recipe identifier order then in the order they
 *     were made; the recipes with an {@code ERROR} finding are absent from {@code recipes}
 */
public record RecipeLoadResult(RecipeSet recipes, List<RecipeLoadFinding> findings) {}

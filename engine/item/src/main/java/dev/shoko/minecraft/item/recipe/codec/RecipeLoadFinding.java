package dev.shoko.minecraft.item.recipe.codec;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Finding;

/**
 * A problem met while loading one recipe.
 *
 * @param recipe the identifier of the recipe file
 * @param finding what went wrong
 */
public record RecipeLoadFinding(Identifier recipe, Finding finding) {}

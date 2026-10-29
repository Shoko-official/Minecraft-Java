package dev.shoko.minecraft.item.recipe;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import java.util.SequencedMap;

/**
 * The crafting serializers whose behavior depends on the content of the stacks (RC-R43 to RC-R49e).
 * The file only names the items that play each role; the algorithm is selected by the kind.
 *
 * @param id the recipe identifier
 * @param group the recipe book group, empty when absent
 * @param kind the serializer
 * @param parts the ingredients by field name of the file, in file order; nested objects are
 *     flattened with a dot ({@code shapes.burst}); empty for {@link SpecialKind#REPAIR_ITEM}
 * @param result the result; derived when the file has none (RC-R12)
 */
public record SpecialRecipe(
    Identifier id,
    String group,
    SpecialKind kind,
    SequencedMap<String, Ingredient> parts,
    RecipeResult result)
    implements Recipe {

  /**
   * Returns {@link RecipeType#CRAFTING}.
   *
   * @return the type
   */
  @Override
  public RecipeType type() {
    return RecipeType.CRAFTING;
  }
}

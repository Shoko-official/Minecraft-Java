package dev.shoko.minecraft.item.recipe.codec;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.loader.ElementRegistryData;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

/**
 * Turns the {@code recipe} elements found by the datapack loader into a {@link
 * dev.shoko.minecraft.item.recipe.RecipeSet} (RC-R1, RC-R5, RC-R55). The winner of a path between
 * packs is already chosen by {@code data}; this decodes the winners only.
 */
public final class RecipeLoader {

  private RecipeLoader() {}

  /**
   * Decodes every recipe, never stopping at the first problem (RC-R13).
   *
   * @param recipes the elements of the recipe registry in the loaded data
   * @param access the loaded registries and tags
   * @param items the loaded items
   * @return the recipes that decoded and the problems of the others
   */
  public static RecipeLoadResult load(
      ElementRegistryData recipes, RegistryAccess access, ItemCatalog items) {
    RecipeCodec codec = RecipeCodec.of(items);
    SequencedMap<Identifier, Recipe> loaded = new LinkedHashMap<>();
    List<RecipeLoadFinding> findings = new ArrayList<>();
    for (Map.Entry<Identifier, JsonNode> element : recipes.elements().entrySet()) {
      Decoding<Recipe> decoded = codec.decode(element.getKey(), element.getValue(), access);
      for (Finding finding : decoded.findings()) {
        findings.add(new RecipeLoadFinding(element.getKey(), finding));
      }
      if (decoded instanceof Decoding.Success<Recipe> success) {
        loaded.put(element.getKey(), success.value());
      }
    }
    return new RecipeLoadResult(RecipeSet.of(loaded), List.copyOf(findings));
  }
}

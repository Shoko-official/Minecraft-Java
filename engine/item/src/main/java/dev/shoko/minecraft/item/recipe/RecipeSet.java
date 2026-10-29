package dev.shoko.minecraft.item.recipe;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.TreeMap;

/**
 * The loaded recipes, in resolution order. When several recipes of one machine accept the same
 * input, the first in this order is retained (RC-R54, provisional hypothesis H-ord-a): ascending
 * identifier. A recipe replaced by a pack keeps the place of the original (RC-R55). Immutable.
 */
public final class RecipeSet {

  private final List<Recipe> all;
  // Lookup only: never iterated, so its order is not observable.
  private final Map<Identifier, Recipe> byId;
  private final Map<RecipeType, List<Recipe>> byType;

  private RecipeSet(List<Recipe> all, Map<Identifier, Recipe> byId) {
    this.all = all;
    this.byId = byId;
    Map<RecipeType, List<Recipe>> grouped = new EnumMap<>(RecipeType.class);
    for (RecipeType type : RecipeType.values()) {
      grouped.put(type, new ArrayList<>());
    }
    all.forEach(recipe -> grouped.get(recipe.type()).add(recipe));
    grouped.replaceAll((type, recipes) -> Collections.unmodifiableList(recipes));
    this.byType = grouped;
  }

  /**
   * Builds a set.
   *
   * @param recipes the recipes by identifier; the key of an entry equals {@code recipe.id()}
   * @return the set, ordered by ascending identifier whatever the order of the map
   * @throws IllegalArgumentException when a key differs from the identifier of its recipe
   */
  public static RecipeSet of(SequencedMap<Identifier, Recipe> recipes) {
    Map<Identifier, Recipe> sorted = new TreeMap<>();
    recipes.forEach(
        (id, recipe) -> {
          if (!id.equals(recipe.id())) {
            throw new IllegalArgumentException("key " + id + " holds the recipe " + recipe.id());
          }
          sorted.put(id, recipe);
        });
    return new RecipeSet(List.copyOf(sorted.values()), new HashMap<>(sorted));
  }

  /**
   * Returns the number of recipes.
   *
   * @return the size, 2042 for the reference data
   */
  public int size() {
    return all.size();
  }

  /**
   * Finds a recipe.
   *
   * @param id a recipe identifier
   * @return the recipe, or empty when none has this identifier
   */
  public Optional<Recipe> get(Identifier id) {
    return Optional.ofNullable(byId.get(id));
  }

  /**
   * Returns every recipe in resolution order.
   *
   * @return the recipes, unmodifiable
   */
  public List<Recipe> all() {
    return all;
  }

  /**
   * Returns the recipes of one machine in resolution order.
   *
   * @param type a recipe type
   * @return the recipes, unmodifiable
   */
  public List<Recipe> ofType(RecipeType type) {
    return byType.get(type);
  }
}

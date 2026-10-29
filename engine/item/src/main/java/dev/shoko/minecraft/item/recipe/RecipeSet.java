package dev.shoko.minecraft.item.recipe;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * The loaded recipes, in resolution order. When several recipes of one machine accept the same
 * input, the first in this order is retained (RC-R54, provisional hypothesis H-ord-a): ascending
 * identifier. A recipe replaced by a pack keeps the place of the original (RC-R55). Immutable.
 */
public final class RecipeSet {

  private RecipeSet() {}

  /**
   * Builds a set.
   *
   * @param recipes the recipes by identifier; the key of an entry equals {@code recipe.id()}
   * @return the set, ordered by ascending identifier whatever the order of the map
   * @throws IllegalArgumentException when a key differs from the identifier of its recipe
   */
  public static RecipeSet of(SequencedMap<Identifier, Recipe> recipes) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of recipes.
   *
   * @return the size, 2042 for the reference data
   */
  public int size() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Finds a recipe.
   *
   * @param id a recipe identifier
   * @return the recipe, or empty when none has this identifier
   */
  public Optional<Recipe> get(Identifier id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns every recipe in resolution order.
   *
   * @return the recipes, unmodifiable
   */
  public List<Recipe> all() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the recipes of one machine in resolution order.
   *
   * @param type a recipe type
   * @return the recipes, unmodifiable
   */
  public List<Recipe> ofType(RecipeType type) {
    return Skeleton.throwNotImplemented();
  }
}

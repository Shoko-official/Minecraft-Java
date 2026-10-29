package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.CookingRecipe;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.RecipeCategory;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.recipe.ShapelessRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.recipe.ingredient.ShapedPattern;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Grids;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builders of recipe records and grids for the tests of this package. Recipes are built directly as
 * records so that a failure points at the matcher or lookup under test, not at the JSON codec.
 */
final class MatchFixtures {

  private static final Pattern BLANKS = Pattern.compile("\\s+");

  private MatchFixtures() {}

  static Identifier rid(String path) {
    return Identifier.minecraft(path);
  }

  /** A shaped recipe; a space in a row is an empty cell, other characters are keys. */
  static ShapedRecipe shaped(
      String id, ItemStack result, Map<Character, Ingredient> key, String... rows) {
    int width = rows[0].length();
    List<Ingredient> cells = new ArrayList<>();
    for (String row : rows) {
      for (int i = 0; i < row.length(); i++) {
        char c = row.charAt(i);
        cells.add(c == ' ' ? Ingredient.none() : key.get(c));
      }
    }
    return new ShapedRecipe(
        rid(id),
        "",
        RecipeCategory.MISC,
        true,
        new ShapedPattern(width, rows.length, cells),
        new RecipeResult.Fixed(result));
  }

  static ShapelessRecipe shapeless(String id, ItemStack result, Ingredient... ingredients) {
    return new ShapelessRecipe(
        rid(id), "", RecipeCategory.MISC, List.of(ingredients), new RecipeResult.Fixed(result));
  }

  static CookingRecipe cooking(String id, RecipeType type, Ingredient ingredient, ItemStack out) {
    int time = type == RecipeType.CAMPFIRE_COOKING ? 600 : 200;
    return new CookingRecipe(rid(id), "", RecipeCategory.FOOD, type, ingredient, out, time, 0.35f);
  }

  /** The recipes in a set; the order of the arguments is the insertion order of the map. */
  static RecipeSet set(Recipe... recipes) {
    LinkedHashMap<Identifier, Recipe> map = new LinkedHashMap<>();
    for (Recipe recipe : recipes) {
      map.put(recipe.id(), recipe);
    }
    return RecipeSet.of(map);
  }

  /**
   * A grid from compact text: cells separated by blanks, rows separated by a slash, each cell a
   * {@code Cells} text such as {@code stone}, {@code stone:3} or {@code -}.
   */
  static CraftingGrid grid(String compact) {
    List<String> cells = new ArrayList<>();
    int width = -1;
    int height = 0;
    for (String row : compact.split("/", -1)) {
      String[] tokens = BLANKS.split(row.trim());
      if (width < 0) {
        width = tokens.length;
      } else if (width != tokens.length) {
        throw new IllegalArgumentException("ragged grid: " + compact);
      }
      cells.addAll(List.of(tokens));
      height++;
    }
    return Grids.of(width, height, cells.toArray(new String[0]));
  }

  /** A grid with the given cell texts at the given indices ({@code x + y * width}). */
  static CraftingGrid sparse(int width, int height, Map<Integer, String> cells) {
    String[] all = new String[width * height];
    Arrays.fill(all, "-");
    cells.forEach((index, text) -> all[index] = text);
    return Grids.of(width, height, all);
  }
}

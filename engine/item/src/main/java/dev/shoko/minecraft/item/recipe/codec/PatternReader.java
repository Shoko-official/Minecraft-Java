package dev.shoko.minecraft.item.recipe.codec;

import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.FindingException;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.ShapedPattern;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the {@code pattern} and {@code key} of a shaped recipe (RC-R14, RC-R15): the shape is
 * checked first, then the keys against the characters of the pattern.
 */
final class PatternReader {

  private static final int MAX_SIDE = 3;
  private static final char EMPTY_CELL = ' ';

  private final RecipeReader reader;

  PatternReader(RecipeReader reader) {
    this.reader = reader;
  }

  ShapedPattern read(JsonObject root, String path) {
    List<String> rows = rows(RecipeReader.required(root, "pattern", path), path + ".pattern");
    int width = checkShape(rows, path + ".pattern");
    JsonObject keys = RecipeReader.object(RecipeReader.required(root, "key", path), path + ".key");
    Map<Character, Ingredient> ingredients = new LinkedHashMap<>();
    List<Finding> problems = new ArrayList<>();
    keys.members()
        .forEach(
            (name, node) -> {
              String at = path + ".key." + name;
              Ingredient ingredient = reader.ingredient(node, at);
              if (name.length() != 1 || name.charAt(0) == EMPTY_CELL) {
                problems.add(unused(at, "a key must be one character other than the space"));
              } else {
                ingredients.put(name.charAt(0), ingredient);
              }
            });
    Set<Character> used = new LinkedHashSet<>();
    List<Ingredient> cells = new ArrayList<>();
    for (String row : rows) {
      for (int i = 0; i < row.length(); i++) {
        cells.add(cell(row.charAt(i), ingredients, used, problems, path + ".pattern"));
      }
    }
    for (Character key : ingredients.keySet()) {
      if (!used.contains(key)) {
        problems.add(unused(path + ".key." + key, "key " + key + " is not in the pattern"));
      }
    }
    if (problems.isEmpty() && used.isEmpty()) {
      problems.add(
          new Finding(FindingCode.PATTERN_SHAPE, path + ".pattern", "the pattern has no cell"));
    }
    if (!problems.isEmpty()) {
      throw new FindingException(problems);
    }
    return new ShapedPattern(width, rows.size(), cells);
  }

  private static Ingredient cell(
      char c,
      Map<Character, Ingredient> ingredients,
      Set<Character> used,
      List<Finding> problems,
      String path) {
    if (c == EMPTY_CELL) {
      return Ingredient.none();
    }
    used.add(c);
    Ingredient ingredient = ingredients.get(c);
    if (ingredient == null) {
      problems.add(
          new Finding(FindingCode.PATTERN_KEY_MISSING, path, "no key for the character " + c));
      return Ingredient.none();
    }
    return ingredient;
  }

  private static Finding unused(String path, String message) {
    return new Finding(FindingCode.PATTERN_KEY_UNUSED, path, message);
  }

  private static List<String> rows(JsonNode node, String path) {
    if (!(node instanceof JsonArray array)) {
      throw new FindingException(FindingCode.TYPE_MISMATCH, path, "expected a list of rows");
    }
    List<String> rows = new ArrayList<>();
    for (int i = 0; i < array.elements().size(); i++) {
      rows.add(RecipeReader.string(array.elements().get(i), path + "[" + i + "]"));
    }
    return rows;
  }

  /** Checks 1 to 3 rows of one length from 1 to 3 and returns the width. */
  private static int checkShape(List<String> rows, String path) {
    int width = rows.isEmpty() ? 0 : rows.getFirst().length();
    boolean ragged = rows.stream().anyMatch(row -> row.length() != width);
    if (rows.isEmpty() || rows.size() > MAX_SIDE || ragged || width < 1 || width > MAX_SIDE) {
      throw new FindingException(
          FindingCode.PATTERN_SHAPE, path, "a pattern has 1 to 3 rows of 1 to 3 cells");
    }
    return width;
  }
}

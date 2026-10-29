package dev.shoko.minecraft.item.recipe.codec;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.FindingException;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.recipe.ShapelessRecipe;
import dev.shoko.minecraft.item.recipe.TransmuteRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.CountRange;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads the crafting serializers with a grid: shaped, shapeless and transmute (RC-R14 to RC-R20).
 */
final class CraftingReaders {

  /** The crafting grid holds nine cells, which bounds the ingredients of a shapeless recipe. */
  private static final int MAX_INGREDIENTS = 9;

  private final RecipeReader reader;

  CraftingReaders(RecipeReader reader) {
    this.reader = reader;
  }

  Recipe shaped(Identifier id, JsonObject root) {
    String path = "$";
    boolean notify =
        root.get("show_notification")
            .map(node -> RecipeReader.bool(node, path + ".show_notification"))
            .orElse(true);
    return new ShapedRecipe(
        id,
        RecipeReader.group(root, path),
        RecipeReader.category(root, path),
        notify,
        new PatternReader(reader).read(root, path),
        new RecipeResult.Fixed(
            reader.stack(RecipeReader.required(root, "result", path), path + ".result")));
  }

  Recipe shapeless(Identifier id, JsonObject root) {
    String path = "$";
    JsonNode node = RecipeReader.required(root, "ingredients", path);
    String at = path + ".ingredients";
    if (!(node instanceof JsonArray array)) {
      throw new FindingException(FindingCode.TYPE_MISMATCH, at, "expected a list");
    }
    int size = array.elements().size();
    if (size == 0) {
      throw new FindingException(FindingCode.EMPTY_INGREDIENT, at, "no ingredient");
    }
    if (size > MAX_INGREDIENTS) {
      throw new FindingException(FindingCode.VALUE_RANGE, at, "more than nine ingredients");
    }
    List<Ingredient> ingredients = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      ingredients.add(reader.ingredient(array.elements().get(i), at + "[" + i + "]"));
    }
    return new ShapelessRecipe(
        id,
        RecipeReader.group(root, path),
        RecipeReader.category(root, path),
        ingredients,
        new RecipeResult.Fixed(
            reader.stack(RecipeReader.required(root, "result", path), path + ".result")));
  }

  Recipe transmute(Identifier id, JsonObject root) {
    String path = "$";
    boolean addCount =
        root.get("add_material_count_to_result")
            .map(node -> RecipeReader.bool(node, path + ".add_material_count_to_result"))
            .orElse(false);
    RecipeReader.required(root, "result", path);
    return new TransmuteRecipe(
        id,
        RecipeReader.group(root, path),
        RecipeReader.category(root, path),
        reader.ingredient(RecipeReader.required(root, "input", path), path + ".input"),
        reader.ingredient(RecipeReader.required(root, "material", path), path + ".material"),
        materialCount(root, path),
        addCount,
        reader.derivableResult(root, path));
  }

  /** A single value gives equal bounds; an object gives {@code min} and {@code max}. */
  private static CountRange materialCount(JsonObject root, String parent) {
    String path = parent + ".material_count";
    Optional<JsonNode> node = root.get("material_count");
    if (node.isEmpty()) {
      return new CountRange(1, 1);
    }
    if (node.get() instanceof JsonNumber) {
      int count = RecipeReader.integer(node.get(), path);
      return checked(count, count, path);
    }
    JsonObject range = RecipeReader.object(node.get(), path);
    int min = RecipeReader.integer(RecipeReader.required(range, "min", path), path + ".min");
    int max = RecipeReader.integer(RecipeReader.required(range, "max", path), path + ".max");
    return checked(min, max, path);
  }

  private static CountRange checked(int min, int max, String path) {
    if (min < 1 || max < min) {
      throw new FindingException(FindingCode.VALUE_RANGE, path, "not a range of counts from 1");
    }
    return new CountRange(min, max);
  }
}

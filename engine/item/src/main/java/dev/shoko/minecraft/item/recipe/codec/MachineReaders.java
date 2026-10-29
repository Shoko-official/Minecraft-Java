package dev.shoko.minecraft.item.recipe.codec;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.item.recipe.BrewingRecipe;
import dev.shoko.minecraft.item.recipe.CookingRecipe;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.recipe.SmithingTransformRecipe;
import dev.shoko.minecraft.item.recipe.SmithingTrimRecipe;
import dev.shoko.minecraft.item.recipe.StonecuttingRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.BrewingInput;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.PotionMatch;
import java.util.Optional;

/** Reads the serializers of the other machines: cooking, stonecutter, smithing and brewing. */
final class MachineReaders {

  private final RecipeReader reader;

  MachineReaders(RecipeReader reader) {
    this.reader = reader;
  }

  private Ingredient ingredient(JsonObject root, String field) {
    return reader.ingredient(RecipeReader.required(root, field, "$"), "$." + field);
  }

  Recipe cooking(Identifier id, RecipeType machine, JsonObject root) {
    String path = "$";
    return new CookingRecipe(
        id,
        RecipeReader.group(root, path),
        RecipeReader.category(root, path),
        machine,
        ingredient(root, "ingredient"),
        reader.stack(RecipeReader.required(root, "result", path), path + ".result"),
        RecipeReader.integer(
            RecipeReader.required(root, "cookingtime", path), path + ".cookingtime"),
        RecipeReader.decimal(
            RecipeReader.required(root, "experience", path), path + ".experience"));
  }

  Recipe stonecutting(Identifier id, JsonObject root) {
    return new StonecuttingRecipe(
        id,
        ingredient(root, "ingredient"),
        reader.stack(RecipeReader.required(root, "result", "$"), "$.result"));
  }

  Recipe smithingTransform(Identifier id, JsonObject root) {
    return new SmithingTransformRecipe(
        id,
        ingredient(root, "template"),
        ingredient(root, "base"),
        ingredient(root, "addition"),
        reader.stack(RecipeReader.required(root, "result", "$"), "$.result"));
  }

  Recipe smithingTrim(Identifier id, JsonObject root) {
    return new SmithingTrimRecipe(
        id,
        ingredient(root, "template"),
        ingredient(root, "base"),
        ingredient(root, "addition"),
        RecipeReader.identifier(RecipeReader.required(root, "pattern", "$"), "$.pattern"));
  }

  Recipe brewing(Identifier id, JsonObject root) {
    JsonObject input = RecipeReader.object(RecipeReader.required(root, "input", "$"), "$.input");
    JsonObject reagent =
        RecipeReader.object(RecipeReader.required(root, "reagent", "$"), "$.reagent");
    Ingredient bottle =
        reader.ingredient(RecipeReader.required(input, "item", "$.input"), "$.input.item");
    Ingredient material =
        reader.ingredient(RecipeReader.required(reagent, "item", "$.reagent"), "$.reagent.item");
    return new BrewingRecipe(
        id,
        new BrewingInput(bottle, potion(input)),
        material,
        reader.stack(RecipeReader.required(root, "output", "$"), "$.output"));
  }

  /** The optional condition {@code potion_contents.potions} of a brewing input (RC-R10). */
  private static PotionMatch potion(JsonObject input) {
    Optional<JsonNode> contents = input.get("potion_contents");
    if (contents.isEmpty()) {
      return new PotionMatch.Any();
    }
    String path = "$.input.potion_contents";
    JsonObject object = RecipeReader.object(contents.get(), path);
    Optional<JsonNode> potions = object.get("potions");
    if (potions.isEmpty()) {
      return new PotionMatch.Any();
    }
    return new PotionMatch.Exact(RecipeReader.identifier(potions.get(), path + ".potions"));
  }
}

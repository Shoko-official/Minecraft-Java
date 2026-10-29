package dev.shoko.minecraft.item.recipe.codec;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.SpecialKind;
import dev.shoko.minecraft.item.recipe.SpecialRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

/**
 * Reads the eleven serializers whose result depends on the content of the stacks (RC-R43 to
 * RC-R49e). The file only names the items that play each role: every field listed for the kind is
 * an ingredient, the nested object {@code shapes} is flattened with a dot.
 */
final class SpecialReader {

  private static final Map<SpecialKind, List<String>> PARTS = parts();

  private final RecipeReader reader;

  SpecialReader(RecipeReader reader) {
    this.reader = reader;
  }

  private static Map<SpecialKind, List<String>> parts() {
    Map<SpecialKind, List<String>> parts = new EnumMap<>(SpecialKind.class);
    parts.put(SpecialKind.BANNER_DUPLICATE, List.of("banner"));
    parts.put(SpecialKind.BOOK_CLONING, List.of("source", "material"));
    parts.put(SpecialKind.FIREWORK_ROCKET, List.of("shell", "fuel", "star"));
    parts.put(
        SpecialKind.FIREWORK_STAR,
        List.of(
            "fuel",
            "dye",
            "trail",
            "twinkle",
            "shapes.burst",
            "shapes.creeper",
            "shapes.large_ball",
            "shapes.star"));
    parts.put(SpecialKind.FIREWORK_STAR_FADE, List.of("target", "dye"));
    parts.put(SpecialKind.MAP_EXTENDING, List.of("map", "material"));
    parts.put(SpecialKind.REPAIR_ITEM, List.of());
    parts.put(SpecialKind.SHIELD_DECORATION, List.of("target", "banner"));
    parts.put(SpecialKind.DECORATED_POT, List.of("back", "front", "left", "right"));
    parts.put(SpecialKind.DYE, List.of("target", "dye"));
    parts.put(SpecialKind.IMBUE, List.of("source", "material"));
    return parts;
  }

  Recipe read(Identifier id, SpecialKind kind, JsonObject root) {
    String path = "$";
    SequencedMap<String, Ingredient> parts = new LinkedHashMap<>();
    for (String name : PARTS.get(kind)) {
      parts.put(name, reader.ingredient(part(root, name, path), path + "." + name));
    }
    return new SpecialRecipe(
        id, RecipeReader.group(root, path), kind, parts, reader.derivableResult(root, path));
  }

  /** A part is a field of the file, or a field of the {@code shapes} object. */
  private static JsonNode part(JsonObject root, String name, String path) {
    if (!name.startsWith("shapes.")) {
      return RecipeReader.required(root, name, path);
    }
    JsonObject shapes =
        RecipeReader.object(RecipeReader.required(root, "shapes", path), path + ".shapes");
    return RecipeReader.required(shapes, name.substring("shapes.".length()), path + ".shapes");
  }
}

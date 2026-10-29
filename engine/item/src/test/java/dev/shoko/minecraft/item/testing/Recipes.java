package dev.shoko.minecraft.item.testing;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.Holder;
import dev.shoko.minecraft.data.registry.HolderSet;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.TagKey;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.codec.RecipeCodec;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.registry.ItemType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

/** Builders of recipes and ingredients over the fake items. */
public final class Recipes {

  private Recipes() {}

  public static RecipeCodec codec() {
    return RecipeCodec.of(TestItems.catalog());
  }

  /** Decodes recipe JSON ({@code '} for quotes) found at {@code minecraft:<path>}. */
  public static Decoding<Recipe> decode(String path, String relaxedJson) {
    return decode(path, relaxedJson, TestAccess.standard());
  }

  public static Decoding<Recipe> decode(String path, String relaxedJson, RegistryAccess access) {
    return codec().decode(Identifier.minecraft(path), Json.parse(relaxedJson), access);
  }

  /** Decodes and casts a recipe that must load. */
  public static <R extends Recipe> R recipe(Class<R> type, String path, String relaxedJson) {
    Recipe recipe = Findings.value(decode(path, relaxedJson));
    assertThat(recipe).isInstanceOf(type);
    return type.cast(recipe);
  }

  /** A set of recipes from alternating paths and JSON texts; all must load. */
  public static RecipeSet set(String... pathsAndJson) {
    SequencedMap<Identifier, Recipe> recipes = new LinkedHashMap<>();
    for (int i = 0; i < pathsAndJson.length; i += 2) {
      Recipe recipe = Findings.value(decode(pathsAndJson[i], pathsAndJson[i + 1]));
      recipes.put(recipe.id(), recipe);
    }
    return RecipeSet.of(recipes);
  }

  /** An ingredient accepting any of these items. */
  public static Ingredient items(String... paths) {
    List<Holder<ItemType>> holders = new ArrayList<>();
    for (String path : paths) {
      holders.add(TestItems.catalog().registry().holder(TestItems.id(path)).orElseThrow());
    }
    return new Ingredient(HolderSet.list(holders));
  }

  /** An ingredient that is a tag of the standard fake access. */
  public static Ingredient tag(String path) {
    return new Ingredient(
        HolderSet.tag(
            new TagKey<>(ItemCatalog.REGISTRY, TestItems.id(path)), TestAccess.standard()));
  }
}

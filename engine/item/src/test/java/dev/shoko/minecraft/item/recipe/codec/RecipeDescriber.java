package dev.shoko.minecraft.item.recipe.codec;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.Holder;
import dev.shoko.minecraft.data.registry.HolderSet;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.TagKey;
import dev.shoko.minecraft.item.recipe.BrewingRecipe;
import dev.shoko.minecraft.item.recipe.CookingRecipe;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.recipe.ShapelessRecipe;
import dev.shoko.minecraft.item.recipe.SmithingTransformRecipe;
import dev.shoko.minecraft.item.recipe.SmithingTrimRecipe;
import dev.shoko.minecraft.item.recipe.SpecialRecipe;
import dev.shoko.minecraft.item.recipe.StonecuttingRecipe;
import dev.shoko.minecraft.item.recipe.TransmuteRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.registry.ItemType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Turns a recipe into a comparable list of values. The ingredients hold a {@code HolderSet} that
 * has no value equality, so two decodings of one file are compared through this description:
 * ingredients by tag or by item identifiers, stacks and identifiers by their own equality.
 */
final class RecipeDescriber {

  private RecipeDescriber() {}

  static String ingredient(Ingredient ingredient) {
    if (ingredient.isNone()) {
      return "none";
    }
    HolderSet<ItemType> set = ingredient.items();
    return set.tagKey().map(tag -> "#" + tag.id()).orElseGet(() -> holderIds(set).toString());
  }

  private static List<String> holderIds(HolderSet<ItemType> set) {
    List<String> ids = new ArrayList<>();
    for (Holder<ItemType> holder : set.holders()) {
      ids.add(holder.id().map(Identifier::toString).orElse("direct"));
    }
    return ids;
  }

  /** The item identifiers an ingredient accepts, tags expanded through the access. */
  static Set<Identifier> expand(Ingredient ingredient, RegistryAccess access) {
    Set<Identifier> ids = new TreeSet<>();
    if (ingredient.isNone()) {
      return ids;
    }
    HolderSet<ItemType> set = ingredient.items();
    if (set.tagKey().isPresent()) {
      TagKey<ItemType> tag = set.tagKey().get();
      ids.addAll(access.tagMembers(tag).orElseThrow());
    } else {
      for (Holder<ItemType> holder : set.holders()) {
        ids.add(holder.id().orElseThrow());
      }
    }
    return ids;
  }

  private static Object result(RecipeResult result) {
    return switch (result) {
      case RecipeResult.Fixed fixed -> fixed.stack();
      case RecipeResult.Derived ignored -> "derived";
    };
  }

  private static List<String> ingredients(List<Ingredient> list) {
    return list.stream().map(RecipeDescriber::ingredient).toList();
  }

  static List<Object> describe(Recipe recipe) {
    return switch (recipe) {
      case ShapedRecipe r -> shaped(r);
      case ShapelessRecipe r ->
          List.of(
              "shapeless",
              r.id(),
              r.group(),
              r.category(),
              ingredients(r.ingredients()),
              result(r.result()));
      case TransmuteRecipe r -> transmute(r);
      case SpecialRecipe r -> special(r);
      case CookingRecipe r -> cooking(r);
      case StonecuttingRecipe r ->
          List.of("stonecutting", r.id(), ingredient(r.ingredient()), r.result());
      case SmithingTransformRecipe r ->
          List.of(
              "transform",
              r.id(),
              ingredient(r.template()),
              ingredient(r.base()),
              ingredient(r.addition()),
              r.result());
      case SmithingTrimRecipe r ->
          List.of(
              "trim",
              r.id(),
              ingredient(r.template()),
              ingredient(r.base()),
              ingredient(r.addition()),
              r.pattern());
      case BrewingRecipe r ->
          List.of(
              "brewing",
              r.id(),
              ingredient(r.input().item()),
              r.input().potion(),
              ingredient(r.reagent()),
              r.output());
    };
  }

  private static List<Object> shaped(ShapedRecipe r) {
    return List.of(
        "shaped",
        r.id(),
        r.group(),
        r.category(),
        r.showNotification(),
        r.pattern().width(),
        r.pattern().height(),
        ingredients(r.pattern().cells()),
        result(r.result()));
  }

  private static List<Object> transmute(TransmuteRecipe r) {
    return List.of(
        "transmute",
        r.id(),
        r.group(),
        r.category(),
        ingredient(r.input()),
        ingredient(r.material()),
        r.materialCount(),
        r.addMaterialCountToResult(),
        result(r.result()));
  }

  private static List<Object> special(SpecialRecipe r) {
    Map<String, String> parts = new LinkedHashMap<>();
    r.parts().forEach((key, value) -> parts.put(key, ingredient(value)));
    return List.of("special", r.id(), r.group(), r.kind(), parts, result(r.result()));
  }

  private static List<Object> cooking(CookingRecipe r) {
    return List.of(
        "cooking",
        r.id(),
        r.group(),
        r.category(),
        r.type(),
        ingredient(r.ingredient()),
        r.result(),
        r.cookingTime(),
        r.experience());
  }
}

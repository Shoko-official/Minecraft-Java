package dev.shoko.minecraft.item.recipe.codec;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.recipe.RecipeCategory;
import dev.shoko.minecraft.item.recipe.ShapelessRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: RC-R3, RC-R9, RC-R18, V-R5: crafting_shapeless files
class RecipeCodecShapelessTest {

  private static ItemStack fixed(ShapelessRecipe recipe) {
    assertThat(recipe.result()).isInstanceOf(RecipeResult.Fixed.class);
    return ((RecipeResult.Fixed) recipe.result()).stack();
  }

  @Test
  void aPlanksFileHasOneTagIngredientAndAResultOfFour() {
    // spec: V-R5 (one tag ingredient, result x4, group, category building)
    ShapelessRecipe recipe =
        Recipes.recipe(
            ShapelessRecipe.class,
            "planks",
            "{'type':'minecraft:crafting_shapeless','category':'building','group':'planks',"
                + "'ingredients':['#minecraft:logs'],'result':{'count':4,'id':'minecraft:oak_planks'}}");
    assertThat(recipe.ingredients()).hasSize(1);
    assertThat(recipe.ingredients().getFirst().items().tagKey().orElseThrow().id())
        .isEqualTo(TestItems.id("logs"));
    assertThat(recipe.ingredients().getFirst().matches(TestItems.stack("oak_log"))).isTrue();
    assertThat(recipe.category()).isEqualTo(RecipeCategory.BUILDING);
    assertThat(recipe.group()).isEqualTo("planks");
    assertThat(fixed(recipe).count()).isEqualTo(4);
    assertThat(fixed(recipe).type()).isEqualTo(TestItems.type("oak_planks"));
  }

  @Test
  void ingredientListsOfOneToNineEntriesKeepFileOrder() {
    // spec: RC-R18 (1 to 9 ingredients)
    List<String> items =
        List.of(
            "stone",
            "dirt",
            "cobblestone",
            "stick",
            "diamond",
            "coal",
            "paper",
            "gunpowder",
            "redstone");
    for (int n = 1; n <= 9; n++) {
      List<String> quoted = new ArrayList<>();
      for (String item : items.subList(0, n)) {
        quoted.add("'minecraft:" + item + "'");
      }
      ShapelessRecipe recipe =
          Recipes.recipe(
              ShapelessRecipe.class,
              "n" + n,
              "{'type':'minecraft:crafting_shapeless','ingredients':["
                  + String.join(",", quoted)
                  + "],'result':{'id':'minecraft:stone_slab'}}");
      assertThat(recipe.ingredients()).as("n=%d", n).hasSize(n);
      for (int i = 0; i < n; i++) {
        assertThat(recipe.ingredients().get(i).matches(TestItems.stack(items.get(i))))
            .as("n=%d ingredient %d", n, i)
            .isTrue();
      }
    }
  }

  @Test
  void anIngredientMayBeAListOfItems() {
    // spec: RC-R9 (113 shapeless ingredients are lists of identifiers)
    ShapelessRecipe recipe =
        Recipes.recipe(
            ShapelessRecipe.class,
            "lists",
            "{'type':'minecraft:crafting_shapeless',"
                + "'ingredients':[['minecraft:stone','minecraft:dirt'],'minecraft:stick'],"
                + "'result':{'id':'minecraft:stone_slab'}}");
    assertThat(recipe.ingredients()).hasSize(2);
    assertThat(recipe.ingredients().get(0).matches(TestItems.stack("stone"))).isTrue();
    assertThat(recipe.ingredients().get(0).matches(TestItems.stack("dirt"))).isTrue();
    assertThat(recipe.ingredients().get(0).matches(TestItems.stack("stick"))).isFalse();
    assertThat(recipe.ingredients().get(1).matches(TestItems.stack("stick"))).isTrue();
  }

  @Test
  void duplicateIngredientsAreKept() {
    // spec: RC-R18 (doubles are two ingredients)
    ShapelessRecipe recipe =
        Recipes.recipe(
            ShapelessRecipe.class,
            "twice",
            "{'type':'minecraft:crafting_shapeless',"
                + "'ingredients':['minecraft:stone','minecraft:stone'],"
                + "'result':{'id':'minecraft:stone_slab'}}");
    assertThat(recipe.ingredients()).hasSize(2);
  }

  @Test
  void provisionalMissingOptionalFieldsTakeTheirDefaults() {
    // spec: RC-R6, RC-R7, RC-R28
    ShapelessRecipe recipe =
        Recipes.recipe(
            ShapelessRecipe.class,
            "plain",
            "{'type':'minecraft:crafting_shapeless','ingredients':['minecraft:stone'],"
                + "'result':{'id':'minecraft:stone_slab'}}");
    assertThat(recipe.group()).isEmpty();
    assertThat(recipe.category()).isEqualTo(RecipeCategory.MISC);
    assertThat(fixed(recipe).count()).isEqualTo(1);
  }
}

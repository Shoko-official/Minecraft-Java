package dev.shoko.minecraft.item.recipe.codec;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.recipe.CookingRecipe;
import dev.shoko.minecraft.item.recipe.RecipeCategory;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import org.junit.jupiter.api.Test;

// spec: RC-R3, RC-R25, V-R6: smelting, blasting, smoking and campfire_cooking files
class RecipeCodecCookingTest {

  private static String cooking(String serializer, String category, int time, String experience) {
    return "{'type':'minecraft:"
        + serializer
        + "','category':'"
        + category
        + "','cookingtime':"
        + time
        + ",'experience':"
        + experience
        + ",'ingredient':'minecraft:potato','result':{'id':'minecraft:baked_potato'}}";
  }

  @Test
  void eachOfTheFourSerializersGivesItsOwnMachineType() {
    // spec: RC-R25 (smelting, blasting, smoking, campfire_cooking carry their own type)
    Object[][] table = {
      {"smelting", RecipeType.SMELTING, 200},
      {"blasting", RecipeType.BLASTING, 200},
      {"smoking", RecipeType.SMOKING, 200},
      {"campfire_cooking", RecipeType.CAMPFIRE_COOKING, 600},
    };
    for (Object[] row : table) {
      String serializer = (String) row[0];
      CookingRecipe recipe =
          Recipes.recipe(
              CookingRecipe.class,
              "baked_potato_" + serializer,
              cooking(serializer, "food", (int) row[2], "0.35"));
      assertThat(recipe.type()).as(serializer).isEqualTo(row[1]);
      assertThat(recipe.cookingTime()).as(serializer).isEqualTo((int) row[2]);
    }
  }

  @Test
  void aBakedPotatoFileKeepsIngredientResultTimeExperienceAndCategory() {
    // spec: V-R6 (smelting, cookingtime 200, experience 0.35)
    CookingRecipe recipe =
        Recipes.recipe(
            CookingRecipe.class, "baked_potato", cooking("smelting", "food", 200, "0.35"));
    assertThat(recipe.category()).isEqualTo(RecipeCategory.FOOD);
    assertThat(recipe.cookingTime()).isEqualTo(200);
    assertThat(recipe.experience()).isEqualTo(0.35f);
    assertThat(recipe.ingredient().matches(TestItems.stack("potato"))).isTrue();
    assertThat(recipe.ingredient().matches(TestItems.stack("baked_potato"))).isFalse();
    assertThat(recipe.result().type()).isEqualTo(TestItems.type("baked_potato"));
    assertThat(recipe.id()).isEqualTo(TestItems.id("baked_potato"));
  }

  @Test
  void theExperienceValuesOfTheFilesAreKeptAsFloats() {
    // spec: RC-R25 (0.1, 0.15, 0.2, 0.3, 0.35, 0.7, 1.0, 2.0)
    for (String experience :
        new String[] {"0.1", "0.15", "0.2", "0.3", "0.35", "0.7", "1.0", "2.0"}) {
      CookingRecipe recipe =
          Recipes.recipe(CookingRecipe.class, "x", cooking("smelting", "blocks", 200, experience));
      assertThat(recipe.experience())
          .as(experience)
          .isEqualTo((float) Double.parseDouble(experience));
    }
  }

  @Test
  void theBlocksCategoryOfFurnaceFilesIsRead() {
    // spec: RC-R7 (blocks: smelting and blasting)
    CookingRecipe recipe =
        Recipes.recipe(CookingRecipe.class, "b", cooking("blasting", "blocks", 200, "0.7"));
    assertThat(recipe.category()).isEqualTo(RecipeCategory.BLOCKS);
  }

  @Test
  void anIngredientMayBeAListOfItems() {
    // spec: RC-R9, RC-R31 (3 smelting recipes have a list of identifiers)
    CookingRecipe recipe =
        Recipes.recipe(
            CookingRecipe.class,
            "tools",
            "{'type':'minecraft:smelting','cookingtime':200,'experience':0.1,"
                + "'ingredient':['minecraft:stone','minecraft:dirt'],"
                + "'result':{'id':'minecraft:baked_potato'}}");
    assertThat(recipe.ingredient().matches(TestItems.stack("stone"))).isTrue();
    assertThat(recipe.ingredient().matches(TestItems.stack("dirt"))).isTrue();
    assertThat(recipe.ingredient().matches(TestItems.stack("potato"))).isFalse();
  }

  @Test
  void provisionalGroupDefaultsToEmptyAndCategoryToMiscWhenAbsent() {
    // spec: RC-R6, RC-R7 (30 smelting and 23 blasting files have no category)
    CookingRecipe recipe =
        Recipes.recipe(
            CookingRecipe.class,
            "plain",
            "{'type':'minecraft:smelting','cookingtime':200,'experience':0.1,"
                + "'ingredient':'minecraft:potato','result':{'id':'minecraft:baked_potato'}}");
    assertThat(recipe.group()).isEmpty();
    assertThat(recipe.category()).isEqualTo(RecipeCategory.MISC);
    assertThat(recipe.result().count()).isEqualTo(1);
  }

  @Test
  void provisionalAGroupIsKept() {
    // spec: RC-R6
    CookingRecipe recipe =
        Recipes.recipe(
            CookingRecipe.class,
            "grouped",
            "{'type':'minecraft:smelting','group':'iron_ingot','cookingtime':200,"
                + "'experience':0.7,'ingredient':'minecraft:potato',"
                + "'result':{'id':'minecraft:baked_potato'}}");
    assertThat(recipe.group()).isEqualTo("iron_ingot");
  }

  @Test
  void provisionalAFileWithoutIngredientOrResultIsMissingAField() {
    // spec: RC-R3 (ingredient and result are in every cooking file)
    Findings.assertFailure(
        Recipes.decode(
            "noingredient",
            "{'type':'minecraft:smelting','cookingtime':200,'experience':0.1,"
                + "'result':{'id':'minecraft:baked_potato'}}"),
        FindingCode.MISSING_FIELD);
    Findings.assertFailure(
        Recipes.decode(
            "noresult",
            "{'type':'minecraft:smelting','cookingtime':200,'experience':0.1,"
                + "'ingredient':'minecraft:potato'}"),
        FindingCode.MISSING_FIELD);
  }
}

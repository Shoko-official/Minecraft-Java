package dev.shoko.minecraft.item.recipe.book;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.testing.Recipes;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

// spec: RC-R7, RC-R53: the recipe book category of a recipe. The correspondence (type, category)
// to the 13 categories is level S and not given by the data: only the facts that the skeleton
// states or that the category names make unambiguous are tested; the crafting, furnace and blast
// furnace rows of the mapping are left out on purpose.
class BookCategoriesTest {

  private static Recipe cooking(String serializer, String category) {
    return Recipes.recipe(
        Recipe.class,
        "c",
        "{'type':'minecraft:"
            + serializer
            + "','category':'"
            + category
            + "','cookingtime':200,'experience':0.1,'ingredient':'minecraft:potato',"
            + "'result':{'id':'minecraft:baked_potato'}}");
  }

  @Test
  void bookCategoryHasTheThirteenEntriesOfTheRegistry() {
    // spec: RC-R7 (recipe_book_category has 13 entries)
    assertThat(Arrays.stream(BookCategory.values()).map(Enum::name))
        .containsExactlyInAnyOrder(
            "CRAFTING_BUILDING_BLOCKS",
            "CRAFTING_REDSTONE",
            "CRAFTING_EQUIPMENT",
            "CRAFTING_MISC",
            "FURNACE_FOOD",
            "FURNACE_BLOCKS",
            "FURNACE_MISC",
            "BLAST_FURNACE_BLOCKS",
            "BLAST_FURNACE_MISC",
            "SMOKER_FOOD",
            "STONECUTTER",
            "SMITHING",
            "CAMPFIRE");
  }

  @Test
  void provisionalBrewingRecipesHaveNoBookCategory() {
    // spec: BookCategories#of Javadoc (RC-R53 lists them but no category of the registry fits)
    Recipe brewing =
        Recipes.recipe(
            Recipe.class,
            "brewing/x",
            "{'type':'minecraft:brewing','input':{'item':'minecraft:potion'},"
                + "'output':{'id':'minecraft:splash_potion'},"
                + "'reagent':{'item':'minecraft:gunpowder'}}");
    assertThat(BookCategories.of(brewing)).isEmpty();
  }

  @Test
  void provisionalStonecuttingRecipesAreInTheStonecutterCategory() {
    // spec: RC-R7 (category named after the machine)
    Recipe cutting =
        Recipes.recipe(
            Recipe.class,
            "cut",
            "{'type':'minecraft:stonecutting','ingredient':'minecraft:stone',"
                + "'result':{'id':'minecraft:stone_slab'}}");
    assertThat(BookCategories.of(cutting)).contains(BookCategory.STONECUTTER);
  }

  @Test
  void provisionalBothSmithingSerializersAreInTheSmithingCategory() {
    // spec: RC-R7, RC-R4 (smithing_transform and smithing_trim both run on the smithing table)
    Recipe transform =
        Recipes.recipe(
            Recipe.class,
            "t",
            "{'type':'minecraft:smithing_transform','addition':'#minecraft:netherite_tool_materials',"
                + "'base':'minecraft:diamond_axe','result':{'id':'minecraft:netherite_axe'},"
                + "'template':'minecraft:netherite_upgrade_smithing_template'}");
    Recipe trim =
        Recipes.recipe(
            Recipe.class,
            "u",
            "{'type':'minecraft:smithing_trim','addition':'#minecraft:trim_materials',"
                + "'base':'#minecraft:trimmable_armor','pattern':'minecraft:bolt',"
                + "'template':'minecraft:bolt_armor_trim_smithing_template'}");
    assertThat(BookCategories.of(transform)).contains(BookCategory.SMITHING);
    assertThat(BookCategories.of(trim)).contains(BookCategory.SMITHING);
  }

  @Test
  void provisionalCampfireRecipesAreInTheCampfireCategory() {
    // spec: RC-R7 (category named after the machine)
    assertThat(BookCategories.of(cooking("campfire_cooking", "food")))
        .contains(BookCategory.CAMPFIRE);
  }

  @Test
  void provisionalSmokerFoodRecipesAreInTheSmokerFoodCategory() {
    // spec: RC-R7 (smoker + food)
    assertThat(BookCategories.of(cooking("smoking", "food"))).contains(BookCategory.SMOKER_FOOD);
  }
}

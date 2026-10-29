package dev.shoko.minecraft.item.recipe.match;

import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.cooking;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.rid;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.set;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.shapeless;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.item.recipe.CookingRecipe;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// spec: item-recipes RC-R25, RC-R26a, RC-R31, RC-R54 (cooking lookups; provisional level S)
class CookingLookupTest {

  private static RecipeSet recipes() {
    return set(
        cooking(
            "smelt_potato",
            RecipeType.SMELTING,
            Recipes.items("potato"),
            TestItems.stack("baked_potato")),
        cooking(
            "smoke_potato",
            RecipeType.SMOKING,
            Recipes.items("potato"),
            TestItems.stack("baked_potato")),
        cooking(
            "campfire_potato",
            RecipeType.CAMPFIRE_COOKING,
            Recipes.items("potato"),
            TestItems.stack("baked_potato")),
        cooking(
            "blast_ingot",
            RecipeType.BLASTING,
            Recipes.items("iron_ingot"),
            TestItems.stack("gold_ingot")),
        cooking(
            "smelt_stone",
            RecipeType.SMELTING,
            Recipes.items("cobblestone", "stone"),
            TestItems.stack("stone_bricks")),
        shapeless("craft_potato", TestItems.stack("bowl"), Recipes.items("potato")));
  }

  private static Optional<String> idOf(RecipeType machine, ItemStack input) {
    return CookingLookup.of(recipes()).find(machine, input).map(recipe -> recipe.id().path());
  }

  @Test
  void provisionalEachMachineReadsOnlyItsOwnRecipeType() {
    // spec: RC-R26a, RC-R4
    ItemStack potato = TestItems.stack("potato");
    assertThat(idOf(RecipeType.SMELTING, potato)).contains("smelt_potato");
    assertThat(idOf(RecipeType.SMOKING, potato)).contains("smoke_potato");
    assertThat(idOf(RecipeType.CAMPFIRE_COOKING, potato)).contains("campfire_potato");
    assertThat(idOf(RecipeType.BLASTING, potato)).isEmpty();
  }

  @Test
  void provisionalAFurnaceDoesNotReadABlastingRecipe() {
    // spec: RC-R26a (and conversely)
    ItemStack ingot = TestItems.stack("iron_ingot");
    assertThat(idOf(RecipeType.BLASTING, ingot)).contains("blast_ingot");
    assertThat(idOf(RecipeType.SMELTING, ingot)).isEmpty();
    assertThat(idOf(RecipeType.SMOKING, ingot)).isEmpty();
    assertThat(idOf(RecipeType.CAMPFIRE_COOKING, ingot)).isEmpty();
  }

  @Test
  void provisionalAnItemNothingCooksGivesNoRecipe() {
    // spec: RC-R31
    for (RecipeType machine :
        List.of(
            RecipeType.SMELTING,
            RecipeType.BLASTING,
            RecipeType.SMOKING,
            RecipeType.CAMPFIRE_COOKING)) {
      assertThat(idOf(machine, TestItems.stack("dirt"))).as("%s", machine).isEmpty();
    }
  }

  @Test
  void provisionalTheEmptyStackGivesNoRecipe() {
    // spec: skeleton Javadoc of CookingLookup (empty when the stack is empty)
    for (RecipeType machine :
        List.of(
            RecipeType.SMELTING,
            RecipeType.BLASTING,
            RecipeType.SMOKING,
            RecipeType.CAMPFIRE_COOKING)) {
      assertThat(idOf(machine, ItemStack.empty())).as("%s", machine).isEmpty();
    }
  }

  @Test
  void aMachineThatDoesNotCookIsRefused() {
    // spec: skeleton Javadoc of CookingLookup (IllegalArgumentException)
    CookingLookup lookup = CookingLookup.of(recipes());
    ItemStack potato = TestItems.stack("potato");
    for (RecipeType machine :
        List.of(
            RecipeType.CRAFTING,
            RecipeType.STONECUTTING,
            RecipeType.SMITHING,
            RecipeType.BREWING)) {
      assertThatThrownBy(() -> lookup.find(machine, potato))
          .as("%s", machine)
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void provisionalAListIngredientIsFoundForEachOfItsElements() {
    // spec: RC-R9, RC-R31 (a recipe with a list of items is found by any of them)
    assertThat(idOf(RecipeType.SMELTING, TestItems.stack("stone"))).contains("smelt_stone");
    assertThat(idOf(RecipeType.SMELTING, TestItems.stack("cobblestone"))).contains("smelt_stone");
  }

  @Test
  void provisionalOnlyTheItemOfTheInputIsTested() {
    // spec: skeleton Javadoc of CookingLookup (only its item is tested)
    ItemStack many = TestItems.stack("potato", 5);
    ItemStack epic = TestItems.stack("potato", 1, "rarity", "'epic'");
    assertThat(idOf(RecipeType.SMELTING, many)).contains("smelt_potato");
    assertThat(idOf(RecipeType.SMELTING, epic)).contains("smelt_potato");
  }

  @Test
  void provisionalTheRecipeKeepsItsCookingParameters() {
    // spec: RC-R25 (cookingtime 200 on furnaces, 600 on the campfire; experience)
    CookingLookup lookup = CookingLookup.of(recipes());
    ItemStack potato = TestItems.stack("potato");
    CookingRecipe furnace = lookup.find(RecipeType.SMELTING, potato).orElseThrow();
    CookingRecipe campfire = lookup.find(RecipeType.CAMPFIRE_COOKING, potato).orElseThrow();
    assertThat(furnace.cookingTime()).isEqualTo(200);
    assertThat(campfire.cookingTime()).isEqualTo(600);
    assertThat(furnace.result()).isEqualTo(TestItems.stack("baked_potato"));
    assertThat(furnace.type()).isEqualTo(RecipeType.SMELTING);
  }

  @Test
  void provisionalTheFirstRecipeInResolutionOrderWinsForAPack() {
    // spec: RC-R54 (hypothesis H-ord-a), RC-R31 (conflict only possible in a pack)
    RecipeSet conflict =
        set(
            cooking(
                "b_potato", RecipeType.SMELTING, Recipes.items("potato"), TestItems.stack("bowl")),
            cooking(
                "a_potato",
                RecipeType.SMELTING,
                Recipes.items("potato"),
                TestItems.stack("bucket")));
    CookingRecipe found =
        CookingLookup.of(conflict)
            .find(RecipeType.SMELTING, TestItems.stack("potato"))
            .orElseThrow();
    assertThat(found.id()).isEqualTo(rid("a_potato"));
  }

  @Test
  void provisionalACraftingRecipeIsNotACookingRecipe() {
    // spec: RC-R4
    RecipeSet onlyCrafting =
        set(shapeless("craft_potato", TestItems.stack("bowl"), Recipes.items("potato")));
    assertThat(CookingLookup.of(onlyCrafting).find(RecipeType.SMELTING, TestItems.stack("potato")))
        .isEmpty();
  }
}

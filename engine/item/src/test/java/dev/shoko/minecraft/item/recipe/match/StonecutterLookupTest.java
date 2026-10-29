package dev.shoko.minecraft.item.recipe.match;

import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.cooking;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.rid;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.set;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.recipe.StonecuttingRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-recipes RC-R38 (stonecutter lookups; usage is level S, hence provisional)
class StonecutterLookupTest {

  private static StonecuttingRecipe cut(String id, Ingredient input, String result, int count) {
    return new StonecuttingRecipe(rid(id), input, TestItems.stack(result, count));
  }

  private static RecipeSet recipes() {
    return set(
        cut("stairs_from_stone", Recipes.items("stone"), "stone_stairs", 1),
        cut("slab_from_stone", Recipes.items("stone"), "stone_slab", 2),
        cut("bricks_from_stone", Recipes.items("stone"), "stone_bricks", 1),
        cut("slab_from_cobblestone", Recipes.items("cobblestone"), "stone_slab", 2),
        cut("stick_from_planks", Recipes.tag("planks"), "stick", 4),
        cooking(
            "smelt_stone",
            RecipeType.SMELTING,
            Recipes.items("stone"),
            TestItems.stack("stone_bricks")));
  }

  private static List<String> ids(ItemStack input) {
    return StonecutterLookup.of(recipes()).recipesFor(input).stream()
        .map(recipe -> recipe.id().path())
        .toList();
  }

  @Test
  void provisionalEveryRecipeWhoseIngredientAcceptsTheInputIsOffered() {
    // spec: RC-R38
    assertThat(ids(TestItems.stack("stone")))
        .containsExactlyInAnyOrder("stairs_from_stone", "slab_from_stone", "bricks_from_stone");
    assertThat(ids(TestItems.stack("cobblestone"))).containsExactly("slab_from_cobblestone");
  }

  @Test
  void provisionalTheRecipesComeInResolutionOrder() {
    // spec: RC-R38 (the order of proposal is not specified), RC-R54 (H-ord-a ascending identifier)
    assertThat(ids(TestItems.stack("stone")))
        .containsExactly("bricks_from_stone", "slab_from_stone", "stairs_from_stone");
  }

  @Test
  void provisionalAnInputNothingCutsOffersNothing() {
    // spec: RC-R38
    assertThat(ids(TestItems.stack("dirt"))).isEmpty();
  }

  @Test
  void provisionalTheEmptyStackOffersNothing() {
    // spec: skeleton Javadoc of StonecutterLookup
    assertThat(ids(ItemStack.empty())).isEmpty();
  }

  @Test
  void provisionalATagIngredientIsFoundForEveryMember() {
    // spec: RC-R9, RC-R38
    assertThat(ids(TestItems.stack("oak_planks"))).containsExactly("stick_from_planks");
    assertThat(ids(TestItems.stack("birch_planks"))).containsExactly("stick_from_planks");
  }

  @Test
  void provisionalOnlyTheItemOfTheInputIsTested() {
    // spec: RC-R17, RC-R38
    ItemStack many = TestItems.stack("cobblestone", 40);
    ItemStack epic = TestItems.stack("cobblestone", 1, "rarity", "'epic'");
    assertThat(ids(many)).containsExactly("slab_from_cobblestone");
    assertThat(ids(epic)).containsExactly("slab_from_cobblestone");
  }

  @Test
  void provisionalTheRecipesOfOtherMachinesAreNotOffered() {
    // spec: RC-R4 (the smelting recipe for stone is not a stonecutting recipe)
    assertThat(ids(TestItems.stack("stone"))).doesNotContain("smelt_stone");
  }

  @Test
  void provisionalTheOfferedRecipeKeepsItsResult() {
    // spec: RC-R38 (the stack obtained from one unit)
    StonecuttingRecipe slab =
        StonecutterLookup.of(recipes()).recipesFor(TestItems.stack("cobblestone")).getFirst();
    assertThat(slab.result()).isEqualTo(TestItems.stack("stone_slab", 2));
  }
}

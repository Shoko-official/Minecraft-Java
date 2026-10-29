package dev.shoko.minecraft.item.recipe.match;

import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.grid;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.shapeless;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.recipe.ShapelessRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-recipes RC-R18 to RC-R20 (shapeless recognition, provisional level S)
class ShapelessMatcherTest {

  private static ShapelessRecipe recipe(Ingredient... ingredients) {
    return shapeless("shapeless", TestItems.stack("chest"), ingredients);
  }

  @Test
  void provisionalOrderAndPositionDoNotMatter() {
    // spec: RC-R19
    ShapelessRecipe recipe =
        recipe(Recipes.items("iron_ingot"), Recipes.items("gold_ingot"), Recipes.items("stick"));
    assertThat(
            ShapelessMatcher.matches(recipe, grid("iron_ingot gold_ingot stick / - - - / - - -")))
        .isTrue();
    assertThat(
            ShapelessMatcher.matches(recipe, grid("stick gold_ingot iron_ingot / - - - / - - -")))
        .isTrue();
    assertThat(
            ShapelessMatcher.matches(recipe, grid("- - gold_ingot / stick - - / - iron_ingot -")))
        .isTrue();
    assertThat(ShapelessMatcher.matches(recipe, grid("gold_ingot - / stick iron_ingot")))
        .as("player grid")
        .isTrue();
  }

  @Test
  void provisionalMoreOrFewerStacksThanIngredientsDoNotMatch() {
    // spec: RC-R19 (as many non empty stacks as ingredients)
    ShapelessRecipe recipe = recipe(Recipes.items("iron_ingot"), Recipes.items("gold_ingot"));
    assertThat(ShapelessMatcher.matches(recipe, grid("iron_ingot - - / - - - / - - -"))).isFalse();
    assertThat(
            ShapelessMatcher.matches(recipe, grid("iron_ingot gold_ingot stick / - - - / - - -")))
        .isFalse();
    assertThat(ShapelessMatcher.matches(recipe, grid("- - - / - - - / - - -"))).isFalse();
  }

  @Test
  void provisionalDuplicateIngredientsNeedDuplicateStacks() {
    // spec: RC-R19 (multi-set pairing)
    ShapelessRecipe recipe =
        recipe(Recipes.items("stick"), Recipes.items("stick"), Recipes.items("stone"));
    assertThat(ShapelessMatcher.matches(recipe, grid("stick stone stick"))).isTrue();
    assertThat(ShapelessMatcher.matches(recipe, grid("stick stone stone"))).isFalse();
    assertThat(ShapelessMatcher.matches(recipe, grid("stick stick stick"))).isFalse();
  }

  @Test
  void provisionalOneStackSatisfiesOneIngredientOnly() {
    // spec: RC-R19 (a stack satisfies a single ingredient, whatever its count)
    ShapelessRecipe recipe = recipe(Recipes.items("stone"), Recipes.items("stone"));
    assertThat(ShapelessMatcher.matches(recipe, grid("stone:2 - / - -"))).isFalse();
    assertThat(ShapelessMatcher.matches(recipe, grid("stone:2 stone:1 / - -"))).isTrue();
  }

  @Test
  void provisionalAnIngredientIsSatisfiedByOneStackOnly() {
    // spec: RC-R19 (an ingredient is satisfied by a single stack)
    ShapelessRecipe recipe = recipe(Recipes.items("stone"));
    assertThat(ShapelessMatcher.matches(recipe, grid("stone stone / - -"))).isFalse();
    assertThat(ShapelessMatcher.matches(recipe, grid("- - / - stone:5"))).isTrue();
  }

  @Test
  void provisionalAnOverlapOfTagAndIdentifierNeedsACompleteAssignment() {
    // spec: RC-R20 (the answer does not depend on the order of trial)
    List<ShapelessRecipe> recipes =
        List.of(
            recipe(Recipes.tag("planks"), Recipes.items("oak_planks")),
            recipe(Recipes.items("oak_planks"), Recipes.tag("planks")));
    for (ShapelessRecipe recipe : recipes) {
      assertThat(ShapelessMatcher.matches(recipe, grid("oak_planks birch_planks"))).isTrue();
      assertThat(ShapelessMatcher.matches(recipe, grid("birch_planks oak_planks"))).isTrue();
      assertThat(ShapelessMatcher.matches(recipe, grid("oak_planks oak_planks"))).isTrue();
      assertThat(ShapelessMatcher.matches(recipe, grid("birch_planks birch_planks"))).isFalse();
    }
  }

  @Test
  void provisionalAThreeWayOverlapIsResolvedWhateverTheCellOrder() {
    // spec: RC-R20 (a complete pairing exists or not, independently of positions)
    ShapelessRecipe recipe =
        recipe(
            Recipes.tag("planks"),
            Recipes.items("oak_planks", "birch_planks"),
            Recipes.items("oak_planks"));
    List<String> cells = List.of("birch_planks", "birch_planks", "oak_planks");
    for (int shift = 0; shift < 3; shift++) {
      List<String> rotated = new ArrayList<>(cells.subList(shift, 3));
      rotated.addAll(cells.subList(0, shift));
      assertThat(ShapelessMatcher.matches(recipe, grid(String.join(" ", rotated)))).isTrue();
    }
    assertThat(ShapelessMatcher.matches(recipe, grid("birch_planks birch_planks birch_planks")))
        .isFalse();
  }

  @Test
  void provisionalASingleStackGridMatchesASingleIngredientRecipe() {
    // spec: RC-R19, RC-R18 (one ingredient, as for planks from logs)
    ShapelessRecipe recipe = recipe(Recipes.tag("logs"));
    assertThat(ShapelessMatcher.matches(recipe, grid("- - - / - oak_log - / - - -"))).isTrue();
    assertThat(ShapelessMatcher.matches(recipe, grid("- - - / - stone - / - - -"))).isFalse();
  }

  @Test
  void provisionalNineIngredientsFillTheWholeGrid() {
    // spec: RC-R18 (maximum of nine ingredients), RC-R19
    Ingredient[] nine = new Ingredient[9];
    for (int i = 0; i < nine.length; i++) {
      nine[i] = Recipes.items("stone");
    }
    ShapelessRecipe recipe = recipe(nine);
    assertThat(
            ShapelessMatcher.matches(
                recipe, grid("stone stone stone / stone stone stone / stone stone stone")))
        .isTrue();
    assertThat(
            ShapelessMatcher.matches(
                recipe, grid("stone stone stone / stone stone stone / stone stone -")))
        .isFalse();
  }

  @Test
  void provisionalANineIngredientRecipeNeverMatchesThePlayerGrid() {
    // spec: RC-R18, RC-R19 (four cells cannot hold nine stacks)
    Ingredient[] nine = new Ingredient[9];
    for (int i = 0; i < nine.length; i++) {
      nine[i] = Recipes.items("stone");
    }
    assertThat(ShapelessMatcher.matches(recipe(nine), grid("stone stone / stone stone"))).isFalse();
  }

  @Test
  void provisionalTheComponentsOfTheInputAreNotTested() {
    // spec: RC-R17, RC-R19 (only the item is tested)
    ShapelessRecipe recipe = recipe(Recipes.items("stone"), Recipes.items("dirt"));
    var enchanted = TestItems.stack("stone", 1, "enchantments", "{'minecraft:sharpness':1}");
    CraftingGrid grid = new CraftingGrid(2, 1, List.of(enchanted, TestItems.stack("dirt")));
    assertThat(ShapelessMatcher.matches(recipe, grid)).isTrue();
  }

  @Test
  void provisionalAWrongItemRefuses() {
    // spec: RC-R19
    ShapelessRecipe recipe = recipe(Recipes.items("stone"), Recipes.items("dirt"));
    assertThat(ShapelessMatcher.matches(recipe, grid("stone cobblestone"))).isFalse();
  }
}

package dev.shoko.minecraft.item.recipe.match;

import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.grid;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.shaped;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.sparse;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: item-recipes RC-R14 to RC-R17 (shaped recognition, all provisional level S)
class ShapedMatcherTest {

  private static ShapedRecipe table() {
    return shaped(
        "crafting_table", TestItems.stack("chest"), Map.of('#', Recipes.tag("planks")), "##", "##");
  }

  private static ShapedRecipe boat() {
    Map<Character, Ingredient> key = Map.of('#', Recipes.items("oak_planks"));
    return shaped("boat", TestItems.stack("chest"), key, "# #", "###");
  }

  /** Two planks wide and two tall, one corner empty: it differs from its left-right mirror. */
  private static ShapedRecipe corner() {
    Map<Character, Ingredient> key = Map.of('#', Recipes.items("oak_planks"));
    return shaped("corner", TestItems.stack("chest"), key, "##", "# ");
  }

  private static CraftingGrid squareAt(int offsetX, int offsetY, String item) {
    Map<Integer, String> cells = new HashMap<>();
    for (int dy = 0; dy < 2; dy++) {
      for (int dx = 0; dx < 2; dx++) {
        cells.put(offsetX + dx + (offsetY + dy) * 3, item);
      }
    }
    return sparse(3, 3, cells);
  }

  @Test
  void provisionalPlanksSquareMatchesInThePlayerGrid() {
    // spec: RC-R16, vector V-R13 (a 2x2 pattern fits the 2x2 window grid)
    assertThat(
            ShapedMatcher.matches(table(), grid("oak_planks oak_planks / oak_planks oak_planks")))
        .isTrue();
  }

  @Test
  void provisionalSquareMatchesAtEveryOffsetOfTheTable() {
    // spec: RC-R16 (translation by any whole offset), vector V-R13
    ShapedRecipe recipe = table();
    for (int oy = 0; oy < 2; oy++) {
      for (int ox = 0; ox < 2; ox++) {
        assertThat(ShapedMatcher.matches(recipe, squareAt(ox, oy, "oak_planks")))
            .as("offset %d,%d", ox, oy)
            .isTrue();
      }
    }
  }

  @Test
  void provisionalAStackOutsideThePatternRectangleRefuses() {
    // spec: RC-R16 (every cell outside the rectangle must be empty)
    ShapedRecipe recipe = table();
    assertThat(
            ShapedMatcher.matches(
                recipe, grid("oak_planks oak_planks - / oak_planks oak_planks - / - - stick")))
        .isFalse();
    assertThat(
            ShapedMatcher.matches(
                recipe, grid("oak_planks oak_planks stick / oak_planks oak_planks - / - - -")))
        .isFalse();
    assertThat(
            ShapedMatcher.matches(
                recipe, grid("stick oak_planks oak_planks / - oak_planks oak_planks / - - -")))
        .isFalse();
  }

  @Test
  void provisionalAPatternLargerThanTheGridNeverMatches() {
    // spec: RC-R16 (a 2x2 grid only recognizes patterns of width and height at most 2)
    Map<Character, Ingredient> key = Map.of('#', Recipes.items("oak_planks"));
    ShapedRecipe wide = shaped("wide", TestItems.stack("chest"), key, "###");
    ShapedRecipe tall = shaped("tall", TestItems.stack("chest"), key, "#", "#", "#");
    ShapedRecipe full = shaped("full", TestItems.stack("chest"), key, "###", "###", "###");
    CraftingGrid player = grid("oak_planks oak_planks / oak_planks oak_planks");
    assertThat(ShapedMatcher.matches(wide, player)).isFalse();
    assertThat(ShapedMatcher.matches(tall, player)).isFalse();
    assertThat(ShapedMatcher.matches(full, player)).isFalse();
  }

  @Test
  void provisionalAFullPatternMatchesTheTableExactly() {
    // spec: RC-R16
    Map<Character, Ingredient> key = Map.of('#', Recipes.items("stone"));
    ShapedRecipe full = shaped("furnace_like", TestItems.stack("chest"), key, "###", "# #", "###");
    assertThat(
            ShapedMatcher.matches(
                full, grid("stone stone stone / stone - stone / stone stone stone")))
        .isTrue();
    assertThat(
            ShapedMatcher.matches(
                full, grid("stone stone stone / stone stone stone / stone stone stone")))
        .isFalse();
  }

  @Test
  void provisionalAGapInsideThePatternMustStayEmpty() {
    // spec: RC-R14 (a space is an empty cell), RC-R16
    ShapedRecipe recipe = boat();
    assertThat(
            ShapedMatcher.matches(
                recipe, grid("- - - / oak_planks - oak_planks / oak_planks oak_planks oak_planks")))
        .isTrue();
    assertThat(
            ShapedMatcher.matches(
                recipe,
                grid(
                    "- - - / oak_planks oak_planks oak_planks / oak_planks oak_planks oak_planks")))
        .isFalse();
  }

  @Test
  void provisionalAMirroredPatternIsRecognized() {
    // spec: RC-R16, hypothesis of Questions ouvertes (mirror accepted for every shaped recipe)
    ShapedRecipe recipe = corner();
    assertThat(ShapedMatcher.matches(recipe, grid("oak_planks oak_planks / oak_planks -")))
        .as("as written")
        .isTrue();
    assertThat(ShapedMatcher.matches(recipe, grid("oak_planks oak_planks / - oak_planks")))
        .as("mirrored left to right")
        .isTrue();
  }

  @Test
  void provisionalOnlyTheLeftRightMirrorIsAccepted() {
    // spec: RC-R16 (symmetry gauche-droite only)
    ShapedRecipe recipe = corner();
    assertThat(ShapedMatcher.matches(recipe, grid("oak_planks - / oak_planks oak_planks")))
        .as("flipped top to bottom")
        .isFalse();
    assertThat(ShapedMatcher.matches(recipe, grid("- oak_planks / oak_planks oak_planks")))
        .as("rotated half a turn")
        .isFalse();
  }

  @Test
  void provisionalAMirroredPatternMatchesAtAnOffset() {
    // spec: RC-R16, vector V-R14 (mirror and translation combine)
    ShapedRecipe recipe = corner();
    CraftingGrid mirroredAtOffset = grid("- - - / - oak_planks oak_planks / - - oak_planks");
    assertThat(ShapedMatcher.matches(recipe, mirroredAtOffset)).isTrue();
  }

  @Test
  void provisionalAOneCellPatternMatchesAnywhere() {
    // spec: RC-R16 (pattern of one cell)
    Map<Character, Ingredient> key = Map.of('D', Recipes.items("diamond"));
    ShapedRecipe single = shaped("single", TestItems.stack("chest"), key, "D");
    for (int index = 0; index < 9; index++) {
      assertThat(ShapedMatcher.matches(single, sparse(3, 3, Map.of(index, "diamond"))))
          .as("cell %d", index)
          .isTrue();
    }
    assertThat(ShapedMatcher.matches(single, sparse(3, 3, Map.of(0, "diamond", 8, "diamond"))))
        .isFalse();
    assertThat(ShapedMatcher.matches(single, sparse(3, 3, Map.of()))).isFalse();
  }

  @Test
  void provisionalAWrongItemRefuses() {
    // spec: RC-R17 (an identifier ingredient needs that item)
    assertThat(
            ShapedMatcher.matches(
                boat(), grid("- - - / oak_planks - oak_planks / oak_planks dirt oak_planks")))
        .isFalse();
    assertThat(
            ShapedMatcher.matches(
                boat(),
                grid("- - - / oak_planks - oak_planks / oak_planks birch_planks oak_planks")))
        .isFalse();
  }

  @Test
  void provisionalATagIngredientAcceptsAnyMemberOfTheTag() {
    // spec: RC-R17 (a tag needs an item of the tag)
    ShapedRecipe recipe = table();
    assertThat(
            ShapedMatcher.matches(
                recipe, grid("oak_planks birch_planks / birch_planks oak_planks")))
        .isTrue();
    assertThat(ShapedMatcher.matches(recipe, grid("oak_planks birch_planks / birch_planks stone")))
        .isFalse();
  }

  @Test
  void provisionalAListIngredientAcceptsOnlyItsElements() {
    // spec: RC-R17 (a list needs one of its elements)
    Map<Character, Ingredient> key = Map.of('#', Recipes.items("stone", "cobblestone"));
    ShapedRecipe recipe = shaped("list", TestItems.stack("chest"), key, "##");
    assertThat(ShapedMatcher.matches(recipe, grid("stone cobblestone"))).isTrue();
    assertThat(ShapedMatcher.matches(recipe, grid("cobblestone cobblestone"))).isTrue();
    assertThat(ShapedMatcher.matches(recipe, grid("stone dirt"))).isFalse();
  }

  @Test
  void provisionalComponentsOfTheInputAreNotTested() {
    // spec: RC-R17 (the components of the input stack are not tested)
    Map<Character, Ingredient> key = Map.of('#', Recipes.items("stone"));
    ShapedRecipe recipe = shaped("pair", TestItems.stack("chest"), key, "##");
    ItemStack enchanted = TestItems.stack("stone", 1, "enchantments", "{'minecraft:sharpness':1}");
    CraftingGrid grid = new CraftingGrid(2, 1, List.of(enchanted, TestItems.stack("stone")));
    assertThat(ShapedMatcher.matches(recipe, grid)).isTrue();
  }

  @Test
  void provisionalTheCountOfTheInputIsNotTested() {
    // spec: RC-R17 (only the item is tested; consumption removes one unit later)
    Map<Character, Ingredient> key = Map.of('#', Recipes.items("stone"));
    ShapedRecipe recipe = shaped("pair", TestItems.stack("chest"), key, "##");
    assertThat(ShapedMatcher.matches(recipe, grid("stone:64 stone:3"))).isTrue();
  }

  @Test
  void provisionalAnEmptyGridMatchesNothing() {
    // spec: RC-R14 (a pattern has at least one non empty cell)
    assertThat(ShapedMatcher.matches(table(), grid("- - / - -"))).isFalse();
    assertThat(ShapedMatcher.matches(table(), grid("- - - / - - - / - - -"))).isFalse();
  }

  @Test
  void provisionalAPartialPatternDoesNotMatch() {
    // spec: RC-R16 (the whole pattern must be present)
    assertThat(
            ShapedMatcher.matches(
                table(), grid("oak_planks oak_planks - / oak_planks - - / - - -")))
        .isFalse();
  }
}

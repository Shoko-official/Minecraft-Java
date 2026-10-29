package dev.shoko.minecraft.item.recipe.codec;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.recipe.RecipeCategory;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.recipe.ingredient.ShapedPattern;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import org.junit.jupiter.api.Test;

// spec: RC-R3, RC-R6 to RC-R8, RC-R14, RC-R15, V-R2 to V-R4: crafting_shaped files
class RecipeCodecShapedTest {

  private static final String BOAT =
      "{'type':'minecraft:crafting_shaped','group':'boat','key':{'#':'minecraft:oak_planks'},"
          + "'pattern':['# #','###'],'result':{'id':'minecraft:chest'}}";

  private static ItemStack fixed(ShapedRecipe recipe) {
    assertThat(recipe.result()).isInstanceOf(RecipeResult.Fixed.class);
    return ((RecipeResult.Fixed) recipe.result()).stack();
  }

  private static boolean accepts(Ingredient ingredient, String... items) {
    for (String item : items) {
      if (!ingredient.matches(TestItems.stack(item))) {
        return false;
      }
    }
    return true;
  }

  @Test
  void aBoatShapedFileKeepsItsPatternKeyGroupAndResult() {
    // spec: V-R2 (pattern "# #", "###", key # a planks item, group, result)
    ShapedRecipe recipe = Recipes.recipe(ShapedRecipe.class, "boat", BOAT);
    assertThat(recipe.id()).isEqualTo(TestItems.id("boat"));
    assertThat(recipe.group()).isEqualTo("boat");
    ShapedPattern pattern = recipe.pattern();
    assertThat(pattern.width()).isEqualTo(3);
    assertThat(pattern.height()).isEqualTo(2);
    assertThat(pattern.cells()).hasSize(6);
    assertThat(accepts(pattern.cell(0, 0), "oak_planks")).isTrue();
    assertThat(pattern.cell(1, 0).isNone()).isTrue();
    assertThat(accepts(pattern.cell(2, 0), "oak_planks")).isTrue();
    for (int x = 0; x < 3; x++) {
      assertThat(accepts(pattern.cell(x, 1), "oak_planks")).isTrue();
    }
    assertThat(pattern.cell(0, 0).matches(TestItems.stack("stone"))).isFalse();
    assertThat(fixed(recipe).type()).isEqualTo(TestItems.type("chest"));
  }

  @Test
  void provisionalAShapedRecipeRunsOnTheCraftingMachine() {
    // spec: RC-R4
    assertThat(Recipes.recipe(ShapedRecipe.class, "boat", BOAT).type())
        .isEqualTo(RecipeType.CRAFTING);
  }

  @Test
  void aDoorFileKeepsCategoryGroupAndCount() {
    // spec: V-R3 (pattern ##/##/##, category redstone, group, result x3)
    ShapedRecipe recipe =
        Recipes.recipe(
            ShapedRecipe.class,
            "door",
            "{'type':'minecraft:crafting_shaped','category':'redstone','group':'wooden_door',"
                + "'key':{'#':'minecraft:oak_planks'},'pattern':['##','##','##'],"
                + "'result':{'count':3,'id':'minecraft:stone_slab'}}");
    assertThat(recipe.category()).isEqualTo(RecipeCategory.REDSTONE);
    assertThat(recipe.group()).isEqualTo("wooden_door");
    assertThat(recipe.pattern().width()).isEqualTo(2);
    assertThat(recipe.pattern().height()).isEqualTo(3);
    assertThat(fixed(recipe).count()).isEqualTo(3);
    assertThat(fixed(recipe).type()).isEqualTo(TestItems.type("stone_slab"));
  }

  @Test
  void theCategoryValuesOfCraftingFilesAreRead() {
    // spec: RC-R7 (building, redstone, equipment)
    for (String category : new String[] {"building", "redstone", "equipment"}) {
      ShapedRecipe recipe =
          Recipes.recipe(
              ShapedRecipe.class,
              "c",
              "{'type':'minecraft:crafting_shaped','category':'"
                  + category
                  + "','key':{'#':'minecraft:stone'},'pattern':['#'],"
                  + "'result':{'id':'minecraft:stone_slab'}}");
      assertThat(recipe.category().name()).isEqualToIgnoringCase(category);
    }
  }

  @Test
  void aTableFileWithATagKeyAndNoNotificationIsRead() {
    // spec: V-R4 (show_notification false, 2x2 pattern of the planks tag)
    ShapedRecipe recipe =
        Recipes.recipe(
            ShapedRecipe.class,
            "crafting_table",
            "{'type':'minecraft:crafting_shaped','key':{'#':'#minecraft:planks'},"
                + "'pattern':['##','##'],'result':{'id':'minecraft:chest'},"
                + "'show_notification':false}");
    assertThat(recipe.showNotification()).isFalse();
    assertThat(recipe.pattern().width()).isEqualTo(2);
    assertThat(recipe.pattern().height()).isEqualTo(2);
    for (Ingredient cell : recipe.pattern().cells()) {
      assertThat(cell.items().tagKey()).isPresent();
      assertThat(cell.items().tagKey().orElseThrow().id()).isEqualTo(TestItems.id("planks"));
    }
    assertThat(recipe.pattern().cell(1, 1).matches(TestItems.stack("birch_planks"))).isTrue();
  }

  @Test
  void aKeyMayBeAListOfItems() {
    // spec: RC-R9 (13 keys of shaped recipes are lists of identifiers)
    ShapedRecipe recipe =
        Recipes.recipe(
            ShapedRecipe.class,
            "list",
            "{'type':'minecraft:crafting_shaped',"
                + "'key':{'#':['minecraft:stone','minecraft:dirt']},"
                + "'pattern':['#'],'result':{'id':'minecraft:chest'}}");
    Ingredient cell = recipe.pattern().cell(0, 0);
    assertThat(accepts(cell, "stone", "dirt")).isTrue();
    assertThat(cell.matches(TestItems.stack("cobblestone"))).isFalse();
  }

  @Test
  void aSingleCellAndAFullThreeByThreePatternAreRead() {
    // spec: RC-R14 (width 1 to 3, height 1 to 3)
    ShapedRecipe one =
        Recipes.recipe(
            ShapedRecipe.class,
            "one",
            "{'type':'minecraft:crafting_shaped','key':{'#':'minecraft:stone'},"
                + "'pattern':['#'],'result':{'id':'minecraft:chest'}}");
    assertThat(one.pattern().width()).isEqualTo(1);
    assertThat(one.pattern().height()).isEqualTo(1);
    ShapedRecipe full =
        Recipes.recipe(
            ShapedRecipe.class,
            "full",
            "{'type':'minecraft:crafting_shaped','key':{'#':'minecraft:stone','X':'minecraft:dirt'},"
                + "'pattern':['###','#X#','###'],'result':{'id':'minecraft:chest'}}");
    assertThat(full.pattern().width()).isEqualTo(3);
    assertThat(full.pattern().height()).isEqualTo(3);
    assertThat(accepts(full.pattern().cell(1, 1), "dirt")).isTrue();
    assertThat(accepts(full.pattern().cell(0, 0), "stone")).isTrue();
  }

  @Test
  void aSpaceInThePatternIsAnEmptyCell() {
    // spec: RC-R14 (the space denotes an empty cell)
    ShapedRecipe recipe =
        Recipes.recipe(
            ShapedRecipe.class,
            "gaps",
            "{'type':'minecraft:crafting_shaped','key':{'#':'minecraft:stone'},"
                + "'pattern':[' # ','#  '],'result':{'id':'minecraft:chest'}}");
    assertThat(recipe.pattern().cell(0, 0).isNone()).isTrue();
    assertThat(recipe.pattern().cell(1, 0).isNone()).isFalse();
    assertThat(recipe.pattern().cell(2, 0).isNone()).isTrue();
    assertThat(recipe.pattern().cell(0, 1).isNone()).isFalse();
    assertThat(recipe.pattern().cell(1, 1).isNone()).isTrue();
  }

  @Test
  void theDecodedPatternIsAsWrittenAndNotMirrored() {
    // spec: RC-R14 (the file is decoded as written; the mirror is a matter of recognition)
    ShapedRecipe recipe =
        Recipes.recipe(
            ShapedRecipe.class,
            "asym",
            "{'type':'minecraft:crafting_shaped','key':{'A':'minecraft:stone','B':'minecraft:dirt'},"
                + "'pattern':['AB'],'result':{'id':'minecraft:chest'}}");
    assertThat(accepts(recipe.pattern().cell(0, 0), "stone")).isTrue();
    assertThat(accepts(recipe.pattern().cell(1, 0), "dirt")).isTrue();
  }

  @Test
  void provisionalMissingOptionalFieldsTakeTheirDefaults() {
    // spec: RC-R6 (group absent: empty string), RC-R7 (category absent: misc), RC-R8
    // (show_notification absent: true), RC-R28 (count absent: 1)
    ShapedRecipe recipe =
        Recipes.recipe(
            ShapedRecipe.class,
            "plain",
            "{'type':'minecraft:crafting_shaped','key':{'#':'minecraft:stone'},"
                + "'pattern':['#'],'result':{'id':'minecraft:chest'}}");
    assertThat(recipe.group()).isEmpty();
    assertThat(recipe.category()).isEqualTo(RecipeCategory.MISC);
    assertThat(recipe.showNotification()).isTrue();
    assertThat(fixed(recipe).count()).isEqualTo(1);
  }

  @Test
  void provisionalTheResultComposesPrototypeAndComponentsOfTheFile() {
    // spec: RC-R11b (components of the file cover the prototype)
    ShapedRecipe recipe =
        Recipes.recipe(
            ShapedRecipe.class,
            "rare",
            "{'type':'minecraft:crafting_shaped','key':{'#':'minecraft:stone'},'pattern':['#'],"
                + "'result':{'id':'minecraft:chest','count':2,"
                + "'components':{'minecraft:rarity':'epic'}}}");
    ItemStack expected =
        TestItems.stack("chest", 2)
            .withComponent(TestItems.id("rarity"), TestItems.value("rarity", "'epic'"));
    assertThat(fixed(recipe)).isEqualTo(expected);
  }
}

package dev.shoko.minecraft.item.recipe.match;

import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.cooking;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.grid;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.rid;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.set;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.shaped;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.shapeless;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.recipe.RecipeCategory;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.recipe.ShapelessRecipe;
import dev.shoko.minecraft.item.recipe.SpecialKind;
import dev.shoko.minecraft.item.recipe.SpecialRecipe;
import dev.shoko.minecraft.item.recipe.TransmuteRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.CountRange;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// spec: item-recipes RC-R16 to RC-R21, RC-R49a, RC-R49e, RC-R49f, RC-R54 (recognition of a grid
// among the CRAFTING recipes; every rule here is provisional level S)
class CraftingRecognizerTest {

  private static CraftingRecognizer recognizer(RecipeSet recipes) {
    return CraftingRecognizer.of(recipes, TestItems.catalog());
  }

  private static ShapedRecipe bricks() {
    Map<Character, Ingredient> key = Map.of('#', Recipes.items("stone"));
    return shaped("bricks", TestItems.stack("stone_bricks", 4), key, "##", "##");
  }

  private static SpecialRecipe repairItem() {
    return new SpecialRecipe(
        rid("repair_item"),
        "",
        SpecialKind.REPAIR_ITEM,
        new LinkedHashMap<>(),
        new RecipeResult.Derived());
  }

  /** A 3x3 grid with the center stack surrounded by eight copies of the other one. */
  private static CraftingGrid ring(ItemStack around, ItemStack center) {
    List<ItemStack> cells = new ArrayList<>();
    for (int i = 0; i < 9; i++) {
      cells.add(i == 4 ? center : around);
    }
    return new CraftingGrid(3, 3, cells);
  }

  @Test
  void provisionalAnOrdinaryResultIsACopyOfTheFileStack() {
    // spec: RC-R21 (count included)
    RecipeSet recipes = set(bricks());
    Optional<CraftingMatch> match =
        recognizer(recipes).recognize(grid("stone stone - / stone stone - / - - -"));
    assertThat(match).isPresent();
    assertThat(match.orElseThrow().recipe().id()).isEqualTo(rid("bricks"));
    assertThat(match.orElseThrow().result()).isEqualTo(TestItems.stack("stone_bricks", 4));
  }

  @Test
  void provisionalAShapelessResultIsACopyOfTheFileStack() {
    // spec: RC-R21
    RecipeSet recipes =
        set(shapeless("planks", TestItems.stack("oak_planks", 4), Recipes.tag("logs")));
    CraftingMatch match = recognizer(recipes).recognize(grid("oak_log - / - -")).orElseThrow();
    assertThat(match.result()).isEqualTo(TestItems.stack("oak_planks", 4));
  }

  @Test
  void provisionalTheComponentsOfTheInputsDoNotReachAnOrdinaryResult() {
    // spec: RC-R21
    ItemStack enchanted = TestItems.stack("stone", 1, "enchantments", "{'minecraft:sharpness':3}");
    CraftingGrid grid = new CraftingGrid(2, 2, List.of(enchanted, enchanted, enchanted, enchanted));
    CraftingMatch match = recognizer(set(bricks())).recognize(grid).orElseThrow();
    assertThat(match.result()).isEqualTo(TestItems.stack("stone_bricks", 4));
  }

  @Test
  void provisionalAnEmptyGridMatchesNothing() {
    // spec: skeleton Javadoc of CraftingRecognizer (an empty grid matches nothing)
    CraftingRecognizer recognizer =
        recognizer(set(bricks(), repairItem(), shapeless("any", TestItems.stack("chest"))));
    CraftingGrid empty = grid("- - - / - - - / - - -");
    assertThat(recognizer.recognize(empty)).isEmpty();
    assertThat(recognizer.allMatches(empty)).isEmpty();
  }

  @Test
  void provisionalAGridNoRecipeAcceptsMatchesNothing() {
    // spec: RC-R16, RC-R19
    CraftingRecognizer recognizer = recognizer(set(bricks()));
    assertThat(recognizer.recognize(grid("dirt dirt / dirt dirt"))).isEmpty();
    assertThat(recognizer.allMatches(grid("dirt dirt / dirt dirt"))).isEmpty();
  }

  @Test
  void provisionalAThreeByThreePatternIsNotRecognizedInThePlayerGrid() {
    // spec: RC-R16
    Map<Character, Ingredient> key = Map.of('#', Recipes.items("stone"));
    ShapedRecipe big = shaped("big", TestItems.stack("chest"), key, "###", "###", "###");
    CraftingRecognizer recognizer = recognizer(set(big));
    assertThat(recognizer.recognize(grid("stone stone / stone stone"))).isEmpty();
    assertThat(
            recognizer.recognize(grid("stone stone stone / stone stone stone / stone stone stone")))
        .isPresent();
  }

  @Test
  void provisionalTheFirstRecipeInAscendingIdentifierOrderWins() {
    // spec: RC-R54 (hypothesis H-ord-a), skeleton Javadoc of RecipeSet
    ShapelessRecipe b = shapeless("b_recipe", TestItems.stack("chest"), Recipes.items("stone"));
    ShapelessRecipe a = shapeless("a_recipe", TestItems.stack("bowl"), Recipes.items("stone"));
    CraftingMatch match = recognizer(set(b, a)).recognize(grid("stone - / - -")).orElseThrow();
    assertThat(match.recipe().id()).isEqualTo(rid("a_recipe"));
    assertThat(match.result()).isEqualTo(TestItems.stack("bowl"));
  }

  @Test
  void provisionalAllMatchesListsEveryRecipeInResolutionOrder() {
    // spec: RC-R54, probe PR12 (conflict detection)
    Map<Character, Ingredient> key = Map.of('#', Recipes.items("stone"));
    ShapedRecipe c = shaped("c_shaped", TestItems.stack("chest"), key, "#");
    ShapelessRecipe b = shapeless("b_shapeless", TestItems.stack("bowl"), Recipes.items("stone"));
    ShapelessRecipe a = shapeless("a_shapeless", TestItems.stack("bucket"), Recipes.items("stone"));
    ShapelessRecipe other = shapeless("d_dirt", TestItems.stack("stick"), Recipes.items("dirt"));
    CraftingRecognizer recognizer = recognizer(set(c, other, b, a));
    CraftingGrid grid = grid("- - - / - stone - / - - -");
    List<CraftingMatch> matches = recognizer.allMatches(grid);
    assertThat(matches.stream().map(m -> m.recipe().id().path()))
        .containsExactly("a_shapeless", "b_shapeless", "c_shaped");
    assertThat(recognizer.recognize(grid)).contains(matches.getFirst());
  }

  @Test
  void provisionalCookingRecipesAreNeverCraftingCandidates() {
    // spec: RC-R4 (a cooking recipe belongs to another machine)
    RecipeSet recipes =
        set(
            cooking(
                "baked",
                RecipeType.SMELTING,
                Recipes.items("potato"),
                TestItems.stack("baked_potato")),
            bricks());
    assertThat(recognizer(recipes).recognize(grid("potato - / - -"))).isEmpty();
  }

  @Test
  void provisionalATransmuteResultTakesTheItemOfTheFileAndTheComponentsOfTheInput() {
    // spec: RC-R49f (the shulker box keeps its content: here the wear and the enchantments)
    TransmuteRecipe recipe =
        new TransmuteRecipe(
            rid("axe_upgrade"),
            "",
            RecipeCategory.EQUIPMENT,
            Recipes.items("diamond_axe"),
            Recipes.items("netherite_ingot"),
            new CountRange(1, 1),
            false,
            new RecipeResult.Fixed(TestItems.stack("netherite_axe")));
    ItemStack axe =
        TestItems.stack("diamond_axe", 1, "damage", "100")
            .withComponent(
                TestItems.id("enchantments"),
                TestItems.value("enchantments", "{'minecraft:sharpness':2}"));
    CraftingGrid grid =
        new CraftingGrid(
            2,
            2,
            List.of(axe, TestItems.stack("netherite_ingot"), ItemStack.empty(), ItemStack.empty()));
    ItemStack result = recognizer(set(recipe)).recognize(grid).orElseThrow().result();
    assertThat(result.type()).isEqualTo(TestItems.type("netherite_axe"));
    assertThat(result.component(WellKnownComponents.DAMAGE))
        .isEqualTo(axe.component(WellKnownComponents.DAMAGE));
    assertThat(result.component(WellKnownComponents.ENCHANTMENTS))
        .isEqualTo(axe.component(WellKnownComponents.ENCHANTMENTS));
  }

  @Test
  void provisionalATransmuteDoesNotMatchWithoutItsMaterial() {
    // spec: RC-R49f
    TransmuteRecipe recipe =
        new TransmuteRecipe(
            rid("axe_upgrade"),
            "",
            RecipeCategory.EQUIPMENT,
            Recipes.items("diamond_axe"),
            Recipes.items("netherite_ingot"),
            new CountRange(1, 1),
            false,
            new RecipeResult.Fixed(TestItems.stack("netherite_axe")));
    assertThat(recognizer(set(recipe)).recognize(grid("diamond_axe - / - -"))).isEmpty();
    assertThat(recognizer(set(recipe)).recognize(grid("diamond_axe stone / - -"))).isEmpty();
  }

  @Test
  void provisionalRepairItemJoinsTwoWornItemsOfTheSameKind() {
    // spec: RC-R49a (remaining wear is the sum of the remaining durabilities plus a bonus; the
    // bonus percentage is not asserted)
    ItemStack first = TestItems.stack("stone_pickaxe", 1, "damage", "120");
    ItemStack second = TestItems.stack("stone_pickaxe", 1, "damage", "125");
    CraftingGrid grid =
        new CraftingGrid(2, 2, List.of(first, second, ItemStack.empty(), ItemStack.empty()));
    ItemStack result = recognizer(set(repairItem())).recognize(grid).orElseThrow().result();
    assertThat(result.type()).isEqualTo(TestItems.type("stone_pickaxe"));
    assertThat(result.damage()).isLessThan(120);
    assertThat(result.damage()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void provisionalRepairItemLosesTheEnchantments() {
    // spec: RC-R49a (enchantments are lost)
    ItemStack plain = TestItems.stack("stone_pickaxe", 1, "damage", "10");
    ItemStack enchanted =
        TestItems.stack("stone_pickaxe", 1, "damage", "10")
            .withComponent(
                TestItems.id("enchantments"),
                TestItems.value("enchantments", "{'minecraft:sharpness':2}"));
    CraftingGrid grid =
        new CraftingGrid(2, 2, List.of(plain, enchanted, ItemStack.empty(), ItemStack.empty()));
    ItemStack result = recognizer(set(repairItem())).recognize(grid).orElseThrow().result();
    assertThat(result.component(WellKnownComponents.ENCHANTMENTS))
        .isEqualTo(TestItems.stack("stone_pickaxe").component(WellKnownComponents.ENCHANTMENTS));
  }

  @Test
  void provisionalRepairItemNeedsTwoItemsOfTheSameKind() {
    // spec: RC-R49a (two items of the same type)
    CraftingRecognizer recognizer = recognizer(set(repairItem()));
    assertThat(recognizer.recognize(grid("stone_pickaxe - / - -"))).isEmpty();
    assertThat(recognizer.recognize(grid("stone_pickaxe diamond_axe / - -"))).isEmpty();
    assertThat(recognizer.recognize(grid("stone stone / - -"))).isEmpty();
  }

  @Test
  void provisionalImbueGivesBackTheStackWithThePotionOfTheSource() {
    // spec: RC-R49e (eight stacks around a lingering potion; the snowball plays the arrow here and
    // the parts are keyed by the field names of the file, as SpecialKind documents)
    LinkedHashMap<String, Ingredient> parts = new LinkedHashMap<>();
    parts.put("source", Recipes.items("lingering_potion"));
    parts.put("material", Recipes.items("snowball"));
    SpecialRecipe imbue =
        new SpecialRecipe(
            rid("tipped"),
            "",
            SpecialKind.IMBUE,
            parts,
            new RecipeResult.Fixed(TestItems.stack("snowball", 8)));
    ItemStack potion =
        TestItems.stack("lingering_potion", 1, "potion_contents", "{'potion':'minecraft:awkward'}");
    CraftingGrid grid = ring(TestItems.stack("snowball"), potion);
    ItemStack result = recognizer(set(imbue)).recognize(grid).orElseThrow().result();
    assertThat(result.type()).isEqualTo(TestItems.type("snowball"));
    assertThat(result.count()).isEqualTo(8);
    assertThat(result.component(WellKnownComponents.POTION_CONTENTS))
        .isEqualTo(potion.component(WellKnownComponents.POTION_CONTENTS));
  }

  @Test
  void provisionalImbueDoesNotMatchAnIncompleteRing() {
    // spec: RC-R49e (eight arrows around the potion)
    LinkedHashMap<String, Ingredient> parts = new LinkedHashMap<>();
    parts.put("source", Recipes.items("lingering_potion"));
    parts.put("material", Recipes.items("snowball"));
    SpecialRecipe imbue =
        new SpecialRecipe(
            rid("tipped"),
            "",
            SpecialKind.IMBUE,
            parts,
            new RecipeResult.Fixed(TestItems.stack("snowball", 8)));
    assertThat(
            recognizer(set(imbue))
                .recognize(
                    grid("snowball snowball snowball / snowball lingering_potion - / - - -")))
        .isEmpty();
  }

  @Test
  void provisionalJsonRecipesAreRecognizedEndToEnd() {
    // spec: RC-R14 to RC-R21, RC-R54 (the whole path: JSON, codec, set, recognizer)
    RecipeSet recipes =
        Recipes.set(
            "crafting_table",
            "{'type':'minecraft:crafting_shaped','key':{'#':'#minecraft:planks'},"
                + "'pattern':['##','##'],'result':{'id':'minecraft:chest'}}",
            "sticks",
            "{'type':'minecraft:crafting_shapeless','ingredients':['#minecraft:planks'],"
                + "'result':{'count':4,'id':'minecraft:stick'}}");
    CraftingRecognizer recognizer = recognizer(recipes);
    CraftingMatch table =
        recognizer
            .recognize(grid("birch_planks oak_planks / oak_planks birch_planks"))
            .orElseThrow();
    assertThat(table.result()).isEqualTo(TestItems.stack("chest"));
    CraftingMatch sticks = recognizer.recognize(grid("- - / - birch_planks")).orElseThrow();
    assertThat(sticks.result()).isEqualTo(TestItems.stack("stick", 4));
  }
}

package dev.shoko.minecraft.item.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.ingredient.BrewingInput;
import dev.shoko.minecraft.item.recipe.ingredient.CountRange;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.PotionMatch;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.recipe.ingredient.ShapedPattern;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: RC-R2, RC-R4, RC-R7: the machine that runs each recipe record and the closed enums
class RecipeTypesTest {

  private static final Identifier ID = Identifier.minecraft("r");

  private static Ingredient stone() {
    return Recipes.items("stone");
  }

  private static RecipeResult fixed() {
    return new RecipeResult.Fixed(TestItems.stack("stone_slab", 2));
  }

  @Test
  void recipeTypeHasTheEightMachinesOfTheRegistry() {
    // spec: RC-R2 (recipe_type has 8 entries)
    assertThat(Arrays.stream(RecipeType.values()).map(Enum::name))
        .containsExactly(
            "CRAFTING",
            "SMELTING",
            "BLASTING",
            "SMOKING",
            "CAMPFIRE_COOKING",
            "STONECUTTING",
            "SMITHING",
            "BREWING");
  }

  @Test
  void specialKindHasTheElevenSerializersWhoseResultDependsOnContent() {
    // spec: RC-R3 (eleven crafting serializers other than shaped, shapeless and transmute)
    assertThat(SpecialKind.values()).hasSize(11);
  }

  @Test
  void recipeCategoryHasTheSixValuesOfTheFiles() {
    // spec: RC-R7 (building, redstone, equipment, misc, food, blocks)
    assertThat(Arrays.stream(RecipeCategory.values()).map(Enum::name))
        .containsExactlyInAnyOrder("BUILDING", "REDSTONE", "EQUIPMENT", "MISC", "FOOD", "BLOCKS");
  }

  @Test
  void provisionalShapedShapelessAndTransmuteRunOnTheCraftingMachine() {
    // spec: RC-R4
    ShapedRecipe shaped =
        new ShapedRecipe(
            ID, "", RecipeCategory.MISC, true, new ShapedPattern(1, 1, List.of(stone())), fixed());
    ShapelessRecipe shapeless =
        new ShapelessRecipe(ID, "", RecipeCategory.MISC, List.of(stone()), fixed());
    TransmuteRecipe transmute =
        new TransmuteRecipe(
            ID, "", RecipeCategory.MISC, stone(), stone(), new CountRange(1, 1), false, fixed());
    assertThat(shaped.type()).isEqualTo(RecipeType.CRAFTING);
    assertThat(shapeless.type()).isEqualTo(RecipeType.CRAFTING);
    assertThat(transmute.type()).isEqualTo(RecipeType.CRAFTING);
  }

  @Test
  void provisionalEverySpecialKindRunsOnTheCraftingMachine() {
    // spec: RC-R4 (crafting_special_*, crafting_dye, crafting_imbue, crafting_decorated_pot)
    for (SpecialKind kind : SpecialKind.values()) {
      SpecialRecipe special =
          new SpecialRecipe(ID, "", kind, new LinkedHashMap<>(), new RecipeResult.Derived());
      assertThat(special.type()).as("%s", kind).isEqualTo(RecipeType.CRAFTING);
    }
  }

  @Test
  void provisionalCookingRecipesCarryTheirMachine() {
    // spec: RC-R4, RC-R25 (smelting, blasting, smoking and campfire_cooking keep their own type)
    ItemStack out = TestItems.stack("baked_potato");
    for (RecipeType machine :
        List.of(
            RecipeType.SMELTING,
            RecipeType.BLASTING,
            RecipeType.SMOKING,
            RecipeType.CAMPFIRE_COOKING)) {
      CookingRecipe cooking =
          new CookingRecipe(ID, "", RecipeCategory.FOOD, machine, stone(), out, 200, 0.35f);
      assertThat(cooking.type()).isEqualTo(machine);
      assertThat(cooking.id()).isEqualTo(ID);
    }
  }

  @Test
  void provisionalStonecuttingSmithingAndBrewingHaveTheirOwnMachine() {
    // spec: RC-R4 (smithing_transform and smithing_trim both run on the smithing table)
    StonecuttingRecipe cutting = new StonecuttingRecipe(ID, stone(), TestItems.stack("stone_slab"));
    SmithingTransformRecipe transform =
        new SmithingTransformRecipe(
            ID, stone(), stone(), stone(), TestItems.stack("netherite_axe"));
    SmithingTrimRecipe trim =
        new SmithingTrimRecipe(ID, stone(), stone(), stone(), Identifier.minecraft("bolt"));
    BrewingRecipe brewing =
        new BrewingRecipe(
            ID,
            new BrewingInput(Recipes.items("potion"), new PotionMatch.Any()),
            Recipes.items("blaze_powder"),
            TestItems.stack("splash_potion"));
    assertThat(cutting.type()).isEqualTo(RecipeType.STONECUTTING);
    assertThat(transform.type()).isEqualTo(RecipeType.SMITHING);
    assertThat(trim.type()).isEqualTo(RecipeType.SMITHING);
    assertThat(brewing.type()).isEqualTo(RecipeType.BREWING);
  }
}

package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.container.BrewingStandData;
import dev.shoko.minecraft.item.container.CampfireData;
import dev.shoko.minecraft.item.container.ContainerContents;
import dev.shoko.minecraft.item.container.FurnaceData;
import dev.shoko.minecraft.item.container.LootBinding;
import dev.shoko.minecraft.item.menu.click.ClickOperation;
import dev.shoko.minecraft.item.menu.click.MouseButton;
import dev.shoko.minecraft.item.recipe.RecipeCategory;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.ShapelessRecipe;
import dev.shoko.minecraft.item.recipe.SpecialKind;
import dev.shoko.minecraft.item.recipe.SpecialRecipe;
import dev.shoko.minecraft.item.recipe.codec.RecipeLoadFinding;
import dev.shoko.minecraft.item.recipe.codec.RecipeLoadResult;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.recipe.match.Consumption;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

// spec: ADR 0004 and the Javadoc of the records ("unmodifiable", "immutable"): a record built from
// a
// list keeps its own copy, so a later change to the list given is not seen (finding 13).
class ImmutabilityHuntTest {

  private static List<ItemStack> emptySlots(int count) {
    return new ArrayList<>(Collections.nCopies(count, ItemStack.empty()));
  }

  @Test
  void aBrewingStandKeepsItsOwnSlots() {
    List<ItemStack> slots = emptySlots(5);
    BrewingStandData stand = new BrewingStandData(slots, 0, 0);
    slots.set(0, TestItems.stack("stone", 5));
    slots.add(TestItems.stack("dirt", 1));
    assertThat(stand.slots()).hasSize(5);
    assertThat(stand.slots().get(0).isEmpty()).isTrue();
  }

  @Test
  void containerContentsKeepItsOwnSlots() {
    List<ItemStack> slots = emptySlots(3);
    ContainerContents contents = new ContainerContents(slots, new LootBinding.None());
    slots.set(1, TestItems.stack("dirt", 3));
    assertThat(contents.slots().get(1).isEmpty()).isTrue();
  }

  @Test
  void aFurnaceKeepsItsOwnSlotsAndRecipesUsed() {
    List<ItemStack> slots = emptySlots(3);
    SequencedMap<Identifier, Integer> used = new LinkedHashMap<>();
    used.put(Identifier.minecraft("baked_potato"), 2);
    FurnaceData furnace = new FurnaceData(slots, 0, 0, 0, 0, used);
    slots.set(0, TestItems.stack("potato", 4));
    used.put(Identifier.minecraft("other"), 9);
    used.remove(Identifier.minecraft("baked_potato"));
    assertThat(furnace.slots().get(0).isEmpty()).isTrue();
    assertThat(furnace.recipesUsed()).containsOnlyKeys(Identifier.minecraft("baked_potato"));
  }

  @Test
  void aCampfireKeepsItsOwnLists() {
    List<ItemStack> slots = emptySlots(4);
    List<Integer> times = new ArrayList<>(List.of(0, 0, 0, 0));
    List<Integer> totals = new ArrayList<>(List.of(600, 600, 600, 600));
    CampfireData campfire = new CampfireData(slots, times, totals);
    slots.set(2, TestItems.stack("potato", 1));
    times.set(0, 99);
    totals.clear();
    assertThat(campfire.slots().get(2).isEmpty()).isTrue();
    assertThat(campfire.cookingTimes()).containsExactly(0, 0, 0, 0);
    assertThat(campfire.cookingTotalTimes()).containsExactly(600, 600, 600, 600);
  }

  @Test
  void aShapelessRecipeKeepsItsOwnIngredients() {
    List<Ingredient> ingredients = new ArrayList<>(List.of(Recipes.items("stone")));
    ShapelessRecipe recipe =
        new ShapelessRecipe(
            Identifier.minecraft("x"),
            "",
            RecipeCategory.MISC,
            ingredients,
            new RecipeResult.Fixed(TestItems.stack("stone")));
    ingredients.add(Recipes.items("dirt"));
    assertThat(recipe.ingredients()).hasSize(1);
  }

  @Test
  void aSpecialRecipeKeepsItsOwnParts() {
    SequencedMap<String, Ingredient> parts = new LinkedHashMap<>();
    parts.put("banner", Recipes.items("white_banner"));
    SpecialRecipe recipe =
        new SpecialRecipe(
            Identifier.minecraft("x"),
            "",
            SpecialKind.BANNER_DUPLICATE,
            parts,
            new RecipeResult.Derived());
    parts.put("extra", Recipes.items("stone"));
    parts.remove("banner");
    assertThat(recipe.parts()).containsOnlyKeys("banner");
  }

  @Test
  void aDragKeepsItsOwnSlots() {
    List<Integer> slots = new ArrayList<>(List.of(0, 1));
    ClickOperation.Drag drag = new ClickOperation.Drag(MouseButton.LEFT, slots);
    slots.add(2);
    slots.set(0, 40);
    assertThat(drag.slots()).containsExactly(0, 1);
  }

  @Test
  void aConsumptionKeepsItsOwnLists() {
    List<ItemStack> grid = emptySlots(4);
    List<ItemStack> remainders = emptySlots(4);
    Consumption consumption = new Consumption(grid, remainders);
    grid.set(0, TestItems.stack("stone", 1));
    remainders.set(1, TestItems.stack("bowl", 1));
    assertThat(consumption.grid().get(0).isEmpty()).isTrue();
    assertThat(consumption.remainders().get(1).isEmpty()).isTrue();
  }

  @Test
  void aLoadResultKeepsItsOwnFindings() {
    List<RecipeLoadFinding> findings = new ArrayList<>();
    findings.add(
        new RecipeLoadFinding(
            Identifier.minecraft("bad"), new Finding(FindingCode.MISSING_FIELD, "$", "m")));
    RecipeLoadResult result = new RecipeLoadResult(RecipeSet.of(new LinkedHashMap<>()), findings);
    findings.clear();
    assertThat(result.findings()).hasSize(1);
  }
}

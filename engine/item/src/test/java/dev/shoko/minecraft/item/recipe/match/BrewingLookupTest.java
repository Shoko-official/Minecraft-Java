package dev.shoko.minecraft.item.recipe.match;

import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.rid;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.set;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.recipe.BrewingRecipe;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.ingredient.BrewingInput;
import dev.shoko.minecraft.item.recipe.ingredient.PotionMatch;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// spec: item-recipes RC-R10, RC-R39 to RC-R42 (brewing lookups, provisional level S). The bottle
// filter of the slots and the infusion itself belong to game and are not tested.
class BrewingLookupTest {

  private static ItemStack potion(String bottle, String potion) {
    return TestItems.stack(bottle, 1, "potion_contents", "{'potion':'minecraft:" + potion + "'}");
  }

  private static RecipeSet recipes() {
    return set(
        new BrewingRecipe(
            rid("brewing/potion_awkward_blaze_powder"),
            new BrewingInput(Recipes.items("potion"), new PotionMatch.Exact(rid("awkward"))),
            Recipes.items("blaze_powder"),
            potion("potion", "strength")),
        new BrewingRecipe(
            rid("brewing/splash_awkward_gunpowder"),
            new BrewingInput(Recipes.items("potion"), new PotionMatch.Exact(rid("awkward"))),
            Recipes.items("gunpowder"),
            potion("splash_potion", "awkward")),
        new BrewingRecipe(
            rid("brewing/any_splash_redstone"),
            new BrewingInput(Recipes.items("splash_potion"), new PotionMatch.Any()),
            Recipes.items("redstone"),
            TestItems.stack("lingering_potion")));
  }

  private static Optional<String> found(ItemStack bottle, ItemStack reagent) {
    return BrewingLookup.of(recipes()).find(bottle, reagent).map(recipe -> recipe.id().path());
  }

  @Test
  void provisionalTheBottleItemThePotionAndTheReagentSelectTheRecipe() {
    // spec: RC-R10, RC-R40, vector V-R8
    ItemStack awkward = potion("potion", "awkward");
    assertThat(found(awkward, TestItems.stack("blaze_powder")))
        .contains("brewing/potion_awkward_blaze_powder");
    assertThat(found(awkward, TestItems.stack("gunpowder")))
        .contains("brewing/splash_awkward_gunpowder");
  }

  @Test
  void provisionalAnotherPotionDoesNotMatchAnExactInput() {
    // spec: RC-R10 (potion_contents.potions must be the potion of the input)
    assertThat(found(potion("potion", "water"), TestItems.stack("blaze_powder"))).isEmpty();
    assertThat(found(TestItems.stack("potion"), TestItems.stack("blaze_powder"))).isEmpty();
  }

  @Test
  void provisionalAnotherReagentDoesNotMatch() {
    // spec: RC-R40 (the reagent must match reagent.item)
    assertThat(found(potion("potion", "awkward"), TestItems.stack("stone"))).isEmpty();
    assertThat(found(potion("potion", "awkward"), TestItems.stack("nether_wart"))).isEmpty();
  }

  @Test
  void provisionalAnotherBottleItemDoesNotMatch() {
    // spec: RC-R10 (the input item must match)
    assertThat(found(potion("splash_potion", "awkward"), TestItems.stack("blaze_powder")))
        .isEmpty();
    assertThat(found(TestItems.stack("glass_bottle"), TestItems.stack("blaze_powder"))).isEmpty();
  }

  @Test
  void provisionalAnyPotionMatchIgnoresThePotionContents() {
    // spec: RC-R10 (the condition on potion_contents is optional)
    assertThat(found(potion("splash_potion", "healing"), TestItems.stack("redstone")))
        .contains("brewing/any_splash_redstone");
    assertThat(found(TestItems.stack("splash_potion"), TestItems.stack("redstone")))
        .contains("brewing/any_splash_redstone");
  }

  @Test
  void provisionalEmptyStacksGiveNoRecipe() {
    // spec: RC-R40
    assertThat(found(ItemStack.empty(), TestItems.stack("blaze_powder"))).isEmpty();
    assertThat(found(potion("potion", "awkward"), ItemStack.empty())).isEmpty();
    assertThat(found(ItemStack.empty(), ItemStack.empty())).isEmpty();
  }

  @Test
  void provisionalBrewingReplacesTheBottleWithTheOutput() {
    // spec: RC-R40 (the output item and the components of the output)
    BrewingLookup lookup = BrewingLookup.of(recipes());
    ItemStack awkward = potion("potion", "awkward");
    BrewingRecipe recipe = lookup.find(awkward, TestItems.stack("blaze_powder")).orElseThrow();
    ItemStack result = lookup.brew(recipe, awkward);
    assertThat(result.type()).isEqualTo(TestItems.type("potion"));
    assertThat(result.count()).isEqualTo(1);
    assertThat(result.component(WellKnownComponents.POTION_CONTENTS))
        .isEqualTo(potion("potion", "strength").component(WellKnownComponents.POTION_CONTENTS));
  }

  @Test
  void provisionalBrewingChangesTheItemWhenTheOutputIsAnotherBottle() {
    // spec: RC-R41 (gunpowder turns a potion into a splash potion by data), RC-R40
    BrewingLookup lookup = BrewingLookup.of(recipes());
    ItemStack awkward = potion("potion", "awkward");
    BrewingRecipe recipe = lookup.find(awkward, TestItems.stack("gunpowder")).orElseThrow();
    ItemStack result = lookup.brew(recipe, awkward);
    assertThat(result.type()).isEqualTo(TestItems.type("splash_potion"));
    assertThat(result.component(WellKnownComponents.POTION_CONTENTS))
        .isEqualTo(
            potion("splash_potion", "awkward").component(WellKnownComponents.POTION_CONTENTS));
  }

  @Test
  void provisionalComponentsOfTheInputNotReplacedByTheOutputAreCopied() {
    // spec: RC-R40 (provisional: the components of the input that the output does not set are
    // copied)
    BrewingLookup lookup = BrewingLookup.of(recipes());
    ItemStack epic =
        potion("potion", "awkward")
            .withComponent(TestItems.id("rarity"), TestItems.value("rarity", "'epic'"));
    BrewingRecipe recipe = lookup.find(epic, TestItems.stack("blaze_powder")).orElseThrow();
    ItemStack result = lookup.brew(recipe, epic);
    assertThat(result.component(TestItems.id("rarity")))
        .isEqualTo(epic.component(TestItems.id("rarity")));
  }

  @Test
  void provisionalTheBrewedBottleHasACountOfOne() {
    // spec: skeleton Javadoc of BrewingLookup#brew (a count of 1)
    BrewingLookup lookup = BrewingLookup.of(recipes());
    ItemStack two = potion("potion", "awkward").withCount(2);
    BrewingRecipe recipe = lookup.find(two, TestItems.stack("blaze_powder")).orElseThrow();
    assertThat(lookup.brew(recipe, two).count()).isEqualTo(1);
  }

  @Test
  void provisionalIsReagentTellsWhichStacksSomeRecipeAccepts() {
    // spec: RC-R40 (filter of the ingredient slot)
    BrewingLookup lookup = BrewingLookup.of(recipes());
    assertThat(lookup.isReagent(TestItems.stack("blaze_powder"))).isTrue();
    assertThat(lookup.isReagent(TestItems.stack("gunpowder"))).isTrue();
    assertThat(lookup.isReagent(TestItems.stack("redstone"))).isTrue();
    assertThat(lookup.isReagent(TestItems.stack("stone"))).isFalse();
    assertThat(lookup.isReagent(ItemStack.empty())).isFalse();
  }
}

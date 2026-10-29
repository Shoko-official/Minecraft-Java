package dev.shoko.minecraft.item.recipe.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.data.registry.HolderSet;
import dev.shoko.minecraft.data.registry.TagKey;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestAccess;
import dev.shoko.minecraft.item.testing.TestItems;
import org.junit.jupiter.api.Test;

// spec: RC-R9, RC-R17: an ingredient tests the item of a stack, never its components
class IngredientTest {

  @Test
  void provisionalAnIdentifierIngredientAcceptsThatItemOnly() {
    // spec: RC-R17
    Ingredient stone = Recipes.items("stone");
    assertThat(stone.matches(TestItems.stack("stone", 1))).isTrue();
    assertThat(stone.matches(TestItems.stack("stone", 64))).isTrue();
    assertThat(stone.matches(TestItems.stack("dirt"))).isFalse();
  }

  @Test
  void provisionalAListIngredientAcceptsAnyOfItsItems() {
    // spec: RC-R9, RC-R17
    Ingredient either = Recipes.items("stone", "dirt");
    assertThat(either.matches(TestItems.stack("stone"))).isTrue();
    assertThat(either.matches(TestItems.stack("dirt"))).isTrue();
    assertThat(either.matches(TestItems.stack("cobblestone"))).isFalse();
  }

  @Test
  void provisionalATagIngredientAcceptsTheMembersOfTheTag() {
    // spec: RC-R9, RC-R17
    Ingredient planks = Recipes.tag("planks");
    assertThat(planks.matches(TestItems.stack("oak_planks"))).isTrue();
    assertThat(planks.matches(TestItems.stack("birch_planks"))).isTrue();
    assertThat(planks.matches(TestItems.stack("oak_log"))).isFalse();
  }

  @Test
  void provisionalTheComponentsOfTheStackAreNotTested() {
    // spec: RC-R17 (components are not tested outside special recipes)
    ItemStack epic = TestItems.stack("stone", 1, "rarity", "'epic'");
    ItemStack enchanted =
        TestItems.stack("oak_planks", 1, "enchantments", "{'minecraft:sharpness':1}");
    assertThat(Recipes.items("stone").matches(epic)).isTrue();
    assertThat(Recipes.tag("planks").matches(enchanted)).isTrue();
  }

  @Test
  void provisionalTheEmptyStackIsNotAcceptedByARealIngredient() {
    // spec: RC-R17
    assertThat(Recipes.items("stone").matches(ItemStack.empty())).isFalse();
    assertThat(Recipes.tag("planks").matches(ItemStack.empty())).isFalse();
  }

  @Test
  void noneIsTheEmptyCellAndAcceptsOnlyTheEmptyStack() {
    // spec: RC-R14 (a space denotes an empty cell)
    Ingredient none = Ingredient.none();
    assertThat(none.isNone()).isTrue();
    assertThat(none.matches(ItemStack.empty())).isTrue();
    assertThat(none.matches(TestItems.stack("stone"))).isFalse();
  }

  @Test
  void aRealIngredientIsNotNone() {
    assertThat(Recipes.items("stone").isNone()).isFalse();
    assertThat(Recipes.tag("planks").isNone()).isFalse();
  }

  @Test
  void aTagThatIsNotLoadedMakesMatchingFail() {
    // spec: Ingredient#matches Javadoc (IllegalStateException for an unloaded tag)
    Ingredient ghost =
        new Ingredient(
            HolderSet.tag(
                new TagKey<>(ItemCatalog.REGISTRY, TestItems.id("ghost")), TestAccess.standard()));
    ItemStack stone = TestItems.stack("stone");
    assertThatThrownBy(() -> ghost.matches(stone)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void provisionalAnEmptyButLoadedTagAcceptsNothing() {
    // spec: RC-R13, RC-R17 (an empty tag is loaded: no member, no match)
    Ingredient empty = Recipes.tag("empty_tag");
    assertThat(empty.matches(TestItems.stack("stone"))).isFalse();
  }
}

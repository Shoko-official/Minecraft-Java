package dev.shoko.minecraft.item.recipe.match;

import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.grid;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.shapeless;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-recipes RC-R22, RC-R23 (consumption and remainders; provisional level S). Where a
// remainder goes is decided by game and is not tested.
class CraftingConsumerTest {

  private static Consumption consume(CraftingGrid grid) {
    CraftingMatch match =
        new CraftingMatch(
            shapeless("any", TestItems.stack("chest"), Recipes.items("stone")),
            TestItems.stack("chest"));
    return CraftingConsumer.of(TestItems.catalog()).consume(grid, match);
  }

  private static List<String> texts(List<ItemStack> stacks) {
    return stacks.stream().map(Cells::text).toList();
  }

  @Test
  void provisionalEachNonEmptyCellLosesOneUnit() {
    // spec: RC-R22
    Consumption consumption = consume(grid("stone:3 - dirt:1 / - stick:64 - / - - coal:2"));
    assertThat(texts(consumption.grid()))
        .containsExactly("stone:2", "-", "-", "-", "stick:63", "-", "-", "-", "coal:1");
  }

  @Test
  void provisionalAnItemWithoutRemainderLeavesAnEmptyRemainder() {
    // spec: RC-R22 (for any other item the remainder is empty)
    Consumption consumption = consume(grid("stone:3 - dirt:1 / - stick:64 - / - - coal:2"));
    assertThat(consumption.remainders()).hasSize(9).allMatch(ItemStack::isEmpty);
  }

  @Test
  void provisionalGridAndRemaindersKeepTheSizeAndOrderOfTheGrid() {
    // spec: RC-R22 (skeleton Javadoc of Consumption)
    assertThat(consume(grid("stone - - / - - - / - - -")).grid()).hasSize(9);
    assertThat(consume(grid("stone - - / - - - / - - -")).remainders()).hasSize(9);
    assertThat(consume(grid("stone - / - -")).grid()).hasSize(4);
    assertThat(consume(grid("stone - / - -")).remainders()).hasSize(4);
    assertThat(consume(grid("stone - - / dirt - -")).grid()).hasSize(6);
  }

  @Test
  void provisionalAMilkBucketLeavesABucketInItsOwnCell() {
    // spec: RC-R22 (use_remainder of milk_bucket is bucket), vector V-I4
    Consumption consumption = consume(grid("- - - / - milk_bucket - / - - -"));
    assertThat(texts(consumption.grid())).allMatch("-"::equals);
    assertThat(texts(consumption.remainders()))
        .containsExactly("-", "-", "-", "-", "bucket:1", "-", "-", "-", "-");
  }

  @Test
  void provisionalASoupLeavesABowlAndAPotionLeavesAGlassBottle() {
    // spec: RC-R22 (mushroom_stew gives bowl, potion gives glass_bottle)
    Consumption consumption = consume(grid("mushroom_stew potion / - -"));
    assertThat(texts(consumption.grid())).containsExactly("-", "-", "-", "-");
    assertThat(texts(consumption.remainders()))
        .containsExactly("bowl:1", "glass_bottle:1", "-", "-");
  }

  @Test
  void provisionalARemainderStaysAtThePositionOfItsCell() {
    // spec: RC-R22, RC-R23 (the remainder list is parallel to the grid)
    Consumption consumption = consume(grid("stone milk_bucket stick / - - - / mushroom_stew - -"));
    assertThat(texts(consumption.remainders()))
        .containsExactly("-", "bucket:1", "-", "-", "-", "-", "bowl:1", "-", "-");
    assertThat(texts(consumption.grid()))
        .containsExactly("-", "-", "-", "-", "-", "-", "-", "-", "-");
  }

  @Test
  void provisionalAnEmptyGridConsumesNothing() {
    // spec: RC-R22
    Consumption consumption = consume(grid("- - / - -"));
    assertThat(texts(consumption.grid())).containsExactly("-", "-", "-", "-");
    assertThat(consumption.remainders()).allMatch(ItemStack::isEmpty);
  }

  @Test
  void provisionalConsumingDoesNotChangeTheGivenGrid() {
    // spec: RC-R22 (stacks are immutable, the grid before is untouched)
    CraftingGrid before = grid("stone:3 - / - dirt:2");
    List<ItemStack> snapshot = List.copyOf(before.stacks());
    consume(before);
    assertThat(before.stacks()).isEqualTo(snapshot);
  }
}

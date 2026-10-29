package dev.shoko.minecraft.item.recipe.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: RC-R14: cells of a pattern are in row-major order, x + y * width
class ShapedPatternTest {

  private static ShapedPattern twoByThree() {
    // width 2, height 3: stone dirt / cobblestone oak_planks / stick none
    return new ShapedPattern(
        2,
        3,
        List.of(
            Recipes.items("stone"),
            Recipes.items("dirt"),
            Recipes.items("cobblestone"),
            Recipes.items("oak_planks"),
            Recipes.items("stick"),
            Ingredient.none()));
  }

  @Test
  void cellsAreReadInRowMajorOrder() {
    ShapedPattern pattern = twoByThree();
    assertThat(pattern.cell(0, 0).matches(TestItems.stack("stone"))).isTrue();
    assertThat(pattern.cell(1, 0).matches(TestItems.stack("dirt"))).isTrue();
    assertThat(pattern.cell(0, 1).matches(TestItems.stack("cobblestone"))).isTrue();
    assertThat(pattern.cell(1, 1).matches(TestItems.stack("oak_planks"))).isTrue();
    assertThat(pattern.cell(0, 2).matches(TestItems.stack("stick"))).isTrue();
    assertThat(pattern.cell(1, 2).isNone()).isTrue();
  }

  @Test
  void aCellOutsideThePatternIsRefusedEvenWhenItsFlatIndexExists() {
    ShapedPattern pattern = twoByThree();
    // x = width would flat-index into the next row; x = -1 with y = 1 would land inside the list
    assertThatThrownBy(() -> pattern.cell(2, 0)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> pattern.cell(-1, 1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> pattern.cell(0, 3)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> pattern.cell(0, -1)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void aSingleCellPatternHasOneReadableCell() {
    // spec: RC-R14 (width 1 to 3, height 1 to 3)
    ShapedPattern one = new ShapedPattern(1, 1, List.of(Recipes.items("stone")));
    assertThat(one.cell(0, 0).matches(TestItems.stack("stone"))).isTrue();
    assertThatThrownBy(() -> one.cell(1, 0)).isInstanceOf(IndexOutOfBoundsException.class);
  }
}

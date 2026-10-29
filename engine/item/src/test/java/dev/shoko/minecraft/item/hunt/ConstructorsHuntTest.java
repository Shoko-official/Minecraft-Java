package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.item.container.Container;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.ShapedPattern;
import dev.shoko.minecraft.item.recipe.match.CraftingGrid;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-recipes RC-R14, RC-R16 and item-components-inventory IC-R30: constructors refuse the
// dimensions and limits their Javadoc describes (findings 11 and 12, defect B7).
class ConstructorsHuntTest {

  @Test
  void aGridWhoseCellCountOverflowsAnIntIsRefused() {
    assertThatThrownBy(() -> new CraftingGrid(65_536, 65_536, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aGridWithNegativeDimensionsIsRefused() {
    assertThatThrownBy(() -> new CraftingGrid(-1, -1, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new CraftingGrid(-2, 3, Collections.nCopies(6, ItemStack.empty())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aPatternWithAZeroDimensionIsRefused() {
    assertThatThrownBy(() -> new ShapedPattern(0, 5, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ShapedPattern(5, 0, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ShapedPattern(-1, -1, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aValidGridAndPatternAreStillBuilt() {
    CraftingGrid grid = new CraftingGrid(2, 2, Collections.nCopies(4, ItemStack.empty()));
    assertThat(grid.stacks()).hasSize(4);
    ShapedPattern pattern = new ShapedPattern(1, 1, List.of(Ingredient.none()));
    assertThat(pattern.cells()).hasSize(1);
  }

  @Test
  void provisionalAContainerOfNegativeSizeIsRefusedWithAnIllegalArgument() {
    // spec: IC-R30 (defect B7: NegativeArraySizeException leaks)
    assertThatThrownBy(() -> Container.of(-1, 64)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Container.copyOf(List.of(), 64).get(0))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void provisionalCopyOfRefusesAStackOverTheLimitLikeSetDoes() {
    // spec: IC-R30 (defect B7 and finding 11)
    List<ItemStack> over = List.of(TestItems.stack("stone", 64));
    Container container = Container.of(1, 10);
    assertThatThrownBy(() -> container.set(0, TestItems.stack("stone", 64)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Container.copyOf(over, 10))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void provisionalCopyOfAcceptsStacksAtTheLimit() {
    // spec: IC-R30
    Container container = Container.copyOf(List.of(TestItems.stack("stone", 10)), 10);
    assertThat(container.get(0).count()).isEqualTo(10);
    Container capped = Container.copyOf(List.of(TestItems.stack("ender_pearl", 16)), 64);
    assertThat(capped.get(0).count()).isEqualTo(16);
  }
}

package dev.shoko.minecraft.item.recipe.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.Grids;
import org.junit.jupiter.api.Test;

// spec: RC-R16 (grid of 2x2 or 3x3 cells, row-major as documented on CraftingGrid)
class CraftingGridTest {

  @Test
  void getReadsCellsInRowMajorOrder() {
    CraftingGrid grid =
        Grids.table("stone", "dirt", "stick", "diamond", "-", "coal", "bowl", "bucket", "paper");
    assertThat(Cells.text(grid.get(0, 0))).isEqualTo("stone:1");
    assertThat(Cells.text(grid.get(1, 0))).isEqualTo("dirt:1");
    assertThat(Cells.text(grid.get(2, 0))).isEqualTo("stick:1");
    assertThat(Cells.text(grid.get(0, 1))).isEqualTo("diamond:1");
    assertThat(grid.get(1, 1).isEmpty()).isTrue();
    assertThat(Cells.text(grid.get(2, 1))).isEqualTo("coal:1");
    assertThat(Cells.text(grid.get(0, 2))).isEqualTo("bowl:1");
    assertThat(Cells.text(grid.get(2, 2))).isEqualTo("paper:1");
  }

  @Test
  void getOnANonSquareGridUsesTheWidthAsStride() {
    CraftingGrid grid = Grids.of(2, 3, "stone", "dirt", "stick", "diamond", "coal", "bowl");
    assertThat(Cells.text(grid.get(1, 0))).isEqualTo("dirt:1");
    assertThat(Cells.text(grid.get(0, 1))).isEqualTo("stick:1");
    assertThat(Cells.text(grid.get(1, 2))).isEqualTo("bowl:1");
  }

  @Test
  void getKeepsTheCountOfTheStack() {
    CraftingGrid grid = Grids.player("stone:7", "-", "-", "-");
    assertThat(grid.get(0, 0).count()).isEqualTo(7);
  }

  @Test
  void anEmptyCellIsTheEmptyStack() {
    CraftingGrid grid = Grids.player("-", "-", "-", "-");
    assertThat(grid.get(1, 1)).isEqualTo(ItemStack.empty());
  }

  @Test
  void getOutsideTheGridThrowsIndexOutOfBounds() {
    CraftingGrid table = Grids.table("-", "-", "-", "-", "-", "-", "-", "-", "-");
    assertThatThrownBy(() -> table.get(3, 0)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> table.get(0, 3)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> table.get(-1, 0)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> table.get(0, -1)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void aPlayerGridHasOnlyTwoColumnsAndRows() {
    CraftingGrid player = Grids.player("-", "-", "-", "-");
    assertThatThrownBy(() -> player.get(2, 0)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> player.get(0, 2)).isInstanceOf(IndexOutOfBoundsException.class);
  }
}

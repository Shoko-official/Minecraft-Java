package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.recipe.match.CraftingGrid;
import dev.shoko.minecraft.item.recipe.match.CraftingMatch;
import dev.shoko.minecraft.item.recipe.match.CraftingRecognizer;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Patterns written with blank edge columns in the reference data (creaking_heart, mace...). */
@Tag("oracle")
class PatternEdgesHuntTest {

  private static Optional<CraftingMatch> recognize(CraftingGrid grid) {
    RealData.Loaded data = RealData.get();
    return CraftingRecognizer.of(data.recipes().recipes(), data.items()).recognize(grid);
  }

  @Test
  void creakingHeartIsRecognizedInTheLeftColumnOfTheTable() {
    RealData.Loaded data = RealData.get();
    ItemStack log = data.stack("pale_oak_log", 1);
    ItemStack resin = data.stack("resin_block", 1);
    ItemStack none = ItemStack.empty();
    CraftingGrid left =
        new CraftingGrid(3, 3, List.of(log, none, none, resin, none, none, log, none, none));
    assertThat(recognize(left)).map(m -> m.result().type().id().path()).contains("creaking_heart");
  }

  @Test
  void maceIsRecognizedInThePlayerWindowGrid() {
    RealData.Loaded data = RealData.get();
    CraftingGrid grid =
        new CraftingGrid(
            2,
            2,
            List.of(
                data.stack("heavy_core", 1),
                ItemStack.empty(),
                data.stack("breeze_rod", 1),
                ItemStack.empty()));
    assertThat(recognize(grid)).map(m -> m.result().type().id().path()).contains("mace");
  }

  @Test
  void spyglassIsRecognizedInTheRightColumnOfTheTable() {
    RealData.Loaded data = RealData.get();
    ItemStack shard = data.stack("amethyst_shard", 1);
    ItemStack copper = data.stack("copper_ingot", 1);
    ItemStack none = ItemStack.empty();
    CraftingGrid right =
        new CraftingGrid(3, 3, List.of(none, none, shard, none, none, copper, none, none, copper));
    assertThat(recognize(right)).map(m -> m.result().type().id().path()).contains("spyglass");
  }

  @Test
  void waxedChiseledCopperIsRecognizedInThePlayerWindowGrid() {
    RealData.Loaded data = RealData.get();
    ItemStack slab = data.stack("waxed_cut_copper_slab", 1);
    CraftingGrid grid =
        new CraftingGrid(2, 2, List.of(slab, ItemStack.empty(), slab, ItemStack.empty()));
    assertThat(recognize(grid))
        .map(m -> m.result().type().id().path())
        .contains("waxed_chiseled_copper");
  }
}

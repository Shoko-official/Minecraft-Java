package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.match.CraftingGrid;
import dev.shoko.minecraft.item.recipe.match.CraftingMatch;
import dev.shoko.minecraft.item.recipe.match.CraftingRecognizer;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// spec: item-recipes RC-R16 (the pattern is recognized at any whole offset): a pattern written with
// blank edge columns or rows, as the reference files do (mace, spyglass, creaking_heart), has the
// shape of its filled part. Reproduction of defect B1 without the reference data.
class TrimmedPatternHuntTest {

  private static final String COLUMN_BLANK_BOTH_SIDES =
      "{'type':'minecraft:crafting_shaped','key':{'X':'minecraft:stone','Y':'minecraft:stick'},"
          + "'pattern':[' X ',' Y '],'result':{'id':'minecraft:chest'}}";
  private static final String COLUMN_BLANK_ON_THE_LEFT =
      "{'type':'minecraft:crafting_shaped','key':{'X':'minecraft:stone','Y':'minecraft:stick'},"
          + "'pattern':[' X',' Y'],'result':{'id':'minecraft:chest'}}";
  private static final String COLUMN_BLANK_ON_THE_RIGHT =
      "{'type':'minecraft:crafting_shaped','key':{'X':'minecraft:stone','Y':'minecraft:stick'},"
          + "'pattern':['X ','Y '],'result':{'id':'minecraft:chest'}}";
  private static final String ROW_BLANK_BELOW =
      "{'type':'minecraft:crafting_shaped','key':{'X':'minecraft:stone'},"
          + "'pattern':['X ','  '],'result':{'id':'minecraft:chest'}}";

  private static Optional<CraftingMatch> recognize(String recipe, CraftingGrid grid) {
    RecipeSet set = HuntFixtures.recipes("chest_recipe", recipe);
    return CraftingRecognizer.of(set, HuntFixtures.catalog()).recognize(grid);
  }

  private static ItemStack stone() {
    return HuntFixtures.stack("stone");
  }

  private static ItemStack stick() {
    return HuntFixtures.stack("stick");
  }

  private static ItemStack none() {
    return ItemStack.empty();
  }

  private static void assertChest(Optional<CraftingMatch> match) {
    assertThat(match).map(m -> m.result().type().id().path()).contains("chest");
  }

  @Test
  void provisionalAPatternWithBlankSideColumnsIsRecognizedInThePlayerGrid() {
    // spec: RC-R16 (2x2 grid: the filled part is 1x2)
    assertChest(
        recognize(COLUMN_BLANK_BOTH_SIDES, HuntFixtures.grid(2, 2, stone(), none(), stick())));
    assertChest(
        recognize(
            COLUMN_BLANK_BOTH_SIDES, HuntFixtures.grid(2, 2, none(), stone(), none(), stick())));
  }

  @Test
  void provisionalAPatternWithBlankSideColumnsIsRecognizedInEveryColumnOfTheTable() {
    // spec: RC-R16
    for (int column = 0; column < 3; column++) {
      ItemStack[] cells = new ItemStack[9];
      java.util.Arrays.fill(cells, none());
      cells[column] = stone();
      cells[column + 3] = stick();
      assertChest(recognize(COLUMN_BLANK_BOTH_SIDES, HuntFixtures.grid(3, 3, cells)));
    }
  }

  @Test
  void provisionalABlankLeftColumnIsIgnored() {
    // spec: RC-R16
    assertChest(
        recognize(COLUMN_BLANK_ON_THE_LEFT, HuntFixtures.grid(2, 2, stone(), none(), stick())));
    assertChest(
        recognize(
            COLUMN_BLANK_ON_THE_LEFT, HuntFixtures.grid(2, 2, none(), stone(), none(), stick())));
  }

  @Test
  void provisionalABlankRightColumnIsIgnored() {
    // spec: RC-R16
    assertChest(
        recognize(COLUMN_BLANK_ON_THE_RIGHT, HuntFixtures.grid(2, 2, stone(), none(), stick())));
    assertChest(
        recognize(
            COLUMN_BLANK_ON_THE_RIGHT, HuntFixtures.grid(2, 2, none(), stone(), none(), stick())));
  }

  @Test
  void provisionalABlankBottomRowIsIgnored() {
    // spec: RC-R16
    assertChest(recognize(ROW_BLANK_BELOW, HuntFixtures.grid(2, 2, none(), none(), stone())));
    assertChest(recognize(ROW_BLANK_BELOW, HuntFixtures.grid(1, 1, stone())));
  }

  @Test
  void provisionalTheTrimmedPatternStillRefusesAnythingElseInTheGrid() {
    // spec: RC-R16 (every cell outside the pattern rectangle is empty)
    assertThat(
            recognize(
                COLUMN_BLANK_BOTH_SIDES,
                HuntFixtures.grid(3, 3, stone(), none(), none(), stick(), none(), none(), stick())))
        .isEmpty();
    assertThat(
            recognize(
                COLUMN_BLANK_BOTH_SIDES,
                HuntFixtures.grid(2, 2, stone(), stone(), stick(), none())))
        .isEmpty();
  }
}

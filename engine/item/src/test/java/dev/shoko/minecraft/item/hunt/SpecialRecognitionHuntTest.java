package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.match.CraftingGrid;
import dev.shoko.minecraft.item.recipe.match.CraftingMatch;
import dev.shoko.minecraft.item.recipe.match.CraftingRecognizer;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("oracle")
class SpecialRecognitionHuntTest {

  private static CraftingGrid grid(ItemStack... stacks) {
    List<ItemStack> cells = new ArrayList<>(List.of(stacks));
    while (cells.size() < 9) {
      cells.add(ItemStack.empty());
    }
    return new CraftingGrid(3, 3, cells);
  }

  private static Optional<CraftingMatch> recognize(CraftingGrid grid) {
    RealData.Loaded data = RealData.get();
    return CraftingRecognizer.of(data.recipes().recipes(), data.items()).recognize(grid);
  }

  private static String recipe(Optional<CraftingMatch> match) {
    return match.map(m -> m.recipe().id().path()).orElse("none");
  }

  @Test
  void extendingAMapNeedsExactlyTheRingOfPaper() {
    RealData.Loaded data = RealData.get();
    ItemStack paper = data.stack("paper", 1);
    ItemStack map = data.stack("filled_map", 1);
    CraftingGrid ring = grid(paper, paper, paper, paper, map, paper, paper, paper, paper);
    assertThat(recipe(recognize(ring))).isEqualTo("map_extending");
    CraftingGrid hole =
        grid(paper, paper, paper, paper, map, paper, paper, paper, ItemStack.empty());
    assertThat(recipe(recognize(hole))).isNotEqualTo("map_extending");
    CraftingGrid offCenter = grid(map, paper, paper, paper, paper, paper, paper, paper, paper);
    assertThat(recipe(recognize(offCenter))).isNotEqualTo("map_extending");
  }

  @Test
  void aPotionAmongArrowsGivesTippedArrows() {
    RealData.Loaded data = RealData.get();
    ItemStack arrow = data.stack("arrow", 1);
    ItemStack potion = data.stack("lingering_potion", 1);
    Optional<CraftingMatch> match =
        recognize(grid(arrow, arrow, arrow, arrow, potion, arrow, arrow, arrow, arrow));
    assertThat(match).isPresent();
    assertThat(match.get().result().type().id().path()).isEqualTo("tipped_arrow");
    assertThat(match.get().result().count()).isEqualTo(8);
    Optional<CraftingMatch> splash =
        recognize(
            grid(
                arrow,
                arrow,
                arrow,
                arrow,
                data.stack("splash_potion", 1),
                arrow,
                arrow,
                arrow,
                arrow));
    assertThat(recipe(splash)).isNotEqualTo("tipped_arrow");
  }

  @Test
  void aDecoratedPotNeedsTheFourSidesOnly() {
    RealData.Loaded data = RealData.get();
    ItemStack brick = data.stack("brick", 1);
    ItemStack none = ItemStack.empty();
    CraftingGrid cross = grid(none, brick, none, brick, none, brick, none, brick, none);
    assertThat(recognize(cross).map(m -> m.result().type().id().path())).contains("decorated_pot");
    CraftingGrid corner = grid(brick, brick, none, brick, none, brick, none, brick, none);
    assertThat(recognize(corner).map(m -> m.result().type().id().path()))
        .isNotEqualTo(Optional.of("decorated_pot"));
  }

  @Test
  void aShieldTakesTheColorOfAnyBannerOnlyOnce() {
    RealData.Loaded data = RealData.get();
    Optional<CraftingMatch> match =
        recognize(grid(data.stack("shield", 1), data.stack("red_banner", 1)));
    assertThat(match).isPresent();
    assertThat(match.get().result().component(Identifier.minecraft("base_color"))).isPresent();
    Optional<CraftingMatch> again =
        recognize(grid(match.get().result(), data.stack("blue_banner", 1)));
    assertThat(recipe(again)).isNotEqualTo("shield_decoration");
  }

  @Test
  void leatherArmorTakesDyes() {
    RealData.Loaded data = RealData.get();
    Optional<CraftingMatch> match =
        recognize(
            grid(
                data.stack("leather_helmet", 1),
                data.stack("red_dye", 1),
                data.stack("blue_dye", 1)));
    assertThat(recipe(match)).contains("dye");
    assertThat(match.get().result().type().id().path()).isEqualTo("leather_helmet");
    Optional<CraftingMatch> two =
        recognize(
            grid(
                data.stack("leather_helmet", 1),
                data.stack("leather_boots", 1),
                data.stack("blue_dye", 1)));
    assertThat(recipe(two)).doesNotContain("dye");
  }

  @Test
  void fireworksFollowTheirRules() {
    RealData.Loaded data = RealData.get();
    ItemStack paper = data.stack("paper", 1);
    ItemStack powder = data.stack("gunpowder", 1);
    ItemStack star = data.stack("firework_star", 1);
    Optional<CraftingMatch> rocket =
        recognize(grid(paper, powder, powder, powder, star, star, star, star, star));
    assertThat(rocket.map(m -> m.result().type().id().path())).contains("firework_rocket");
    Optional<CraftingMatch> tooMuch =
        recognize(grid(paper, powder, powder, powder, powder, star, star, star, star));
    assertThat(tooMuch.map(m -> m.result().type().id().path()))
        .isNotEqualTo(Optional.of("firework_rocket"));
    Optional<CraftingMatch> made =
        recognize(
            grid(
                powder,
                data.stack("red_dye", 1),
                data.stack("diamond", 1),
                data.stack("glowstone_dust", 1)));
    assertThat(made.map(m -> m.result().type().id().path())).contains("firework_star");
    Optional<CraftingMatch> twoShapes =
        recognize(
            grid(
                powder,
                data.stack("red_dye", 1),
                data.stack("fire_charge", 1),
                data.stack("gold_nugget", 1)));
    assertThat(twoShapes.map(m -> m.result().type().id().path()))
        .isNotEqualTo(Optional.of("firework_star"));
    Optional<CraftingMatch> noDye = recognize(grid(powder, data.stack("diamond", 1)));
    assertThat(noDye.map(m -> m.result().type().id().path()))
        .isNotEqualTo(Optional.of("firework_star"));
  }
}

package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.NumericTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.match.Consumption;
import dev.shoko.minecraft.item.recipe.match.CraftingConsumer;
import dev.shoko.minecraft.item.recipe.match.CraftingGrid;
import dev.shoko.minecraft.item.recipe.match.CraftingMatch;
import dev.shoko.minecraft.item.recipe.match.CraftingRecognizer;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// spec: item-recipes RC-R43, RC-R44 (defects B2 and B3 without the reference data): the banner to
// copy and the signed book are given back; a book of generation 2 or more does not clone, whatever
// the numeric type that wrote the generation.
class SpecialItemsHuntTest {

  private static final Identifier BANNER_PATTERNS = Identifier.minecraft("banner_patterns");
  private static final Identifier BOOK = Identifier.minecraft("written_book_content");

  private static final String BANNER_DUPLICATE =
      "{'type':'minecraft:crafting_special_bannerduplicate','banner':'minecraft:white_banner',"
          + "'result':{'id':'minecraft:white_banner'}}";
  private static final String BOOK_CLONING =
      "{'type':'minecraft:crafting_special_bookcloning','source':'minecraft:written_book',"
          + "'material':'#minecraft:book_cloning_target','result':{'id':'minecraft:written_book'}}";

  private static RecipeSet recipes() {
    return HuntFixtures.recipes("banner_duplicate", BANNER_DUPLICATE, "book_cloning", BOOK_CLONING);
  }

  private static Optional<CraftingMatch> recognize(CraftingGrid grid) {
    return CraftingRecognizer.of(recipes(), HuntFixtures.catalog()).recognize(grid);
  }

  static ItemStack bannerWithLayers(int layers) {
    List<NbtTag> list = new ArrayList<>();
    for (int i = 0; i < layers; i++) {
      LinkedHashMap<String, NbtTag> layer = new LinkedHashMap<>();
      layer.put("pattern", new StringTag("minecraft:border"));
      layer.put("color", new StringTag("red"));
      list.add(CompoundTag.of(layer));
    }
    return HuntFixtures.stack("white_banner")
        .withComponent(BANNER_PATTERNS, new ComponentValue(ListTag.of(list)));
  }

  private static ItemStack bookOfGeneration(NbtTag generation) {
    LinkedHashMap<String, NbtTag> content = new LinkedHashMap<>();
    content.put("generation", generation);
    return HuntFixtures.stack("written_book")
        .withComponent(BOOK, new ComponentValue(CompoundTag.of(content)));
  }

  private static List<ItemStack> given(Consumption consumption) {
    List<ItemStack> kept = new ArrayList<>(consumption.grid());
    kept.addAll(consumption.remainders());
    return kept;
  }

  @Test
  void provisionalDuplicatingABannerGivesTheBannerToCopyBack() {
    // spec: RC-R43 "la banniere a copier est rendue"
    ItemStack patterned = bannerWithLayers(2);
    CraftingGrid grid = HuntFixtures.grid(3, 3, patterned, HuntFixtures.stack("white_banner"));
    CraftingMatch match = recognize(grid).orElseThrow();
    Consumption consumption = CraftingConsumer.of(HuntFixtures.catalog()).consume(grid, match);
    assertThat(given(consumption)).anyMatch(stack -> stack.sameContent(patterned));
    assertThat(consumption.grid().get(1).isEmpty()).as("the plain banner is used up").isTrue();
  }

  @Test
  void provisionalCloningABookGivesTheSignedBookBack() {
    // spec: RC-R44 "le livre source est rendu"
    ItemStack signed = bookOfGeneration(new IntTag(0));
    CraftingGrid grid = HuntFixtures.grid(3, 3, signed, HuntFixtures.stack("writable_book"));
    CraftingMatch match = recognize(grid).orElseThrow();
    Consumption consumption = CraftingConsumer.of(HuntFixtures.catalog()).consume(grid, match);
    assertThat(given(consumption)).anyMatch(stack -> stack.sameContent(signed));
    assertThat(consumption.grid().get(1).isEmpty()).as("the blank book is used up").isTrue();
  }

  @Test
  void provisionalTheGenerationOfABookIsReadWhateverItsNumericType() {
    // spec: RC-R44 "un livre de generation 2 ou plus ne se copie pas" (defect B3)
    List<NumericTag> tooOld =
        List.of(
            new IntTag(2),
            new ByteTag((byte) 2),
            new ShortTag((short) 2),
            new LongTag(2L),
            new IntTag(3),
            new ByteTag((byte) 3));
    for (NumericTag generation : tooOld) {
      CraftingGrid grid =
          HuntFixtures.grid(
              3, 3, bookOfGeneration(generation), HuntFixtures.stack("writable_book"));
      assertThat(recognize(grid)).as("generation %s", generation).isEmpty();
    }
  }

  @Test
  void provisionalTheGenerationBelowTwoStillClonesWhateverItsNumericType() {
    // spec: RC-R44 (the boundary: generations 0 and 1 are copies of an original and of a copy)
    List<NumericTag> allowed =
        List.of(
            new IntTag(0),
            new IntTag(1),
            new ByteTag((byte) 0),
            new ByteTag((byte) 1),
            new ShortTag((short) 1),
            new LongTag(1L));
    for (NumericTag generation : allowed) {
      CraftingGrid grid =
          HuntFixtures.grid(
              3, 3, bookOfGeneration(generation), HuntFixtures.stack("writable_book"));
      assertThat(recognize(grid)).as("generation %s", generation).isPresent();
    }
  }

  @Test
  void provisionalABannerWithSixLayersCopiesAndOneWithSevenDoesNot() {
    // spec: RC-R43 "limite de six motifs"
    CraftingGrid six =
        HuntFixtures.grid(3, 3, bannerWithLayers(6), HuntFixtures.stack("white_banner"));
    assertThat(recognize(six)).isPresent();
    CraftingGrid seven =
        HuntFixtures.grid(3, 3, bannerWithLayers(7), HuntFixtures.stack("white_banner"));
    assertThat(recognize(seven)).isEmpty();
  }
}

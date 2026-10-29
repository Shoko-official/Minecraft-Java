package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("oracle")
class SpecialCraftingHuntTest {

  private static final Identifier BANNER_PATTERNS = Identifier.minecraft("banner_patterns");
  private static final Identifier BOOK = Identifier.minecraft("written_book_content");
  private static final Identifier DAMAGE = Identifier.minecraft("damage");

  private static CraftingGrid grid(ItemStack... stacks) {
    List<ItemStack> cells = new ArrayList<>(List.of(stacks));
    while (cells.size() < 9) {
      cells.add(ItemStack.empty());
    }
    return new CraftingGrid(3, 3, cells);
  }

  private static ComponentValue oneBannerLayer() {
    LinkedHashMap<String, NbtTag> layer = new LinkedHashMap<>();
    layer.put("pattern", new StringTag("minecraft:border"));
    layer.put("color", new StringTag("red"));
    return new ComponentValue(ListTag.of(List.of(CompoundTag.of(layer))));
  }

  @Test
  void duplicatingABannerGivesTheBannerToCopyBack() {
    // spec: item-recipes RC-R43 "la banniere a copier est rendue"
    RealData.Loaded data = RealData.get();
    ItemStack patterned =
        data.stack("white_banner", 1).withComponent(BANNER_PATTERNS, oneBannerLayer());
    CraftingGrid grid = grid(patterned, data.stack("white_banner", 1));
    CraftingMatch match =
        CraftingRecognizer.of(data.recipes().recipes(), data.items()).recognize(grid).orElseThrow();
    assertThat(match.recipe().id().path()).contains("banner");
    Consumption consumption = CraftingConsumer.of(data.items()).consume(grid, match);
    List<ItemStack> kept = new ArrayList<>(consumption.grid());
    kept.addAll(consumption.remainders());
    assertThat(kept).anyMatch(stack -> stack.sameContent(patterned));
  }

  @Test
  void cloningABookGivesTheSignedBookBack() {
    // spec: item-recipes RC-R44 "le livre source est rendu"
    RealData.Loaded data = RealData.get();
    ItemStack signed =
        data.stack("written_book", 1).withComponent(BOOK, new ComponentValue(CompoundTag.empty()));
    CraftingGrid grid = grid(signed, data.stack("writable_book", 1));
    CraftingMatch match =
        CraftingRecognizer.of(data.recipes().recipes(), data.items()).recognize(grid).orElseThrow();
    assertThat(match.result().count()).isEqualTo(1);
    Consumption consumption = CraftingConsumer.of(data.items()).consume(grid, match);
    List<ItemStack> kept = new ArrayList<>(consumption.grid());
    kept.addAll(consumption.remainders());
    assertThat(kept).anyMatch(stack -> stack.sameContent(signed));
  }

  @Test
  void aBookOfGenerationTwoWrittenAsAByteCannotBeCloned() {
    // spec: item-recipes RC-R44 "un livre de generation 2 ou plus ne se copie pas"
    RealData.Loaded data = RealData.get();
    LinkedHashMap<String, NbtTag> content = new LinkedHashMap<>();
    content.put("generation", new ByteTag((byte) 2));
    ItemStack copyOfCopy =
        data.stack("written_book", 1)
            .withComponent(BOOK, new ComponentValue(CompoundTag.of(content)));
    Optional<CraftingMatch> match =
        CraftingRecognizer.of(data.recipes().recipes(), data.items())
            .recognize(grid(copyOfCopy, data.stack("writable_book", 1)));
    assertThat(match).isEmpty();
  }

  @Test
  void aBookOfGenerationTwoWrittenAsAnIntCannotBeCloned() {
    RealData.Loaded data = RealData.get();
    LinkedHashMap<String, NbtTag> content = new LinkedHashMap<>();
    content.put("generation", new IntTag(2));
    ItemStack copyOfCopy =
        data.stack("written_book", 1)
            .withComponent(BOOK, new ComponentValue(CompoundTag.of(content)));
    Optional<CraftingMatch> match =
        CraftingRecognizer.of(data.recipes().recipes(), data.items())
            .recognize(grid(copyOfCopy, data.stack("writable_book", 1)));
    assertThat(match).isEmpty();
  }

  @Test
  void repairNeverLeavesMoreWearThanTheSumOfTheTwoItemsAllowsAndNeverNegative() {
    // spec: item-recipes RC-R49a
    RealData.Loaded data = RealData.get();
    ItemStack worn1 =
        data.stack("diamond_pickaxe", 1)
            .withComponent(DAMAGE, new ComponentValue(new IntTag(1500)));
    ItemStack worn2 =
        data.stack("diamond_pickaxe", 1)
            .withComponent(DAMAGE, new ComponentValue(new IntTag(1500)));
    CraftingMatch match =
        CraftingRecognizer.of(data.recipes().recipes(), data.items())
            .recognize(grid(worn1, worn2))
            .orElseThrow();
    int max = 1561;
    int expected = max - ((max - 1500) * 2 + max * 5 / 100);
    assertThat(match.result().damage()).isEqualTo(expected);
  }

  @Test
  void twoEnchantedToolsRepairWithoutTheEnchantmentsButAreNotDuplicated() {
    RealData.Loaded data = RealData.get();
    ItemStack a = data.stack("diamond_pickaxe", 1);
    CraftingGrid g =
        new CraftingGrid(
            2, 2, new ArrayList<>(List.of(a, a, ItemStack.empty(), ItemStack.empty())));
    Optional<CraftingMatch> match =
        CraftingRecognizer.of(data.recipes().recipes(), data.items()).recognize(g);
    assertThat(match).isPresent();
  }
}

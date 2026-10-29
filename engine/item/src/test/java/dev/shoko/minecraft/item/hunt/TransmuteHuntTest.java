package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
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
class TransmuteHuntTest {

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

  @Test
  void dyeingAShulkerBoxKeepsItsContentWhateverThePositions() {
    RealData.Loaded data = RealData.get();
    LinkedHashMap<String, dev.shoko.minecraft.core.nbt.NbtTag> entry = new LinkedHashMap<>();
    entry.put("slot", new IntTag(0));
    entry.put(
        "item", dev.shoko.minecraft.item.testing.Nbt.compound("{id:\"minecraft:stone\",count:5}"));
    ComponentValue content = new ComponentValue(ListTag.of(List.of(CompoundTag.of(entry))));
    Identifier container = Identifier.minecraft("container");
    ItemStack box = data.stack("shulker_box", 1).withComponent(container, content);
    ItemStack dye = data.stack("red_dye", 1);
    for (CraftingGrid g :
        List.of(
            grid(box, dye),
            grid(dye, box),
            grid(ItemStack.empty(), ItemStack.empty(), dye, ItemStack.empty(), box))) {
      Optional<CraftingMatch> match = recognize(g);
      assertThat(match).isPresent();
      assertThat(match.get().result().type().id().path()).isEqualTo("red_shulker_box");
      assertThat(match.get().result().component(container)).contains(content);
      assertThat(match.get().result().count()).isEqualTo(1);
    }
  }

  @Test
  void cloningAMapGivesOneCopyPerBlankMapPlusTheOriginalAndKeepsTheMapId() {
    RealData.Loaded data = RealData.get();
    Identifier mapId = Identifier.minecraft("map_id");
    ItemStack filled =
        data.stack("filled_map", 1).withComponent(mapId, new ComponentValue(new IntTag(7)));
    for (int blanks = 1; blanks <= 8; blanks++) {
      List<ItemStack> stacks = new ArrayList<>(List.of(filled));
      for (int k = 0; k < blanks; k++) {
        stacks.add(data.stack("map", 1));
      }
      Optional<CraftingMatch> match = recognize(grid(stacks.toArray(new ItemStack[0])));
      assertThat(match).as("blanks " + blanks).isPresent();
      assertThat(match.get().result().count()).as("blanks " + blanks).isEqualTo(blanks + 1);
      assertThat(match.get().result().component(mapId)).contains(new ComponentValue(new IntTag(7)));
    }
  }
}

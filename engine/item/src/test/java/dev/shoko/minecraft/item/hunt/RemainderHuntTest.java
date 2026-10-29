package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.recipe.match.Consumption;
import dev.shoko.minecraft.item.recipe.match.CraftingConsumer;
import dev.shoko.minecraft.item.recipe.match.CraftingGrid;
import dev.shoko.minecraft.item.recipe.match.CraftingMatch;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("oracle")
class RemainderHuntTest {

  private static final Map<String, String> REMAINDERS =
      Map.of(
          "beetroot_soup", "bowl",
          "honey_bottle", "glass_bottle",
          "milk_bucket", "bucket",
          "mushroom_stew", "bowl",
          "potion", "glass_bottle",
          "rabbit_stew", "bowl",
          "suspicious_stew", "bowl");

  @Test
  void everyUseRemainderItemLeavesItsRemainderAndConsumesOneUnit() {
    RealData.Loaded data = RealData.get();
    CraftingConsumer consumer = CraftingConsumer.of(data.items());
    List<String> problems = new ArrayList<>();
    for (Map.Entry<String, String> entry : REMAINDERS.entrySet()) {
      ItemStack stack = data.stack(entry.getKey(), 1);
      List<ItemStack> cells = new ArrayList<>(List.of(stack, data.stack("stone", 5)));
      while (cells.size() < 9) {
        cells.add(ItemStack.empty());
      }
      CraftingGrid grid = new CraftingGrid(3, 3, cells);
      Consumption c = consumer.consume(grid, null);
      if (!c.remainders().get(0).type().id().path().equals(entry.getValue())) {
        problems.add(entry.getKey() + " -> " + c.remainders().get(0));
      }
      if (!c.grid().get(0).isEmpty()
          || c.grid().get(1).count() != 4
          || !c.remainders().get(1).isEmpty()) {
        problems.add("consumption " + entry.getKey());
      }
    }
    assertThat(problems).isEmpty();
  }

  @Test
  void aStackOfTwoLosesOneUnitAndLeavesOneRemainderPerUnitConsumed() {
    RealData.Loaded data = RealData.get();
    ItemStack soup = data.stack("beetroot_soup", 1).withCount(2);
    CraftingGrid grid =
        new CraftingGrid(
            2, 2, List.of(soup, ItemStack.empty(), ItemStack.empty(), ItemStack.empty()));
    Consumption c = CraftingConsumer.of(data.items()).consume(grid, (CraftingMatch) null);
    assertThat(c.grid().get(0).count()).isEqualTo(1);
    assertThat(c.remainders().get(0).count()).isEqualTo(1);
  }
}

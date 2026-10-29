package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.menu.MenuLayout;
import dev.shoko.minecraft.item.menu.MenuLayouts;
import dev.shoko.minecraft.item.menu.PlacementRules;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("oracle")
class ArmorSlotsHuntTest {

  @Test
  void everyEquippableItemFitsExactlyTheArmorSlotItNames() {
    RealData.Loaded data = RealData.get();
    MenuLayout player = MenuLayouts.playerWindow();
    List<String> names = List.of("head", "chest", "legs", "feet");
    List<String> problems = new ArrayList<>();
    int equippable = 0;
    for (Identifier id : data.items().registry().ids()) {
      ItemType type = data.items().find(id).orElseThrow();
      ItemStack stack = ItemStack.of(type, 1);
      String slot =
          stack
              .component(WellKnownComponents.EQUIPPABLE)
              .map(v -> ((CompoundTag) v.tag()).getString("slot"))
              .orElse(null);
      if (type.isAir()) {
        continue;
      }
      if (slot != null) {
        equippable++;
      }
      for (int i = 0; i < 4; i++) {
        boolean accepted = PlacementRules.mayPlace(player, 5 + i, stack, (m, s, st) -> true);
        boolean expected = names.get(i).equals(slot);
        if (accepted != expected) {
          problems.add(id + " slot=" + slot + " window=" + (5 + i) + " accepted=" + accepted);
        }
      }
    }
    assertThat(equippable).isEqualTo(84);
    assertThat(problems).isEmpty();
  }
}

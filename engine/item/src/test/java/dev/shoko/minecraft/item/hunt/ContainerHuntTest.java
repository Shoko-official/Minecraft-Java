package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.container.Container;
import dev.shoko.minecraft.item.container.ContainerItemsCodec;
import dev.shoko.minecraft.item.container.NestedContents;
import dev.shoko.minecraft.item.container.SlottedStack;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Nbt;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContainerHuntTest {

  private static final ContainerItemsCodec ITEMS = ContainerItemsCodec.of(TestItems.stackCodec());

  @Test
  void copyOfRefusesWhatSetRefuses() {
    List<ItemStack> over = List.of(TestItems.stack("stone", 64));
    assertThatCode(() -> Container.copyOf(over, 10).set(0, TestItems.stack("stone", 64)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatCode(() -> Container.copyOf(over, 10))
        .as("a container built over its own slot limit")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aContainerOfMoreThan128SlotsKeepsAllItsStacksThroughItemsList() {
    Container big = Container.of(300, 64);
    big.set(200, TestItems.stack("stone", 3));
    ListTag written = ITEMS.write(big.snapshot());
    List<ItemStack> read = Findings.value(ITEMS.read(written, 300));
    assertThat(read.get(200)).isEqualTo(TestItems.stack("stone", 3));
  }

  private static ComponentValue nest(String leaf, int depth) {
    CompoundTag item = Nbt.compound("{id:\"minecraft:" + leaf + "\",count:1}");
    for (int level = 1; level < depth; level++) {
      LinkedHashMap<String, NbtTag> entry = new LinkedHashMap<>();
      entry.put("slot", new IntTag(0));
      entry.put("item", item);
      LinkedHashMap<String, NbtTag> components = new LinkedHashMap<>();
      components.put("minecraft:container", ListTag.of(List.of(CompoundTag.of(entry))));
      LinkedHashMap<String, NbtTag> outer = new LinkedHashMap<>();
      outer.put("id", new dev.shoko.minecraft.core.nbt.StringTag("minecraft:chest"));
      outer.put("count", new IntTag(1));
      outer.put("components", CompoundTag.of(components));
      item = CompoundTag.of(outer);
    }
    LinkedHashMap<String, NbtTag> top = new LinkedHashMap<>();
    top.put("slot", new IntTag(0));
    top.put("item", item);
    return new ComponentValue(ListTag.of(List.of(CompoundTag.of(top))));
  }

  @Test
  void nestingAtTheLimitIsAcceptedAndOneAboveIsRefused() {
    NestedContents view = NestedContents.of(TestItems.stackCodec(), 100);
    assertThat(view.read(nest("stone", 100))).isInstanceOf(Decoding.Success.class);
    Decoding<List<SlottedStack>> tooDeep = view.read(nest("stone", 101));
    Findings.assertFailure(tooDeep, FindingCode.NESTING_TOO_DEEP);
  }

  @Test
  void aVeryDeepNestingIsAFindingNotAStackOverflow() {
    NestedContents view = NestedContents.of(TestItems.stackCodec(), Integer.MAX_VALUE);
    ComponentValue deep = nest("stone", 20000);
    assertThatCode(() -> view.read(deep)).doesNotThrowAnyException();
  }

  @Test
  void aNegativeOrDuplicateSlotOfTheContainerComponentIsNotReadAsIs() {
    NestedContents view = NestedContents.of(TestItems.stackCodec(), 10);
    ListTag list =
        (ListTag)
            Nbt.parse(
                "[{slot:-5,item:{id:\"minecraft:stone\",count:1}},"
                    + "{slot:7,item:{id:\"minecraft:stone\",count:1}},"
                    + "{slot:7,item:{id:\"minecraft:dirt\",count:1}}]");
    Decoding<List<SlottedStack>> read = view.read(new ComponentValue(list));
    if (read instanceof Decoding.Success<List<SlottedStack>> ok) {
      assertThat(ok.value()).extracting(SlottedStack::slot).doesNotHaveDuplicates();
      assertThat(ok.value()).extracting(SlottedStack::slot).allMatch(slot -> slot >= 0);
    }
  }

  @Test
  void aDeeperNestingThanTheLimitIsRefusedWithoutWalkingItAll() {
    NestedContents view = NestedContents.of(TestItems.stackCodec(), 100);
    Findings.assertFailure(view.read(nest("stone", 20000)), FindingCode.NESTING_TOO_DEEP);
  }
}

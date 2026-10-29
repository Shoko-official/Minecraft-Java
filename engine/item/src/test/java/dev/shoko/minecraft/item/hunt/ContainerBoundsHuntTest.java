package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.ListTag;
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
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R24, IC-R25 (a sparse Items list keeps its slots) and IC-R23
// (a slot of the container component is an index of the container): defects D1 and D2.
class ContainerBoundsHuntTest {

  private static final ContainerItemsCodec ITEMS = ContainerItemsCodec.of(TestItems.stackCodec());

  @Test
  void provisionalEverySlotOfALargeContainerSurvivesTheItemsList() {
    // spec: IC-R25 (defect D1: Slot written as (byte) slot loses the stacks past 127)
    for (int size : List.of(128, 129, 255, 256, 257, 1000)) {
      Container container = Container.of(size, 64);
      for (int slot : List.of(0, 1, 126, 127, 128, size - 1)) {
        if (slot < size) {
          container.set(slot, TestItems.stack("stone", 1 + slot % 60));
        }
      }
      ListTag written = ITEMS.write(container.snapshot());
      List<ItemStack> read = Findings.value(ITEMS.read(written, size));
      assertThat(read).as("size %d", size).isEqualTo(container.snapshot());
    }
  }

  @Test
  void provisionalASlotPastTheByteRangeIsNotReadAsAnotherSlot() {
    // spec: IC-R25 (a Byte Slot of 255 is -1, never slot 255; defect D1 reversed)
    CompoundTag entry = Nbt.compound("{Slot:-1b,id:\"minecraft:stone\",count:1}");
    Decoding<List<ItemStack>> read = ITEMS.read(ListTag.of(List.of(entry)), 300);
    assertThat(Findings.value(read).get(255).isEmpty()).isTrue();
    assertThat(Findings.codesOf(read)).contains(FindingCode.INVALID_SLOT);
  }

  private static Decoding<List<SlottedStack>> readNested(String snbt) {
    NestedContents view = NestedContents.of(TestItems.stackCodec(), 10);
    return view.read(new ComponentValue(Nbt.parse(snbt)));
  }

  @Test
  void provisionalANegativeSlotOfTheContainerComponentIsReportedNotKept() {
    // spec: IC-R23, IC-R25 (defect D2: same INVALID_SLOT warning as the Items lists)
    Decoding<List<SlottedStack>> read =
        readNested(
            "[{slot:-5,item:{id:\"minecraft:stone\",count:1}},"
                + "{slot:3,item:{id:\"minecraft:dirt\",count:1}}]");
    if (read instanceof Decoding.Success<List<SlottedStack>> ok) {
      assertThat(ok.value()).extracting(SlottedStack::slot).containsExactly(3);
      assertThat(Findings.codesOf(read)).contains(FindingCode.INVALID_SLOT);
    } else {
      Findings.assertAnyFailure(read);
    }
  }

  @Test
  void provisionalADuplicateSlotOfTheContainerComponentIsReportedNotMerged() {
    // spec: IC-R23, IC-R25 (defect D2)
    Decoding<List<SlottedStack>> read =
        readNested(
            "[{slot:7,item:{id:\"minecraft:stone\",count:1}},"
                + "{slot:7,item:{id:\"minecraft:dirt\",count:1}}]");
    if (read instanceof Decoding.Success<List<SlottedStack>> ok) {
      assertThat(ok.value()).extracting(SlottedStack::slot).doesNotHaveDuplicates();
      assertThat(Findings.codesOf(read)).contains(FindingCode.INVALID_SLOT);
    } else {
      Findings.assertAnyFailure(read);
    }
  }

  @Test
  void provisionalDistinctNonNegativeSlotsAreReadAsWritten() {
    // spec: IC-R23
    Decoding<List<SlottedStack>> read =
        readNested(
            "[{slot:0,item:{id:\"minecraft:stone\",count:1}},"
                + "{slot:26,item:{id:\"minecraft:dirt\",count:2}}]");
    assertThat(Findings.value(read)).extracting(SlottedStack::slot).containsExactly(0, 26);
    assertThat(Findings.codesOf(read)).doesNotContain(FindingCode.INVALID_SLOT);
  }
}

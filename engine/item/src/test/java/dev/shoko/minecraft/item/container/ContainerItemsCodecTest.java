package dev.shoko.minecraft.item.container;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Nbt;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R24, IC-R25 (sparse Items lists, V-I7, V-I8), IC-R26 (stack
// findings kept), PI6 (entries with a bad Slot: provisional).
class ContainerItemsCodecTest {

  private static final String POTION =
      "{count:1,Slot:0b,id:'minecraft:potion',"
          + "components:{'minecraft:potion_contents':{potion:'minecraft:strong_healing'}}}";

  private static ContainerItemsCodec codec() {
    return ContainerItemsCodec.of(TestItems.stackCodec());
  }

  private static ListTag list(String... entries) {
    List<CompoundTag> compounds = new ArrayList<>();
    for (String entry : entries) {
      compounds.add(Nbt.compound(entry));
    }
    return ListTag.of(compounds);
  }

  private static List<String> texts(List<ItemStack> stacks) {
    return stacks.stream().map(Cells::text).toList();
  }

  private static CompoundTag element(ListTag written, int index) {
    return (CompoundTag) written.elements().get(index);
  }

  @Test
  void writeKeepsOnlyNonEmptyStacksInSlotOrder() {
    // spec: IC-R25
    ListTag written = codec().write(Cells.stacks("-", "stone:3", "-", "dirt:5", "-"));
    assertThat(written.elements()).hasSize(2);
    assertThat(element(written, 0).get("Slot")).contains(new ByteTag((byte) 1));
    assertThat(element(written, 1).get("Slot")).contains(new ByteTag((byte) 3));
    assertThat(element(written, 0).getString("id")).isEqualTo("minecraft:stone");
    assertThat(element(written, 1).getString("id")).isEqualTo("minecraft:dirt");
  }

  @Test
  void writeOfOnlyEmptySlotsGivesTheEmptyList() {
    // spec: IC-R25 (an empty list is written as is)
    ListTag written = codec().write(Cells.stacks("-", "-", "-"));
    assertThat(written.elements()).isEmpty();
    assertThat(codec().write(List.of()).elements()).isEmpty();
  }

  @Test
  void writeUsesAnIntCountAndAByteSlot() {
    // spec: IC-R24 (count Int, Slot Byte on the 21 stacks of the templates)
    ListTag written = codec().write(Cells.stacks("-", "stone:24"));
    CompoundTag stack = element(written, 0);
    assertThat(stack.get("count")).contains(new IntTag(24));
    assertThat(stack.get("Slot")).contains(new ByteTag((byte) 1));
  }

  @Test
  void writeStartsWithCountThenSlot() {
    // spec: IC-R24 (key order {count, Slot, ...} as in the templates; the position of id and
    // components is left open: the Javadoc and the example of IC-R24 disagree)
    ListTag written = codec().write(Cells.stacks("stone:2"));
    assertThat(List.copyOf(element(written, 0).entries().keySet()).subList(0, 2))
        .containsExactly("count", "Slot");
    assertThat(element(written, 0).entries().keySet())
        .containsExactlyInAnyOrder("count", "Slot", "id");
  }

  @Test
  void writeAddsComponentsOnlyForAStackWithModifications() {
    // spec: IC-R24, IC-R27
    ItemStack healing =
        TestItems.stack("potion", 1, "potion_contents", "{'potion':'minecraft:strong_healing'}");
    ListTag written = codec().write(List.of(healing, Cells.stack("stone")));
    CompoundTag withComponents = element(written, 0);
    assertThat(withComponents.entries().keySet())
        .containsExactlyInAnyOrder("count", "Slot", "id", "components");
    assertThat(withComponents.getCompound("components").entries())
        .containsOnlyKeys("minecraft:potion_contents");
    assertThat(element(written, 1).get("components")).isEmpty();
  }

  @Test
  void readGivesOneStackPerSlotOfTheContainer() {
    // spec: IC-R25 (V-I8 shape: slots 1 and 2 of a furnace)
    ListTag items =
        list("{count:1,Slot:1b,id:'minecraft:stick'}", "{count:24,Slot:2b,id:'minecraft:stone'}");
    List<ItemStack> stacks = Findings.value(codec().read(items, 3));
    assertThat(texts(stacks)).containsExactly("-", "stick:1", "stone:24");
  }

  @Test
  void readOfAnEmptyListGivesOnlyEmptyStacks() {
    // spec: IC-R25
    List<ItemStack> stacks = Findings.value(codec().read(ListTag.empty(), 4));
    assertThat(texts(stacks)).containsExactly("-", "-", "-", "-");
  }

  @Test
  void readKeepsTheComponentsOfAStack() {
    // spec: IC-R24 (V-I7: a potion with potion_contents in slot 0 of a brewing stand)
    List<ItemStack> stacks = Findings.value(codec().read(list(POTION), 5));
    ItemStack expected =
        TestItems.stack("potion", 1, "potion_contents", "{'potion':'minecraft:strong_healing'}");
    assertThat(stacks).hasSize(5);
    assertThat(stacks.get(0)).isEqualTo(expected);
    assertThat(stacks.subList(1, 5)).allMatch(ItemStack::isEmpty);
  }

  @Test
  void readAcceptsTheLastSlotOfTheContainer() {
    // spec: IC-R25
    List<ItemStack> stacks =
        Findings.value(codec().read(list("{count:2,Slot:4b,id:'minecraft:dirt'}"), 5));
    assertThat(texts(stacks)).containsExactly("-", "-", "-", "-", "dirt:2");
  }

  @Test
  void writeThenReadGivesBackTheSlots() {
    // spec: IC-R24, IC-R25 (round trip)
    List<ItemStack> slots =
        List.of(
            Cells.stack("stone:64"),
            ItemStack.empty(),
            TestItems.stack("stone", 7, "rarity", "'epic'"),
            TestItems.stack(
                "potion", 1, "potion_contents", "{'potion':'minecraft:strong_healing'}"),
            ItemStack.empty());
    ListTag written = codec().write(slots);
    assertThat(Findings.value(codec().read(written, slots.size()))).isEqualTo(slots);
  }

  @Test
  void provisionalAnEntryWithoutSlotIsSkippedWithAWarning() {
    // spec: IC-R25, PI6 (provisional: the behavior of the game is not specified)
    ListTag items = list("{count:1,id:'minecraft:stone'}", "{count:2,Slot:1b,id:'minecraft:dirt'}");
    Decoding<List<ItemStack>> decoding = codec().read(items, 3);
    List<ItemStack> stacks = Findings.assertWarnings(decoding, FindingCode.INVALID_SLOT);
    assertThat(texts(stacks)).containsExactly("-", "dirt:2", "-");
  }

  @Test
  void provisionalAnEntryWithATextSlotIsSkippedWithAWarning() {
    // spec: IC-R25, PI6
    ListTag items = list("{count:1,Slot:'1',id:'minecraft:stone'}");
    List<ItemStack> stacks =
        Findings.assertWarnings(codec().read(items, 3), FindingCode.INVALID_SLOT);
    assertThat(texts(stacks)).containsExactly("-", "-", "-");
  }

  @Test
  void provisionalEntriesOutsideTheContainerAreSkippedWithWarnings() {
    // spec: IC-R25, PI6
    ListTag items =
        list(
            "{count:1,Slot:3b,id:'minecraft:stone'}",
            "{count:1,Slot:-1b,id:'minecraft:stone'}",
            "{count:1,Slot:127b,id:'minecraft:stone'}",
            "{count:1,Slot:0b,id:'minecraft:dirt'}");
    List<ItemStack> stacks =
        Findings.assertWarnings(
            codec().read(items, 3),
            FindingCode.INVALID_SLOT,
            FindingCode.INVALID_SLOT,
            FindingCode.INVALID_SLOT);
    assertThat(texts(stacks)).containsExactly("dirt:1", "-", "-");
  }

  @Test
  void provisionalAnAlreadyFilledSlotKeepsItsFirstStack() {
    // spec: IC-R25, PI6 (the Javadoc: a slot already filled is skipped)
    ListTag items =
        list("{count:5,Slot:1b,id:'minecraft:stone'}", "{count:9,Slot:1b,id:'minecraft:dirt'}");
    List<ItemStack> stacks =
        Findings.assertWarnings(codec().read(items, 2), FindingCode.INVALID_SLOT);
    assertThat(texts(stacks)).containsExactly("-", "stone:5");
  }

  @Test
  void provisionalTheFindingsOfAStackAreKept() {
    // spec: IC-R26, IC-R25 (an unknown item is read as the empty stack with a warning)
    ListTag items =
        list(
            "{count:1,Slot:0b,id:'minecraft:inconnu'}",
            "{count:0,Slot:1b,id:'minecraft:stone'}",
            "{count:3,Slot:2b,id:'minecraft:stone'}");
    Decoding<List<ItemStack>> decoding = codec().read(items, 3);
    List<ItemStack> stacks = Findings.value(decoding);
    assertThat(Findings.codesOf(decoding))
        .contains(FindingCode.UNKNOWN_ITEM, FindingCode.NON_POSITIVE_COUNT);
    assertThat(texts(stacks)).containsExactly("-", "-", "stone:3");
  }

  @Test
  void provisionalAGoodListHasNoFinding() {
    // spec: IC-R25
    Decoding<List<ItemStack>> decoding =
        codec().read(list("{count:1,Slot:0b,id:'minecraft:stone'}"), 2);
    assertThat(decoding.findings()).isEmpty();
  }
}

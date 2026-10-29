package dev.shoko.minecraft.item.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R24 to IC-R27, probes PI2 and PI6. The shape of the stacks of
// the structure templates is level D; the defaults, the lenient fallbacks and the writing of
// removals are level S.
class StackCodecNbtTest {

  private static StackCodec codec() {
    return TestItems.stackCodec();
  }

  private static ItemStack read(String relaxedSnbt) {
    return Findings.value(codec().fromNbt(StackSupport.nbt(relaxedSnbt)));
  }

  @Test
  void aStackOfAnInventoryIsReadWithItsItemCountAndComponents() {
    // spec: IC-R24, V-I7 (alembic stack of the structure templates, D)
    ItemStack stack =
        read(
            "{count:1,Slot:0b,components:{'minecraft:potion_contents':"
                + "{potion:'minecraft:strong_healing'}},id:'minecraft:potion'}");
    assertThat(stack.type()).isEqualTo(TestItems.type("potion"));
    assertThat(stack.count()).isEqualTo(1);
    assertThat(stack.component(TestItems.id("potion_contents"))).isPresent();
    assertThat(stack.patch().isEmpty()).isFalse();
  }

  @Test
  void provisionalAStackWithoutComponentsHasTheComponentsOfThePrototype() {
    // spec: IC-R24, V-I8 (furnace stack {count: 24, Slot: 2b, id}, D)
    ItemStack stack = read("{count:24,Slot:2b,id:'minecraft:stone'}");
    assertThat(stack).isEqualTo(TestItems.stack("stone", 24));
  }

  @Test
  void provisionalAMissingCountIsOne() {
    // spec: IC-R26, IC-R24 (sherds {id} stacks have no count), probe PI6
    assertThat(read("{id:'minecraft:stone'}")).isEqualTo(TestItems.stack("stone", 1));
  }

  @Test
  void provisionalExtraKeysSuchAsSlotAreIgnored() {
    // spec: IC-R24 (Javadoc of fromNbt)
    ItemStack stack = read("{id:'minecraft:stone',count:2,Slot:7b,unknown:'x'}");
    assertThat(stack).isEqualTo(TestItems.stack("stone", 2));
  }

  @Test
  void provisionalAUnknownItemIsReadAsTheEmptyStackWithAWarning() {
    // spec: IC-R26, probe PI6
    ItemStack stack =
        Findings.assertWarnings(
            codec().fromNbt(StackSupport.nbt("{id:'minecraft:inconnu',count:1}")),
            FindingCode.UNKNOWN_ITEM);
    assertThat(stack.isEmpty()).isTrue();
  }

  @Test
  void provisionalACountOfZeroIsReadAsTheEmptyStackWithAWarning() {
    // spec: IC-R15, IC-R26, probe PI6
    ItemStack stack =
        Findings.assertWarnings(
            codec().fromNbt(StackSupport.nbt("{id:'minecraft:stone',count:0}")),
            FindingCode.NON_POSITIVE_COUNT);
    assertThat(stack.isEmpty()).isTrue();
  }

  @Test
  void provisionalANegativeCountIsReadAsTheEmptyStackWithAWarning() {
    // spec: IC-R15, IC-R26, probe PI6
    ItemStack stack =
        Findings.assertWarnings(
            codec().fromNbt(StackSupport.nbt("{id:'minecraft:stone',count:-5}")),
            FindingCode.NON_POSITIVE_COUNT);
    assertThat(stack.isEmpty()).isTrue();
  }

  @Test
  void provisionalAMissingIdIsAFailure() {
    // spec: IC-R26 (Javadoc of fromNbt: a missing id is a failure)
    Findings.assertAnyFailure(codec().fromNbt(StackSupport.nbt("{count:1}")));
    Findings.assertAnyFailure(codec().fromNbt(CompoundTag.empty()));
  }

  @Test
  void provisionalAWronglyTypedFieldIsAFailure() {
    // spec: IC-R26 (Javadoc of fromNbt: a wrongly typed field is a failure)
    Findings.assertAnyFailure(codec().fromNbt(StackSupport.nbt("{id:5,count:1}")));
    Findings.assertAnyFailure(
        codec().fromNbt(StackSupport.nbt("{id:'minecraft:stone',count:'x'}")));
    Findings.assertAnyFailure(
        codec().fromNbt(StackSupport.nbt("{id:'minecraft:stone',count:1,components:5}")));
  }

  @Test
  void provisionalAnUnknownComponentFailsTheWholeStack() {
    // spec: IC-R10, IC-R26 (an error in components is a failure)
    Findings.assertFailure(
        codec()
            .fromNbt(
                StackSupport.nbt(
                    "{id:'minecraft:stone',count:1,components:{'minecraft:inconnu':{}}}")),
        FindingCode.UNKNOWN_COMPONENT);
  }

  @Test
  void provisionalAStringComponentIsReadIntoThePatch() {
    // spec: IC-R24, IC-R14
    ItemStack stack = read("{id:'minecraft:stone',count:3,components:{'minecraft:rarity':'epic'}}");
    assertThat(stack).isEqualTo(StackSupport.withRarity("stone", 3, "epic"));
  }

  @Test
  void provisionalAKeyPrefixedByAnExclamationMarkRemovesAComponent() {
    // spec: IC-R27
    ItemStack stack = read("{id:'minecraft:stone',count:1,components:{'!minecraft:rarity':{}}}");
    assertThat(stack.component(StackSupport.rarityId())).isEmpty();
    assertThat(stack)
        .isEqualTo(TestItems.stack("stone", 1).withoutComponent(StackSupport.rarityId()));
  }

  @Test
  void provisionalTheWrittenFormHasIdAndCountAnInt() {
    // spec: IC-R24, IC-R27 (count is an Int, Javadoc of toNbt)
    CompoundTag tag = codec().toNbt(TestItems.stack("stone", 3));
    assertThat(tag.get("id")).contains(new StringTag("minecraft:stone"));
    assertThat(tag.get("count")).contains(new IntTag(3));
    assertThat(tag.entries()).doesNotContainKey("components");
  }

  @Test
  void provisionalTheWrittenKeysFollowTheOrderOfTheJavadoc() {
    // The Javadoc of toNbt orders the keys id, count, components; the example of IC-R24 shows
    // count first. Only the skeleton contract is asserted (key order is not part of NBT equality).
    // spec: IC-R24, IC-R27
    CompoundTag tag = codec().toNbt(StackSupport.withRarity("stone", 3, "epic"));
    assertThat(List.copyOf(tag.entries().keySet())).containsExactly("id", "count", "components");
  }

  @Test
  void provisionalComponentsAreWrittenOnlyWhenThePatchIsNotEmpty() {
    // spec: IC-R27
    CompoundTag plain = codec().toNbt(TestItems.stack("stone", 1));
    assertThat(plain.entries()).doesNotContainKey("components");
    CompoundTag epic = codec().toNbt(StackSupport.withRarity("stone", 1, "epic"));
    CompoundTag components = epic.getCompound("components");
    assertThat(components.get("minecraft:rarity")).contains(new StringTag("epic"));
  }

  @Test
  void provisionalAModificationEqualToThePrototypeIsNotWritten() {
    // spec: IC-R17, IC-R27
    ItemStack common = StackSupport.withRarity("stone", 1, "common");
    assertThat(codec().toNbt(common).entries()).doesNotContainKey("components");
  }

  @Test
  void provisionalARemovedComponentIsWrittenUnderABangKeyWithAnEmptyCompound() {
    // spec: IC-R27, probe PI2
    ItemStack bare = TestItems.stack("stone", 1).withoutComponent(StackSupport.rarityId());
    CompoundTag components = codec().toNbt(bare).getCompound("components");
    assertThat(components.get("!minecraft:rarity")).contains(CompoundTag.empty());
    assertThat(components.entries()).doesNotContainKey("minecraft:rarity");
  }

  @Test
  void writingTheEmptyStackIsRefused() {
    // spec: IC-R15 (the empty stack is written as an absence by the caller, Javadoc of toNbt)
    assertThatIllegalArgumentException().isThrownBy(() -> codec().toNbt(ItemStack.empty()));
  }

  @Test
  void provisionalStacksSurviveAWriteAndARead() {
    // spec: IC-R24, IC-R27
    List<ItemStack> stacks =
        List.of(
            TestItems.stack("stone", 3),
            StackSupport.withRarity("stone", 64, "epic"),
            TestItems.stack("stone", 1).withoutComponent(StackSupport.rarityId()),
            TestItems.stack("stone_pickaxe", 1, "damage", "17"),
            TestItems.stack(
                "potion", 1, "potion_contents", "{'potion':'minecraft:strong_healing'}"),
            TestItems.stack("bread", 2, "food", "{'nutrition':1,'saturation':-0.0}"),
            TestItems.stack("stone", 5, "enchantments", "{'minecraft:sharpness':3}"));
    for (ItemStack stack : stacks) {
      assertThat(Findings.value(codec().fromNbt(codec().toNbt(stack)))).isEqualTo(stack);
    }
  }

  @Test
  void provisionalReadingThenWritingThenReadingGivesTheSameStack() {
    // spec: IC-R24 to IC-R27 (NBT to stack to NBT to stack)
    ItemStack first =
        read(
            "{count:1,Slot:0b,components:{'minecraft:potion_contents':"
                + "{potion:'minecraft:strong_healing'}},id:'minecraft:potion'}");
    CompoundTag written = codec().toNbt(first);
    assertThat(Findings.value(codec().fromNbt(written))).isEqualTo(first);
    assertThat(codec().toNbt(Findings.value(codec().fromNbt(written)))).isEqualTo(written);
  }
}

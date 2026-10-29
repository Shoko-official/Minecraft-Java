package dev.shoko.minecraft.item.stack;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.testing.TestItems;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R16, IC-R19, IC-R20, V-I13, V-I14 (merge, split, give). All
// level S, hence the provisional prefix.
class ItemStackMergeSplitTest {

  @Test
  void provisionalMergeFillsUpToTheStackLimitAndKeepsTheRest() {
    // spec: IC-R19, V-I13 (slot of 60 stones receives a stack of 64)
    MergeResult result = TestItems.stack("stone", 60).mergeFrom(TestItems.stack("stone", 64));
    assertThat(result.merged()).isEqualTo(TestItems.stack("stone", 64));
    assertThat(result.remainder()).isEqualTo(TestItems.stack("stone", 60));
  }

  @Test
  void provisionalMergeThatFitsLeavesNoRemainder() {
    // spec: IC-R19
    MergeResult result = TestItems.stack("stone", 10).mergeFrom(TestItems.stack("stone", 20));
    assertThat(result.merged()).isEqualTo(TestItems.stack("stone", 30));
    assertThat(result.remainder().isEmpty()).isTrue();
  }

  @Test
  void provisionalMergeIntoAFullStackMovesNothing() {
    // spec: IC-R19
    MergeResult result = TestItems.stack("stone", 64).mergeFrom(TestItems.stack("stone", 1));
    assertThat(result.merged()).isEqualTo(TestItems.stack("stone", 64));
    assertThat(result.remainder()).isEqualTo(TestItems.stack("stone", 1));
  }

  @Test
  void provisionalTheLimitIsThatOfTheItem() {
    // spec: IC-R19 (ender pearls stack to 16)
    MergeResult result =
        TestItems.stack("ender_pearl", 10).mergeFrom(TestItems.stack("ender_pearl", 10));
    assertThat(result.merged().count()).isEqualTo(16);
    assertThat(result.remainder().count()).isEqualTo(4);
  }

  @Test
  void provisionalItemsThatDoNotStackNeverMerge() {
    // spec: IC-R19, IC-R5 (max_stack_size 1)
    MergeResult result =
        TestItems.stack("stone_pickaxe", 1).mergeFrom(TestItems.stack("stone_pickaxe", 1));
    assertThat(result.merged()).isEqualTo(TestItems.stack("stone_pickaxe", 1));
    assertThat(result.remainder()).isEqualTo(TestItems.stack("stone_pickaxe", 1));
  }

  @Test
  void provisionalDifferentItemsNeverMerge() {
    // spec: IC-R19
    ItemStack target = TestItems.stack("stone", 10);
    ItemStack source = TestItems.stack("cobblestone", 5);
    MergeResult result = target.mergeFrom(source);
    assertThat(result.merged()).isEqualTo(target);
    assertThat(result.remainder()).isEqualTo(source);
  }

  @Test
  void provisionalContentsThatDifferOnlyByAComponentNeverMerge() {
    // spec: IC-R19 (enchantments, name, lore or durability that differ)
    ItemStack target = TestItems.stack("stone", 10);
    ItemStack source = StackSupport.withRarity("stone", 5, "epic");
    MergeResult result = target.mergeFrom(source);
    assertThat(result.merged()).isEqualTo(target);
    assertThat(result.remainder()).isEqualTo(source);
  }

  @Test
  void provisionalAnEmptyStackReceivesTheContentOfTheSource() {
    // spec: IC-R19 (Javadoc of mergeFrom: an empty stack takes the content of the source)
    ItemStack source = StackSupport.withRarity("stone", 10, "epic");
    MergeResult result = ItemStack.empty().mergeFrom(source);
    assertThat(result.merged()).isEqualTo(source);
    assertThat(result.remainder().isEmpty()).isTrue();
  }

  @Test
  void provisionalAnEmptyStackReceivesAtMostTheStackLimit() {
    // spec: IC-R19 (up to the stack limit of the received content)
    MergeResult result = ItemStack.empty().mergeFrom(TestItems.stack("ender_pearl", 20));
    assertThat(result.merged()).isEqualTo(TestItems.stack("ender_pearl", 16));
    assertThat(result.remainder()).isEqualTo(TestItems.stack("ender_pearl", 4));
  }

  @Test
  void provisionalMergingAnEmptySourceChangesNothing() {
    // spec: IC-R19
    ItemStack target = TestItems.stack("stone", 10);
    MergeResult result = target.mergeFrom(ItemStack.empty());
    assertThat(result.merged()).isEqualTo(target);
    assertThat(result.remainder().isEmpty()).isTrue();
  }

  @Test
  void provisionalMergeKeepsTheComponentsOfTheTarget() {
    // spec: IC-R19
    ItemStack target = StackSupport.withRarity("stone", 10, "epic");
    MergeResult result = target.mergeFrom(StackSupport.withRarity("stone", 5, "epic"));
    assertThat(result.merged().count()).isEqualTo(15);
    assertThat(result.merged().sameContent(target)).isTrue();
  }

  @Test
  void provisionalSplitTakesTheAmountAndKeepsTheRest() {
    // spec: IC-R20
    SplitResult result = TestItems.stack("stone", 10).split(3);
    assertThat(result.taken()).isEqualTo(TestItems.stack("stone", 3));
    assertThat(result.remaining()).isEqualTo(TestItems.stack("stone", 7));
  }

  @Test
  void provisionalSplitOfTheWholeCountLeavesTheEmptyStack() {
    // spec: IC-R20
    SplitResult result = TestItems.stack("stone", 10).split(10);
    assertThat(result.taken()).isEqualTo(TestItems.stack("stone", 10));
    assertThat(result.remaining().isEmpty()).isTrue();
  }

  @Test
  void provisionalSplitIsBoundedByTheCountAvailable() {
    // spec: IC-R20
    SplitResult result = TestItems.stack("stone", 10).split(50);
    assertThat(result.taken()).isEqualTo(TestItems.stack("stone", 10));
    assertThat(result.remaining().isEmpty()).isTrue();
  }

  @Test
  void provisionalSplitOfZeroTakesNothing() {
    // spec: IC-R20 (bounded: nothing taken)
    ItemStack stack = TestItems.stack("stone", 10);
    SplitResult result = stack.split(0);
    assertThat(result.taken().isEmpty()).isTrue();
    assertThat(result.remaining()).isEqualTo(stack);
  }

  @Test
  void provisionalSplitOfTheEmptyStackGivesTwoEmptyStacks() {
    // spec: IC-R20
    SplitResult result = ItemStack.empty().split(5);
    assertThat(result.taken().isEmpty()).isTrue();
    assertThat(result.remaining().isEmpty()).isTrue();
  }

  @Test
  void provisionalSplitLosesNoComponent() {
    // spec: IC-R20 (no component is lost when splitting)
    ItemStack original =
        TestItems.stack("stone", 10, "enchantments", "{'minecraft:sharpness':2}")
            .withComponent(StackSupport.rarityId(), StackSupport.text("epic"));
    SplitResult result = original.split(4);
    assertThat(result.taken().sameContent(original)).isTrue();
    assertThat(result.remaining().sameContent(original)).isTrue();
    assertThat(result.taken().components()).isEqualTo(original.components());
    assertThat(result.taken().count() + result.remaining().count()).isEqualTo(10);
  }

  @Test
  void provisionalSplitDoesNotChangeTheOriginal() {
    // spec: IC-R20 (immutability)
    ItemStack original = TestItems.stack("stone", 10);
    assertThat(original.split(4).taken().count()).isEqualTo(4);
    assertThat(original.count()).isEqualTo(10);
  }

  @Test
  void provisionalGivingOneHundredStonesMakesAStackOf64AndOneOf36() {
    // spec: IC-R16, V-I14
    List<ItemStack> stacks = TestItems.stack("stone", 1).stacksFor(100);
    assertThat(stacks).containsExactly(TestItems.stack("stone", 64), TestItems.stack("stone", 36));
  }

  @Test
  void provisionalGivingFortyEnderPearlsMakesStacksOf16Then8() {
    // spec: IC-R16, probe PI1
    List<ItemStack> stacks = TestItems.stack("ender_pearl", 1).stacksFor(40);
    assertThat(stacks)
        .containsExactly(
            TestItems.stack("ender_pearl", 16),
            TestItems.stack("ender_pearl", 16),
            TestItems.stack("ender_pearl", 8));
  }

  @Test
  void provisionalAnExactMultipleMakesFullStacksOnly() {
    // spec: IC-R16
    assertThat(TestItems.stack("stone", 1).stacksFor(64))
        .containsExactly(TestItems.stack("stone", 64));
    assertThat(TestItems.stack("stone", 1).stacksFor(128))
        .containsExactly(TestItems.stack("stone", 64), TestItems.stack("stone", 64));
  }

  @Test
  void provisionalATotalOfZeroOrLessGivesNoStack() {
    // spec: IC-R16 (empty list for a total of 0 or less)
    assertThat(TestItems.stack("stone", 1).stacksFor(0)).isEmpty();
    assertThat(TestItems.stack("stone", 1).stacksFor(-4)).isEmpty();
  }

  @Test
  void provisionalItemsThatDoNotStackGiveOneStackPerUnit() {
    // spec: IC-R16, IC-R5
    List<ItemStack> stacks = TestItems.stack("stone_pickaxe", 1).stacksFor(3);
    assertThat(stacks).hasSize(3);
    assertThat(stacks).allMatch(s -> s.count() == 1);
  }

  @Test
  void provisionalGivenStacksKeepTheContentOfTheModel() {
    // spec: IC-R16
    ItemStack model = StackSupport.withRarity("stone", 1, "epic");
    List<ItemStack> stacks = model.stacksFor(70);
    assertThat(stacks).hasSize(2);
    assertThat(stacks).allMatch(s -> s.sameContent(model));
    assertThat(stacks.get(0).count()).isEqualTo(64);
    assertThat(stacks.get(1).count()).isEqualTo(6);
  }

  @Test
  void provisionalAModifiedStackLimitGovernsTheDistribution() {
    // spec: IC-R16, probe PI1 (max_stack_size 1 on a stone gives single units)
    ItemStack single = TestItems.stack("stone", 1, "max_stack_size", "1");
    assertThat(single.stacksFor(3)).hasSize(3);
  }
}

package dev.shoko.minecraft.item.stack;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.component.ComponentChange;
import dev.shoko.minecraft.item.component.ComponentPatch;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R14 to IC-R16 (stack identity, count, effective components).
// The stack level is level S in the spec: the tests are provisional unless they read a constant.
class ItemStackTest {

  @Test
  void maxCountIsTheBoundOfTheQuantityArguments() {
    // spec: IC-R16 (bounds 1 to 99 of the command arguments, D)
    assertThat(ItemStack.MAX_COUNT).isEqualTo(99);
  }

  @Test
  void provisionalTheEmptyStackHasNoContent() {
    // spec: IC-R15
    ItemStack empty = ItemStack.empty();
    assertThat(empty.isEmpty()).isTrue();
    assertThat(empty.count()).isZero();
    assertThat(empty.type().isAir()).isTrue();
    assertThat(empty.patch().isEmpty()).isTrue();
  }

  @Test
  void provisionalTheEmptyStackHasTheDefaultStackLimit() {
    // spec: IC-R15, ItemStack#maxStackSize (64 for the empty stack)
    assertThat(ItemStack.empty().maxStackSize()).isEqualTo(64);
  }

  @Test
  void provisionalAirGivesTheEmptyStackWhateverTheCount() {
    // spec: IC-R15
    ItemStack air = ItemStack.of(TestItems.catalog().air(), 5);
    assertThat(air.isEmpty()).isTrue();
    assertThat(air.count()).isZero();
    assertThat(air).isEqualTo(ItemStack.empty());
  }

  @Test
  void provisionalACountOfZeroOrLessGivesTheEmptyStack() {
    // spec: IC-R15
    assertThat(ItemStack.of(TestItems.type("stone"), 0).isEmpty()).isTrue();
    assertThat(ItemStack.of(TestItems.type("stone"), -1).isEmpty()).isTrue();
    assertThat(ItemStack.of(TestItems.type("stone"), Integer.MIN_VALUE).isEmpty()).isTrue();
    ComponentPatch patch = TestItems.patchOf("rarity", "'epic'");
    assertThat(ItemStack.of(TestItems.type("stone"), 0, patch)).isEqualTo(ItemStack.empty());
  }

  @Test
  void provisionalAStackKeepsItsItemAndCount() {
    // spec: IC-R14
    ItemStack stack = TestItems.stack("stone", 7);
    assertThat(stack.isEmpty()).isFalse();
    assertThat(stack.type()).isEqualTo(TestItems.type("stone"));
    assertThat(stack.count()).isEqualTo(7);
    assertThat(stack.patch().isEmpty()).isTrue();
  }

  @Test
  void provisionalWithoutModificationsTheComponentsAreThePrototype() {
    // spec: IC-R14
    ItemStack stack = TestItems.stack("stone_pickaxe", 1);
    assertThat(stack.components()).isEqualTo(stack.type().prototype());
    assertThat(stack.component(TestItems.id("max_damage")))
        .isEqualTo(stack.type().prototype().get(TestItems.id("max_damage")));
  }

  @Test
  void provisionalTheRaritySetByThePatchCoversThePrototypeValue() {
    // spec: IC-R14
    ItemStack plain = TestItems.stack("stone", 1);
    ItemStack epic = StackSupport.withRarity("stone", 1, "epic");
    assertThat(plain.component(StackSupport.rarityId())).contains(StackSupport.text("common"));
    assertThat(epic.component(StackSupport.rarityId())).contains(StackSupport.text("epic"));
    assertThat(epic.components().get(StackSupport.rarityId())).contains(StackSupport.text("epic"));
    assertThat(epic.components().get(TestItems.id("max_stack_size")))
        .isEqualTo(plain.components().get(TestItems.id("max_stack_size")));
  }

  @Test
  void provisionalASetIsRecordedInThePatch() {
    // spec: IC-R14
    ItemStack epic = StackSupport.withRarity("stone", 1, "epic");
    assertThat(epic.patch().changes().get(StackSupport.rarityId()))
        .isEqualTo(new ComponentChange.Set(StackSupport.text("epic")));
  }

  @Test
  void provisionalAComponentAbsentFromThePrototypeCanBeAdded() {
    // spec: IC-R14 (a modification may add a value)
    ItemStack stack = TestItems.stack("stone", 1);
    ItemStack marked = stack.withComponent(TestItems.id("unbreakable"), StackSupport.presence());
    assertThat(stack.component(TestItems.id("unbreakable"))).isEmpty();
    assertThat(marked.component(TestItems.id("unbreakable")))
        .isEqualTo(Optional.of(StackSupport.presence()));
    assertThat(marked.components().contains(TestItems.id("unbreakable"))).isTrue();
  }

  @Test
  void provisionalAComponentOfThePrototypeCanBeRemoved() {
    // spec: IC-R14 (a modification may remove a component of the prototype)
    ItemStack stack = TestItems.stack("stone", 1);
    ItemStack bare = stack.withoutComponent(StackSupport.rarityId());
    assertThat(bare.component(StackSupport.rarityId())).isEmpty();
    assertThat(bare.components().contains(StackSupport.rarityId())).isFalse();
    assertThat(bare.patch().changes().get(StackSupport.rarityId()))
        .isEqualTo(new ComponentChange.Removed());
    assertThat(stack.type().prototype().contains(StackSupport.rarityId())).isTrue();
  }

  @Test
  void provisionalWithersLeaveTheOriginalStackUntouched() {
    // spec: IC-R14 (immutability)
    ItemStack stack = TestItems.stack("stone", 5);
    ItemStack changed =
        stack.withCount(9).withComponent(StackSupport.rarityId(), StackSupport.text("epic"));
    assertThat(stack.count()).isEqualTo(5);
    assertThat(stack.patch().isEmpty()).isTrue();
    assertThat(changed.count()).isEqualTo(9);
  }

  @Test
  void provisionalTheStackLimitIsTheEffectiveMaxStackSize() {
    // spec: IC-R5, IC-R16
    assertThat(TestItems.stack("stone", 1).maxStackSize()).isEqualTo(64);
    assertThat(TestItems.stack("ender_pearl", 1).maxStackSize()).isEqualTo(16);
    assertThat(TestItems.stack("stone_pickaxe", 1).maxStackSize()).isEqualTo(1);
  }

  @Test
  void provisionalAModifiedMaxStackSizeChangesTheLimit() {
    // spec: IC-R16, probe PI1 (max_stack_size set to 1 on a stack of a 64 item)
    ItemStack single = TestItems.stack("stone", 1, "max_stack_size", "1");
    assertThat(single.maxStackSize()).isEqualTo(1);
    ComponentValue sixteen = TestItems.value("max_stack_size", "16");
    assertThat(
            TestItems.stack("stone", 1)
                .withComponent(TestItems.id("max_stack_size"), sixteen)
                .maxStackSize())
        .isEqualTo(16);
  }

  @Test
  void provisionalTheCountIsNotClampedToTheStackSize() {
    // spec: IC-R16 (the count is not clamped: stacksFor splits a total), probe PI1
    assertThat(TestItems.stack("ender_pearl", 40).count()).isEqualTo(40);
    assertThat(TestItems.stack("stone", 99).count()).isEqualTo(99);
    assertThat(TestItems.stack("stone_pickaxe", 3).count()).isEqualTo(3);
  }

  @Test
  void provisionalWithCountKeepsTheContent() {
    // spec: IC-R14, IC-R15
    ItemStack epic = StackSupport.withRarity("stone", 5, "epic");
    ItemStack more = epic.withCount(10);
    assertThat(more.count()).isEqualTo(10);
    assertThat(more.sameContent(epic)).isTrue();
    assertThat(more.patch()).isEqualTo(epic.patch());
  }

  @Test
  void provisionalWithCountOfZeroOrLessGivesTheEmptyStack() {
    // spec: IC-R15
    ItemStack stack = TestItems.stack("stone", 5);
    assertThat(stack.withCount(0).isEmpty()).isTrue();
    assertThat(stack.withCount(-3).isEmpty()).isTrue();
    assertThat(stack.withCount(0)).isEqualTo(ItemStack.empty());
  }

  @Test
  void provisionalAnEmptyStackStaysEmptyWhateverTheCountGiven() {
    // spec: IC-R15 (the empty stack is air, and air gives the empty stack)
    assertThat(ItemStack.empty().withCount(5).isEmpty()).isTrue();
  }

  @Test
  void provisionalToStringDescribesEveryStack() {
    // spec: ItemStack#toString (wording not part of the contract)
    assertThat(ItemStack.empty().toString()).isNotBlank();
    assertThat(StackSupport.withRarity("stone", 3, "epic").toString()).isNotBlank();
  }
}

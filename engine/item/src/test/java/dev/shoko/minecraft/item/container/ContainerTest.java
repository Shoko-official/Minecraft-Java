package dev.shoko.minecraft.item.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R30 (slot arrays, limit per slot), IC-R19 and IC-R42a (insert
// order), IC-R20 (remove). IC-R30 is level S: the behavior tests are provisional.
class ContainerTest {

  private static Container of(int limit, String... cells) {
    return Container.copyOf(Cells.stacks(cells), limit);
  }

  private static List<String> texts(Container container) {
    return container.snapshot().stream().map(Cells::text).toList();
  }

  @Test
  void provisionalAFreshContainerHasEmptySlotsAndItsLimit() {
    // spec: IC-R30
    Container container = Container.of(5, 64);
    assertThat(container.size()).isEqualTo(5);
    assertThat(container.slotLimit()).isEqualTo(64);
    assertThat(container.isEmpty()).isTrue();
    for (int slot = 0; slot < 5; slot++) {
      assertThat(container.get(slot).isEmpty()).isTrue();
    }
  }

  @Test
  void provisionalCopyOfKeepsTheStacksInSlotOrder() {
    // spec: IC-R30
    Container container = of(64, "stone:3", "-", "dirt:7");
    assertThat(container.size()).isEqualTo(3);
    assertThat(container.isEmpty()).isFalse();
    assertThat(texts(container)).containsExactly("stone:3", "-", "dirt:7");
  }

  @Test
  void provisionalCopyOfIsIndependentOfLaterChangesToTheList() {
    // spec: IC-R30 (the Javadoc of copyOf)
    List<ItemStack> source = new ArrayList<>(Cells.stacks("stone:3", "-"));
    Container container = Container.copyOf(source, 64);
    source.set(0, Cells.stack("dirt:9"));
    source.add(Cells.stack("stone"));
    assertThat(container.size()).isEqualTo(2);
    assertThat(texts(container)).containsExactly("stone:3", "-");
  }

  @Test
  void provisionalMaxCountForIsTheSmallerOfStackSizeAndLimit() {
    // spec: IC-R30
    Container chest = Container.of(1, 64);
    assertThat(chest.maxCountFor(Cells.stack("stone"))).isEqualTo(64);
    assertThat(chest.maxCountFor(Cells.stack("ender_pearl"))).isEqualTo(16);
    assertThat(chest.maxCountFor(Cells.stack("stone_pickaxe"))).isEqualTo(1);
    Container small = Container.of(1, 10);
    assertThat(small.maxCountFor(Cells.stack("stone"))).isEqualTo(10);
    assertThat(small.maxCountFor(Cells.stack("ender_pearl"))).isEqualTo(10);
    assertThat(small.maxCountFor(Cells.stack("stone_pickaxe"))).isEqualTo(1);
  }

  @Test
  void provisionalMaxCountForUsesTheEffectiveMaxStackSizeOfTheStack() {
    // spec: IC-R30, IC-R5 (max_stack_size is a component and a stack may modify it)
    ItemStack single = TestItems.stack("stone", 1, "max_stack_size", "1");
    assertThat(Container.of(1, 64).maxCountFor(single)).isEqualTo(1);
  }

  @Test
  void provisionalSetThenGetReturnsTheStack() {
    // spec: IC-R30
    Container container = Container.of(3, 64);
    container.set(1, Cells.stack("stone:12"));
    assertThat(texts(container)).containsExactly("-", "stone:12", "-");
    container.set(1, ItemStack.empty());
    assertThat(container.isEmpty()).isTrue();
  }

  @Test
  void provisionalSetAcceptsExactlyTheMaxCount() {
    // spec: IC-R30
    Container container = Container.of(2, 64);
    container.set(0, Cells.stack("stone:64"));
    container.set(1, Cells.stack("ender_pearl:16"));
    assertThat(texts(container)).containsExactly("stone:64", "ender_pearl:16");
  }

  @Test
  void provisionalSetAboveTheMaxCountIsRefused() {
    // spec: IC-R30 (the Javadoc of set)
    Container container = Container.of(2, 10);
    ItemStack tooMany = Cells.stack("stone:11");
    ItemStack pearls = Cells.stack("ender_pearl:17");
    assertThatThrownBy(() -> container.set(0, tooMany))
        .isInstanceOf(IllegalArgumentException.class);
    Container pearlChest = Container.of(1, 64);
    assertThatThrownBy(() -> pearlChest.set(0, pearls))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(container.isEmpty()).isTrue();
  }

  @Test
  void aSlotOutsideTheContainerIsRefusedByGetSetAndRemove() {
    // spec: IC-R30 (the Javadoc: IndexOutOfBoundsException)
    Container container = Container.of(3, 64);
    ItemStack stone = Cells.stack("stone");
    for (int slot : new int[] {-1, 3, 100, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
      assertThatThrownBy(() -> container.get(slot)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> container.set(slot, stone))
          .isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> container.remove(slot, 1))
          .isInstanceOf(IndexOutOfBoundsException.class);
    }
  }

  @Test
  void provisionalRemoveTakesTheAmountWantedWhenAvailable() {
    // spec: IC-R20
    Container container = of(64, "stone:10");
    ItemStack taken = container.remove(0, 4);
    assertThat(Cells.text(taken)).isEqualTo("stone:4");
    assertThat(texts(container)).containsExactly("stone:6");
  }

  @Test
  void provisionalRemoveIsBoundedByTheCountAvailable() {
    // spec: IC-R20
    Container container = of(64, "stone:10", "dirt:2");
    ItemStack taken = container.remove(0, 64);
    assertThat(Cells.text(taken)).isEqualTo("stone:10");
    assertThat(texts(container)).containsExactly("-", "dirt:2");
  }

  @Test
  void provisionalRemoveFromAnEmptySlotGivesTheEmptyStack() {
    // spec: IC-R20
    Container container = Container.of(2, 64);
    assertThat(container.remove(1, 5).isEmpty()).isTrue();
    assertThat(container.isEmpty()).isTrue();
  }

  @Test
  void provisionalRemoveKeepsTheComponentsOfTheStack() {
    // spec: IC-R20 (no component is lost when taking)
    ItemStack epic = TestItems.stack("stone", 8, "rarity", "'epic'");
    Container container = Container.copyOf(List.of(epic), 64);
    ItemStack taken = container.remove(0, 3);
    assertThat(taken.sameContent(epic)).isTrue();
    assertThat(taken.count()).isEqualTo(3);
    assertThat(container.get(0).sameContent(epic)).isTrue();
    assertThat(container.get(0).count()).isEqualTo(5);
  }

  @Test
  void provisionalInsertGoesFirstToStacksOfTheSameContentInSlotOrder() {
    // spec: IC-R19, IC-R42a
    Container container = of(64, "stone:60", "-", "stone:10");
    ItemStack rest = container.insert(Cells.stack("stone:10"));
    assertThat(rest.isEmpty()).isTrue();
    assertThat(texts(container)).containsExactly("stone:64", "-", "stone:16");
  }

  @Test
  void provisionalInsertPrefersAnExistingStackOverAnEarlierEmptySlot() {
    // spec: IC-R19, IC-R42a (merge first, then empty slots)
    Container container = of(64, "-", "stone:5");
    ItemStack rest = container.insert(Cells.stack("stone:3"));
    assertThat(rest.isEmpty()).isTrue();
    assertThat(texts(container)).containsExactly("-", "stone:8");
  }

  @Test
  void provisionalInsertFillsEmptySlotsInOrderAfterMerging() {
    // spec: IC-R42a
    Container container = of(64, "stone:60", "dirt:1", "-", "-");
    ItemStack rest = container.insert(Cells.stack("stone:70"));
    assertThat(rest.isEmpty()).isTrue();
    assertThat(texts(container)).containsExactly("stone:64", "dirt:1", "stone:64", "stone:2");
  }

  @Test
  void provisionalInsertRespectsTheSlotLimitOfTheContainer() {
    // spec: IC-R30
    Container container = Container.of(3, 10);
    ItemStack rest = container.insert(Cells.stack("stone:25"));
    assertThat(rest.isEmpty()).isTrue();
    assertThat(texts(container)).containsExactly("stone:10", "stone:10", "stone:5");
  }

  @Test
  void provisionalInsertReturnsWhatDoesNotFit() {
    // spec: IC-R19
    Container container = of(64, "stone:64", "dirt:1");
    ItemStack rest = container.insert(Cells.stack("stone:5"));
    assertThat(Cells.text(rest)).isEqualTo("stone:5");
    assertThat(texts(container)).containsExactly("stone:64", "dirt:1");
  }

  @Test
  void provisionalInsertReturnsThePartThatDoesNotFit() {
    // spec: IC-R19
    Container container = of(64, "stone:62", "dirt:1");
    ItemStack rest = container.insert(Cells.stack("stone:5"));
    assertThat(Cells.text(rest)).isEqualTo("stone:3");
    assertThat(texts(container)).containsExactly("stone:64", "dirt:1");
  }

  @Test
  void provisionalInsertDoesNotMergeStacksOfDifferentContent() {
    // spec: IC-R19 (a different rarity is a different content)
    ItemStack epic = TestItems.stack("stone", 5, "rarity", "'epic'");
    Container container = Container.copyOf(List.of(Cells.stack("stone:5"), ItemStack.empty()), 64);
    ItemStack rest = container.insert(epic);
    assertThat(rest.isEmpty()).isTrue();
    assertThat(container.get(0)).isEqualTo(Cells.stack("stone:5"));
    assertThat(container.get(1)).isEqualTo(epic);
  }

  @Test
  void provisionalInsertOfUnstackableItemsTakesOneSlotEach() {
    // spec: IC-R30 (stone_pickaxe has max_stack_size 1)
    Container container = of(64, "stone_pickaxe", "-");
    ItemStack rest = container.insert(Cells.stack("stone_pickaxe"));
    assertThat(rest.isEmpty()).isTrue();
    assertThat(texts(container)).containsExactly("stone_pickaxe:1", "stone_pickaxe:1");
    assertThat(Cells.text(container.insert(Cells.stack("stone_pickaxe"))))
        .isEqualTo("stone_pickaxe:1");
  }

  @Test
  void provisionalInsertOfTheEmptyStackChangesNothing() {
    // spec: IC-R19
    Container container = of(64, "stone:3", "-");
    assertThat(container.insert(ItemStack.empty()).isEmpty()).isTrue();
    assertThat(texts(container)).containsExactly("stone:3", "-");
  }

  @Test
  void provisionalSnapshotIsAnUnmodifiableCopy() {
    // spec: IC-R30
    Container container = of(64, "stone:3", "-");
    List<ItemStack> snapshot = container.snapshot();
    ItemStack dirt = Cells.stack("dirt");
    assertThatThrownBy(() -> snapshot.set(1, dirt))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> snapshot.add(dirt)).isInstanceOf(UnsupportedOperationException.class);
    container.set(1, dirt);
    assertThat(snapshot.get(1).isEmpty()).isTrue();
    assertThat(container.snapshot().get(1)).isEqualTo(dirt);
  }

  @Test
  void provisionalIsEmptyIsFalseAsSoonAsOneSlotHoldsAStack() {
    // spec: IC-R30
    Container container = Container.of(4, 64);
    assertThat(container.isEmpty()).isTrue();
    container.set(3, Cells.stack("dirt"));
    assertThat(container.isEmpty()).isFalse();
    container.remove(3, 1);
    assertThat(container.isEmpty()).isTrue();
  }
}

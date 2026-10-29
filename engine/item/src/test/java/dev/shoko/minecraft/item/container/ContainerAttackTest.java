package dev.shoko.minecraft.item.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: item-components-inventory IC-R30 (limit per slot), IC-R19 and IC-R42a (insert order),
// IC-R20 (remove) against a small reference model written from the rules (adversarial, fixed
// seed). IC-R30 is level S, so the test is provisional.
class ContainerAttackTest {

  private static final String[] KINDS = {"stone", "dirt", "ender_pearl", "stone_pickaxe", "epic"};
  private static final int[] LIMITS = {1, 10, 64, 99};

  /** The model: a kind and a count per slot, written from the rules and nothing else. */
  private static final class Model {
    final String[] kind;
    final int[] count;
    final int limit;

    Model(int size, int limit) {
      this.kind = new String[size];
      this.count = new int[size];
      this.limit = limit;
    }

    static int stackSize(String kind) {
      return switch (kind) {
        case "ender_pearl" -> 16;
        case "stone_pickaxe" -> 1;
        default -> 64;
      };
    }

    int cap(String k) {
      return Math.min(stackSize(k), limit);
    }

    int insert(String k, int amount) {
      int rest = amount;
      for (int i = 0; i < kind.length && rest > 0; i++) {
        if (k.equals(kind[i]) && count[i] > 0) {
          int moved = Math.min(cap(k) - count[i], rest);
          count[i] += moved;
          rest -= moved;
        }
      }
      for (int i = 0; i < kind.length && rest > 0; i++) {
        if (count[i] == 0) {
          int moved = Math.min(cap(k), rest);
          kind[i] = k;
          count[i] = moved;
          rest -= moved;
        }
      }
      return rest;
    }
  }

  private static ItemStack make(String kind, int count, ComponentValue epic) {
    if (count <= 0) {
      return ItemStack.empty();
    }
    if (kind.equals("epic")) {
      return TestItems.stack("stone", count).withComponent(TestItems.id("rarity"), epic);
    }
    return TestItems.stack(kind, count);
  }

  private static void assertSame(Container container, Model model, ComponentValue epic) {
    for (int i = 0; i < model.kind.length; i++) {
      ItemStack expected =
          model.count[i] == 0 ? ItemStack.empty() : make(model.kind[i], model.count[i], epic);
      assertThat(container.get(i)).as("slot %d", i).isEqualTo(expected);
    }
  }

  @Test
  @Timeout(60)
  void provisionalRandomOperationsFollowTheReferenceModel() {
    // spec: IC-R30, IC-R19, IC-R42a, IC-R20
    Random random = new Random(0xC0117A1AL);
    ComponentValue epic = TestItems.value("rarity", "'epic'");
    for (int round = 0; round < 120; round++) {
      int size = 1 + random.nextInt(12);
      int limit = LIMITS[random.nextInt(LIMITS.length)];
      Container container = Container.of(size, limit);
      Model model = new Model(size, limit);
      for (int step = 0; step < 50; step++) {
        switch (random.nextInt(3)) {
          case 0 -> checkSet(random, container, model, epic);
          case 1 -> checkRemove(random, container, model, epic);
          default -> checkInsert(random, container, model, epic);
        }
        assertSame(container, model, epic);
      }
    }
  }

  private static void checkSet(
      Random random, Container container, Model model, ComponentValue epic) {
    int slot = random.nextInt(model.kind.length);
    String kind = KINDS[random.nextInt(KINDS.length)];
    int count = random.nextInt(model.cap(kind) + 3);
    ItemStack stack = make(kind, count, epic);
    if (count > model.cap(kind)) {
      assertThatThrownBy(() -> container.set(slot, stack))
          .isInstanceOf(IllegalArgumentException.class);
      return;
    }
    container.set(slot, stack);
    model.kind[slot] = kind;
    model.count[slot] = count;
  }

  private static void checkRemove(
      Random random, Container container, Model model, ComponentValue epic) {
    int slot = random.nextInt(model.kind.length);
    int amount = 1 + random.nextInt(70);
    int taken = Math.min(amount, model.count[slot]);
    ItemStack removed = container.remove(slot, amount);
    ItemStack expected = taken == 0 ? ItemStack.empty() : make(model.kind[slot], taken, epic);
    assertThat(removed).isEqualTo(expected);
    model.count[slot] -= taken;
  }

  private static void checkInsert(
      Random random, Container container, Model model, ComponentValue epic) {
    String kind = KINDS[random.nextInt(KINDS.length)];
    int amount = 1 + random.nextInt(150);
    int restCount = model.insert(kind, amount);
    ItemStack remainder = container.insert(make(kind, amount, epic));
    ItemStack expected = restCount == 0 ? ItemStack.empty() : make(kind, restCount, epic);
    assertThat(remainder).isEqualTo(expected);
  }

  @Test
  @Timeout(60)
  void provisionalInsertConservesUnitsAndNeverExceedsTheLimit() {
    // spec: IC-R30, IC-R19 (units inserted = units stored + units returned)
    Random random = new Random(0x5EED5L);
    for (int round = 0; round < 200; round++) {
      int limit = LIMITS[random.nextInt(LIMITS.length)];
      Container container = Container.of(1 + random.nextInt(8), limit);
      int inserted = 0;
      int returned = 0;
      for (int step = 0; step < 20; step++) {
        int amount = 1 + random.nextInt(120);
        inserted += amount;
        returned += container.insert(TestItems.stack("stone", amount)).count();
      }
      int stored = 0;
      List<ItemStack> slots = container.snapshot();
      for (ItemStack slot : slots) {
        stored += slot.count();
        assertThat(slot.count()).isLessThanOrEqualTo(Math.min(limit, 64));
      }
      assertThat(stored + returned).isEqualTo(inserted);
    }
  }

  @Test
  @Timeout(60)
  void aSlotOutsideTheContainerIsAlwaysRefusedWhateverTheOperation() {
    // spec: IC-R30 (IndexOutOfBoundsException, never another failure)
    Random random = new Random(0xBAD1D3AL);
    for (int round = 0; round < 200; round++) {
      int size = 1 + random.nextInt(5);
      Container container = Container.of(size, 64);
      int slot = random.nextBoolean() ? size + random.nextInt(1000) : -1 - random.nextInt(1000);
      ItemStack stone = TestItems.stack("stone");
      assertThatThrownBy(() -> container.get(slot)).isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> container.set(slot, stone))
          .isInstanceOf(IndexOutOfBoundsException.class);
      assertThatThrownBy(() -> container.remove(slot, 3))
          .isInstanceOf(IndexOutOfBoundsException.class);
    }
  }
}

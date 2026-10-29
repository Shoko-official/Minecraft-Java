package dev.shoko.minecraft.item.container;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R34 under hypothesis H-inv-a (conventional Inventory slots:
// 0..8 hotbar, 9..35 main, 100..103 feet, legs, chest, head, -106 off hand) combined with IC-R36
// (player window: 0 result, 1..4 grid, 5..8 head to feet, 9..35 main, 36..44 hotbar, 45 off hand).
// Hypothesis H-inv-b (equipment in a separate compound) is undecided until probe PI7: every test
// here is provisional.
class PlayerSlotSpaceTest {

  @Test
  void provisionalHotbarSlotsMapToWindowIndices36To44() {
    // spec: IC-R34, IC-R36 (H-inv-a)
    for (int slot = 0; slot <= 8; slot++) {
      assertThat(PlayerSlotSpace.windowIndexOf(slot)).as("slot %d", slot).hasValue(36 + slot);
    }
  }

  @Test
  void provisionalMainInventorySlotsKeepTheirIndex() {
    // spec: IC-R34, IC-R36 (H-inv-a)
    for (int slot = 9; slot <= 35; slot++) {
      assertThat(PlayerSlotSpace.windowIndexOf(slot)).as("slot %d", slot).hasValue(slot);
    }
  }

  @Test
  void provisionalArmorSlotsMapToWindowIndices8To5() {
    // spec: IC-R34, IC-R36 (H-inv-a: 100 feet, 101 legs, 102 chest, 103 head)
    assertThat(PlayerSlotSpace.windowIndexOf(100)).hasValue(8);
    assertThat(PlayerSlotSpace.windowIndexOf(101)).hasValue(7);
    assertThat(PlayerSlotSpace.windowIndexOf(102)).hasValue(6);
    assertThat(PlayerSlotSpace.windowIndexOf(103)).hasValue(5);
  }

  @Test
  void provisionalTheOffHandIsSlotMinus106AndWindowIndex45() {
    // spec: IC-R34, IC-R36 (H-inv-a)
    assertThat(PlayerSlotSpace.windowIndexOf(-106)).hasValue(45);
  }

  @Test
  void provisionalValuesThatNameNoSlotAreEmpty() {
    // spec: IC-R34 (H-inv-a)
    int[] unnamed = {
      -1,
      -105,
      -107,
      36,
      44,
      45,
      46,
      99,
      104,
      105,
      106,
      255,
      -128,
      Integer.MIN_VALUE,
      Integer.MAX_VALUE
    };
    for (int slot : unnamed) {
      assertThat(PlayerSlotSpace.windowIndexOf(slot)).as("slot %d", slot).isEmpty();
    }
  }

  @Test
  void provisionalWindowIndicesMapBackToInventorySlots() {
    // spec: IC-R34, IC-R36 (H-inv-a)
    for (int index = 36; index <= 44; index++) {
      assertThat(PlayerSlotSpace.nbtSlotOf(index)).as("index %d", index).hasValue(index - 36);
    }
    for (int index = 9; index <= 35; index++) {
      assertThat(PlayerSlotSpace.nbtSlotOf(index)).as("index %d", index).hasValue(index);
    }
    assertThat(PlayerSlotSpace.nbtSlotOf(5)).hasValue(103);
    assertThat(PlayerSlotSpace.nbtSlotOf(6)).hasValue(102);
    assertThat(PlayerSlotSpace.nbtSlotOf(7)).hasValue(101);
    assertThat(PlayerSlotSpace.nbtSlotOf(8)).hasValue(100);
    assertThat(PlayerSlotSpace.nbtSlotOf(45)).hasValue(-106);
  }

  @Test
  void provisionalTheResultAndTheCraftingGridAreNotStoredInInventory() {
    // spec: IC-R36 (0 result, 1..4 grid), the Javadoc of nbtSlotOf
    for (int index = 0; index <= 4; index++) {
      assertThat(PlayerSlotSpace.nbtSlotOf(index)).as("index %d", index).isEmpty();
    }
  }

  @Test
  void provisionalTheTwoConversionsAreInverseBijections() {
    // spec: IC-R34, IC-R36 (H-inv-a): 41 stored window slots, 41 distinct NBT slots
    Set<Integer> seen = new HashSet<>();
    for (int index = 5; index <= 45; index++) {
      int nbt = PlayerSlotSpace.nbtSlotOf(index).orElseThrow();
      assertThat(seen.add(nbt)).as("nbt slot %d reused", nbt).isTrue();
      assertThat(PlayerSlotSpace.windowIndexOf(nbt)).as("index %d", index).hasValue(index);
    }
    assertThat(seen).hasSize(41);
  }

  @Test
  void provisionalEveryNamedSlotRoundTripsFromTheNbtSide() {
    // spec: IC-R34, IC-R36 (H-inv-a)
    int[] named =
        IntStream.concat(IntStream.rangeClosed(0, 35), IntStream.of(100, 101, 102, 103, -106))
            .toArray();
    assertThat(named).hasSize(41);
    for (int slot : named) {
      int index = PlayerSlotSpace.windowIndexOf(slot).orElseThrow();
      assertThat(PlayerSlotSpace.nbtSlotOf(index)).as("slot %d", slot).hasValue(slot);
    }
  }
}

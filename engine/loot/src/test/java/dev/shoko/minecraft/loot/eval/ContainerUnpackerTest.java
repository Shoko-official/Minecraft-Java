package dev.shoko.minecraft.loot.eval;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.container.LootBinding;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.random.DrawReason;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.DrawLog;
import dev.shoko.minecraft.loot.testing.SequenceStore;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: LT-R68 (unpacking), LT-R69 (chest context), LT-R70 (placement of the stacks), LT-R54
// (split),
// LT-R63 (seed of the container), V-L13
class ContainerUnpackerTest {

  private static final int SIZE = 27;
  private static final Identifier TABLE = Identifier.parse("minecraft:chests/test");

  private static LootTable fixed() {
    return Tables.named(
        "chests/test",
        Tables.pool(1, Tables.modified(Tables.item("stone"), Tables.setCount(2))),
        Tables.pool(1, Tables.modified(Tables.item("dirt"), Tables.setCount(3))),
        Tables.pool(1, Tables.item("stick")));
  }

  private static LootContext context(LootTable table, DrawLog log) {
    return Ctx.builder(Map.of("chests/test", table), Lcg48Random.ofSeed(3)).observer(log).build();
  }

  private static UnpackResult unpack(LootTable table, long seed, boolean seeded) {
    return ContainerUnpacker.unpack(
        new LootBinding.Bound(TABLE, seeded, seed),
        SIZE,
        context(table, new DrawLog()),
        new SequenceStore(1));
  }

  private static Map<String, Integer> multiset(UnpackResult result) {
    Map<String, Integer> counts = new HashMap<>();
    for (ItemStack stack : result.slots()) {
      if (!stack.isEmpty()) {
        counts.merge(stack.type().id().path(), stack.count(), Integer::sum);
      }
    }
    return counts;
  }

  private static long filled(UnpackResult result) {
    return result.slots().stream().filter(stack -> !stack.isEmpty()).count();
  }

  @Test
  void provisionalTheResultHasOneStackPerSlotOfTheContainer() {
    // spec: UnpackResult contract (the size of the container, the empty stack for a free slot)
    UnpackResult result = unpack(fixed(), 42L, true);
    assertThat(result.slots()).hasSize(SIZE);
  }

  @Test
  void provisionalTheStacksOfTheTableOccupyDistinctSlotsWithoutLossOrDuplication() {
    // spec: LT-R70
    UnpackResult result = unpack(fixed(), 42L, true);
    assertThat(filled(result)).isEqualTo(3);
    assertThat(multiset(result))
        .containsEntry("stone", 2)
        .containsEntry("dirt", 3)
        .containsEntry("stick", 1);
  }

  @Test
  void provisionalTheMultisetDoesNotDependOnTheSeed() {
    // spec: LT-R70
    Map<String, Integer> baseline = multiset(unpack(fixed(), 1L, true));
    for (long seed = 2; seed <= 25; seed++) {
      assertThat(multiset(unpack(fixed(), seed, true))).as("seed %d", seed).isEqualTo(baseline);
    }
  }

  @Test
  void provisionalASplitStackTakesTwoSlots() {
    // spec: LT-R54, LT-R70
    LootTable big =
        Tables.named(
            "chests/test",
            Tables.pool(1, Tables.modified(Tables.item("stone"), Tables.setCount(100))));
    UnpackResult result = unpack(big, 5L, true);
    assertThat(filled(result)).isEqualTo(2);
    assertThat(multiset(result)).containsEntry("stone", 100);
  }

  @Test
  void provisionalTheSameSeedGivesTheSameContent() {
    // spec: LT-R68, V-L13
    LootTable random =
        Tables.named(
            "chests/test",
            Tables.pool(Tables.uniform(2, 6), Tables.item("stone", 2), Tables.item("dirt")));
    UnpackResult first = unpack(random, 987654L, true);
    UnpackResult second =
        ContainerUnpacker.unpack(
            new LootBinding.Bound(TABLE, true, 987654L),
            SIZE,
            Ctx.builder(Map.of("chests/test", random), Lcg48Random.ofSeed(999)).build(),
            new SequenceStore(77));
    assertThat(second.slots()).isEqualTo(first.slots());
  }

  @Test
  void provisionalDifferentSeedsGiveDifferentPlacements() {
    // spec: LT-R70
    List<List<ItemStack>> seen = new ArrayList<>();
    for (long seed = 1; seed <= 20; seed++) {
      seen.add(unpack(fixed(), seed, true).slots());
    }
    assertThat(seen.stream().distinct().count()).isGreaterThan(10);
  }

  @Test
  void provisionalASingleStackLandsInEverySlotWithRoughlyEqualFrequency() {
    // spec: LT-R70, test statistique
    LootTable single = Tables.named("chests/test", Tables.pool(1, Tables.item("diamond")));
    int[] hits = new int[SIZE];
    int seeds = 2700;
    for (long seed = 1; seed <= seeds; seed++) {
      List<ItemStack> slots = unpack(single, seed, true).slots();
      for (int slot = 0; slot < SIZE; slot++) {
        if (!slots.get(slot).isEmpty()) {
          hits[slot]++;
        }
      }
    }
    for (int slot = 0; slot < SIZE; slot++) {
      assertThat(hits[slot]).as("slot %d", slot).isBetween(55, 150);
    }
  }

  @Test
  void provisionalTheChoiceOfSlotsIsReportedAsContainerPlacement() {
    // spec: ContainerUnpacker contract (reason CONTAINER_PLACEMENT)
    DrawLog log = new DrawLog();
    LootTable single = Tables.named("chests/test", Tables.pool(1, Tables.item("diamond")));
    ContainerUnpacker.unpack(
        new LootBinding.Bound(TABLE, true, 11L), SIZE, context(single, log), new SequenceStore(1));
    assertThat(log.reasons()).contains(DrawReason.CONTAINER_PLACEMENT);
  }

  @Test
  void provisionalATableThatIsNotLoadedGivesEmptySlotsAndAFinding() {
    // spec: ContainerUnpacker contract (TABLE_NOT_FOUND)
    LootContext context = Ctx.builder(Lcg48Random.ofSeed(1)).build();
    UnpackResult result =
        ContainerUnpacker.unpack(
            new LootBinding.Bound(Identifier.parse("minecraft:chests/none"), false, 0L),
            SIZE,
            context,
            new SequenceStore(1));
    assertThat(result.slots()).hasSize(SIZE);
    assertThat(filled(result)).isZero();
    assertThat(result.findings().stream().map(LootFinding::code).toList())
        .contains(LootFindingCode.TABLE_NOT_FOUND);
  }

  @Test
  void provisionalWithoutSeedTheNamedSequenceOfTheTableIsUsed() {
    // spec: LT-R63 (2), LT-R68 (graine absente)
    SequenceStore store = new SequenceStore(1);
    ContainerUnpacker.unpack(
        new LootBinding.Bound(TABLE, false, 0L), SIZE, context(fixed(), new DrawLog()), store);
    assertThat(store.requested()).contains(TABLE);
  }

  @Test
  void provisionalASeedOfZeroMeansNoSeed() {
    // spec: LT-R63, LT-R68
    SequenceStore store = new SequenceStore(1);
    ContainerUnpacker.unpack(
        new LootBinding.Bound(TABLE, true, 0L), SIZE, context(fixed(), new DrawLog()), store);
    assertThat(store.requested()).contains(TABLE);
  }

  @Test
  void provisionalASeededContainerDoesNotTouchTheSequences() {
    // spec: LT-R63
    SequenceStore store = new SequenceStore(1);
    ContainerUnpacker.unpack(
        new LootBinding.Bound(TABLE, true, 31L), SIZE, context(fixed(), new DrawLog()), store);
    assertThat(store.requested()).isEmpty();
  }
}

package dev.shoko.minecraft.world.block;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Failures;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/** BS-R3, BS-R5 to BS-R7: the loader verifies the report and fails on any inconsistency. */
class BlockStateRegistryLoadTest {

  private static BlockReportException loadFailure(List<BlockReportEntry> entries) {
    return Failures.expect(
        BlockReportException.class, () -> BlockStateRegistry.load(Blocks.sourceOf(entries)));
  }

  private static List<BlockReportEntry> withBlock(
      String id, UnaryOperator<List<ReportedState>> change) {
    List<BlockReportEntry> entries = Blocks.entries();
    BlockReportEntry original = Blocks.entryNamed(entries, id);
    BlockReportEntry changed =
        new BlockReportEntry(id, original.properties(), change.apply(original.states()));
    return Blocks.replacing(entries, id, changed);
  }

  private static ReportedState withId(ReportedState state, int id) {
    return new ReportedState(id, state.properties(), state.isDefault());
  }

  private static ReportedState withDefault(ReportedState state, boolean isDefault) {
    return new ReportedState(state.id(), state.properties(), isDefault);
  }

  @Test
  void aConsistentReportLoads() {
    // spec: BS-R7 (the made-up report follows R5 and R6)
    assertThat(Blocks.registry().stateCount()).isEqualTo(Blocks.STATE_COUNT);
  }

  @Test
  void duplicateStateIdentifierIsRejected() {
    // spec: world-block-states "Entrees / sorties" (identifiant en double)
    List<BlockReportEntry> entries =
        withBlock(
            Blocks.OAK_LOG,
            states -> {
              List<ReportedState> copy = new ArrayList<>(states);
              copy.set(1, withId(states.get(1), states.getFirst().id()));
              return copy;
            });

    loadFailure(entries);
  }

  @Test
  void missingStateIdentifierIsRejected() {
    // spec: world-block-states "Entrees / sorties" (identifiant manquant), BS-R3
    List<BlockReportEntry> entries =
        withBlock(Blocks.OAK_LOG, states -> states.subList(0, states.size() - 1));

    loadFailure(entries);
  }

  @Test
  void identifierThatContradictsTheFormulaNamesTheBlock() {
    // spec: BS-R6, BS-R7, BlockReportException (the fault names the block)
    List<BlockReportEntry> entries =
        withBlock(
            Blocks.GRASS_BLOCK,
            states ->
                List.of(
                    withId(states.get(0), states.get(1).id()),
                    withId(states.get(1), states.get(0).id())));

    BlockReportException failure = loadFailure(entries);

    assertThat(failure.blockId()).contains(Blocks.GRASS_BLOCK);
  }

  @Test
  void valueListInReverseOrderContradictsTheReportedIdentifiers() {
    // spec: BS-R6 (values keep the order of their list, the rank gives the identifier)
    List<BlockReportEntry> entries = Blocks.entries();
    BlockReportEntry log = Blocks.entryNamed(entries, Blocks.OAK_LOG);
    BlockReportEntry reversed =
        new BlockReportEntry(
            Blocks.OAK_LOG, List.of(Blocks.prop("axis", "z", "y", "x")), log.states());

    BlockReportException failure = loadFailure(Blocks.replacing(entries, Blocks.OAK_LOG, reversed));

    assertThat(failure.blockId()).contains(Blocks.OAK_LOG);
  }

  @Test
  void blockWithoutDefaultStateIsRejected() {
    // spec: BS-R3 (exactly one default state per block)
    List<BlockReportEntry> entries =
        withBlock(
            Blocks.OAK_LOG, states -> states.stream().map(s -> withDefault(s, false)).toList());

    assertThat(loadFailure(entries).blockId()).contains(Blocks.OAK_LOG);
  }

  @Test
  void blockWithTwoDefaultStatesIsRejected() {
    // spec: BS-R3 (exactly one default state per block)
    List<BlockReportEntry> entries =
        withBlock(
            Blocks.OAK_LOG, states -> states.stream().map(s -> withDefault(s, true)).toList());

    assertThat(loadFailure(entries).blockId()).contains(Blocks.OAK_LOG);
  }

  @Test
  void stateWithAValueOutsideTheListsIsRejected() {
    // spec: BlockStateRegistry.load contract (not a point of the product of the property lists)
    List<BlockReportEntry> entries =
        withBlock(
            Blocks.OAK_LOG,
            states -> {
              List<ReportedState> copy = new ArrayList<>(states);
              ReportedState first = states.getFirst();
              copy.set(0, new ReportedState(first.id(), Map.of("axis", "w"), first.isDefault()));
              return copy;
            });

    assertThat(loadFailure(entries).blockId()).contains(Blocks.OAK_LOG);
  }

  @Test
  void stateThatLacksAPropertyIsRejected() {
    // spec: BS-R2 (a state carries all the properties of its block)
    List<BlockReportEntry> entries =
        withBlock(
            Blocks.OAK_LOG,
            states -> {
              List<ReportedState> copy = new ArrayList<>(states);
              copy.set(0, new ReportedState(states.getFirst().id(), Map.of(), false));
              return copy;
            });

    loadFailure(entries);
  }

  @Test
  void stateWithAnExtraPropertyIsRejected() {
    // spec: BS-R2 (a state carries exactly the properties of its block)
    List<BlockReportEntry> entries =
        withBlock(
            Blocks.OAK_LOG,
            states -> {
              List<ReportedState> copy = new ArrayList<>(states);
              Map<String, String> values = new LinkedHashMap<>(states.getFirst().properties());
              values.put("color", "red");
              copy.set(0, new ReportedState(states.getFirst().id(), values, false));
              return copy;
            });

    loadFailure(entries);
  }

  @Test
  void duplicateBlockIdentifierIsRejected() {
    // spec: BlockStateRegistry.load contract (a duplicate block identifier)
    List<BlockReportEntry> entries = new ArrayList<>(Blocks.entries());
    BlockReportEntry stone = Blocks.entryNamed(entries, Blocks.STONE);
    entries.add(
        Blocks.entry(new Blocks.Def(Blocks.STONE, List.of(), Map.of()), Blocks.STATE_COUNT));
    assertThat(stone.states()).hasSize(1);

    assertThat(loadFailure(entries).blockId()).contains(Blocks.STONE);
  }

  @Test
  void identifiersThatDoNotStartAtZeroAreRejected() {
    // spec: BS-R3 (identifiers cover 0 to n-1)
    List<BlockReportEntry> shifted =
        Blocks.entries().stream()
            .map(
                entry ->
                    new BlockReportEntry(
                        entry.id(),
                        entry.properties(),
                        entry.states().stream().map(s -> withId(s, s.id() + 1)).toList()))
            .toList();

    loadFailure(shifted);
  }

  @Test
  void holeBetweenTwoBlocksIsRejected() {
    // spec: BS-R3, BS-R5 (the first id of a block is the sum of the counts before it)
    List<BlockReportEntry> entries = Blocks.entries();
    List<BlockReportEntry> gap = new ArrayList<>(entries.subList(0, 3));
    for (BlockReportEntry entry : entries.subList(3, entries.size())) {
      gap.add(
          new BlockReportEntry(
              entry.id(),
              entry.properties(),
              entry.states().stream().map(s -> withId(s, s.id() + 1)).toList()));
    }

    loadFailure(gap);
  }

  @Test
  void blocksListedOutOfIdentifierOrderAreRejected() {
    // spec: BS-R4, BS-R5 (blocks are in protocol order, ranges follow it)
    List<BlockReportEntry> swapped = new ArrayList<>(Blocks.entries());
    BlockReportEntry first = swapped.get(0);
    swapped.set(0, swapped.get(1));
    swapped.set(1, first);

    loadFailure(swapped);
  }
}

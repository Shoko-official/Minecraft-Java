package dev.shoko.minecraft.world.block;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Failures;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BlockStateRegistryTest {

  private static BlockStateRegistry registry() {
    return Blocks.registry();
  }

  @Test
  void countsComeFromTheReport() {
    // spec: BS-R3 (block and state counts), BS-R12
    assertThat(registry().blockCount()).isEqualTo(Blocks.BLOCK_COUNT);
    assertThat(registry().stateCount()).isEqualTo(Blocks.STATE_COUNT);
    assertThat(registry().blocks()).hasSize(Blocks.BLOCK_COUNT);
  }

  @Test
  void blocksAreInProtocolOrder() {
    // spec: BS-R4, BS-R5 (protocol id i is the position i)
    List<BlockType> blocks = registry().blocks();

    for (int k = 0; k < blocks.size(); k++) {
      assertThat(blocks.get(k).protocolId()).isEqualTo(k);
    }
    assertThat(blocks.getFirst().id()).isEqualTo(Blocks.AIR);
    assertThat(blocks.get(6).id()).isEqualTo(Blocks.CHEST);
  }

  @Test
  void stateRangesAreConsecutiveAndCoverEveryIdentifierOnce() {
    // spec: BS-R3, BS-R5 (ranges follow protocol order, ids 0 to n-1 without hole)
    int expectedFirst = 0;
    for (BlockType block : registry().blocks()) {
      assertThat(block.firstStateId()).as(block.id()).isEqualTo(expectedFirst);
      assertThat(block.defaultStateId())
          .as(block.id())
          .isBetween(expectedFirst, expectedFirst + block.stateCount() - 1);
      expectedFirst += block.stateCount();
    }
    assertThat(expectedFirst).isEqualTo(registry().stateCount());
  }

  @Test
  void blockLookupByIdentifier() {
    assertThat(registry().block(Blocks.OAK_LOG)).isPresent();
    assertThat(registry().block(Blocks.OAK_LOG).orElseThrow().protocolId()).isEqualTo(5);
    assertThat(registry().block("minecraft:unknown")).isEmpty();
    assertThat(registry().block("oak_log")).isEmpty();
    assertThat(registry().block("")).isEmpty();
  }

  @Test
  void blockLookupByProtocolId() {
    assertThat(registry().blockByProtocolId(0).id()).isEqualTo(Blocks.AIR);
    assertThat(registry().blockByProtocolId(11).id()).isEqualTo(Blocks.BARREL);
    Failures.expect(IllegalArgumentException.class, () -> registry().blockByProtocolId(-1));
    Failures.expect(
        IllegalArgumentException.class, () -> registry().blockByProtocolId(Blocks.BLOCK_COUNT));
  }

  @Test
  void stateLookupCoversTheWholeRangeAndNothingElse() {
    // spec: BS-R5, BlockStateRegistry.state contract (IllegalArgumentException out of range)
    assertThat(registry().state(0).block().id()).isEqualTo(Blocks.AIR);
    assertThat(registry().state(Blocks.STATE_COUNT - 1).block().id()).isEqualTo(Blocks.BARREL);
    Failures.expect(IllegalArgumentException.class, () -> registry().state(-1));
    Failures.expect(IllegalArgumentException.class, () -> registry().state(Blocks.STATE_COUNT));
    Failures.expect(IllegalArgumentException.class, () -> registry().state(Integer.MIN_VALUE));
    Failures.expect(IllegalArgumentException.class, () -> registry().state(Integer.MAX_VALUE));
  }

  @Test
  void everyIdentifierIsAStateOfTheBlockThatOwnsItsRange() {
    // spec: BS-R5, BS-R6
    for (int id = 0; id < registry().stateCount(); id++) {
      BlockState state = registry().state(id);
      BlockType block = state.block();
      assertThat(state.id()).isEqualTo(id);
      assertThat(id).isBetween(block.firstStateId(), block.firstStateId() + block.stateCount() - 1);
    }
  }

  @Test
  void stateByValuesAgreesWithTheIdentifierLookup() {
    // spec: BS-R6, BlockStateRegistry.state(BlockType, Map)
    BlockType chest = registry().block(Blocks.CHEST).orElseThrow();
    Map<String, String> values = Map.of("facing", "north", "type", "left", "waterlogged", "true");

    assertThat(registry().state(chest, values)).isEqualTo(registry().state(26));
    Failures.expect(IllegalArgumentException.class, () -> registry().state(chest, Map.of()));
  }

  @Test
  void defaultStateIsNotAlwaysTheFirstState() {
    // spec: BS-R10
    BlockType grass = registry().block(Blocks.GRASS_BLOCK).orElseThrow();
    BlockType log = registry().block(Blocks.OAK_LOG).orElseThrow();
    BlockType hopper = registry().block(Blocks.HOPPER).orElseThrow();

    assertThat(registry().defaultState(grass).id()).isEqualTo(3);
    assertThat(registry().defaultState(grass).value("snowy")).isEqualTo("false");
    assertThat(registry().defaultState(log).id()).isEqualTo(22);
    assertThat(registry().defaultState(log).value("axis")).isEqualTo("y");
    assertThat(registry().defaultState(hopper).id()).isEqualTo(hopper.firstStateId());
  }

  @Test
  void defaultStateMatchesTheDefaultStateIdOfEachBlock() {
    // spec: BS-R10
    for (BlockType block : registry().blocks()) {
      assertThat(registry().defaultState(block).id())
          .as(block.id())
          .isEqualTo(block.defaultStateId());
    }
  }

  @Test
  void airIsTheStateZero() {
    // spec: BS-R4, BS-R11 (default value of the registry is air, identifier 0)
    assertThat(registry().air().id()).isZero();
    assertThat(registry().air().block().id()).isEqualTo(Blocks.AIR);
    assertThat(registry().air()).isEqualTo(registry().state(0));
  }

  @Test
  void blockListCannotBeModified() {
    // spec: BlockStateRegistry.blocks contract (unmodifiable)
    List<BlockType> blocks = registry().blocks();

    Failures.expect(UnsupportedOperationException.class, () -> blocks.removeFirst());
  }

  @Test
  void loadingTheSameReportTwiceGivesTheSameIdentifiers() {
    // spec: determinism (same input, same output)
    BlockStateRegistry first = registry();
    BlockStateRegistry second = registry();

    for (int id = 0; id < first.stateCount(); id++) {
      assertThat(second.state(id)).isEqualTo(first.state(id));
    }
  }
}

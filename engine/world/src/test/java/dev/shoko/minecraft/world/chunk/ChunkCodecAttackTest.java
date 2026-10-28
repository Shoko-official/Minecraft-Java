package dev.shoko.minecraft.world.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.RichChunk;
import dev.shoko.minecraft.world.support.TreeMutator;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Seeded attack on {@link ChunkCodec}: structural damage to valid roots. Reading never throws for a
 * fault of the data (ChunkCodec Javadoc); whatever is read can be written, and what is written
 * reads back to the same chunk.
 */
class ChunkCodecAttackTest {

  private static final int MUTATIONS_PER_SEED = 12;

  static Stream<Long> seeds() {
    return LongStream.range(0, 40).boxed();
  }

  private static List<CompoundTag> bases() {
    return List.of(
        ChunkNbt.chunk(-1, -1, true, "bedrock"),
        ChunkNbt.chunk(-1, -1, true, "light_sections"),
        ChunkNbt.chunk(-1, -1, true, "postprocessing"),
        ChunkNbt.chunk(-1, -1, true, "skylight_chain"),
        Chunks.codec().encode(RichChunk.build(Blocks.registry())));
  }

  private static void checkOutcome(ReadResult<Chunk> result) {
    switch (result) {
      case ReadResult.Present<Chunk> present -> {
        Chunk chunk = present.value();
        assertThat(present.warnings()).isNotNull();
        assertThat(chunk.sections()).hasSize(24);
        for (ChunkSection section : chunk.sections()) {
          assertThat(section.blockStates().size()).isEqualTo(4096);
          assertThat(section.biomes().size()).isEqualTo(64);
          assertThat(section.blockStates().get(4095)).isNotNull();
          assertThat(section.biomes().get(63)).isNotNull();
        }
        CompoundTag written = Chunks.codec().encode(chunk);
        ReadResult.Present<Chunk> again = Chunks.present(Chunks.decode(written));
        assertThat(again.warnings()).isEmpty();
        Chunks.assertSameChunk(chunk, again.value());
        assertThat(ChunkNbt.bytes(Chunks.codec().encode(again.value())))
            .isEqualTo(ChunkNbt.bytes(Chunks.codec().encode(chunk)));
      }
      case ReadResult.Absent<Chunk> absent -> assertThat(absent.cause()).isNotNull();
      case ReadResult.Refused<Chunk> refused -> {
        assertThat(refused.cause()).isNotNull();
        assertThat(refused.message()).isNotBlank();
      }
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void damagedRootsGiveOneOfTheThreeIssuesAndNeverThrow(long seed) {
    // spec: ChunkCodec.decode contract ("never throws for a fault of the data"), AR-R26
    int index = 0;
    for (CompoundTag base : bases()) {
      TreeMutator mutator = new TreeMutator(seed * 31 + index++);
      for (int round = 0; round < MUTATIONS_PER_SEED / 4; round++) {
        CompoundTag damaged = mutator.mutate(base, 1 + round % 3);
        checkOutcome(Chunks.decode(damaged));
      }
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void heavilyDamagedRootsStillGiveAnIssue(long seed) {
    // spec: ChunkCodec.decode contract; many mutations at once
    TreeMutator mutator = new TreeMutator(1_000 + seed);
    for (CompoundTag base : bases()) {
      checkOutcome(Chunks.decode(mutator.mutate(base, 25)));
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void emptyAndRandomRootsAreNeverTakenForAChunk(long seed) {
    // spec: CF-R35 (a root without DataVersion, Status and sections is not a chunk)
    TreeMutator mutator = new TreeMutator(5_000 + seed);
    CompoundTag random = mutator.randomTag(2) instanceof CompoundTag c ? c : CompoundTag.empty();

    assertThat(Chunks.decode(CompoundTag.empty())).isNotInstanceOf(ReadResult.Present.class);
    ReadResult<Chunk> result = Chunks.decode(random);
    assertThat(result).isNotInstanceOf(ReadResult.Present.class);
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void netherRootsWithDamageDoNotThrow(long seed) {
    // spec: ChunkCodec.decode contract with another dimension height
    TreeMutator mutator = new TreeMutator(9_000 + seed);
    CompoundTag damaged = mutator.mutate(ChunkNbt.netherChunk(1, 2), 3);

    ReadResult<Chunk> result = Chunks.codec().decode(damaged, Chunks.NETHER);

    if (result instanceof ReadResult.Present<Chunk> present) {
      assertThat(present.value().sections()).hasSize(16);
      CompoundTag written = Chunks.codec().encode(present.value());
      ReadResult.Present<Chunk> again =
          Chunks.present(Chunks.codec().decode(written, Chunks.NETHER));
      Chunks.assertSameChunk(present.value(), again.value());
    }
  }
}

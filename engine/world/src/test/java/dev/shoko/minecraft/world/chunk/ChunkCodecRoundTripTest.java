package dev.shoko.minecraft.world.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.RichChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** CF-R20 to CF-R35: what is read, written and read again is the same chunk. */
class ChunkCodecRoundTripTest {

  private final BlockStateRegistry registry = Blocks.registry();

  @Test
  void aChunkThatUsesEveryPartOfTheModelSurvivesAWriteAndARead() {
    // spec: "Cas limites" of chunk-format (read, written and read again: same sections, lights,
    // attachments)
    Chunk original = RichChunk.build(registry);

    ReadResult.Present<Chunk> back = Chunks.present(Chunks.decode(Chunks.codec().encode(original)));

    assertThat(back.warnings()).isEmpty();
    Chunks.assertSameChunk(original, back.value());
  }

  @Test
  void aWrittenChunkKeepsItsPositionWhenReadAgain() {
    // spec: CF-R20 (xPos and zPos are the absolute position)
    ReadResult.Present<Chunk> back =
        Chunks.present(Chunks.decode(Chunks.codec().encode(RichChunk.build(registry))));

    assertThat(back.value().pos()).isEqualTo(new ChunkPos(-3, 7));
    assertThat(back.value().lastUpdate()).isEqualTo(123_456L);
    assertThat(back.value().inhabitedTime()).isEqualTo(789L);
  }

  @Test
  void aSectionWithEveryStateOfTheRegistryRoundTrips() {
    // spec: CF-R7 (palette of 1455 entries: 11 bits, 820 longs)
    Chunk original = RichChunk.build(registry);
    CompoundTag written = Chunks.codec().encode(original);

    Chunk back = Chunks.decodeClean(written);

    for (int index = 0; index < Blocks.STATE_COUNT; index++) {
      assertThat(back.section(0).blockStates().get(index).id()).isEqualTo(index);
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "bedrock",
        "string_palette",
        "partial_props",
        "unknown_block",
        "bad_value",
        "unknown_biome",
        "missing_block_states",
        "missing_biomes",
        "missing_section",
        "light_sections",
        "far_sections",
        "unsorted_sections",
        "ypos_wrong",
        "ypos_absent",
        "skylight_chain",
        "bad_heightmap_length",
        "heightmap_overflow",
        "postprocessing"
      })
  void everyReadableVectorKeepsItsDecodedStateThroughARewrite(String variant) {
    // spec: RG3, RG4 (read without modification then written: same decoded chunk, CF-R30, CF-R31)
    ReadResult.Present<Chunk> first =
        Chunks.present(Chunks.decode(ChunkNbt.chunk(-1, -1, true, variant)));

    ReadResult.Present<Chunk> second =
        Chunks.present(Chunks.decode(Chunks.codec().encode(first.value())));

    Chunks.assertSameChunk(first.value(), second.value());
  }

  @Test
  void rewrittenVectorChunkReadsWithoutAnyWarning() {
    // spec: CF-R32 f, CF-R34 (a rewrite is complete and valid: nothing left to correct)
    for (String variant :
        new String[] {
          "unknown_block", "bad_value", "missing_section", "ypos_wrong", "bad_heightmap_length"
        }) {
      Chunk first = Chunks.present(Chunks.decode(ChunkNbt.chunk(-1, -1, true, variant))).value();

      ReadResult.Present<Chunk> second =
          Chunks.present(Chunks.decode(Chunks.codec().encode(first)));

      assertThat(second.warnings()).as(variant).isEmpty();
    }
  }

  @Test
  void chunkReadWithAnUnknownHeightmapLosesThatTypeOnly() {
    // spec: CF-R34 (a rewrite without recompute loses the unknown type, keeps the valid ones)
    Chunk first =
        Chunks.present(Chunks.decode(ChunkNbt.chunk(-1, -1, true, "bad_heightmap_length"))).value();

    CompoundTag written = Chunks.codec().encode(first);

    assertThat(written.getCompound("Heightmaps").entries().keySet())
        .containsExactlyInAnyOrder("MOTION_BLOCKING_NO_LEAVES", "OCEAN_FLOOR", "WORLD_SURFACE");
  }

  @Test
  void netherChunkRoundTrips() {
    // spec: CF-R1, CF-R2, CF-R33
    Chunk first =
        Chunks.present(Chunks.codec().decode(ChunkNbt.netherChunk(2, -2), Chunks.NETHER)).value();

    Chunk second =
        Chunks.present(Chunks.codec().decode(Chunks.codec().encode(first), Chunks.NETHER)).value();

    Chunks.assertSameChunk(first, second);
    assertThat(second.sections()).hasSize(16);
  }

  @Test
  void writtenTreeOfAReadVectorHasTheSameDecodedBlocksAsTheOriginalTree() {
    // spec: ChunkCodec Javadoc (the decoded tree, not the bytes, is compared with the reference)
    CompoundTag original = ChunkNbt.chunk(-1, -1, true, "bedrock");
    Chunk chunk = Chunks.decodeClean(original);

    Chunk back = Chunks.decodeClean(Chunks.codec().encode(chunk));

    for (int index = 0; index < 4096; index++) {
      assertThat(back.section(-4).blockStates().get(index))
          .isEqualTo(chunk.section(-4).blockStates().get(index));
    }
    assertThat(back.section(-4).blockStates().get(0).id()).isEqualTo(4);
    assertThat(back.section(-4).blockStates().get(256).id()).isZero();
  }
}

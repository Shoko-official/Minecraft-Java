package dev.shoko.minecraft.world.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.RichChunk;
import dev.shoko.minecraft.world.support.TreeMutator;
import org.junit.jupiter.api.Test;

/** Bug-hunter probes: what a lenient read keeps must be a fixpoint of write then read. */
class ChunkFixpointHuntTest {

  @Test
  void readWriteReadIsAFixpointAndTheSecondReadHasNoWarning() {
    CompoundTag clean = Chunks.codec().encode(RichChunk.build(Blocks.registry()));
    int present = 0;
    for (long seed = 1; seed <= 6000; seed++) {
      CompoundTag damaged = new TreeMutator(seed).mutate(clean, 1 + (int) (seed % 3));
      ReadResult<Chunk> first = Chunks.decode(damaged);
      if (!(first instanceof ReadResult.Present<Chunk> read)) {
        continue;
      }
      present++;
      CompoundTag written = Chunks.codec().encode(read.value());
      ReadResult<Chunk> second = Chunks.decode(written);
      assertThat(second)
          .as("seed %d, first warnings %s", seed, read.warnings())
          .isInstanceOf(ReadResult.Present.class);
      ReadResult.Present<Chunk> again = (ReadResult.Present<Chunk>) second;
      assertThat(again.warnings())
          .as("seed %d, first warnings %s", seed, read.warnings())
          .isEmpty();
      Chunks.assertSameChunk(read.value(), again.value());
      assertThat(Chunks.codec().encode(again.value())).as("seed %d", seed).isEqualTo(written);
    }
    assertThat(present).isGreaterThan(1000);
  }
}

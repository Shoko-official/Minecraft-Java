package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.RichChunk;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Bug-hunter probes: byte level damage of the NBT inside a record, read through the store. */
class StoreFuzzHuntTest {

  @TempDir Path base;

  @Test
  void byteDamageOfTheNbtAlwaysGivesOneOfTheThreeIssues() throws Exception {
    byte[] clean = ChunkNbt.bytes(Chunks.codec().encode(RichChunk.build(Blocks.registry())));
    ChunkPos pos = RichChunk.POS;
    SplittableRandom random = new SplittableRandom(26_10_28L);
    int[] interesting = {0, 1, 0x7F, 0x80, 0xFF, 0x0A, 0x09, 0x0C, 0x07, 0x0B};
    for (int round = 0; round < 3000; round++) {
      byte[] damaged = clean.clone();
      int edits = 1 + random.nextInt(4);
      for (int k = 0; k < edits; k++) {
        int at = random.nextInt(damaged.length);
        damaged[at] =
            random.nextBoolean()
                ? (byte) interesting[random.nextInt(interesting.length)]
                : (byte) random.nextInt(256);
      }
      if (random.nextInt(8) == 0) {
        damaged = Arrays.copyOf(damaged, random.nextInt(damaged.length));
      }
      Path folder = base.resolve("r" + (round % 4));
      Files.createDirectories(folder);
      try (RegionFile file = RegionFile.open(folder, pos.region())) {
        file.write(pos, damaged, RegionCompression.NONE, 1L);
      }
      try (AnvilChunkStore store =
          AnvilChunkStore.open(folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB)) {
        ReadResult<Chunk> result;
        try {
          result = store.load(pos);
        } catch (RuntimeException | StackOverflowError | OutOfMemoryError e) {
          throw new AssertionError("round " + round + " edits " + List.of(edits), e);
        }
        assertThat(result).isNotNull();
      }
    }
  }
}

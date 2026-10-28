package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.RichChunk;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Bug-hunter probes: resources held by a store, and determinism of what it writes. */
class StoreResourceHuntTest {

  @TempDir Path base;

  private static long openHandles() throws IOException, InterruptedException {
    Path descriptors = Path.of("/proc/self/fd");
    if (Files.isDirectory(descriptors)) {
      try (var list = Files.list(descriptors)) {
        return list.count();
      }
    }
    String name = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);
    assumeTrue(name.contains("win"), "no way to count handles on " + name);
    Process process =
        new ProcessBuilder(
                "powershell",
                "-NoProfile",
                "-Command",
                "(Get-Process -Id " + ProcessHandle.current().pid() + ").HandleCount")
            .redirectErrorStream(true)
            .start();
    String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    process.waitFor();
    return Long.parseLong(out);
  }

  @Test
  void loadingChunksOfManyRegionsDoesNotKeepOneFileOpenPerRegion() throws Exception {
    int regions = 1500;
    byte[] nbt = {10, 0, 0, 0};
    for (int r = 0; r < regions; r++) {
      ChunkPos pos = new ChunkPos(r * 32, 0);
      try (RegionFile file = RegionFile.open(base, pos.region())) {
        file.write(pos, nbt, RegionCompression.NONE, 1L);
      }
    }
    long before = openHandles();
    try (AnvilChunkStore store =
        AnvilChunkStore.open(base, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB)) {
      for (int r = 0; r < regions; r++) {
        ReadResult<Chunk> result = store.load(new ChunkPos(r * 32, 0));
        assertThat(result).isNotNull();
      }
      long during = openHandles();
      assertThat(during - before)
          .as("open handles gained by reading one chunk in each of %d regions", regions)
          .isLessThan(300);
    }
  }

  @Test
  void savingTheSameChunkInTwoWorldsGivesTheSameBytesForEveryCompression() throws Exception {
    Chunk chunk = RichChunk.build(Blocks.registry());
    for (RegionCompression compression : RegionCompression.values()) {
      Path one = base.resolve("one-" + compression);
      Path two = base.resolve("two-" + compression);
      for (Path folder : new Path[] {one, two}) {
        try (AnvilChunkStore store =
            AnvilChunkStore.open(folder, Chunks.OVERWORLD, Chunks.codec(), compression)) {
          store.save(chunk, 1_790_000_000L);
          store.save(chunk, 1_790_000_001L);
        }
      }
      byte[] a = Files.readAllBytes(one.resolve("r.-1.0.mca"));
      byte[] b = Files.readAllBytes(two.resolve("r.-1.0.mca"));
      assertThat(Arrays.equals(a, b)).as("region file with %s", compression).isTrue();
    }
  }

  @Test
  void aChunkTheStoreSavedCanBeLoadedBackWithTheSameStore() throws Exception {
    Chunk chunk = RichChunk.build(Blocks.registry());
    long limit = 8_000;
    try (AnvilChunkStore store =
        AnvilChunkStore.open(
            base, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB, limit)) {
      store.save(chunk, 1L);
    }
    try (AnvilChunkStore store =
        AnvilChunkStore.open(
            base, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB, limit)) {
      assertThat(store.load(RichChunk.POS)).isInstanceOf(ReadResult.Present.class);
    }
  }
}

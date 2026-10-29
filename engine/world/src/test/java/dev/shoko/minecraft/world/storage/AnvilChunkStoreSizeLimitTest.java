package dev.shoko.minecraft.world.storage;

import static dev.shoko.minecraft.world.support.RegionFixtures.CHUNK_A;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.RefusedCause;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.RegionFixtures;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * AR-R29 at the write: a store refuses to save a chunk whose NBT has more bytes than its
 * decompression limit, because it could not load it back. Exactly the limit passes; one byte more
 * does not, and nothing is written. The size is the one of the NBT of the test chunk, measured
 * here.
 */
class AnvilChunkStoreSizeLimitTest {

  @TempDir Path base;

  private final Chunk chunk = Chunks.empty(Blocks.registry(), CHUNK_A, Chunks.OVERWORLD);

  private long nbtSize() {
    return ChunkNbt.bytes(Chunks.codec().encode(chunk)).length;
  }

  private AnvilChunkStore open(Path folder, RegionCompression compression, long limit) {
    return AnvilChunkStore.open(folder, Chunks.OVERWORLD, Chunks.codec(), compression, limit);
  }

  @ParameterizedTest
  @EnumSource(RegionCompression.class)
  void aChunkWhoseNbtHasExactlyTheLimitIsSavedAndLoadedBack(RegionCompression compression)
      throws IOException {
    // spec: AR-R29 (exactly the limit is accepted), ChunkTooLargeException (more than the limit)
    long size = nbtSize();
    Path folder = base.resolve(compression.name());

    try (AnvilChunkStore store = open(folder, compression, size)) {
      store.save(chunk, 1L);
    }
    try (AnvilChunkStore store = open(folder, compression, size)) {
      assertThat(Chunks.present(store.load(CHUNK_A)).value().pos()).isEqualTo(CHUNK_A);
    }
  }

  @ParameterizedTest
  @EnumSource(RegionCompression.class)
  void aChunkWhoseNbtHasOneByteMoreThanTheLimitIsRefusedAndNothingIsWritten(
      RegionCompression compression) throws IOException {
    // spec: AR-R29 (limit + 1 is refused), ChunkTooLargeException (nothing is written)
    long size = nbtSize();
    Path folder = base.resolve(compression.name());

    try (AnvilChunkStore store = open(folder, compression, size - 1)) {
      ChunkTooLargeException failure =
          Failures.expect(ChunkTooLargeException.class, () -> store.save(chunk, 1L));
      assertThat(failure.getMessage())
          .contains(String.valueOf(size))
          .contains(String.valueOf(size - 1));
    }

    assertThat(Files.exists(folder)).isFalse();
  }

  @Test
  void aRefusedSaveLeavesAnExistingRegionFileAndItsChunkIntact() throws IOException {
    // spec: ChunkTooLargeException (nothing is written): the previous record stays readable
    long size = nbtSize();
    Path folder = base.resolve("region");
    try (AnvilChunkStore store = open(folder, RegionCompression.ZLIB, size)) {
      store.save(chunk, 1L);
    }
    byte[] before = RegionFixtures.read(RegionFixtures.regionFile(folder));

    try (AnvilChunkStore store = open(folder, RegionCompression.ZLIB, size - 1)) {
      Failures.expect(ChunkTooLargeException.class, () -> store.save(chunk, 2L));
      assertThat(Chunks.refused(store.load(CHUNK_A)).cause()).isEqualTo(RefusedCause.TOO_LARGE);
    }

    assertThat(RegionFixtures.read(RegionFixtures.regionFile(folder))).isEqualTo(before);
  }
}

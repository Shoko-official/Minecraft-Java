package dev.shoko.minecraft.world.storage;

import static dev.shoko.minecraft.world.support.RegionFixtures.CHUNK_A;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.RegionFixtures;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * AR-R28: the marks belong to the store, not to its region files. The store keeps at most 256
 * region files open, so a refused position must stay protected after its region file was closed on
 * eviction.
 */
class AnvilChunkStoreMarkEvictionTest {

  // The documented ceiling of open regions, written out so that a change of the constant cannot
  // move the test with it.
  private static final int MAX_OPEN_REGIONS = 256;

  @TempDir Path folder;

  private final BlockStateRegistry registry = Blocks.registry();

  private AnvilChunkStore open() {
    return AnvilChunkStore.open(folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB);
  }

  private Chunk chunkAt(ChunkPos pos) {
    return Chunks.empty(registry, pos, Chunks.OVERWORLD);
  }

  // Reads one chunk in each of more regions than the store keeps open, none of them the region of
  // CHUNK_A (-1, -1): the region files do not exist, which still opens a region entry per read.
  private static void readMoreRegionsThanTheStoreKeepsOpen(AnvilChunkStore store)
      throws IOException {
    for (int region = 0; region <= MAX_OPEN_REGIONS; region++) {
      ReadResult<Chunk> result = store.load(new ChunkPos(32 * region, 0));
      assertThat(result).isNotInstanceOf(ReadResult.Present.class);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"compression_127", "bad_length", "duplicate_section", "missing_mcc"})
  void aRefusedPositionStaysRefusedAfterItsRegionWasEvicted(String variant) throws IOException {
    // spec: AR-R28 (the mark is kept by the store: a marked position is never read again from the
    // file, even when the region file was closed by the limit of open files)
    RegionFixtures.writeVariant(folder, variant);

    try (AnvilChunkStore store = open()) {
      ReadResult.Refused<Chunk> first = Chunks.refused(store.load(CHUNK_A));
      readMoreRegionsThanTheStoreKeepsOpen(store);
      // An external repair: if the file were read again, the chunk would now be present.
      RegionFixtures.writeRg1(folder, 2);

      assertThat(Chunks.refused(store.load(CHUNK_A))).isEqualTo(first);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"compression_127", "bad_length", "duplicate_section", "missing_mcc"})
  void saveAtARefusedPositionStillFailsAfterItsRegionWasEvicted(String variant) throws IOException {
    // spec: AR-R28 (WRITE_ON_REFUSED_POSITION: nothing is written, the region file keeps its
    // content byte for byte)
    byte[] before = RegionFixtures.writeVariant(folder, variant);
    List<String> namesBefore = RegionFixtures.names(folder);

    try (AnvilChunkStore store = open()) {
      Chunks.refused(store.load(CHUNK_A));
      readMoreRegionsThanTheStoreKeepsOpen(store);

      Failures.expect(
          WriteOnRefusedPositionException.class, () -> store.save(chunkAt(CHUNK_A), 777L));
    }

    assertThat(RegionFixtures.read(RegionFixtures.regionFile(folder))).isEqualTo(before);
    assertThat(RegionFixtures.names(folder)).isEqualTo(namesBefore);
  }

  @Test
  void saveFailsAfterEvictionAndAReloadKeepsTheRepairedFileUntouched() throws IOException {
    // spec: AR-R28 (the bytes of a refused position are never touched by the store)
    RegionFixtures.writeVariant(folder, "compression_127");

    byte[] repaired;
    try (AnvilChunkStore store = open()) {
      Chunks.refused(store.load(CHUNK_A));
      readMoreRegionsThanTheStoreKeepsOpen(store);
      repaired = RegionFixtures.writeRg1(folder, 2);

      Failures.expect(
          WriteOnRefusedPositionException.class, () -> store.save(chunkAt(CHUNK_A), 5L));
      Chunks.refused(store.load(CHUNK_A));
    }

    assertThat(RegionFixtures.read(RegionFixtures.regionFile(folder))).isEqualTo(repaired);
  }

  @Test
  void closingTheStoreStillLiftsTheMarkAfterAnEviction() throws IOException {
    // spec: AR-R28 (the mark goes with the closing of the store; an external repair is seen on
    // reopening), here with an eviction in between
    RegionFixtures.writeVariant(folder, "compression_127");
    try (AnvilChunkStore store = open()) {
      Chunks.refused(store.load(CHUNK_A));
      readMoreRegionsThanTheStoreKeepsOpen(store);
    }
    RegionFixtures.writeRg1(folder, 2);

    try (AnvilChunkStore store = open()) {
      Chunks.present(store.load(CHUNK_A));
      store.save(chunkAt(CHUNK_A), 6L);
    }
  }
}

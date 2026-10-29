package dev.shoko.minecraft.world.storage;

import static dev.shoko.minecraft.world.support.RegionFixtures.CHUNK_A;
import static dev.shoko.minecraft.world.support.RegionFixtures.CHUNK_B;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.world.AbsentCause;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.RefusedCause;
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

/** AR-R26 to AR-R28 and decision D7: a refused position is marked and cannot be written. */
class AnvilChunkStoreRefusedPositionTest {

  @TempDir Path folder;

  private final BlockStateRegistry registry = Blocks.registry();

  private AnvilChunkStore open() {
    return AnvilChunkStore.open(folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB);
  }

  private Chunk chunkAt(ChunkPos pos) {
    return Chunks.empty(registry, pos, Chunks.OVERWORLD);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "compression_127",
        "bad_length",
        "duplicate_section",
        "missing_mcc",
        "no_sections_key"
      })
  void saveAtARefusedPositionFailsAndWritesNothing(String variant) throws IOException {
    // spec: AR-R28 (WRITE_ON_REFUSED_POSITION: no record, no timestamp, no .mcc, file unchanged)
    byte[] before = RegionFixtures.writeVariant(folder, variant);
    List<String> namesBefore = RegionFixtures.names(folder);

    try (AnvilChunkStore store = open()) {
      Chunks.refused(store.load(CHUNK_A));
      WriteOnRefusedPositionException failure =
          Failures.expect(
              WriteOnRefusedPositionException.class, () -> store.save(chunkAt(CHUNK_A), 777L));
      assertThat(failure).isInstanceOf(IllegalStateException.class);
      assertThat(failure.getMessage()).contains("r.-1.-1.mca").contains("1023");
    }

    assertThat(RegionFixtures.read(RegionFixtures.regionFile(folder))).isEqualTo(before);
    assertThat(RegionFixtures.names(folder)).isEqualTo(namesBefore);
  }

  @Test
  void externalFileOfARefusedChunkIsNeitherWrittenNorDeleted() throws IOException {
    // spec: AR-R28 (the .mcc is part of the bytes that stay intact), AR-R29 (TOO_LARGE refuses)
    byte[] before = RegionFixtures.writeVariant(folder, "external");
    byte[] mccBefore = RegionFixtures.read(folder.resolve("c.-1.-1.mcc"));

    try (AnvilChunkStore store =
        AnvilChunkStore.open(
            folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB, 100)) {
      assertThat(Chunks.refused(store.load(CHUNK_A)).cause()).isEqualTo(RefusedCause.TOO_LARGE);
      Failures.expect(
          WriteOnRefusedPositionException.class, () -> store.save(chunkAt(CHUNK_A), 1L));
    }

    assertThat(RegionFixtures.read(RegionFixtures.regionFile(folder))).isEqualTo(before);
    assertThat(RegionFixtures.read(folder.resolve("c.-1.-1.mcc"))).isEqualTo(mccBefore);
  }

  @Test
  void laterLoadsAnswerTheSameRefusalWithoutReadingTheFileAgain() throws IOException {
    // spec: AR-R28 (a marked position keeps answering the same refusal, the file is not read again)
    RegionFixtures.writeVariant(folder, "compression_127");

    try (AnvilChunkStore store = open()) {
      ReadResult.Refused<Chunk> first = Chunks.refused(store.load(CHUNK_A));
      RegionFixtures.writeRg1(folder, 2);
      ReadResult.Refused<Chunk> second = Chunks.refused(store.load(CHUNK_A));

      assertThat(second).isEqualTo(first);
      assertThat(second.cause()).isEqualTo(RefusedCause.UNSUPPORTED_COMPRESSION);
    }
  }

  @Test
  void markIsLiftedByClosingTheStoreAndARepairIsSeenOnReopening() throws IOException {
    // spec: AR-R28 (the mark goes with the closing of the store: an external repair is seen on
    // reopening)
    RegionFixtures.writeVariant(folder, "bad_length");
    try (AnvilChunkStore store = open()) {
      Chunks.refused(store.load(CHUNK_A));
    }

    RegionFixtures.writeRg1(folder, 2);

    try (AnvilChunkStore store = open()) {
      Chunks.present(store.load(CHUNK_A));
      store.save(chunkAt(CHUNK_A), 5L);
    }
  }

  @Test
  void anotherPositionOfTheSameFileIsNotMarked() throws IOException {
    // spec: AR-R28 (the mark is for a position)
    RegionFixtures.writeVariant(folder, "compression_127");

    try (AnvilChunkStore store = open()) {
      Chunks.refused(store.load(CHUNK_A));
      store.save(chunkAt(CHUNK_B), 9L);
      assertThat(Chunks.present(store.load(CHUNK_B)).value().pos()).isEqualTo(CHUNK_B);
      Failures.expect(
          WriteOnRefusedPositionException.class, () -> store.save(chunkAt(CHUNK_A), 1L));
    }
  }

  @Test
  void absentPositionIsNeverMarkedAndCanBeSaved() throws IOException {
    // spec: AR-R28 (an ABSENT position is not marked: its generation and writing are normal)
    try (AnvilChunkStore store = open()) {
      assertThat(Chunks.absent(store.load(CHUNK_A)).cause()).isEqualTo(AbsentCause.NO_REGION_FILE);

      store.save(chunkAt(CHUNK_A), 3L);

      assertThat(Chunks.present(store.load(CHUNK_A)).value().pos()).isEqualTo(CHUNK_A);
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"wrong_pos", "bad_offset", "invalid_nbt", "corrupt_zlib", "root_not_compound"})
  void absentBecauseOfADamagedRecordIsOverwrittenByTheSave(String variant) throws IOException {
    // spec: AR "Entrees / sorties" (absent: the next save writes the new chunk at this index)
    RegionFixtures.writeVariant(folder, variant);

    try (AnvilChunkStore store = open()) {
      Chunks.absent(store.load(CHUNK_A));
      store.save(chunkAt(CHUNK_A), 3L);

      assertThat(Chunks.present(store.load(CHUNK_A)).value().pos()).isEqualTo(CHUNK_A);
    }
  }

  @Test
  void positionNeverLoadedIsNotChecked() throws IOException {
    // spec: AR-R28 (limit: a write to a position never read during the life of the store is not
    // checked)
    RegionFixtures.writeVariant(folder, "compression_127");

    try (AnvilChunkStore store = open()) {
      store.save(chunkAt(CHUNK_A), 3L);
    }

    try (AnvilChunkStore store = open()) {
      assertThat(Chunks.present(store.load(CHUNK_A)).value().pos()).isEqualTo(CHUNK_A);
    }
  }

  @Test
  void positionsAreMarkedIndependentlyInTwoRegions() throws IOException {
    // spec: AR-R28 (marks are per position, whatever the region)
    RegionFixtures.writeVariant(folder, "compression_127");
    ChunkPos elsewhere = new ChunkPos(5, 5);
    try (AnvilChunkStore store = open()) {
      Chunks.refused(store.load(CHUNK_A));

      store.save(chunkAt(elsewhere), 1L);

      assertThat(Chunks.present(store.load(elsewhere)).value().pos()).isEqualTo(elsewhere);
    }
  }

  @Test
  void ioFailureSetsNoMarkAndGeneratesNothing() throws IOException {
    // spec: AR-R27 (no mark is set by a failure of the file system)
    java.nio.file.Files.createDirectories(RegionFixtures.regionFile(folder));
    try (AnvilChunkStore store = open()) {
      Failures.expect(IOException.class, () -> store.load(CHUNK_A));
      Failures.expect(IOException.class, () -> store.load(CHUNK_A));
    }
  }
}

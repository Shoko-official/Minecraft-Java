package dev.shoko.minecraft.world.storage;

import static dev.shoko.minecraft.world.support.RegionFixtures.CHUNK_A;
import static dev.shoko.minecraft.world.support.RegionFixtures.CHUNK_B;
import static dev.shoko.minecraft.world.support.RegionFixtures.INDEX_A;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.RegionPos;
import dev.shoko.minecraft.world.AbsentCause;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.ReadWarning;
import dev.shoko.minecraft.world.RefusedCause;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.Hex;
import dev.shoko.minecraft.world.support.RegionFixtures;
import dev.shoko.minecraft.world.support.Regions;
import dev.shoko.minecraft.world.support.Regions.Blob;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** AR-R4 to AR-R12, AR-R14, AR-R19, AR-R20, AR-R26: reading records of a region file. */
class RegionFileReadTest {

  @TempDir Path folder;

  private ReadResult<byte[]> read(ChunkPos chunk) throws IOException {
    try (RegionFile file = RegionFixtures.open(folder)) {
      return file.read(chunk);
    }
  }

  private static void assertInvalidLocation(ReadResult<byte[]> result) {
    ReadResult.Absent<byte[]> absent = Chunks.absent(result);
    assertThat(absent.cause()).isEqualTo(AbsentCause.INVALID_LOCATION);
    assertThat(absent.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.INVALID_LOCATION);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4})
  void chunkAIsReadWhateverItsCompressionType(int type) throws IOException {
    // spec: AR-R13, RG1, RG2 (gzip, zlib, none, lz4)
    RegionFixtures.writeRg1(folder, type);

    ReadResult.Present<byte[]> result = Chunks.present(read(CHUNK_A));

    assertThat(result.value()).isEqualTo(RegionFixtures.nbtOfA());
    assertThat(result.warnings()).isEmpty();
  }

  @Test
  void chunkBStoredWithoutCompressionSpansSeveralSectorsOfNoOneElse() throws IOException {
    // spec: RG1 chunk B (-32, -32), index 0, type 3, L = 3916
    RegionFixtures.writeRg1(folder, 2);

    assertThat(Chunks.present(read(CHUNK_B)).value()).isEqualTo(RegionFixtures.nbtOfB());
  }

  @Test
  void recordOfSeveralSectorsIsReadWhole() throws IOException {
    // spec: AR-R10, AR-R12, RG2 none (chunk A takes two sectors, L = 6000)
    byte[] file = RegionFixtures.writeRg1(folder, 3);

    assertThat(Regions.countOf(file, INDEX_A)).isEqualTo(2);
    assertThat(Regions.lengthOf(file, INDEX_A)).isEqualTo(6000);
    assertThat(Chunks.present(read(CHUNK_A)).value()).isEqualTo(RegionFixtures.nbtOfA());
    assertThat(Chunks.present(read(CHUNK_B)).value()).isEqualTo(RegionFixtures.nbtOfB());
  }

  @Test
  void typesCanBeMixedInOneFile() throws IOException {
    // spec: AR-R14 (the reader accepts 1, 2, 3 and 4 in any file)
    byte[] nbt = RegionFixtures.nbtOfB();
    List<Blob> blobs =
        List.of(
            new Blob(1023, 1, Regions.gzip(nbt), 1L),
            new Blob(0, 2, Regions.zlib(nbt), 2L),
            new Blob(5, 3, nbt, 3L),
            new Blob(700, 4, Regions.compress(4, nbt), 4L));
    RegionFixtures.write(RegionFixtures.regionFile(folder), Regions.file(blobs));

    for (int index : new int[] {1023, 0, 5, 700}) {
      assertThat(Chunks.present(read(RegionFixtures.chunkAtIndex(index))).value())
          .as("index " + index)
          .isEqualTo(nbt);
    }
  }

  @Test
  void headerIndexIsXPlus32TimesZOfTheLowFiveBits() throws IOException {
    // spec: AR-R5, AR-R6 (chunk (30, -3) is index 958 of r.0.-1.mca)
    byte[] nbt = RegionFixtures.nbtOfB();
    byte[] file = Regions.file(List.of(new Blob(958, 3, nbt, 9L)));
    RegionFixtures.write(folder.resolve("r.0.-1.mca"), file);

    try (RegionFile region = RegionFile.open(folder, new RegionPos(0, -1))) {
      assertThat(Chunks.present(region.read(new ChunkPos(30, -3))).value()).isEqualTo(nbt);
      assertThat(Chunks.absent(region.read(new ChunkPos(29, -3))).cause())
          .isEqualTo(AbsentCause.EMPTY_SLOT);
    }
  }

  @Test
  void missingFileMeansEveryChunkIsAbsentWithoutWarning() throws IOException {
    // spec: AR-R19 (NO_REGION_FILE, normal), AR-R26
    ReadResult.Absent<byte[]> result = Chunks.absent(read(CHUNK_A));

    assertThat(result.cause()).isEqualTo(AbsentCause.NO_REGION_FILE);
    assertThat(result.warnings()).isEmpty();
  }

  @Test
  void readingNeverCreatesOrChangesAFile() throws IOException {
    // spec: AR "Entrees / sorties" (the reader never writes during a read)
    read(CHUNK_A);
    assertThat(RegionFixtures.names(folder)).isEmpty();
    byte[] file = RegionFixtures.writeRg1(folder, 2);

    read(CHUNK_A);
    read(CHUNK_B);
    read(RegionFixtures.chunkAtIndex(17));

    assertThat(RegionFixtures.read(RegionFixtures.regionFile(folder))).isEqualTo(file);
    assertThat(RegionFixtures.names(folder)).containsExactly("r.-1.-1.mca");
  }

  @Test
  void zeroLocationIsAnEmptySlotWithoutWarning() throws IOException {
    // spec: AR-R8, AR-R19 (EMPTY_SLOT), "Cas limites" (8192 bytes of zeros: valid, no chunk)
    RegionFixtures.write(RegionFixtures.regionFile(folder), new byte[8192]);

    for (int index : new int[] {0, 1, 500, 1023}) {
      ReadResult.Absent<byte[]> result = Chunks.absent(read(RegionFixtures.chunkAtIndex(index)));
      assertThat(result.cause()).isEqualTo(AbsentCause.EMPTY_SLOT);
      assertThat(result.warnings()).isEmpty();
    }
  }

  @Test
  void otherSlotsOfAFileWithTwoChunksAreEmpty() throws IOException {
    // spec: RG1 (non-zero location entries are 0 and 1023)
    RegionFixtures.writeRg1(folder, 2);

    assertThat(Chunks.absent(read(RegionFixtures.chunkAtIndex(1))).cause())
        .isEqualTo(AbsentCause.EMPTY_SLOT);
    assertThat(Chunks.absent(read(RegionFixtures.chunkAtIndex(1022))).cause())
        .isEqualTo(AbsentCause.EMPTY_SLOT);
  }

  @Test
  void chunkOfAnotherRegionAndNegativeLimitAreRejected() throws IOException {
    // spec: RegionFile.read contract (IllegalArgumentException)
    RegionFixtures.writeRg1(folder, 2);

    try (RegionFile file = RegionFixtures.open(folder)) {
      Failures.expect(IllegalArgumentException.class, () -> file.read(new ChunkPos(0, 0)));
      Failures.expect(IllegalArgumentException.class, () -> file.read(new ChunkPos(-33, -1)));
      Failures.expect(IllegalArgumentException.class, () -> file.read(CHUNK_A, -1L));
    }
  }

  @Test
  void locationInsideTheHeaderIsInvalid() throws IOException {
    // spec: AR-R20, RG3 bad_offset (sector 1), and offset 0 with a count
    byte[] file = Regions.rg1(2, RegionFixtures.nbtOfA(), RegionFixtures.nbtOfB());
    Regions.putInt(file, 4 * INDEX_A, (1 << 8) | 1);
    RegionFixtures.write(RegionFixtures.regionFile(folder), file);
    assertInvalidLocation(read(CHUNK_A));

    Regions.putInt(file, 4 * INDEX_A, 1);
    RegionFixtures.write(RegionFixtures.regionFile(folder), file);
    assertInvalidLocation(read(CHUNK_A));
  }

  @Test
  void zeroCountWithAnOffsetIsInvalid() throws IOException {
    // spec: AR-R20 (count = 0 with a non-zero offset)
    byte[] file = Regions.rg1(2, RegionFixtures.nbtOfA(), RegionFixtures.nbtOfB());
    Regions.putInt(file, 4 * INDEX_A, 2 << 8);
    RegionFixtures.write(RegionFixtures.regionFile(folder), file);

    assertInvalidLocation(read(CHUNK_A));
  }

  @Test
  void sectorsBeyondTheEndOfTheFileAreInvalid() throws IOException {
    // spec: AR-R20, RG3 offset_beyond (sector 200 in a file of 4 sectors)
    byte[] file = Regions.rg1(2, RegionFixtures.nbtOfA(), RegionFixtures.nbtOfB());
    Regions.putInt(file, 4 * INDEX_A, (200 << 8) | 1);
    RegionFixtures.write(RegionFixtures.regionFile(folder), file);
    assertInvalidLocation(read(CHUNK_A));

    Regions.putInt(file, 4 * INDEX_A, (3 << 8) | 3);
    RegionFixtures.write(RegionFixtures.regionFile(folder), file);
    assertInvalidLocation(read(CHUNK_A));
  }

  @Test
  void fileCutInTheMiddleOfAChunkIsInvalidForThatChunkOnly() throws IOException {
    // spec: AR-R20, "Cas limites" (a file shorter than its locations is invalid content)
    byte[] file = Regions.rg1(2, RegionFixtures.nbtOfA(), RegionFixtures.nbtOfB());
    RegionFixtures.write(
        RegionFixtures.regionFile(folder), java.util.Arrays.copyOf(file, file.length - 1000));

    assertInvalidLocation(read(CHUNK_B));
    assertThat(Chunks.present(read(CHUNK_A)).value()).isEqualTo(RegionFixtures.nbtOfA());
  }

  @Test
  void nonPositiveLengthIsInvalid() throws IOException {
    // spec: AR-R20 (L <= 0)
    for (int length : new int[] {0, -1, Integer.MIN_VALUE}) {
      byte[] file = Regions.rg1(2, RegionFixtures.nbtOfA(), RegionFixtures.nbtOfB());
      Regions.putInt(file, Regions.sectorOf(file, INDEX_A) * 4096, length);
      RegionFixtures.write(RegionFixtures.regionFile(folder), file);

      assertInvalidLocation(read(CHUNK_A));
    }
  }

  @Test
  void lengthThatOverflowsItsSectorsIsInvalid() throws IOException {
    // spec: AR-R20 (L + 4 > count * 4096), here 4093 + 4 = 4097 for one sector
    byte[] file = Regions.rg1(2, RegionFixtures.nbtOfA(), RegionFixtures.nbtOfB());
    Regions.putInt(file, Regions.sectorOf(file, INDEX_A) * 4096, 4093);
    RegionFixtures.write(RegionFixtures.regionFile(folder), file);
    assertInvalidLocation(read(CHUNK_A));

    Regions.putInt(file, Regions.sectorOf(file, INDEX_A) * 4096, Integer.MAX_VALUE);
    RegionFixtures.write(RegionFixtures.regionFile(folder), file);
    assertInvalidLocation(read(CHUNK_A));
  }

  @Test
  void recordThatFillsItsSectorExactlyIsValid() throws IOException {
    // spec: AR-R10, AR-R20 (L + 4 = count * 4096 is the last valid length: 4092)
    byte[] raw = new byte[4091];
    byte[] file = Regions.file(List.of(new Blob(0, 3, raw, 1L)));
    RegionFixtures.write(RegionFixtures.regionFile(folder), file);

    assertThat(Regions.lengthOf(file, 0)).isEqualTo(4092);
    assertThat(Chunks.present(read(CHUNK_B)).value()).isEqualTo(raw);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 5, 6, 100, 126, 127})
  void unsupportedCompressionRefusesTheChunkAndNamesFileAndIndex(int type) throws IOException {
    // spec: AR-R14 (127 or unknown: UNSUPPORTED_COMPRESSION), AR-R26 (message names file and index)
    byte[] file = Regions.rg1(2, RegionFixtures.nbtOfA(), RegionFixtures.nbtOfB());
    file[Regions.sectorOf(file, INDEX_A) * 4096 + 4] = (byte) type;
    RegionFixtures.write(RegionFixtures.regionFile(folder), file);

    ReadResult.Refused<byte[]> result = Chunks.refused(read(CHUNK_A));

    assertThat(result.cause()).isEqualTo(RefusedCause.UNSUPPORTED_COMPRESSION);
    assertThat(result.message()).contains("r.-1.-1.mca").contains("1023");
  }

  @Test
  void customCompressionOfVectorCompression127IsRefused() throws IOException {
    // spec: RG4 compression_127 (type 127, a name of 8 bytes "x:custom", then a zlib stream)
    byte[] zlib = Regions.zlib(RegionFixtures.nbtOfA());
    byte[] payload = Hex.concat(Hex.bytes("00 08 78 3a 63 75 73 74 6f 6d"), zlib);
    RegionFixtures.write(
        RegionFixtures.regionFile(folder),
        Regions.file(List.of(new Blob(INDEX_A, 127, payload, 1L))));

    assertThat(Chunks.refused(read(CHUNK_A)).cause())
        .isEqualTo(RefusedCause.UNSUPPORTED_COMPRESSION);
  }

  @Test
  void aRefusalLeavesTheFileUntouched() throws IOException {
    // spec: AR-R26 (the bytes of a refused record are preserved)
    byte[] file = Regions.rg1(2, RegionFixtures.nbtOfA(), RegionFixtures.nbtOfB());
    file[Regions.sectorOf(file, INDEX_A) * 4096 + 4] = 127;
    RegionFixtures.write(RegionFixtures.regionFile(folder), file);

    read(CHUNK_A);

    assertThat(RegionFixtures.read(RegionFixtures.regionFile(folder))).isEqualTo(file);
  }

  @Test
  void aDirectoryInPlaceOfTheRegionFileIsAnInputOutputFailure() {
    // spec: AR-R27 (a file system error is none of the three issues: IOException)
    try {
      Files.createDirectories(RegionFixtures.regionFile(folder));
    } catch (IOException e) {
      throw new java.io.UncheckedIOException(e);
    }

    Failures.expect(
        IOException.class,
        () -> {
          try (RegionFile file = RegionFixtures.open(folder)) {
            file.read(CHUNK_A);
          }
        });
  }
}

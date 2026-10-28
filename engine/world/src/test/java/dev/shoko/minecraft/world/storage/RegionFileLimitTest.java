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
import dev.shoko.minecraft.world.support.RegionFixtures;
import dev.shoko.minecraft.world.support.Regions;
import dev.shoko.minecraft.world.support.Regions.Blob;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** AR-R15, AR-R17, AR-R18, AR-R20, AR-R29, AR-R30 a: corrupt records, limits, external chunks. */
class RegionFileLimitTest {

  private static final long NBT_A = 5999;
  @TempDir Path folder;

  private ReadResult<byte[]> read(ChunkPos chunk, long limit) throws IOException {
    try (RegionFile file = RegionFixtures.open(folder)) {
      return file.read(chunk, limit);
    }
  }

  private ReadResult<byte[]> read(ChunkPos chunk) throws IOException {
    try (RegionFile file = RegionFixtures.open(folder)) {
      return file.read(chunk);
    }
  }

  private void putRecord(int type, byte[] payload) {
    RegionFixtures.write(
        RegionFixtures.regionFile(folder),
        Regions.file(List.of(new Blob(INDEX_A, type, payload, 1L))));
  }

  private static void assertUnreadable(ReadResult<byte[]> result) {
    ReadResult.Absent<byte[]> absent = Chunks.absent(result);
    assertThat(absent.cause()).isEqualTo(AbsentCause.UNREADABLE_RECORD);
    assertThat(absent.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.UNREADABLE_RECORD);
  }

  private static void assertTooLarge(ReadResult<byte[]> result) {
    assertThat(Chunks.refused(result).cause()).isEqualTo(RefusedCause.TOO_LARGE);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4})
  void limitEqualToTheOutputPassesAndOneLessRefuses(int type) throws IOException {
    // spec: AR-R29, LIM1 (limit 5999 present, 5998 TOO_LARGE, 0 TOO_LARGE, default present)
    RegionFixtures.writeRg1(folder, type);

    assertThat(Chunks.present(read(CHUNK_A)).value()).hasSize((int) NBT_A);
    assertThat(Chunks.present(read(CHUNK_A, RegionCompression.DEFAULT_DECOMPRESSION_LIMIT)).value())
        .hasSize((int) NBT_A);
    assertThat(Chunks.present(read(CHUNK_A, NBT_A)).value()).isEqualTo(RegionFixtures.nbtOfA());
    assertTooLarge(read(CHUNK_A, NBT_A - 1));
    assertTooLarge(read(CHUNK_A, 0));
  }

  @Test
  void rawRecordIsComparedWithTheLimitBeforeAnyRead() throws IOException {
    // spec: AR-R29, LIM1 (chunk B, type 3, L - 1 = 3915: limit 3914 refuses, 3915 passes)
    RegionFixtures.writeRg1(folder, 2);

    assertTooLarge(read(CHUNK_B, 3914));
    assertThat(Chunks.present(read(CHUNK_B, 3915)).value()).hasSize(3915);
  }

  @Test
  void zlibBombIsRefusedWithASmallLimitAndPassesWithTheDefault() throws IOException {
    // spec: AR-R29, RG4 bomb (4 MiB of zeros: TOO_LARGE at 1 MiB; default limit decompresses)
    byte[] bomb = new byte[4 << 20];
    putRecord(2, Regions.zlib(bomb));

    assertThat(RegionFixtures.read(RegionFixtures.regionFile(folder)).length).isLessThan(20_000);
    assertTooLarge(read(CHUNK_A, 1 << 20));
    assertThat(Chunks.present(read(CHUNK_A)).value()).hasSize(4 << 20);
  }

  @Test
  void gzipBombIsRefusedWithASmallLimit() throws IOException {
    // spec: AR-R29 (gzip counted like the others)
    putRecord(1, Regions.gzip(new byte[4 << 20]));

    assertTooLarge(read(CHUNK_A, 1 << 20));
  }

  @Test
  void lz4StreamsAreCountedAllBlocksTogether() throws IOException {
    // spec: AR-R29 (LZ4, all blocks together): three blocks of 65536 bytes
    byte[] stream = Regions.compress(4, new byte[3 * 65_536]);
    putRecord(4, stream);

    assertThat(Chunks.present(read(CHUNK_A, 3 * 65_536L)).value()).hasSize(3 * 65_536);
    assertTooLarge(read(CHUNK_A, 3 * 65_536L - 1));
    assertTooLarge(read(CHUNK_A, 65_536L));
  }

  @Test
  void corruptionBeforeTheLimitIsCorruptionAndTheLimitReachedFirstIsTheLimit() throws IOException {
    // spec: AR-R29 (the first event in stream order decides)
    byte[] data = new byte[10_000];
    new SplittableRandom(7L).nextBytes(data);
    byte[] zlib = Regions.zlib(data);
    zlib[zlib.length - 1] ^= 0x55;
    putRecord(2, zlib);

    assertTooLarge(read(CHUNK_A, 9_999));
    assertUnreadable(read(CHUNK_A, 10_000));
    assertUnreadable(read(CHUNK_A));
  }

  @ParameterizedTest
  @ValueSource(strings = {"flipped", "trailer", "truncated", "empty", "garbage"})
  void zlibThatDoesNotDecodeIsAnUnreadableRecord(String damage) throws IOException {
    // spec: AR-R20, RG4 corrupt_zlib (decoding error, wrong trailer, truncation)
    byte[] zlib = Regions.zlib(RegionFixtures.nbtOfA());
    byte[] bad =
        switch (damage) {
          case "flipped" -> flip(zlib, zlib.length / 2);
          case "trailer" -> flip(zlib, zlib.length - 1);
          case "truncated" -> Arrays.copyOf(zlib, zlib.length / 2);
          case "empty" -> new byte[0];
          default -> garbage(300);
        };
    putRecord(2, bad);

    assertUnreadable(read(CHUNK_A));
  }

  @ParameterizedTest
  @ValueSource(strings = {"crc", "truncated", "empty"})
  void gzipThatDoesNotDecodeIsAnUnreadableRecord(String damage) throws IOException {
    // spec: AR-R20 (CRC-32 and ISIZE trailer of RFC 1952)
    byte[] gzip = Regions.gzip(RegionFixtures.nbtOfA());
    byte[] bad =
        switch (damage) {
          case "crc" -> flip(gzip, gzip.length - 6);
          case "truncated" -> Arrays.copyOf(gzip, gzip.length - 20);
          default -> new byte[0];
        };
    putRecord(1, bad);

    assertUnreadable(read(CHUNK_A));
  }

  @ParameterizedTest
  @ValueSource(strings = {"checksum", "no-end-mark", "truncated", "empty", "magic"})
  void lz4ThatDoesNotDecodeIsAnUnreadableRecord(String damage) throws IOException {
    // spec: AR-R15, AR-R20 (checksum, end mark, truncation of the LZ4Block stream)
    byte[] lz4 = Regions.compress(4, RegionFixtures.nbtOfA());
    byte[] bad =
        switch (damage) {
          case "checksum" -> flip(lz4, 17);
          case "no-end-mark" -> Arrays.copyOf(lz4, lz4.length - 21);
          case "truncated" -> Arrays.copyOf(lz4, 2000);
          case "magic" -> flip(lz4, 0);
          default -> new byte[0];
        };
    putRecord(4, bad);

    assertUnreadable(read(CHUNK_A));
  }

  @Test
  void externalChunksOfEveryTypeAreReadFromTheMccFile() throws IOException {
    // spec: AR-R17, RG2 external (record 00 00 00 01 82 and the stream alone in c.-1.-1.mcc)
    for (int type = 1; type <= 4; type++) {
      putRecord(type + 128, new byte[0]);
      RegionFixtures.write(
          folder.resolve("c.-1.-1.mcc"), Regions.compress(type, RegionFixtures.nbtOfA()));

      ReadResult.Present<byte[]> result = Chunks.present(read(CHUNK_A));

      assertThat(result.value()).as("type " + type).isEqualTo(RegionFixtures.nbtOfA());
      assertThat(result.warnings()).isEmpty();
    }
  }

  @Test
  void provisionalExternalRecordIsFiveBytesAndTheMccHoldsOnlyTheStream() throws IOException {
    // spec: AR-R17 (provisional: the .mcc has no length and no type byte), RG2 external
    putRecord(2 + 128, new byte[0]);
    RegionFixtures.write(folder.resolve("c.-1.-1.mcc"), Regions.zlib(RegionFixtures.nbtOfA()));

    byte[] file = RegionFixtures.read(RegionFixtures.regionFile(folder));

    assertThat(Regions.lengthOf(file, INDEX_A)).isEqualTo(1);
    assertThat(Regions.typeOf(file, INDEX_A)).isEqualTo(0x82);
    assertThat(Chunks.present(read(CHUNK_A)).value()).isEqualTo(RegionFixtures.nbtOfA());
  }

  @Test
  void externalFileIsNamedByTheAbsoluteChunkCoordinates() throws IOException {
    // spec: AR-R17 (c.<cx>.<cz>.mcc with absolute coordinates), AR-R6 (chunk (1500, -600) is index
    // 284)
    ChunkPos far = new ChunkPos(1500, -600);
    byte[] file = Regions.file(List.of(new Blob(284, 2 + 128, new byte[0], 5L)));
    RegionFixtures.write(folder.resolve("r.46.-19.mca"), file);
    RegionFixtures.write(folder.resolve("c.1500.-600.mcc"), Regions.zlib(RegionFixtures.nbtOfB()));

    try (RegionFile region = RegionFile.open(folder, new RegionPos(46, -19))) {
      assertThat(Chunks.present(region.read(far)).value()).isEqualTo(RegionFixtures.nbtOfB());
    }
  }

  @Test
  void provisionalMissingExternalFileRefusesTheChunk() throws IOException {
    // spec: AR-R18, RG3 missing_mcc
    putRecord(2 + 128, new byte[0]);

    ReadResult.Refused<byte[]> result = Chunks.refused(read(CHUNK_A));

    assertThat(result.cause()).isEqualTo(RefusedCause.MISSING_EXTERNAL_FILE);
    assertThat(result.message()).contains("1023");
  }

  @Test
  void externalFileIsSubjectToTheLimit() throws IOException {
    // spec: AR-R29 (for an external chunk, the stream of the .mcc is bounded; raw: its size is
    // compared)
    putRecord(2 + 128, new byte[0]);
    RegionFixtures.write(folder.resolve("c.-1.-1.mcc"), Regions.zlib(RegionFixtures.nbtOfA()));
    assertThat(Chunks.present(read(CHUNK_A, NBT_A)).value()).hasSize((int) NBT_A);
    assertTooLarge(read(CHUNK_A, NBT_A - 1));

    putRecord(3 + 128, new byte[0]);
    RegionFixtures.write(folder.resolve("c.-1.-1.mcc"), RegionFixtures.nbtOfA());
    assertThat(Chunks.present(read(CHUNK_A, NBT_A)).value()).hasSize((int) NBT_A);
    assertTooLarge(read(CHUNK_A, NBT_A - 1));
  }

  @Test
  void corruptExternalStreamIsAnUnreadableRecord() throws IOException {
    // spec: AR-R20 (a stream that does not decompress), AR-R17
    putRecord(2 + 128, new byte[0]);
    RegionFixtures.write(folder.resolve("c.-1.-1.mcc"), garbage(500));

    assertUnreadable(read(CHUNK_A));
  }

  private static byte[] flip(byte[] data, int at) {
    byte[] copy = data.clone();
    copy[at] ^= (byte) 0xFF;
    return copy;
  }

  private static byte[] garbage(int length) {
    byte[] bytes = new byte[length];
    new SplittableRandom(99L).nextBytes(bytes);
    bytes[0] = (byte) 0xAB;
    return bytes;
  }
}

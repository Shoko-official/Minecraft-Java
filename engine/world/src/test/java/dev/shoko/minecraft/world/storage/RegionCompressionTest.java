package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.Regions;
import java.util.Arrays;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/** AR-R13, AR-R14, AR-R29: the compression types and their bounded decompression. */
class RegionCompressionTest {

  private static byte[] randomBytes(int length, long seed) {
    byte[] bytes = new byte[length];
    new SplittableRandom(seed).nextBytes(bytes);
    return bytes;
  }

  /** Seeded noise that cannot start any of the three formats. */
  private static byte[] garbage() {
    byte[] bytes = randomBytes(200, 6L);
    bytes[0] = (byte) 0xAB;
    return bytes;
  }

  private static byte[] payloadOf(RegionCompression compression, byte[] data) {
    return Regions.compress(compression.id(), data);
  }

  @ParameterizedTest
  @CsvSource({"GZIP,1", "ZLIB,2", "NONE,3", "LZ4,4"})
  void typesHaveTheBytesOfTheSpec(RegionCompression compression, int id) {
    // spec: AR-R13 (1 gzip, 2 zlib, 3 none, 4 LZ4)
    assertThat(compression.id()).isEqualTo(id);
    assertThat(RegionCompression.byId(id)).contains(compression);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 5, 6, 100, 126, 127, 128, 129, 130, 131, 132, 255, -1, 1000})
  void customAndUnknownTypesAreNotListed(int id) {
    // spec: AR-R14 (127 or unknown: not a compression the reader accepts; 128+ is removed upstream)
    assertThat(RegionCompression.byId(id)).isEmpty();
  }

  @Test
  void defaultLimitIsTwoToThePower27() {
    // spec: AR-R29 (134 217 728 bytes)
    assertThat(RegionCompression.DEFAULT_DECOMPRESSION_LIMIT).isEqualTo(134_217_728L);
    assertThat(RegionCompression.DEFAULT_DECOMPRESSION_LIMIT).isEqualTo(1L << 27);
  }

  @ParameterizedTest
  @EnumSource(RegionCompression.class)
  void everyTypeDecompressesWhatItCompresses(RegionCompression compression) throws Exception {
    // spec: AR-R13
    byte[] noise = randomBytes(20_000, 1L);
    byte[] zeros = new byte[100_000];
    byte[] mixed = Arrays.copyOf(noise, 20_000);
    Arrays.fill(mixed, 0, 10_000, (byte) 9);

    for (byte[] data : new byte[][] {noise, zeros, mixed, new byte[0], new byte[] {1}}) {
      assertThat(compression.decompress(payloadOf(compression, data), 1L << 30))
          .as(compression + " " + data.length)
          .isEqualTo(data);
    }
  }

  @ParameterizedTest
  @EnumSource(RegionCompression.class)
  void outputOfExactlyTheLimitPassesAndOneByteMoreIsExceeded(RegionCompression compression)
      throws Exception {
    // spec: AR-R29, LIM1 (limit = size passes, limit = size - 1 fails)
    byte[] data = randomBytes(5999, 2L);
    byte[] payload = payloadOf(compression, data);

    assertThat(compression.decompress(payload, 5999)).isEqualTo(data);
    OutputLimitExceededException failure =
        Failures.expect(
            OutputLimitExceededException.class, () -> compression.decompress(payload, 5998));
    assertThat(failure.limit()).isEqualTo(5998);
    Failures.expect(OutputLimitExceededException.class, () -> compression.decompress(payload, 0));
  }

  @ParameterizedTest
  @EnumSource(RegionCompression.class)
  void emptyOutputPassesWithALimitOfZero(RegionCompression compression) throws Exception {
    // spec: AR-R29 (a total equal to the limit passes: here 0)
    assertThat(compression.decompress(payloadOf(compression, new byte[0]), 0)).isEmpty();
  }

  @Test
  void rawDataIsComparedWithTheLimitByItsLength() throws Exception {
    // spec: AR-R29 (for the type 3, the length of data is compared with the limit)
    byte[] data = new byte[1000];

    assertThat(RegionCompression.NONE.decompress(data, 1000)).isEqualTo(data);
    Failures.expect(
        OutputLimitExceededException.class, () -> RegionCompression.NONE.decompress(data, 999));
  }

  @Test
  void zlibBombStopsAtTheLimit() {
    // spec: AR-R29, RG4 bomb (a small stream that expands to megabytes)
    byte[] bomb = Regions.zlib(new byte[16 << 20]);

    assertThat(bomb.length).isLessThan(100_000);
    Failures.expect(
        OutputLimitExceededException.class, () -> RegionCompression.ZLIB.decompress(bomb, 1 << 20));
  }

  @Test
  void gzipBombStopsAtTheLimit() {
    // spec: AR-R29
    byte[] bomb = Regions.gzip(new byte[16 << 20]);

    Failures.expect(
        OutputLimitExceededException.class, () -> RegionCompression.GZIP.decompress(bomb, 1 << 20));
  }

  @Test
  void announcedGzipSizeIsNotBelieved() throws Exception {
    // spec: AR-R29 (no announced size is believed, neither the gzip ISIZE nor ...)
    byte[] gzip = Regions.gzip(randomBytes(2000, 3L));
    gzip[gzip.length - 4] = 5;
    gzip[gzip.length - 3] = 0;
    gzip[gzip.length - 2] = 0;
    gzip[gzip.length - 1] = 0;

    Failures.expect(
        OutputLimitExceededException.class, () -> RegionCompression.GZIP.decompress(gzip, 1000));
    Failures.expect(
        CorruptStreamException.class, () -> RegionCompression.GZIP.decompress(gzip, 5000));
  }

  @Test
  void corruptionBeforeTheLimitIsCorruptionAndTheLimitFirstIsTheLimit() {
    // spec: AR-R29 (the first event in stream order decides), AR-R20 (wrong trailer)
    byte[] zlib = Regions.zlib(randomBytes(10_000, 4L));
    zlib[zlib.length - 1] ^= 0x55;

    Failures.expect(
        OutputLimitExceededException.class, () -> RegionCompression.ZLIB.decompress(zlib, 9_999));
    Failures.expect(
        CorruptStreamException.class, () -> RegionCompression.ZLIB.decompress(zlib, 10_000));
    Failures.expect(
        CorruptStreamException.class, () -> RegionCompression.ZLIB.decompress(zlib, 1L << 30));
  }

  @ParameterizedTest
  @CsvSource({"ZLIB", "GZIP", "LZ4"})
  void streamsThatDoNotDecodeAreCorrupt(RegionCompression compression) {
    // spec: AR-R20 (decoding error, wrong or missing trailer, truncation), AR-R30 a
    byte[] good = payloadOf(compression, randomBytes(3000, 5L));
    byte[][] bad = {
      new byte[0],
      Arrays.copyOf(good, good.length / 2),
      Arrays.copyOf(good, good.length - 1),
      garbage(),
      new byte[] {0x78},
      Arrays.copyOf(good, 10)
    };

    for (byte[] stream : bad) {
      if (Arrays.equals(stream, good)) {
        continue;
      }
      Failures.expect(CorruptStreamException.class, () -> compression.decompress(stream, 1L << 30));
    }
  }

  @Test
  void zlibTrailerAndGzipCrcAreChecked() {
    // spec: AR-R20 (trailer faux), core-nbt R61 (Adler-32, CRC-32)
    byte[] zlib = Regions.zlib(randomBytes(500, 7L));
    zlib[zlib.length - 2] ^= 1;
    byte[] gzip = Regions.gzip(randomBytes(500, 8L));
    gzip[gzip.length - 7] ^= 1;

    Failures.expect(
        CorruptStreamException.class, () -> RegionCompression.ZLIB.decompress(zlib, 1L << 20));
    Failures.expect(
        CorruptStreamException.class, () -> RegionCompression.GZIP.decompress(gzip, 1L << 20));
  }

  @Test
  void decompressionDoesNotModifyItsInput() throws Exception {
    // spec: determinism (no hidden state)
    byte[] payload = payloadOf(RegionCompression.ZLIB, randomBytes(4000, 9L));
    byte[] copy = payload.clone();

    byte[] first = RegionCompression.ZLIB.decompress(payload, 1L << 20);
    byte[] second = RegionCompression.ZLIB.decompress(payload, 1L << 20);

    assertThat(payload).isEqualTo(copy);
    assertThat(second).isEqualTo(first);
  }
}

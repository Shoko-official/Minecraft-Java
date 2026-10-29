package dev.shoko.minecraft.world.storage;

import static dev.shoko.minecraft.world.support.RegionFixtures.CHUNK_A;
import static dev.shoko.minecraft.world.support.RegionFixtures.INDEX_A;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Hex;
import dev.shoko.minecraft.world.support.RegionFixtures;
import dev.shoko.minecraft.world.support.Regions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** AR-R11, AR-R17, AR-R18: chunks too large for 255 sectors go to a .mcc file. */
class RegionFileExternalWriteTest {

  /** The largest compressed payload kept inside the region file, AR-R11. */
  private static final int LARGEST_INTERNAL = 1_044_475;

  @TempDir Path folder;

  private void write(byte[] nbt, RegionCompression compression) throws IOException {
    try (RegionFile file = RegionFixtures.open(folder)) {
      file.write(CHUNK_A, nbt, compression, 1L);
    }
  }

  private byte[] readBack() throws IOException {
    try (RegionFile file = RegionFixtures.open(folder)) {
      return Chunks.present(file.read(CHUNK_A)).value();
    }
  }

  private static byte[] randomBytes(int length, long seed) {
    byte[] bytes = new byte[length];
    new SplittableRandom(seed).nextBytes(bytes);
    return bytes;
  }

  @Test
  void lastInternalSizeStaysInTheRegionFile() throws IOException {
    // spec: AR-R11, "Cas limites" (L = 1 044 476 is the last internal case: 255 sectors)
    byte[] nbt = randomBytes(LARGEST_INTERNAL, 1L);

    write(nbt, RegionCompression.NONE);

    byte[] file = RegionFixtures.read(RegionFixtures.regionFile(folder));
    assertThat(Regions.lengthOf(file, INDEX_A)).isEqualTo(1_044_476);
    assertThat(Regions.countOf(file, INDEX_A)).isEqualTo(255);
    assertThat(Regions.typeOf(file, INDEX_A)).isEqualTo(3);
    assertThat(RegionFixtures.names(folder)).containsExactly("r.-1.-1.mca");
    assertThat(readBack()).isEqualTo(nbt);
  }

  @Test
  void oneByteMoreMakesAnExternalChunk() throws IOException {
    // spec: AR-R11, AR-R17 (record 00 00 00 01 83, the raw stream alone in c.-1.-1.mcc)
    byte[] nbt = randomBytes(LARGEST_INTERNAL + 1, 2L);

    write(nbt, RegionCompression.NONE);

    byte[] file = RegionFixtures.read(RegionFixtures.regionFile(folder));
    assertThat(Regions.lengthOf(file, INDEX_A)).isEqualTo(1);
    assertThat(Regions.typeOf(file, INDEX_A)).isEqualTo(0x83);
    assertThat(Regions.countOf(file, INDEX_A)).isEqualTo(1);
    assertThat(file.length % 4096).isZero();
    assertThat(file.length).isLessThan(5 * 4096);
    assertThat(RegionFixtures.names(folder)).containsExactly("c.-1.-1.mcc", "r.-1.-1.mca");
    assertThat(RegionFixtures.read(folder.resolve("c.-1.-1.mcc"))).isEqualTo(nbt);
    assertThat(readBack()).isEqualTo(nbt);
  }

  @Test
  void externalRecordIsFiveBytesFollowedByZeros() throws IOException {
    // spec: AR-R17 (00 00 00 01 and the type plus 128), AR-R10 (zero padding)
    write(randomBytes(LARGEST_INTERNAL + 5, 3L), RegionCompression.NONE);

    byte[] file = RegionFixtures.read(RegionFixtures.regionFile(folder));
    int start = Regions.sectorOf(file, INDEX_A) * 4096;
    assertThat(Hex.format(java.util.Arrays.copyOfRange(file, start, start + 5)))
        .isEqualTo("00 00 00 01 83");
    for (int at = start + 5; at < start + 4096; at++) {
      assertThat(file[at]).isZero();
    }
  }

  @Test
  void compressedStreamTooLargeGoesToTheMccFileAsAZlibStream() throws IOException {
    // spec: AR-R11, AR-R17 (the limit applies to the compressed data)
    byte[] nbt = randomBytes(1_100_000, 4L);

    write(nbt, RegionCompression.ZLIB);

    byte[] file = RegionFixtures.read(RegionFixtures.regionFile(folder));
    assertThat(Regions.typeOf(file, INDEX_A)).isEqualTo(0x82);
    byte[] mcc = RegionFixtures.read(folder.resolve("c.-1.-1.mcc"));
    assertThat(mcc[0]).isEqualTo((byte) 0x78);
    assertThat(readBack()).isEqualTo(nbt);
  }

  @Test
  void compressibleDataOfTheSameSizeStaysInTheRegionFile() throws IOException {
    // spec: AR-R11 (the size that counts is the compressed one)
    write(new byte[3_000_000], RegionCompression.ZLIB);

    assertThat(RegionFixtures.names(folder)).containsExactly("r.-1.-1.mca");
    assertThat(readBack()).hasSize(3_000_000);
  }

  @Test
  void provisionalRewritingASmallChunkRemovesTheMccFile() throws IOException {
    // spec: AR-R18 (provisional: the .mcc is deleted when the chunk becomes small again)
    write(randomBytes(LARGEST_INTERNAL + 100, 5L), RegionCompression.NONE);
    assertThat(RegionFixtures.names(folder)).contains("c.-1.-1.mcc");

    write(randomBytes(1000, 6L), RegionCompression.NONE);

    assertThat(RegionFixtures.names(folder)).containsExactly("r.-1.-1.mca");
    byte[] file = RegionFixtures.read(RegionFixtures.regionFile(folder));
    assertThat(Regions.typeOf(file, INDEX_A)).isEqualTo(3);
    assertThat(readBack()).hasSize(1000);
  }

  @Test
  void rewritingAnExternalChunkWithAnotherLargeOneReplacesTheMccFile() throws IOException {
    // spec: AR-R17 (the .mcc holds the current stream)
    byte[] first = randomBytes(LARGEST_INTERNAL + 100, 7L);
    byte[] second = randomBytes(LARGEST_INTERNAL + 200, 8L);
    write(first, RegionCompression.NONE);

    write(second, RegionCompression.NONE);

    assertThat(RegionFixtures.read(folder.resolve("c.-1.-1.mcc"))).isEqualTo(second);
    assertThat(readBack()).isEqualTo(second);
  }

  @Test
  void aSmallChunkBecomingLargeCreatesTheMccFile() throws IOException {
    // spec: AR-R17
    write(randomBytes(500, 9L), RegionCompression.NONE);
    assertThat(Files.exists(folder.resolve("c.-1.-1.mcc"))).isFalse();

    write(randomBytes(LARGEST_INTERNAL + 10, 10L), RegionCompression.NONE);

    assertThat(Files.exists(folder.resolve("c.-1.-1.mcc"))).isTrue();
    assertThat(readBack()).hasSize(LARGEST_INTERNAL + 10);
  }
}

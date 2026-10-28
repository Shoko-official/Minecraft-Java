package dev.shoko.minecraft.world.storage;

import static dev.shoko.minecraft.world.support.RegionFixtures.CHUNK_A;
import static dev.shoko.minecraft.world.support.RegionFixtures.CHUNK_B;
import static dev.shoko.minecraft.world.support.RegionFixtures.INDEX_A;
import static dev.shoko.minecraft.world.support.RegionFixtures.INDEX_B;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.Lz4Frames;
import dev.shoko.minecraft.world.support.RegionFixtures;
import dev.shoko.minecraft.world.support.Regions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** AR-R7 to AR-R12, AR-R17, AR-R18 and "Ecriture": writing records of a region file. */
class RegionFileWriteTest {

  @TempDir Path folder;

  private static byte[] randomBytes(int length, long seed) {
    byte[] bytes = new byte[length];
    new SplittableRandom(seed).nextBytes(bytes);
    return bytes;
  }

  private void write(ChunkPos chunk, byte[] nbt, RegionCompression compression, long timestamp)
      throws IOException {
    try (RegionFile file = RegionFile.open(folder, chunk.region())) {
      file.write(chunk, nbt, compression, timestamp);
    }
  }

  private byte[] readBack(ChunkPos chunk) throws IOException {
    try (RegionFile file = RegionFile.open(folder, chunk.region())) {
      return Chunks.present(file.read(chunk)).value();
    }
  }

  private byte[] fileBytes() {
    return RegionFixtures.read(RegionFixtures.regionFile(folder));
  }

  private static byte[] inflate(byte[] zlib) throws IOException {
    return new InflaterInputStream(new ByteArrayInputStream(zlib)).readAllBytes();
  }

  private static byte[] gunzip(byte[] gzip) throws IOException {
    return new GZIPInputStream(new ByteArrayInputStream(gzip)).readAllBytes();
  }

  @ParameterizedTest
  @EnumSource(RegionCompression.class)
  void writtenChunkReadsBackWithEveryCompression(RegionCompression compression) throws IOException {
    // spec: AR-R13, AR-R14 (the writer produces 1, 2, 3 and 4)
    write(CHUNK_A, RegionFixtures.nbtOfA(), compression, Regions.TIMESTAMP_A);

    assertThat(readBack(CHUNK_A)).isEqualTo(RegionFixtures.nbtOfA());
  }

  @ParameterizedTest
  @EnumSource(RegionCompression.class)
  void recordFollowsTheLayoutOfTheSpec(RegionCompression compression) throws IOException {
    // spec: AR-R7, AR-R8, AR-R10, AR-R12 (L, type byte, sectors, size multiple of 4096)
    write(CHUNK_A, RegionFixtures.nbtOfA(), compression, Regions.TIMESTAMP_A);

    byte[] file = fileBytes();
    int sector = Regions.sectorOf(file, INDEX_A);
    int length = Regions.lengthOf(file, INDEX_A);
    assertThat(file.length % 4096).isZero();
    assertThat(file.length).isGreaterThanOrEqualTo(8192);
    assertThat(sector).isGreaterThanOrEqualTo(2);
    assertThat(Regions.typeOf(file, INDEX_A)).isEqualTo(compression.id());
    assertThat(Regions.countOf(file, INDEX_A)).isEqualTo(Regions.sectorsFor(length + 4));
    assertThat(file.length)
        .isGreaterThanOrEqualTo((sector + Regions.countOf(file, INDEX_A)) * 4096);
    assertThat(Regions.usedIndices(file)).containsExactly(INDEX_A);
  }

  @Test
  void zlibAndGzipRecordsInflateToTheNbtBytes() throws IOException {
    // spec: AR-R13 (RFC 1950 for type 2, RFC 1952 for type 1)
    write(CHUNK_A, RegionFixtures.nbtOfA(), RegionCompression.ZLIB, 1L);
    assertThat(inflate(Regions.payloadOf(fileBytes(), INDEX_A))).isEqualTo(RegionFixtures.nbtOfA());

    write(CHUNK_A, RegionFixtures.nbtOfA(), RegionCompression.GZIP, 1L);
    assertThat(gunzip(Regions.payloadOf(fileBytes(), INDEX_A))).isEqualTo(RegionFixtures.nbtOfA());
  }

  @Test
  void rawRecordHoldsTheNbtBytesAfterTheTypeByte() throws IOException {
    // spec: AR-R13 (type 3 is the NBT itself), AR-R10 (L = size + 1)
    write(CHUNK_B, RegionFixtures.nbtOfB(), RegionCompression.NONE, 1L);

    byte[] file = fileBytes();
    assertThat(Regions.lengthOf(file, INDEX_B)).isEqualTo(RegionFixtures.nbtOfB().length + 1);
    assertThat(Regions.payloadOf(file, INDEX_B)).isEqualTo(RegionFixtures.nbtOfB());
  }

  @Test
  void lz4RecordIsAStreamOfBlocksOf65536BytesAtMostEndingWithTheEndMark() throws IOException {
    // spec: AR-R15, AR-R16 (tokens 0x16 and 0x26, raw when not smaller, end mark)
    byte[] incompressible = randomBytes(150_000, 3L);
    write(CHUNK_A, incompressible, RegionCompression.LZ4, 1L);

    byte[] stream = Regions.payloadOf(fileBytes(), INDEX_A);
    List<Lz4Frames.Block> blocks = Lz4Frames.parse(stream);

    assertThat(blocks).hasSize(4);
    assertThat(blocks.subList(0, 3))
        .extracting(Lz4Frames.Block::length)
        .containsExactly(65_536, 65_536, 18_928);
    assertThat(blocks.subList(0, 3)).extracting(Lz4Frames.Block::token).containsOnly(0x16);
    Lz4Frames.Block end = blocks.getLast();
    assertThat(end.token()).isEqualTo(0x16);
    assertThat(end.compressedLength()).isZero();
    assertThat(end.length()).isZero();
    assertThat(end.checksum()).isZero();
  }

  @Test
  void lz4RecordOfCompressibleDataUsesTheCompressedMethod() throws IOException {
    // spec: AR-R15 (a block is raw only if compression does not shrink it)
    write(CHUNK_A, new byte[65_536], RegionCompression.LZ4, 1L);

    List<Lz4Frames.Block> blocks = Lz4Frames.parse(Regions.payloadOf(fileBytes(), INDEX_A));

    assertThat(blocks).hasSize(2);
    assertThat(blocks.getFirst().token()).isEqualTo(0x26);
    assertThat(blocks.getFirst().compressedLength()).isLessThan(65_536);
    assertThat(blocks.getFirst().length()).isEqualTo(65_536);
    assertThat(blocks.getFirst().checksum()).isEqualTo(Lz4Frames.checksum(new byte[65_536]));
  }

  @Test
  void timestampIsStoredAsBigEndianUnsignedSeconds() throws IOException {
    // spec: AR-R9 (unsigned 32 bit), RG1 (0x6ab13b80 = 1 790 000 000)
    write(CHUNK_A, RegionFixtures.nbtOfA(), RegionCompression.ZLIB, 1_790_000_000L);

    byte[] file = fileBytes();
    assertThat(Regions.timestamp(file, INDEX_A)).isEqualTo(1_790_000_000L);
    assertThat(Arrays.copyOfRange(file, 4096 + 4 * INDEX_A, 4096 + 4 * INDEX_A + 4))
        .containsExactly((byte) 0x6a, (byte) 0xb1, (byte) 0x3b, (byte) 0x80);
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {0L, 1L, 2_147_483_648L, 4_294_967_295L})
  void timestampsOfTheWholeUnsignedRangeAreAccepted(long timestamp) throws IOException {
    // spec: AR-R9 (unsigned 32 bit value)
    write(CHUNK_A, RegionFixtures.nbtOfA(), RegionCompression.NONE, timestamp);

    assertThat(Regions.timestamp(fileBytes(), INDEX_A)).isEqualTo(timestamp);
  }

  @Test
  void timestampsOutOfRangeAreRejectedAndNothingIsWritten() throws IOException {
    // spec: RegionFile.write contract (IllegalArgumentException)
    try (RegionFile file = RegionFixtures.open(folder)) {
      Failures.expect(
          IllegalArgumentException.class,
          () -> file.write(CHUNK_A, new byte[10], RegionCompression.NONE, -1L));
      Failures.expect(
          IllegalArgumentException.class,
          () -> file.write(CHUNK_A, new byte[10], RegionCompression.NONE, 4_294_967_296L));
    }
    assertThat(RegionFixtures.regionFile(folder)).doesNotExist();
  }

  @Test
  void chunkOfAnotherRegionIsRejected() throws IOException {
    // spec: RegionFile.write contract (IllegalArgumentException)
    try (RegionFile file = RegionFixtures.open(folder)) {
      Failures.expect(
          IllegalArgumentException.class,
          () -> file.write(new ChunkPos(0, 0), new byte[10], RegionCompression.NONE, 1L));
    }
  }

  @Test
  void bytesAfterTheRecordUpToTheEndOfItsSectorAreZero() throws IOException {
    // spec: AR-R10 (padding is zero when writing, provisional)
    write(CHUNK_A, randomBytes(5000, 11L), RegionCompression.NONE, 1L);

    byte[] file = fileBytes();
    int start = Regions.sectorOf(file, INDEX_A) * 4096;
    int end = start + Regions.countOf(file, INDEX_A) * 4096;
    for (int at = start + 4 + Regions.lengthOf(file, INDEX_A); at < end; at++) {
      assertThat(file[at]).as("byte " + at).isZero();
    }
  }

  @Test
  void rewritingAChunkReplacesItAndLeavesTheOthersReadable() throws IOException {
    // spec: "Ecriture" (replacing any previous record), AR-R12 (free placement, no overlap)
    byte[] small = randomBytes(100, 1L);
    byte[] large = randomBytes(20_000, 2L);
    byte[] other = randomBytes(9_000, 3L);
    write(CHUNK_A, small, RegionCompression.NONE, 10L);
    write(CHUNK_B, other, RegionCompression.NONE, 11L);

    write(CHUNK_A, large, RegionCompression.NONE, 12L);

    assertThat(readBack(CHUNK_A)).isEqualTo(large);
    assertThat(readBack(CHUNK_B)).isEqualTo(other);
    assertThat(Regions.timestamp(fileBytes(), INDEX_A)).isEqualTo(12L);
    assertThat(Regions.timestamp(fileBytes(), INDEX_B)).isEqualTo(11L);

    write(CHUNK_A, small, RegionCompression.ZLIB, 13L);
    assertThat(readBack(CHUNK_A)).isEqualTo(small);
    assertThat(readBack(CHUNK_B)).isEqualTo(other);
  }

  @Test
  void manyChunksOfAllSizesNeverOverlap() throws IOException {
    // spec: AR-R12 (a chunk occupies ceil((L + 4) / 4096) consecutive sectors, no overlap)
    SplittableRandom random = new SplittableRandom(2026L);
    List<ChunkPos> chunks = new ArrayList<>();
    List<byte[]> contents = new ArrayList<>();
    for (int step = 0; step < 60; step++) {
      int index = random.nextInt(1024);
      ChunkPos chunk = RegionFixtures.chunkAtIndex(index);
      byte[] nbt = randomBytes(1 + random.nextInt(25_000), step);
      write(chunk, nbt, RegionCompression.NONE, step);
      if (chunks.contains(chunk)) {
        contents.set(chunks.indexOf(chunk), nbt);
      } else {
        chunks.add(chunk);
        contents.add(nbt);
      }
    }

    byte[] file = fileBytes();
    boolean[] taken = new boolean[file.length / 4096];
    for (int index : Regions.usedIndices(file)) {
      int sector = Regions.sectorOf(file, index);
      assertThat(sector).isGreaterThanOrEqualTo(2);
      for (int s = sector; s < sector + Regions.countOf(file, index); s++) {
        assertThat(taken[s]).as("sector " + s).isFalse();
        taken[s] = true;
      }
    }
    for (int k = 0; k < chunks.size(); k++) {
      assertThat(readBack(chunks.get(k))).as("chunk " + chunks.get(k)).isEqualTo(contents.get(k));
    }
    assertThat(file.length % 4096).isZero();
  }

  @Test
  void sameWritesInTwoFoldersGiveTheSameFile() throws IOException {
    // spec: determinism (same input, same output, byte for byte), AR-R12 free placement but stable
    Path other = folder.resolve("other");
    java.nio.file.Files.createDirectories(other);
    for (Path target : new Path[] {folder, other}) {
      try (RegionFile file = RegionFile.open(target, CHUNK_A.region())) {
        file.write(CHUNK_A, RegionFixtures.nbtOfA(), RegionCompression.ZLIB, 5L);
        file.write(CHUNK_B, RegionFixtures.nbtOfB(), RegionCompression.LZ4, 6L);
        file.write(CHUNK_A, randomBytes(7000, 4L), RegionCompression.GZIP, 7L);
      }
    }

    assertThat(RegionFixtures.read(RegionFixtures.regionFile(other))).isEqualTo(fileBytes());
  }
}

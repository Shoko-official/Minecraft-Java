package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.RegionFixtures;
import dev.shoko.minecraft.world.support.Regions;
import dev.shoko.minecraft.world.support.Regions.Blob;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Seeded attack on region files read through {@link RegionFile} and {@link AnvilChunkStore}:
 * whatever the bytes, a read gives one of the three issues, never another exception (AR-R26,
 * AR-R27), and a save at a refused position fails with the one error of AR-R28.
 */
class RegionFileAttackTest {

  static Stream<Long> seeds() {
    return LongStream.range(0, 50).boxed();
  }

  @TempDir Path folder;

  private static byte[] baseFile(int typeOfA) {
    List<Blob> blobs = new ArrayList<>();
    blobs.add(new Blob(1023, typeOfA, Regions.compress(typeOfA, RegionFixtures.nbtOfA()), 1L));
    blobs.add(new Blob(0, 3, RegionFixtures.nbtOfB(), 2L));
    blobs.add(new Blob(77, 2, Regions.zlib(RegionFixtures.nbtOfB()), 3L));
    blobs.add(new Blob(500, 1, Regions.gzip(RegionFixtures.nbtOfB()), 4L));
    blobs.add(new Blob(900, 4, Regions.compress(4, RegionFixtures.nbtOfB()), 5L));
    return Regions.file(blobs);
  }

  private static byte[] damage(byte[] file, SplittableRandom random) {
    byte[] copy = file.clone();
    switch (random.nextInt(5)) {
      case 0 -> {
        int flips = 1 + random.nextInt(8);
        for (int k = 0; k < flips; k++) {
          copy[random.nextInt(copy.length)] ^= (byte) (1 << random.nextInt(8));
        }
      }
      case 1 -> copy = Arrays.copyOf(copy, random.nextInt(copy.length));
      case 2 -> {
        for (int k = 0; k < 6; k++) {
          if (copy.length >= 4096) {
            Regions.putInt(copy, 4 * random.nextInt(1024), random.nextInt());
          } else {
            copy[random.nextInt(copy.length)] ^= (byte) 0xA5;
          }
        }
      }
      case 3 -> {
        if (copy.length >= 12_288) {
          int record = Regions.sectorOf(copy, 1023) * 4096;
          Regions.putInt(
              copy, record, random.nextInt(3) == 0 ? random.nextInt() : random.nextInt(9000));
          copy[record + 4] = (byte) random.nextInt(256);
        } else {
          copy[random.nextInt(copy.length)] ^= (byte) 0x55;
        }
      }
      default -> {
        byte[] noise = new byte[random.nextInt(20_000)];
        random.nextBytes(noise);
        copy = noise;
      }
    }
    return copy;
  }

  private void assertReadable(ChunkPos pos, byte[] file, RegionFile region) throws IOException {
    ReadResult<byte[]> result;
    try {
      result = region.read(pos, 1 << 20);
    } catch (IOException e) {
      assertThat(file.length)
          .as("IOException on a file of " + file.length + " bytes")
          .isLessThan(8192);
      return;
    }
    switch (result) {
      case ReadResult.Present<byte[]> present ->
          assertThat(present.value()).hasSizeLessThanOrEqualTo(1 << 20);
      case ReadResult.Absent<byte[]> absent -> assertThat(absent.cause()).isNotNull();
      case ReadResult.Refused<byte[]> refused -> assertThat(refused.message()).isNotBlank();
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void damagedRegionFilesGiveOneOfTheThreeIssues(long seed) throws IOException {
    // spec: AR-R20, AR-R26, AR-R27 (invalid content is never an exception other than a typed one)
    SplittableRandom random = new SplittableRandom(seed);
    for (int type = 1; type <= 4; type++) {
      byte[] file = damage(baseFile(type), random);
      RegionFixtures.write(RegionFixtures.regionFile(folder), file);
      try (RegionFile region = RegionFile.open(folder, RegionFixtures.REGION)) {
        for (int index :
            new int[] {0, 77, 500, 900, 1023, random.nextInt(1024), random.nextInt(1024)}) {
          assertReadable(RegionFixtures.chunkAtIndex(index), file, region);
        }
      } catch (IOException e) {
        assertThat(file.length).isLessThan(8192);
      }
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void storeOnDamagedFilesGivesIssuesAndSavesFailOnlyOnRefusedPositions(long seed)
      throws IOException {
    // spec: AR-R26, AR-R28 (a refusal marks the position, and only a save there is an error)
    SplittableRandom random = new SplittableRandom(10_000 + seed);
    byte[] file = damage(baseFile(1 + random.nextInt(4)), random);
    RegionFixtures.write(RegionFixtures.regionFile(folder), file);
    try (AnvilChunkStore store =
        AnvilChunkStore.open(
            folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB, 1 << 20)) {
      for (int index : new int[] {1023, 0, 77, 500, 900}) {
        ChunkPos pos = RegionFixtures.chunkAtIndex(index);
        ReadResult<Chunk> result;
        try {
          result = store.load(pos);
        } catch (IOException e) {
          assertThat(file.length).isLessThan(8192);
          continue;
        }
        boolean refused = result instanceof ReadResult.Refused<Chunk>;
        Chunk chunk = Chunks.empty(Blocks.registry(), pos, Chunks.OVERWORLD);
        if (refused) {
          Failures.expect(WriteOnRefusedPositionException.class, () -> store.save(chunk, 1L));
        }
      }
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void externalRecordsOfDamagedMccFilesGiveOneOfTheThreeIssues(long seed) throws IOException {
    // spec: AR-R17, AR-R18, AR-R20 (a .mcc of any content)
    SplittableRandom random = new SplittableRandom(20_000 + seed);
    byte[] file =
        Regions.file(List.of(new Blob(1023, 2 + 128 + random.nextInt(3), new byte[0], 1L)));
    byte[] mcc = Regions.compress(1 + random.nextInt(4), RegionFixtures.nbtOfA());
    mcc = random.nextBoolean() ? damage(mcc, random) : mcc;
    RegionFixtures.write(RegionFixtures.regionFile(folder), file);
    RegionFixtures.write(folder.resolve("c.-1.-1.mcc"), mcc);

    try (RegionFile region = RegionFile.open(folder, RegionFixtures.REGION)) {
      assertReadable(RegionFixtures.CHUNK_A, file, region);
    }
  }
}

package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.RegionPos;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.support.Regions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Bug-hunter probes on the region storage. */
class StorageHuntTest {

  @TempDir Path base;

  private static byte[] sample(int length, long seed) {
    byte[] data = new byte[length];
    new Random(seed).nextBytes(data);
    return data;
  }

  private static byte[] compressible(int length) {
    byte[] data = new byte[length];
    for (int i = 0; i < length; i++) {
      data[i] = (byte) (i % 13);
    }
    return data;
  }

  /** Runs a call on a daemon thread and fails if it does not end: a spinning loop is a bug. */
  private static <T> T within(Callable<T> call) throws Exception {
    AtomicReference<T> value = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread =
        new Thread(
            () -> {
              try {
                value.set(call.call());
              } catch (Throwable e) {
                failure.set(e);
              }
            });
    thread.setDaemon(true);
    thread.start();
    thread.join(10_000);
    assertThat(thread.isAlive()).as("the call did not end within 10 s (endless loop)").isFalse();
    if (failure.get() != null) {
      throw new AssertionError(failure.get());
    }
    return value.get();
  }

  private static byte[] value(ReadResult<byte[]> result) {
    assertThat(result).isInstanceOf(ReadResult.Present.class);
    return ((ReadResult.Present<byte[]>) result).value();
  }

  @Test
  void unlimitedDecompressionLimitDecodesZlib() throws Exception {
    byte[] nbt = compressible(100_000);
    byte[] stream = RegionCompression.ZLIB.compress(nbt);
    assertThat(within(() -> RegionCompression.ZLIB.decompress(stream, Long.MAX_VALUE)))
        .isEqualTo(nbt);
  }

  @Test
  void unlimitedDecompressionLimitDecodesGzip() throws Exception {
    byte[] nbt = compressible(100_000);
    byte[] stream = RegionCompression.GZIP.compress(nbt);
    assertThat(within(() -> RegionCompression.GZIP.decompress(stream, Long.MAX_VALUE)))
        .isEqualTo(nbt);
  }

  @Test
  void unlimitedDecompressionLimitDecodesLz4AndNone() throws Exception {
    byte[] nbt = compressible(100_000);
    assertThat(
            RegionCompression.LZ4.decompress(RegionCompression.LZ4.compress(nbt), Long.MAX_VALUE))
        .isEqualTo(nbt);
    assertThat(RegionCompression.NONE.decompress(nbt, Long.MAX_VALUE)).isEqualTo(nbt);
  }

  @Test
  void unlimitedLimitReadsAnExternalChunk() throws Exception {
    ChunkPos pos = new ChunkPos(3, 4);
    byte[] nbt = compressible(2_000_000);
    try (RegionFile file = RegionFile.open(base, pos.region())) {
      file.write(pos, nbt, RegionCompression.NONE, 1L);
    }
    assertThat(base.resolve("c.3.4.mcc")).exists();
    try (RegionFile file = RegionFile.open(base, pos.region())) {
      assertThat(value(within(() -> file.read(pos, Long.MAX_VALUE)))).isEqualTo(nbt);
    }
  }

  @Test
  void externalZlibChunkIsReadWithAnUnlimitedLimit() throws Exception {
    ChunkPos pos = new ChunkPos(3, 4);
    byte[] nbt = sample(1_200_000, 5);
    try (RegionFile file = RegionFile.open(base, pos.region())) {
      file.write(pos, nbt, RegionCompression.ZLIB, 1L);
    }
    assertThat(base.resolve("c.3.4.mcc")).exists();
    try (RegionFile file = RegionFile.open(base, pos.region())) {
      assertThat(within(() -> file.read(pos, Long.MAX_VALUE)))
          .isInstanceOf(ReadResult.Present.class);
    }
  }

  @Test
  void rewritesOfMixedSizesAndCompressionsKeepEveryOtherChunk() throws Exception {
    Random random = new Random(20261028L);
    Map<Integer, byte[]> expected = new HashMap<>();
    RegionPos region = new RegionPos(-1, 0);
    RegionCompression[] types = RegionCompression.values();
    try (RegionFile file = RegionFile.open(base, region)) {
      for (int step = 0; step < 600; step++) {
        int index = random.nextInt(40);
        ChunkPos pos = new ChunkPos(-32 + (index & 31), index >> 5);
        int size =
            switch (random.nextInt(5)) {
              case 0 -> random.nextInt(20);
              case 1 -> 4090 + random.nextInt(12);
              case 2 -> 8180 + random.nextInt(20);
              case 3 -> random.nextInt(30_000);
              default -> random.nextInt(200_000);
            };
        byte[] nbt = random.nextBoolean() ? sample(size, step) : compressible(size);
        RegionCompression type = types[random.nextInt(types.length)];
        file.write(pos, nbt, type, step);
        expected.put(pos.indexInRegion(), nbt);
        if (step % 25 == 0) {
          verify(file, region, expected);
        }
      }
      verify(file, region, expected);
    }
    byte[] raw = Files.readAllBytes(base.resolve("r.-1.0.mca"));
    assertThat(raw.length % 4096).isZero();
    boolean[] used = new boolean[raw.length / 4096 + 300];
    for (int index = 0; index < 1024; index++) {
      int count = Regions.countOf(raw, index);
      int sector = Regions.sectorOf(raw, index);
      if (count == 0) {
        continue;
      }
      assertThat(sector).isGreaterThanOrEqualTo(2);
      for (int s = sector; s < sector + count; s++) {
        assertThat(used[s]).as("sector %d claimed twice", s).isFalse();
        used[s] = true;
      }
    }
  }

  private static void verify(RegionFile file, RegionPos region, Map<Integer, byte[]> expected)
      throws IOException {
    for (Map.Entry<Integer, byte[]> entry : expected.entrySet()) {
      ChunkPos pos = new ChunkPos(region.x() * 32 + (entry.getKey() & 31), entry.getKey() >> 5);
      assertThat(value(file.read(pos))).as("chunk %s", pos).isEqualTo(entry.getValue());
    }
  }

  @Test
  void twoHandlesInterleavedSeeEachOthersWrites() throws Exception {
    RegionPos region = new RegionPos(0, 0);
    ChunkPos a = new ChunkPos(1, 1);
    ChunkPos b = new ChunkPos(2, 1);
    byte[] da = sample(10_000, 1);
    byte[] db = sample(30_000, 2);
    try (RegionFile one = RegionFile.open(base, region);
        RegionFile two = RegionFile.open(base, region)) {
      one.write(a, da, RegionCompression.NONE, 1);
      two.write(b, db, RegionCompression.NONE, 2);
      one.write(a, da, RegionCompression.ZLIB, 3);
      assertThat(value(one.read(b))).isEqualTo(db);
      assertThat(value(two.read(a))).isEqualTo(da);
    }
  }

  @Test
  void rawRecordAtTheInternalMaximumStaysInternalAndOneMoreByteGoesExternal() throws Exception {
    ChunkPos pos = new ChunkPos(5, 5);
    byte[] max = sample(1_044_475, 3);
    try (RegionFile file = RegionFile.open(base, pos.region())) {
      file.write(pos, max, RegionCompression.NONE, 0xFFFF_FFFFL);
    }
    assertThat(base.resolve("c.5.5.mcc")).doesNotExist();
    byte[] raw = Files.readAllBytes(base.resolve("r.0.0.mca"));
    int index = pos.indexInRegion();
    assertThat(Regions.countOf(raw, index)).isEqualTo(255);
    assertThat(Regions.lengthOf(raw, index)).isEqualTo(1_044_476);
    assertThat(Regions.timestamp(raw, index)).isEqualTo(0xFFFF_FFFFL);
    try (RegionFile file = RegionFile.open(base, pos.region())) {
      assertThat(value(file.read(pos))).isEqualTo(max);
      byte[] over = sample(1_044_476, 4);
      file.write(pos, over, RegionCompression.NONE, 1);
      assertThat(base.resolve("c.5.5.mcc")).exists();
      assertThat(value(file.read(pos))).isEqualTo(over);
      file.write(pos, max, RegionCompression.NONE, 2);
      assertThat(base.resolve("c.5.5.mcc")).doesNotExist();
      assertThat(value(file.read(pos))).isEqualTo(max);
    }
  }

  @Test
  void extremeChunkCoordinatesLandInTheirOwnRegionFiles() throws Exception {
    int[] coordinates = {
      Integer.MIN_VALUE,
      Integer.MIN_VALUE + 1,
      -33,
      -32,
      -17,
      -16,
      -1,
      0,
      1,
      15,
      16,
      31,
      32,
      Integer.MAX_VALUE - 1,
      Integer.MAX_VALUE
    };
    for (int x : coordinates) {
      for (int z : coordinates) {
        ChunkPos pos = new ChunkPos(x, z);
        byte[] nbt = sample(50, ((long) x << 32) ^ z);
        try (RegionFile file = RegionFile.open(base, pos.region())) {
          file.write(pos, nbt, RegionCompression.ZLIB, 1);
        }
      }
    }
    for (int x : coordinates) {
      for (int z : coordinates) {
        ChunkPos pos = new ChunkPos(x, z);
        byte[] nbt = sample(50, ((long) x << 32) ^ z);
        try (RegionFile file = RegionFile.open(base, pos.region())) {
          assertThat(value(file.read(pos))).isEqualTo(nbt);
        }
      }
    }
  }

  @Test
  void aStoreOpenedWithAnUnlimitedLimitLoadsAChunk() throws Exception {
    byte[] nbt = dev.shoko.minecraft.world.support.ChunkNbt.chunkA("bedrock");
    dev.shoko.minecraft.core.coord.ChunkPos pos = new ChunkPos(-1, -1);
    try (RegionFile file = RegionFile.open(base, pos.region())) {
      file.write(pos, nbt, RegionCompression.ZLIB, 1L);
    }
    try (AnvilChunkStore store =
        AnvilChunkStore.open(
            base,
            dev.shoko.minecraft.world.support.Chunks.OVERWORLD,
            dev.shoko.minecraft.world.support.Chunks.codec(),
            RegionCompression.ZLIB,
            Long.MAX_VALUE)) {
      assertThat(within(() -> store.load(pos))).isInstanceOf(ReadResult.Present.class);
    }
  }
}

package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.SplittableRandom;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: core-math R9, R25, R28 to R33: adversarial checks of packings and conversions.
class PackingAttackTest {

  private static final int[] INT_EXTREMES = {
    Integer.MIN_VALUE,
    Integer.MIN_VALUE + 1,
    -(1 << 25) - 1,
    -(1 << 25),
    -(1 << 21) - 1,
    -(1 << 21),
    -30_000_000,
    -2049,
    -2048,
    -1,
    0,
    1,
    2047,
    2048,
    (1 << 21) - 1,
    1 << 21,
    29_999_999,
    (1 << 25) - 1,
    1 << 25,
    Integer.MAX_VALUE - 1,
    Integer.MAX_VALUE
  };

  /** Two's complement value of the low {@code bits} bits of v, computed with floorMod. */
  private static int wrap(long v, int bits) {
    long m = Math.floorMod(v, 1L << bits);
    return (int) (m >= 1L << (bits - 1) ? m - (1L << bits) : m);
  }

  private static long refPackBlock(int x, int y, int z) {
    return Math.floorMod(x, 1L << 26) * (1L << 38)
        + Math.floorMod(z, 1L << 26) * (1L << 12)
        + Math.floorMod(y, 1L << 12);
  }

  private static long refPackSection(int x, int y, int z) {
    return Math.floorMod(x, 1L << 22) * (1L << 42)
        + Math.floorMod(z, 1L << 22) * (1L << 20)
        + Math.floorMod(y, 1L << 20);
  }

  private static boolean blockOk(int x, int y, int z) {
    BlockPos pos = new BlockPos(x, y, z);
    long packed = pos.pack();
    return packed == refPackBlock(x, y, z)
        && BlockPos.unpack(packed).equals(new BlockPos(wrap(x, 26), wrap(y, 12), wrap(z, 26)));
  }

  /** A stride over [-(2^25) - 64, 2^25 + 64) plus every value within 64 of a field edge. */
  private static IntStream horizontalFieldSample() {
    IntStream strided = IntStream.iterate(-(1 << 25) - 64, v -> v < (1 << 25) + 64, v -> v + 509);
    IntStream edges =
        IntStream.of(-(1 << 25), 0, 1 << 25).flatMap(e -> IntStream.range(e - 64, e + 64));
    return IntStream.concat(strided, edges);
  }

  @Test
  @Timeout(20)
  void blockPackEachFieldAgainstExtremesOfTheOthers() {
    int[] corners = {Integer.MIN_VALUE, -(1 << 25), -1, 0, (1 << 25) - 1, Integer.MAX_VALUE};
    long mismatches =
        horizontalFieldSample()
            .filter(
                v -> {
                  int c = corners[Math.floorMod(v, corners.length)];
                  return !blockOk(v, c, c) || !blockOk(c, c, v);
                })
            .count();
    for (int y = -5000; y <= 5000; y++) {
      for (int c : corners) {
        if (!blockOk(c, y, c)) {
          mismatches++;
        }
      }
    }
    for (int x : INT_EXTREMES) {
      for (int y : INT_EXTREMES) {
        for (int z : INT_EXTREMES) {
          if (!blockOk(x, y, z)) {
            mismatches++;
          }
        }
      }
    }
    assertThat(mismatches).isZero();
    // spec: core-math V5, last row (silent wrap)
    assertThat(new BlockPos(33554432, 2048, -33554433).pack()).isEqualTo(0x8000001FFFFFF800L);
    assertThat(BlockPos.unpack(0x8000001FFFFFF800L))
        .isEqualTo(new BlockPos(-33554432, -2048, 33554431));
  }

  @Test
  @Timeout(20)
  void blockAndSectionPackingsAreBijectionsOnLongs() {
    SplittableRandom rnd = new SplittableRandom(29);
    long mismatches = 0;
    long[] specials = {0, -1, Long.MIN_VALUE, Long.MAX_VALUE, 1, 1L << 38, 1L << 37, 1L << 62};
    for (long v : specials) {
      assertThat(BlockPos.unpack(v).pack()).isEqualTo(v);
      assertThat(SectionPos.unpack(v).pack()).isEqualTo(v);
      assertThat(ChunkPos.unpack(v).pack()).isEqualTo(v);
    }
    for (int i = 0; i < 500_000; i++) {
      long v = rnd.nextLong();
      if (BlockPos.unpack(v).pack() != v
          || SectionPos.unpack(v).pack() != v
          || ChunkPos.unpack(v).pack() != v) {
        mismatches++;
      }
    }
    assertThat(mismatches).isZero();
  }

  @Test
  @Timeout(20)
  void sectionPackAgainstReference() {
    for (int x : INT_EXTREMES) {
      for (int y : INT_EXTREMES) {
        for (int z : INT_EXTREMES) {
          SectionPos pos = new SectionPos(x, y, z);
          assertThat(pos.pack()).as("%s", pos).isEqualTo(refPackSection(x, y, z));
          assertThat(SectionPos.unpack(pos.pack()))
              .isEqualTo(new SectionPos(wrap(x, 22), wrap(y, 20), wrap(z, 22)));
        }
      }
    }
    long mismatches = 0;
    for (int v = -(1 << 21) - 8; v < (1 << 21) + 8; v += 7) {
      if (new SectionPos(v, v, -v - 1).pack() != refPackSection(v, v, -v - 1)) {
        mismatches++;
      }
    }
    assertThat(mismatches).isZero();
  }

  @Test
  void chunkPackAgainstReference() {
    for (int x : INT_EXTREMES) {
      for (int z : INT_EXTREMES) {
        ChunkPos pos = new ChunkPos(x, z);
        long expected = (long) z * (1L << 32) + Integer.toUnsignedLong(x);
        assertThat(pos.pack()).as("%s", pos).isEqualTo(expected);
        assertThat(ChunkPos.unpack(expected)).isEqualTo(pos);
        assertThat(pos.region())
            .isEqualTo(new RegionPos(Math.floorDiv(x, 32), Math.floorDiv(z, 32)));
        assertThat(pos.indexInRegion()).isEqualTo(Math.floorMod(x, 32) + 32 * Math.floorMod(z, 32));
        assertThat(pos.minBlockX()).isEqualTo((int) ((long) x * 16));
      }
    }
  }

  @Test
  @Timeout(20)
  void localIndicesAgainstReference() {
    int[] extra = {
      Integer.MIN_VALUE,
      Integer.MAX_VALUE,
      -30_000_000,
      29_999_999,
      -64,
      319,
      -65,
      320,
      1 << 30,
      -(1 << 30)
    };
    int[] values = IntStream.concat(IntStream.range(-20, 20), IntStream.of(extra)).toArray();
    long mismatches = 0;
    for (int x : values) {
      for (int y : values) {
        for (int z : values) {
          int lx = Math.floorMod(x, 16);
          int ly = Math.floorMod(y, 16);
          int lz = Math.floorMod(z, 16);
          if (LocalIndex.sectionStateIndex(x, y, z) != ly * 256 + lz * 16 + lx
              || LocalIndex.postProcessingIndex(x, y, z) != lz * 256 + ly * 16 + lx
              || LocalIndex.sectionUpdateIndex(x, y, z) != lx * 256 + lz * 16 + ly) {
            mismatches++;
          }
        }
      }
    }
    assertThat(mismatches).isZero();
  }

  // Stride over the whole int range (the dense sample lives in CoordinatesTest)
  @Test
  @Timeout(20)
  void scalarConversionsOverAStrideOfTheIntRange() {
    long mismatches = 0;
    for (long v = Integer.MIN_VALUE; v <= Integer.MAX_VALUE; v += 65_521) {
      if (!scalarOk(v)) {
        mismatches++;
      }
    }
    for (long v : new long[] {Integer.MIN_VALUE, Integer.MAX_VALUE, -30_000_001, 30_000_000}) {
      if (!scalarOk(v)) {
        mismatches++;
      }
    }
    assertThat(mismatches).isZero();
  }

  private static boolean scalarOk(long v) {
    int i = (int) v;
    return Coordinates.blockToSection(i) == Math.floorDiv(v, 16)
        && Coordinates.sectionLocal(i) == Math.floorMod(v, 16)
        && Coordinates.blockToQuart(i) == Math.floorDiv(v, 4)
        && Coordinates.chunkToRegion(i) == Math.floorDiv(v, 32)
        && Coordinates.regionLocal(i) == Math.floorMod(v, 32)
        && Coordinates.blockToRegion(i) == Math.floorDiv(v, 512)
        && Coordinates.sectionToBlock(i) == (int) (v * 16)
        && Coordinates.isWithinHorizontalLimit(i) == (v >= -30_000_000 && v < 30_000_000);
  }

  @Test
  @Timeout(20)
  void blockToChunkAndSectionAgreeWithScalarConversions() {
    SplittableRandom rnd = new SplittableRandom(31);
    for (int i = 0; i < 200_000; i++) {
      int x = rnd.nextInt();
      int y = rnd.nextInt();
      int z = rnd.nextInt();
      BlockPos pos = new BlockPos(x, y, z);
      assertThat(pos.chunk()).isEqualTo(new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16)));
      assertThat(pos.section())
          .isEqualTo(
              new SectionPos(Math.floorDiv(x, 16), Math.floorDiv(y, 16), Math.floorDiv(z, 16)));
    }
  }

  @Test
  void worldHeightsOfTheThreeDimensions() {
    WorldHeight overworld = new WorldHeight(-64, 384);
    assertThat(overworld.maxY()).isEqualTo(319);
    assertThat(overworld.contains(-64)).isTrue();
    assertThat(overworld.contains(-65)).isFalse();
    assertThat(overworld.contains(320)).isFalse();
    assertThat(overworld.sectionCount()).isEqualTo(24);
    assertThat(overworld.sectionIndex(-4)).isZero();
    assertThat(overworld.sectionIndex(19)).isEqualTo(23);
    WorldHeight nether = new WorldHeight(0, 256);
    assertThat(nether.sectionCount()).isEqualTo(16);
    assertThat(nether.maxSectionY()).isEqualTo(15);
    assertThat(overworld.contains(Integer.MIN_VALUE)).isFalse();
    assertThat(overworld.contains(Integer.MAX_VALUE)).isFalse();
  }
}

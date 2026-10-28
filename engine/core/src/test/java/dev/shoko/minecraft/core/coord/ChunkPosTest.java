package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class ChunkPosTest {

  // spec: core-math R28, V3: first block is x << 4, silent overflow beyond 2^27
  @ParameterizedTest(name = "chunk ({0}, {1}) starts at block ({2}, {3})")
  @CsvSource({
    "0, 0, 0, 0",
    "-1, 1, -16, 16",
    "1875000, -1875000, 30000000, -30000000",
    "134217728, -134217728, -2147483648, -2147483648"
  })
  void minBlockShiftsLeftByFour(int x, int z, int blockX, int blockZ) {
    ChunkPos chunk = new ChunkPos(x, z);
    assertThat(chunk.minBlockX()).isEqualTo(blockX);
    assertThat(chunk.minBlockZ()).isEqualTo(blockZ);
  }

  // spec: core-math R28, R33, vector V8: region is (x >> 5, z >> 5), negatives floor
  @ParameterizedTest(name = "chunk ({0}, {1}) is in region ({2}, {3})")
  @CsvSource({
    "30, -3, 0, -1",
    "-1, -1, -1, -1",
    "0, 0, 0, 0",
    "31, 31, 0, 0",
    "32, -32, 1, -1",
    "-33, 32, -2, 1",
    "-32, -33, -1, -2",
    "-1875000, 1874999, -58594, 58593"
  })
  void regionUsesArithmeticShift(int x, int z, int rx, int rz) {
    assertThat(new ChunkPos(x, z).region()).isEqualTo(new RegionPos(rx, rz));
  }

  // spec: core-math R33 (S), vector V8: index (x & 31) + 32 * (z & 31)
  @ParameterizedTest(name = "provisional chunk ({0}, {1}) has region index {2}")
  @CsvSource({
    "30, -3, 958",
    "-1, -1, 1023",
    "0, 0, 0",
    "31, 31, 1023",
    "32, -32, 0",
    "-33, 32, 31",
    "-32, -33, 992",
    "-1875000, 1874999, 744"
  })
  void provisionalIndexInRegionMatchesVectors(int x, int z, int index) {
    assertThat(new ChunkPos(x, z).indexInRegion()).isEqualTo(index);
  }

  // spec: core-math R33 (S): the 1024 chunks of a region, negative region included, map one to
  // one onto 0..1023
  @ParameterizedTest(name = "provisional region ({0}, {1}) covers indices 0..1023 once")
  @CsvSource({"0, 0", "-1, -1", "-58594, 58593"})
  void provisionalIndicesCoverTheRegionGridOnce(int rx, int rz) {
    Set<Integer> seen = new HashSet<>();
    for (int dx = 0; dx < 32; dx++) {
      for (int dz = 0; dz < 32; dz++) {
        int index = new ChunkPos(rx * 32 + dx, rz * 32 + dz).indexInRegion();
        assertThat(index).isBetween(0, 1023);
        seen.add(index);
      }
    }
    assertThat(seen).hasSize(1024);
  }

  static Stream<Arguments> packedVectors() {
    return Stream.of(
        Arguments.of(new ChunkPos(0, 0), 0x0000000000000000L),
        Arguments.of(new ChunkPos(1, 0), 0x0000000000000001L),
        Arguments.of(new ChunkPos(0, 1), 0x0000000100000000L),
        Arguments.of(new ChunkPos(-1, -1), 0xFFFFFFFFFFFFFFFFL),
        Arguments.of(new ChunkPos(1, -1), 0xFFFFFFFF00000001L),
        Arguments.of(new ChunkPos(-1, 1), 0x00000001FFFFFFFFL),
        Arguments.of(new ChunkPos(1875000, -1875000), 0xFFE363C8001C9C38L),
        Arguments.of(new ChunkPos(Integer.MIN_VALUE, Integer.MAX_VALUE), 0x7FFFFFFF80000000L));
  }

  // spec: core-math R30 (S), vector V6: x in the low 32 bits taken unsigned, z in the high bits
  @ParameterizedTest(name = "provisional pack {0} = 0x{1}")
  @MethodSource("packedVectors")
  void provisionalPackMatchesVectors(ChunkPos chunk, long packed) {
    assertThat(chunk.pack()).isEqualTo(packed);
  }

  // spec: core-math R30 (S), vector V6: x = (int) v, z = (int) (v >>> 32)
  @ParameterizedTest(name = "provisional unpack 0x{1} = {0}")
  @MethodSource("packedVectors")
  void provisionalUnpackMatchesVectors(ChunkPos chunk, long packed) {
    assertThat(ChunkPos.unpack(packed)).isEqualTo(chunk);
  }

  // spec: core-math R30 (S): packing is a bijection between int pairs and longs
  @Test
  void provisionalPackRoundTripsInBothDirections() {
    SplittableRandom random = new SplittableRandom(31);
    for (int i = 0; i < 10_000; i++) {
      ChunkPos chunk = new ChunkPos(random.nextInt(), random.nextInt());
      assertThat(ChunkPos.unpack(chunk.pack())).isEqualTo(chunk);
      long packed = random.nextLong();
      assertThat(ChunkPos.unpack(packed).pack()).isEqualTo(packed);
    }
  }

  // records: value semantics
  @Test
  void isAValueObject() {
    ChunkPos a = new ChunkPos(-1, 30);
    ChunkPos b = new ChunkPos(-1, 30);
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(new ChunkPos(30, -1));
    assertThat(a.x()).isEqualTo(-1);
    assertThat(a.z()).isEqualTo(30);
  }
}

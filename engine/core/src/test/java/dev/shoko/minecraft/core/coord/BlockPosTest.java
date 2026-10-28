package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.SplittableRandom;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class BlockPosTest {

  // spec: core-math R27 (S, conversion C), probe P4 rows: floor of each coordinate
  @ParameterizedTest(name = "provisional containing({0}, {1}, {2}) = ({3}, {4}, {5})")
  @CsvSource({
    "-0.0001, 70, -0.0001, -1, 70, -1",
    "-0.0, 70, -0.0, 0, 70, 0",
    "15.99999, 70, 15.99999, 15, 70, 15",
    "-16.00001, 70, -16.00001, -17, 70, -17",
    "0.5, -64.5, -0.5, 0, -65, -1",
    "29999999.9, 319.999, -29999999.1, 29999999, 319, -30000000"
  })
  void provisionalContainingFloorsEachCoordinate(
      double x, double y, double z, int bx, int by, int bz) {
    assertThat(BlockPos.containing(x, y, z)).isEqualTo(new BlockPos(bx, by, bz));
  }

  // spec: core-math R28, V4: chunk is (x >> 4, z >> 4)
  @ParameterizedTest(name = "block ({0}, {1}, {2}) is in chunk ({3}, {4})")
  @CsvSource({
    "0, 0, 0, 0, 0",
    "-1, -64, -1, -1, -1",
    "15, 319, 15, 0, 0",
    "17, 63, -17, 1, -2",
    "-16, -1, 16, -1, 1",
    "-17, 0, 16, -2, 1",
    "-2147483648, 0, 2147483647, -134217728, 134217727"
  })
  void chunkUsesArithmeticShift(int x, int y, int z, int cx, int cz) {
    assertThat(new BlockPos(x, y, z).chunk()).isEqualTo(new ChunkPos(cx, cz));
  }

  // spec: core-math R25, R28, V4: section is (x >> 4, y >> 4, z >> 4)
  @ParameterizedTest(name = "block ({0}, {1}, {2}) is in section ({3}, {4}, {5})")
  @CsvSource({
    "0, 0, 0, 0, 0, 0",
    "-1, -64, -1, -1, -4, -1",
    "15, 319, 15, 0, 19, 0",
    "17, 63, -17, 1, 3, -2",
    "-16, -1, 16, -1, -1, 1",
    "0, -65, 0, 0, -5, 0"
  })
  void sectionUsesArithmeticShift(int x, int y, int z, int sx, int sy, int sz) {
    assertThat(new BlockPos(x, y, z).section()).isEqualTo(new SectionPos(sx, sy, sz));
  }

  static Stream<Arguments> packedVectors() {
    return Stream.of(
        Arguments.of(new BlockPos(0, 0, 0), 0x0000000000000000L),
        Arguments.of(new BlockPos(1, 2, 3), 0x0000004000003002L),
        Arguments.of(new BlockPos(-1, -1, -1), 0xFFFFFFFFFFFFFFFFL),
        Arguments.of(new BlockPos(-1, -64, -1), 0xFFFFFFFFFFFFFFC0L),
        Arguments.of(new BlockPos(15, 319, -17), 0x000003FFFFFEF13FL),
        Arguments.of(new BlockPos(-33554432, -2048, -33554432), 0x8000002000000800L),
        Arguments.of(new BlockPos(33554431, 2047, 33554431), 0x7FFFFFDFFFFFF7FFL),
        Arguments.of(new BlockPos(30000000, 319, -30000000), 0x7270E02363C8013FL));
  }

  // spec: core-math R29 (S), vector V5
  @ParameterizedTest(name = "provisional pack {0} = 0x{1}")
  @MethodSource("packedVectors")
  void provisionalPackMatchesVectors(BlockPos pos, long packed) {
    assertThat(pos.pack()).isEqualTo(packed);
  }

  // spec: core-math R29 (S), vector V5: decoding by arithmetic shifts restores negatives
  @ParameterizedTest(name = "provisional unpack 0x{1} = {0}")
  @MethodSource("packedVectors")
  void provisionalUnpackMatchesVectors(BlockPos pos, long packed) {
    assertThat(BlockPos.unpack(packed)).isEqualTo(pos);
  }

  // spec: core-math R29 (S), V5 last row and edge case table: out-of-range fields wrap silently
  @Test
  void provisionalOutOfRangeFieldsWrapSilently() {
    BlockPos outside = new BlockPos(33554432, 2048, -33554433);
    assertThat(outside.pack()).isEqualTo(0x8000001FFFFFF800L);
    assertThat(BlockPos.unpack(outside.pack())).isEqualTo(new BlockPos(-33554432, -2048, 33554431));
  }

  // spec: core-math R29 (S): unpack(pack(p)) == p for x, z in [-2^25, 2^25 - 1], y in
  // [-2048, 2047]; seeded sample plus every corner of the representable box
  @Test
  void provisionalPackRoundTripsOverTheRepresentableRange() {
    int[] xs = {-33554432, -33554431, -30000000, -1, 0, 1, 29999999, 33554431};
    int[] ys = {-2048, -2047, -65, -64, -1, 0, 319, 320, 2047};
    for (int x : xs) {
      for (int y : ys) {
        for (int z : xs) {
          BlockPos pos = new BlockPos(x, y, z);
          assertThat(BlockPos.unpack(pos.pack())).isEqualTo(pos);
        }
      }
    }
    SplittableRandom random = new SplittableRandom(29);
    for (int i = 0; i < 10_000; i++) {
      BlockPos pos =
          new BlockPos(
              random.nextInt(-33554432, 33554432),
              random.nextInt(-2048, 2048),
              random.nextInt(-33554432, 33554432));
      assertThat(BlockPos.unpack(pos.pack())).isEqualTo(pos);
    }
  }

  // spec: core-math R29 (S): every long decodes to a position that packs back to the same long
  @Test
  void provisionalEveryLongIsACanonicalPacking() {
    SplittableRandom random = new SplittableRandom(30);
    for (int i = 0; i < 10_000; i++) {
      long packed = random.nextLong();
      assertThat(BlockPos.unpack(packed).pack()).isEqualTo(packed);
    }
  }

  // records: value semantics
  @Test
  void isAValueObject() {
    BlockPos a = new BlockPos(-1, -64, 16);
    BlockPos b = new BlockPos(-1, -64, 16);
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(new BlockPos(-1, -64, 17));
    assertThat(a.x()).isEqualTo(-1);
    assertThat(a.y()).isEqualTo(-64);
    assertThat(a.z()).isEqualTo(16);
  }
}

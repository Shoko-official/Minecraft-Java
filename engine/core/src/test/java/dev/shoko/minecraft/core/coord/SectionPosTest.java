package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.SplittableRandom;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// spec: core-math R31 is S: network format, to be confirmed by a capture in M8.
class SectionPosTest {

  static Stream<Arguments> packedVectors() {
    return Stream.of(
        Arguments.of(new SectionPos(0, 0, 0), 0x0000000000000000L),
        Arguments.of(new SectionPos(-1, -4, -1), 0xFFFFFFFFFFFFFFFCL),
        Arguments.of(new SectionPos(1, 19, -2), 0x000007FFFFE00013L),
        Arguments.of(new SectionPos(-2097152, -524288, 2097151), 0x800001FFFFF80000L));
  }

  // spec: core-math R31 (S), vector V7: x on 22 bits << 42, z on 22 bits << 20, y on 20 bits
  @ParameterizedTest(name = "provisional pack {0} = 0x{1}")
  @MethodSource("packedVectors")
  void provisionalPackMatchesVectors(SectionPos pos, long packed) {
    assertThat(pos.pack()).isEqualTo(packed);
  }

  // spec: core-math R31 (S), vector V7: fields are sign-extended on decoding
  @ParameterizedTest(name = "provisional unpack 0x{1} = {0}")
  @MethodSource("packedVectors")
  void provisionalUnpackMatchesVectors(SectionPos pos, long packed) {
    assertThat(SectionPos.unpack(packed)).isEqualTo(pos);
  }

  // spec: core-math R31 (S): round trip over x, z in [-2^21, 2^21 - 1], y in [-2^19, 2^19 - 1]
  @Test
  void provisionalPackRoundTripsOverTheRepresentableRange() {
    int[] horizontal = {-2097152, -2097151, -1875000, -1, 0, 1, 1874999, 2097151};
    int[] vertical = {-524288, -524287, -5, -4, -1, 0, 19, 20, 524287};
    for (int x : horizontal) {
      for (int y : vertical) {
        for (int z : horizontal) {
          SectionPos pos = new SectionPos(x, y, z);
          assertThat(SectionPos.unpack(pos.pack())).isEqualTo(pos);
        }
      }
    }
    SplittableRandom random = new SplittableRandom(32);
    for (int i = 0; i < 10_000; i++) {
      SectionPos pos =
          new SectionPos(
              random.nextInt(-2097152, 2097152),
              random.nextInt(-524288, 524288),
              random.nextInt(-2097152, 2097152));
      assertThat(SectionPos.unpack(pos.pack())).isEqualTo(pos);
    }
  }

  // records: value semantics
  @Test
  void isAValueObject() {
    SectionPos a = new SectionPos(-1, -4, 2);
    SectionPos b = new SectionPos(-1, -4, 2);
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(new SectionPos(2, -4, -1));
    assertThat(a.x()).isEqualTo(-1);
    assertThat(a.y()).isEqualTo(-4);
    assertThat(a.z()).isEqualTo(2);
  }
}

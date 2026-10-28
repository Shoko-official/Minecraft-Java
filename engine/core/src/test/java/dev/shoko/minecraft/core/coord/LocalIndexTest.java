package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// spec: core-math R32 is S: three distinct orders, never interchangeable.
class LocalIndexTest {

  // spec: core-math R32 (S), vector V4 columns YZX, 0ZYX, XZY
  @ParameterizedTest(name = "provisional block ({0}, {1}, {2}): YZX {3}, 0ZYX {4}, XZY {5}")
  @CsvSource({
    "0, 0, 0, 0, 0, 0",
    "-1, -64, -1, 255, 3855, 4080",
    "15, 319, 15, 4095, 4095, 4095",
    "17, 63, -17, 4081, 4081, 511",
    "-16, -1, 16, 3840, 240, 15"
  })
  void provisionalOrdersMatchVectors(int x, int y, int z, int yzx, int zyx, int xzy) {
    assertThat(LocalIndex.sectionStateIndex(x, y, z)).isEqualTo(yzx);
    assertThat(LocalIndex.postProcessingIndex(x, y, z)).isEqualTo(zyx);
    assertThat(LocalIndex.sectionUpdateIndex(x, y, z)).isEqualTo(xzy);
  }

  // spec: core-math R32 (S): one unit step on each axis moves the index by the axis weight
  @Test
  void provisionalAxisWeightsDifferPerOrder() {
    assertThat(LocalIndex.sectionStateIndex(1, 0, 0)).isEqualTo(1);
    assertThat(LocalIndex.sectionStateIndex(0, 0, 1)).isEqualTo(16);
    assertThat(LocalIndex.sectionStateIndex(0, 1, 0)).isEqualTo(256);
    assertThat(LocalIndex.postProcessingIndex(1, 0, 0)).isEqualTo(1);
    assertThat(LocalIndex.postProcessingIndex(0, 1, 0)).isEqualTo(16);
    assertThat(LocalIndex.postProcessingIndex(0, 0, 1)).isEqualTo(256);
    assertThat(LocalIndex.sectionUpdateIndex(0, 1, 0)).isEqualTo(1);
    assertThat(LocalIndex.sectionUpdateIndex(0, 0, 1)).isEqualTo(16);
    assertThat(LocalIndex.sectionUpdateIndex(1, 0, 0)).isEqualTo(256);
  }

  // spec: core-math R32 (S): each order maps the 4096 local positions one to one onto 0..4095
  @Test
  void provisionalEachOrderIsABijectionOnTheSection() {
    Set<Integer> yzx = new HashSet<>();
    Set<Integer> zyx = new HashSet<>();
    Set<Integer> xzy = new HashSet<>();
    for (int x = 0; x < 16; x++) {
      for (int y = 0; y < 16; y++) {
        for (int z = 0; z < 16; z++) {
          yzx.add(LocalIndex.sectionStateIndex(x, y, z));
          zyx.add(LocalIndex.postProcessingIndex(x, y, z));
          xzy.add(LocalIndex.sectionUpdateIndex(x, y, z));
        }
      }
    }
    assertThat(yzx).hasSize(4096).allSatisfy(i -> assertThat(i).isBetween(0, 4095));
    assertThat(zyx).hasSize(4096).allSatisfy(i -> assertThat(i).isBetween(0, 4095));
    assertThat(xzy).hasSize(4096).allSatisfy(i -> assertThat(i).isBetween(0, 4095));
  }

  // spec: core-math R28, R32 (S): absolute coordinates are masked with & 15, negatives included
  @Test
  void provisionalAbsoluteCoordinatesAreMaskedFirst() {
    int[] offsets = {-32, -16, 0, 16, Integer.MIN_VALUE, 1 << 30};
    for (int dx : offsets) {
      for (int dy : offsets) {
        for (int dz : offsets) {
          assertThat(LocalIndex.sectionStateIndex(3 + dx, 5 + dy, 7 + dz)).isEqualTo(0x573);
          assertThat(LocalIndex.postProcessingIndex(3 + dx, 5 + dy, 7 + dz)).isEqualTo(0x753);
          assertThat(LocalIndex.sectionUpdateIndex(3 + dx, 5 + dy, 7 + dz)).isEqualTo(0x375);
        }
      }
    }
  }
}

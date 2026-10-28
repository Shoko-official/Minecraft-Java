package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CoordinatesTest {

  // spec: core-math R9, R28, vector V3 (block to section and local, quart)
  @ParameterizedTest(name = "block {0}: section {1}, local {2}, quart {3}")
  @CsvSource({
    "-2147483648, -134217728, 0, -536870912",
    "-30000000, -1875000, 0, -7500000",
    "-513, -33, 15, -129",
    "-512, -32, 0, -128",
    "-511, -32, 1, -128",
    "-17, -2, 15, -5",
    "-16, -1, 0, -4",
    "-15, -1, 1, -4",
    "-1, -1, 15, -1",
    "0, 0, 0, 0",
    "1, 0, 1, 0",
    "15, 0, 15, 3",
    "16, 1, 0, 4",
    "17, 1, 1, 4",
    "31, 1, 15, 7",
    "32, 2, 0, 8",
    "511, 31, 15, 127",
    "512, 32, 0, 128",
    "513, 32, 1, 128",
    "29999999, 1874999, 15, 7499999",
    "2147483647, 134217727, 15, 536870911"
  })
  void negativeBlocksRoundTowardsNegativeInfinity(int block, int section, int local, int quart) {
    assertThat(Coordinates.blockToSection(block)).isEqualTo(section);
    assertThat(Coordinates.sectionLocal(block)).isEqualTo(local);
    assertThat(Coordinates.blockToQuart(block)).isEqualTo(quart);
  }

  // spec: core-math R28, R33, vector V3 columns v >> 5, v & 31, v >> 9
  @ParameterizedTest(name = "value {0}: >> 5 = {1}, & 31 = {2}, block region {3}")
  @CsvSource({
    "-2147483648, -67108864, 0, -4194304",
    "-30000000, -937500, 0, -58594",
    "-513, -17, 31, -2",
    "-512, -16, 0, -1",
    "-511, -16, 1, -1",
    "-33, -2, 31, -1",
    "-32, -1, 0, -1",
    "-17, -1, 15, -1",
    "-16, -1, 16, -1",
    "-1, -1, 31, -1",
    "0, 0, 0, 0",
    "16, 0, 16, 0",
    "31, 0, 31, 0",
    "32, 1, 0, 0",
    "511, 15, 31, 0",
    "512, 16, 0, 1",
    "29999999, 937499, 31, 58593",
    "2147483647, 67108863, 31, 4194303"
  })
  void regionConversionsRoundTowardsNegativeInfinity(
      int v, int region, int local, int blockRegion) {
    assertThat(Coordinates.chunkToRegion(v)).isEqualTo(region);
    assertThat(Coordinates.regionLocal(v)).isEqualTo(local);
    assertThat(Coordinates.blockToRegion(v)).isEqualTo(blockRegion);
  }

  // spec: core-math R28 edge cases: x = 511 / 512 / -513 are regions 0 / 1 / -2 in blocks
  @Test
  void blockRegionBoundaries() {
    assertThat(Coordinates.blockToRegion(511)).isZero();
    assertThat(Coordinates.blockToRegion(512)).isEqualTo(1);
    assertThat(Coordinates.blockToRegion(-512)).isEqualTo(-1);
    assertThat(Coordinates.blockToRegion(-513)).isEqualTo(-2);
  }

  // spec: core-math R28, V3: chunk to first block, overflow beyond 2^27 is silent
  @ParameterizedTest(name = "sectionToBlock({0}) = {1}")
  @CsvSource({
    "0, 0",
    "1, 16",
    "-1, -16",
    "-2, -32",
    "1875000, 30000000",
    "-1875000, -30000000",
    "134217728, -2147483648"
  })
  void sectionToBlockShiftsLeftByFour(int section, int block) {
    assertThat(Coordinates.sectionToBlock(section)).isEqualTo(block);
  }

  // spec: core-math R9: v >> n == floorDiv(v, 2^n) and v & (2^n - 1) == floorMod(v, 2^n)
  @Test
  void shiftsAndMasksEqualFloorDivisionOnDenseSampleAndExtremes() {
    List<String> mismatches = new ArrayList<>();
    sample()
        .forEach(
            v -> {
              compare(
                  "blockToSection",
                  v,
                  Coordinates::blockToSection,
                  Math.floorDiv(v, 16),
                  mismatches);
              compare(
                  "sectionLocal", v, Coordinates::sectionLocal, Math.floorMod(v, 16), mismatches);
              compare(
                  "blockToQuart", v, Coordinates::blockToQuart, Math.floorDiv(v, 4), mismatches);
              compare(
                  "chunkToRegion", v, Coordinates::chunkToRegion, Math.floorDiv(v, 32), mismatches);
              compare("regionLocal", v, Coordinates::regionLocal, Math.floorMod(v, 32), mismatches);
              compare(
                  "blockToRegion",
                  v,
                  Coordinates::blockToRegion,
                  Math.floorDiv(v, 512),
                  mismatches);
            });
    assertThat(mismatches).isEmpty();
  }

  /** Every int in [-2^17, 2^17], a stride over the whole range, and the extremes. */
  private static IntStream sample() {
    IntStream dense = IntStream.rangeClosed(-(1 << 17), 1 << 17);
    IntStream strided =
        IntStream.iterate(Integer.MIN_VALUE, v -> v <= Integer.MAX_VALUE - 65_521, v -> v + 65_521);
    IntStream extremes =
        IntStream.of(
            Integer.MIN_VALUE,
            Integer.MIN_VALUE + 1,
            Integer.MIN_VALUE + 15,
            Integer.MIN_VALUE + 16,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE - 1,
            Integer.MAX_VALUE - 15,
            -30_000_000,
            -30_000_001,
            29_999_999,
            30_000_000);
    return IntStream.concat(IntStream.concat(dense, strided), extremes);
  }

  private static void compare(
      String name, int v, IntUnaryOperator actual, int expected, List<String> mismatches) {
    int result = actual.applyAsInt(v);
    if (result != expected && mismatches.size() < 20) {
      mismatches.add(name + "(" + v + ") = " + result + ", expected " + expected);
    }
  }

  // spec: core-math R26 (S): provisional domain [-30,000,000, 30,000,000)
  @ParameterizedTest(name = "provisional isWithinHorizontalLimit({0}) = {1}")
  @CsvSource({
    "-30000000, true",
    "-30000001, false",
    "29999999, true",
    "30000000, false",
    "0, true",
    "29999984, true",
    "-2147483648, false",
    "2147483647, false"
  })
  void provisionalHorizontalLimitIsHalfOpen(int block, boolean within) {
    assertThat(Coordinates.isWithinHorizontalLimit(block)).isEqualTo(within);
  }

  // spec: core-math R26 (S)
  @Test
  void provisionalHorizontalLimitIsThirtyMillion() {
    assertThat(Coordinates.HORIZONTAL_LIMIT).isEqualTo(30_000_000);
  }
}

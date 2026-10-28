package dev.shoko.minecraft.core.math;

import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameDouble;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class RoundingTest {

  // spec: core-math R4, R6, vectors V1 (in-domain rows, C)
  @ParameterizedTest(name = "floorToInt({0}) = {1}, ceilToInt({0}) = {2}")
  @CsvSource({
    "0.0, 0, 0",
    "-0.0, 0, 0",
    "0.5, 0, 1",
    "-0.5, -1, 0",
    "-1.0, -1, -1",
    "15.999999999999998, 15, 16",
    "-15.000000000000002, -16, -15",
    "-1e-300, -1, 0",
    "29999999.9, 29999999, 30000000",
    "-29999999.1, -30000000, -29999999",
    "16.0, 16, 16",
    "-16.0, -16, -16",
    "-17.5, -18, -17",
    "2147483647.0, 2147483647, 2147483647",
    "-2147483648.0, -2147483648, -2147483648"
  })
  void doubleRoundingGoesTowardsInfinitiesNotZero(double x, int floor, int ceil) {
    assertThat(Rounding.floorToInt(x)).isEqualTo(floor);
    assertThat(Rounding.ceilToInt(x)).isEqualTo(ceil);
  }

  // spec: core-math R4, R6, vectors V1 (out-of-domain rows, S): provisional candidates F1 and C1
  @ParameterizedTest(name = "provisional floorToInt({0}) = {1}, ceilToInt({0}) = {2}")
  @CsvSource({
    "2147483647.5, 2147483647, 2147483647",
    "-2147483648.5, -2147483648, -2147483648",
    "1e20, 2147483647, 2147483647",
    "-1e20, -2147483648, -2147483648",
    "NaN, 0, 0",
    "Infinity, 2147483647, 2147483647",
    "-Infinity, -2147483648, -2147483648"
  })
  void provisionalOutOfDomainDoubleRoundingSaturatesAndMapsNanToZero(
      double x, int floor, int ceil) {
    assertThat(Rounding.floorToInt(x)).isEqualTo(floor);
    assertThat(Rounding.ceilToInt(x)).isEqualTo(ceil);
  }

  static Stream<Arguments> floatFloorVectors() {
    return Stream.of(
        Arguments.of(0x80000000, 0),
        Arguments.of(0xbf000000, -1),
        Arguments.of(0x8da24260, -1),
        Arguments.of(0x3f7fffff, 0),
        Arguments.of(0xbf7fffff, -1),
        Arguments.of(0x4b800000, 16777216),
        Arguments.of(0xcb800000, -16777216));
  }

  // spec: core-math R5, vectors V2 (bits given as in the spec)
  @ParameterizedTest(name = "floorToInt(float bits 0x{0}) = {1}")
  @MethodSource("floatFloorVectors")
  void floatFloorMatchesVectors(int bits, int expected) {
    assertThat(Rounding.floorToInt(Float.intBitsToFloat(bits))).isEqualTo(expected);
  }

  // spec: core-math R4, R5, vector V2 (S): provisional NaN gives 0, F1 saturates
  @Test
  void provisionalOutOfDomainFloatFloorSaturatesAndMapsNanToZero() {
    assertThat(Rounding.floorToInt(Float.NaN)).isZero();
    assertThat(Rounding.floorToInt(-3.0E9f)).isEqualTo(Integer.MIN_VALUE);
    assertThat(Rounding.floorToInt(Float.POSITIVE_INFINITY)).isEqualTo(Integer.MAX_VALUE);
  }

  // spec: core-math R5, V2: 0.99999999 rounds to 1.0f before the float floor
  @Test
  void floatVariantFloorsTheValueAlreadyRoundedToFloat() {
    double nearlyOne = 0.99999999;
    assertThat(Rounding.floorToInt(nearlyOne)).isZero();
    assertThat(Rounding.floorToInt((float) nearlyOne)).isEqualTo(1);
    assertThat(Rounding.ceilToInt((float) nearlyOne)).isEqualTo(1);
  }

  // spec: core-math R6 applied to the float variant (R5), derived from the definition
  @Test
  void floatCeilingGoesTowardsPositiveInfinity() {
    assertThat(Rounding.ceilToInt(-0.0f)).isZero();
    assertThat(Rounding.ceilToInt(-0.5f)).isZero();
    assertThat(Rounding.ceilToInt(Float.intBitsToFloat(0x3f7fffff))).isEqualTo(1);
    assertThat(Rounding.ceilToInt(Float.intBitsToFloat(0xbf7fffff))).isZero();
    assertThat(Rounding.ceilToInt(-1.0E-30f)).isZero();
    assertThat(Rounding.ceilToInt(1.0E-30f)).isEqualTo(1);
    assertThat(Rounding.ceilToInt(16777216f)).isEqualTo(16777216);
    // provisional (S): NaN gives 0
    assertThat(Rounding.ceilToInt(Float.NaN)).isZero();
  }

  // spec: core-math R5. Java picks the float overload for an int argument (int to float is the
  // most specific widening), so an int above 2^24 is rounded to float before the floor. The spec
  // does not say whether callers may do this; these tests pin the hazard so it stays visible.
  @Test
  void intArgumentSelectsTheFloatOverloadAndLosesPrecisionAbove2To24() {
    int justAbove2To24 = 16_777_217;
    assertThat(Rounding.floorToInt(justAbove2To24)).isEqualTo(16_777_216);
    assertThat(Rounding.floorToInt((double) justAbove2To24)).isEqualTo(16_777_217);
    int roundsUpInFloat = 33_554_435;
    assertThat(Rounding.floorToInt(roundsUpInFloat)).isEqualTo(33_554_436);
    assertThat(Rounding.floorToInt((double) roundsUpInFloat)).isEqualTo(33_554_435);
  }

  // spec: core-math R7, domain of R4 transposed to long
  @ParameterizedTest(name = "floorToLong({0}) = {1}")
  @CsvSource({
    "0.0, 0",
    "-0.0, 0",
    "-0.5, -1",
    "-1e-300, -1",
    "2147483647.5, 2147483647",
    "-2147483648.5, -2147483649",
    "4.5E15, 4500000000000000",
    "-4.5E15, -4500000000000000",
    "-4.5000000000000005E15, -4500000000000001"
  })
  void floorToLongRoundsTowardsNegativeInfinity(double x, long expected) {
    assertThat(Rounding.floorToLong(x)).isEqualTo(expected);
  }

  // spec: core-math R7 with R4 provisional (S): saturation, NaN gives 0
  @Test
  void provisionalOutOfDomainFloorToLongSaturatesAndMapsNanToZero() {
    assertThat(Rounding.floorToLong(1e20)).isEqualTo(Long.MAX_VALUE);
    assertThat(Rounding.floorToLong(-1e20)).isEqualTo(Long.MIN_VALUE);
    assertThat(Rounding.floorToLong(Double.NaN)).isZero();
  }

  // spec: core-math R7, vector V10
  @ParameterizedTest(name = "fractionalPart({0}) = {1}")
  // 3.00000000025E9 is above Integer.MAX_VALUE: the floor must be taken in long (R7)
  @CsvSource({
    "-0.25, 0.75",
    "2.75, 0.75",
    "0.0, 0.0",
    "-3.0, 0.0",
    "-1e-20, 1.0",
    "3.00000000025E9, 0.25",
    "-3.00000000025E9, 0.75"
  })
  void fractionalPartIsMeasuredFromTheFloor(double x, double expected) {
    assertSameDouble(Rounding.fractionalPart(x), expected);
  }

  // spec: core-math R7: the rounded result can reach 1 exactly
  @Test
  void fractionalPartOfATinyNegativeIsExactlyOne() {
    assertThat(Rounding.fractionalPart(-1e-20)).isEqualTo(1.0);
  }
}

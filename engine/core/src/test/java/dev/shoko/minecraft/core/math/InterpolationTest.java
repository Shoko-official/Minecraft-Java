package dev.shoko.minecraft.core.math;

import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameDouble;
import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameFloat;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.math.Interpolation.CubeCorners;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InterpolationTest {

  // spec: core-math R11, vector V11, rows where forms A and B agree
  @ParameterizedTest(name = "interpolate({0}, {1}, {2}) = {3}")
  @CsvSource({
    "0, 1, 2, 1.0",
    "1, 1, 2, 2.0",
    "0.5, 1, 2, 1.5",
    "1.5, 0, 10, 15.0",
    "-0.5, 0, 10, -5.0"
  })
  void doubleInterpolationExtrapolatesWithoutClamping(double t, double a, double b, double r) {
    assertSameDouble(Interpolation.interpolate(t, a, b), r);
  }

  // spec: core-math R11 (S), vector V11 column A: provisional form a + t * (b - a)
  @ParameterizedTest(name = "provisional form A interpolate({0}, {1}, {2}) = {3}")
  @CsvSource({"1, 1e20, 1, 0.0", "0.3, -64, 320, 51.19999999999999", "0.3, -1.5, 3.5, 0.0"})
  void provisionalDoubleInterpolationUsesFormA(double t, double a, double b, double r) {
    assertSameDouble(Interpolation.interpolate(t, a, b), r);
  }

  // spec: core-math R11 (S), V11: bits 0x4049999999999998, form B would give 0x404999999999999a
  @Test
  void provisionalFormADoesNotRoundLikeFormB() {
    assertThat(Double.doubleToRawLongBits(Interpolation.interpolate(0.3, -64.0, 320.0)))
        .isEqualTo(0x4049999999999998L);
  }

  // spec: core-math R11 (S), vector V11 float rows: provisional form A computed in float
  @Test
  void provisionalFloatInterpolationUsesFormAInFloat() {
    assertSameFloat(Interpolation.interpolate(1f, 1e20f, 1f), 0f);
    assertSameFloat(Interpolation.interpolate(0.1f, 0.1f, 0.3f), Float.intBitsToFloat(0x3df5c290));
    assertSameFloat(Interpolation.interpolate(0.3f, -64f, 320f), Float.intBitsToFloat(0x424cccce));
    assertSameFloat(Interpolation.interpolate(0.3f, -1.5f, 3.5f), 0f);
  }

  // spec: core-math R11: float and double overloads are not interchangeable
  @Test
  void floatAndDoubleInterpolationDiffer() {
    float inFloat = Interpolation.interpolate(0.3f, -64f, 320f);
    double inDouble = Interpolation.interpolate(0.3, -64.0, 320.0);
    assertThat((double) inFloat).isNotEqualTo(inDouble);
  }

  // spec: core-math R12 (S), V11: provisional x first then y, each by form A
  @Test
  void provisionalBilinearInterpolatesAlongXThenY() {
    double result = Interpolation.bilinear(0.3, 0.7, 0.1, -0.9, 0.35, 0.77);
    assertThat(Double.doubleToRawLongBits(result)).isEqualTo(0x3fd17c1bda5119ccL);
  }

  // spec: core-math R12 (S): the corners follow the v<x><y> naming
  @Test
  void provisionalBilinearReturnsEachCornerAtItsParameters() {
    assertSameDouble(Interpolation.bilinear(0, 0, 1, 2, 3, 4), 1.0);
    assertSameDouble(Interpolation.bilinear(1, 0, 1, 2, 3, 4), 2.0);
    assertSameDouble(Interpolation.bilinear(0, 1, 1, 2, 3, 4), 3.0);
    assertSameDouble(Interpolation.bilinear(1, 1, 1, 2, 3, 4), 4.0);
  }

  // spec: core-math R12 (S), V11: corners in the order 000, 100, 010, 110, 001, 101, 011, 111
  @Test
  void provisionalTrilinearMatchesVector() {
    CubeCorners corners = new CubeCorners(1, 2, 3, 4, 5, 6, 7, 8);
    assertSameDouble(Interpolation.trilinear(0.25, 0.5, 0.75, corners), 5.25);
  }

  // spec: core-math R12 (S): each corner is reached at its own parameters
  @Test
  void provisionalTrilinearReturnsEachCornerAtItsParameters() {
    CubeCorners corners = new CubeCorners(1, 2, 3, 4, 5, 6, 7, 8);
    assertSameDouble(Interpolation.trilinear(0, 0, 0, corners), 1.0);
    assertSameDouble(Interpolation.trilinear(1, 0, 0, corners), 2.0);
    assertSameDouble(Interpolation.trilinear(0, 1, 0, corners), 3.0);
    assertSameDouble(Interpolation.trilinear(1, 1, 0, corners), 4.0);
    assertSameDouble(Interpolation.trilinear(0, 0, 1, corners), 5.0);
    assertSameDouble(Interpolation.trilinear(1, 0, 1, corners), 6.0);
    assertSameDouble(Interpolation.trilinear(0, 1, 1, corners), 7.0);
    assertSameDouble(Interpolation.trilinear(1, 1, 1, corners), 8.0);
  }

  // spec: core-math R13 (S), V11: provisional (v - a) / (b - a), no clamping or guard
  @ParameterizedTest(name = "provisional inverseInterpolate({0}, {1}, {2}) = {3}")
  @CsvSource({
    "5, 0, 10, 0.5",
    "-5, 0, 10, -0.5",
    "5, 10, 0, 0.5",
    "1, 1, 1, NaN",
    "2, 1, 1, Infinity",
    "0, 1, 1, -Infinity"
  })
  void provisionalInverseInterpolateHasNoGuard(double v, double a, double b, double r) {
    assertSameDouble(Interpolation.inverseInterpolate(v, a, b), r);
  }

  // spec: core-math R14 (S), V11: provisional composition, not clamped
  @Test
  void provisionalRemapComposesWithoutClamping() {
    assertSameDouble(Interpolation.remap(0.5, 0, 1, -64, 320), 128.0);
    assertSameDouble(Interpolation.remap(25, 10, 20, 0, 100), 150.0);
  }

  // spec: core-math R14 (S), V11: clamped between min(b0, b1) and max(b0, b1)
  @Test
  void provisionalRemapClampedStaysInTargetRange() {
    assertSameDouble(Interpolation.remapClamped(25, 10, 20, 0, 100), 100.0);
    assertSameDouble(Interpolation.remapClamped(5, 10, 20, 0, 100), 0.0);
    assertSameDouble(Interpolation.remapClamped(15, 10, 20, 0, 100), 50.0);
    assertSameDouble(Interpolation.remapClamped(25, 10, 20, 100, 0), 0.0);
    assertSameDouble(Interpolation.remapClamped(5, 10, 20, 100, 0), 100.0);
  }

  // spec: core-math R14 (S), V11
  @ParameterizedTest(name = "provisional clampedInterpolate({0}, 0, 10) = {1}")
  @CsvSource({"-0.5, 0.0", "1.5, 10.0", "0.5, 5.0", "0, 0.0", "1, 10.0"})
  void provisionalClampedInterpolateReturnsEndpointsOutsideUnitRange(double t, double r) {
    assertSameDouble(Interpolation.clampedInterpolate(t, 0, 10), r);
  }

  // spec: core-math R14 (S): outside [0, 1] the endpoints are returned as given, not recomputed
  @Test
  void provisionalClampedInterpolateReturnsTheEndpointItself() {
    assertSameDouble(Interpolation.clampedInterpolate(-1, -0.0, 5), -0.0);
    assertSameDouble(Interpolation.clampedInterpolate(2, 1e20, 1), 1.0);
  }

  // spec: core-math R16 (S), vector V13 column Horner (provisional)
  @ParameterizedTest(name = "provisional quinticFade({0}) = {1}")
  @CsvSource({
    "0, 0.0",
    "0.1, 0.008560000000000003",
    "0.25, 0.103515625",
    "0.5, 0.5",
    "0.75, 0.896484375",
    "0.9, 0.9914399999999998",
    "1, 1.0",
    "-0.5, -2.375",
    "1.5, 3.375"
  })
  void provisionalQuinticFadeUsesHornerForm(double t, double expected) {
    assertSameDouble(Interpolation.quinticFade(t), expected);
  }

  // spec: core-math R16 (S), V13: bits that separate Horner from the other writings
  @Test
  void provisionalQuinticFadeBitsMatchHorner() {
    assertThat(Double.doubleToRawLongBits(Interpolation.quinticFade(0.1)))
        .isEqualTo(0x3f8187e7c06e19bbL);
    assertThat(Double.doubleToRawLongBits(Interpolation.quinticFade(0.9)))
        .isEqualTo(0x3fefb9e060fe4797L);
  }

  // spec: core-math R12 (records are generated members): corner record equality
  @Test
  void cubeCornersAreValueObjects() {
    assertThat(new CubeCorners(1, 2, 3, 4, 5, 6, 7, 8))
        .isEqualTo(new CubeCorners(1, 2, 3, 4, 5, 6, 7, 8))
        .hasSameHashCodeAs(new CubeCorners(1, 2, 3, 4, 5, 6, 7, 8))
        .isNotEqualTo(new CubeCorners(1, 2, 3, 4, 5, 6, 7, 9));
  }
}

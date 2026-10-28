package dev.shoko.minecraft.core.math;

import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameDouble;
import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameFloat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// spec: core-math R18 and R20 are S: StrictMath.atan2 and the normalization formula are
// provisional until probes P18 and P10.
class AnglesTest {

  private static final double[] SAMPLES = {
    0.0,
    -0.0,
    1.0,
    -1.0,
    0.5,
    -0.5,
    3.0,
    -7.25,
    1e-300,
    -1e-300,
    1e300,
    123456.789,
    Double.MIN_VALUE,
    Double.POSITIVE_INFINITY,
    Double.NEGATIVE_INFINITY,
    Double.NaN
  };

  // spec: core-math R18 (S), R3: provisional StrictMath.atan2, bit for bit, signed zeros included
  @Test
  void provisionalAtan2IsStrictMathAtan2() {
    for (double y : SAMPLES) {
      for (double x : SAMPLES) {
        assertSameDouble(Angles.atan2(y, x), StrictMath.atan2(y, x));
      }
    }
  }

  // spec: core-math R18 (S): argument order is (y, x)
  @Test
  void provisionalAtan2TakesOrdinateFirst() {
    assertSameDouble(Angles.atan2(1.0, 0.0), StrictMath.PI / 2);
    assertSameDouble(Angles.atan2(0.0, -1.0), StrictMath.PI);
    assertSameDouble(Angles.atan2(-0.0, -1.0), -StrictMath.PI);
  }

  // spec: core-math R20 (S), vector V15 (double)
  @ParameterizedTest(name = "provisional normalizeDegrees({0}) = {1}")
  @CsvSource({
    "0.0, 0.0",
    "179.9, 179.9",
    "180.0, -180.0",
    "-180.0, -180.0",
    "-180.1, 179.9",
    "359.5, -0.5",
    "540.0, -180.0",
    "720.0, 0.0",
    "-190.0, 170.0",
    "-0.0, -0.0",
    "1e10, -80.0",
    "NaN, NaN"
  })
  void provisionalDoubleNormalizationMatchesVectors(double degrees, double expected) {
    assertSameDouble(Angles.normalizeDegrees(degrees), expected);
  }

  // spec: core-math R20 (S): same formula in float precision, values derived from it
  @ParameterizedTest(name = "provisional normalizeDegrees({0}f) = {1}f")
  @CsvSource({
    "0.0, 0.0",
    "180.0, -180.0",
    "-180.0, -180.0",
    "-180.1, 179.9",
    "359.5, -0.5",
    "540.0, -180.0",
    "-190.0, 170.0",
    "-0.0, -0.0",
    "1e10, -80.0",
    "NaN, NaN"
  })
  void provisionalFloatNormalizationMatchesFormula(float degrees, float expected) {
    assertSameFloat(Angles.normalizeDegrees(degrees), expected);
  }

  // spec: core-math R20 (S): same formula in int precision, values derived from it
  @ParameterizedTest(name = "provisional normalizeDegrees({0}) = {1}")
  @CsvSource({
    "0, 0",
    "179, 179",
    "180, -180",
    "-180, -180",
    "-181, 179",
    "359, -1",
    "540, -180",
    "720, 0",
    "-190, 170",
    "-2147483648, -128",
    "2147483647, 127"
  })
  void provisionalIntNormalizationMatchesFormula(int degrees, int expected) {
    assertThat(Angles.normalizeDegrees(degrees)).isEqualTo(expected);
  }

  // spec: core-math R20 (S): the result lies in [-180, 180) for every finite double
  @Test
  void provisionalNormalizedAngleIsInHalfOpenRange() {
    for (int tenths = -10_000; tenths <= 10_000; tenths++) {
      double normalized = Angles.normalizeDegrees(tenths / 10.0 * 7);
      assertThat(normalized).isGreaterThanOrEqualTo(-180.0).isLessThan(180.0);
    }
  }
}

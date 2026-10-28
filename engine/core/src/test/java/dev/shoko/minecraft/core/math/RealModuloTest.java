package dev.shoko.minecraft.core.math;

import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameDouble;
import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameFloat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// spec: core-math R10 is S: both functions are internal contracts, tested as provisional.
class RealModuloTest {

  // spec: core-math R10, vector V10, provisional realModFloor = a - floor(a / b) * b
  @ParameterizedTest(name = "provisional realModFloor({0}, {1}) = {2}")
  @CsvSource({
    "-1e-20, 360, 360.0",
    "-1, 360, 359.0",
    "370, 360, 10.0",
    "-720, 360, 0.0",
    "-0.0, 360, 0.0",
    "5.5, -2, -0.5",
    "-1e-14, 1, 0.99999999999999"
  })
  void provisionalRealModFloorMatchesVectors(double a, double b, double expected) {
    assertSameDouble(RealModulo.realModFloor(a, b), expected);
  }

  // spec: core-math R10, vector V10, provisional realModWrap = ((a % b) + b) % b
  @ParameterizedTest(name = "provisional realModWrap({0}, {1}) = {2}")
  @CsvSource({
    "-1e-20, 360, 0.0",
    "-1, 360, 359.0",
    "370, 360, 10.0",
    "-720, 360, 0.0",
    "-0.0, 360, 0.0",
    "5.5, -2, -0.5",
    "-1e-14, 1, 0.99999999999999"
  })
  void provisionalRealModWrapMatchesVectors(double a, double b, double expected) {
    assertSameDouble(RealModulo.realModWrap(a, b), expected);
  }

  // spec: core-math R10: the two forms differ on a tiny negative dividend and stay distinct
  @Test
  void provisionalFormsDivergeOnTinyNegativeDividend() {
    assertSameDouble(RealModulo.realModFloor(-1e-20, 360.0), 360.0);
    assertSameDouble(RealModulo.realModWrap(-1e-20, 360.0), 0.0);
  }

  // spec: core-math R10, each overload computes in the precision of its arguments; values derived
  // from the formulas evaluated in float
  @Test
  void provisionalFloatOverloadsComputeInFloat() {
    assertSameFloat(RealModulo.realModFloor(-1e-20f, 360f), 360f);
    assertSameFloat(RealModulo.realModWrap(-1e-20f, 360f), 0f);
    assertSameFloat(RealModulo.realModFloor(-1f, 360f), 359f);
    assertSameFloat(RealModulo.realModWrap(-1f, 360f), 359f);
    assertSameFloat(RealModulo.realModFloor(5.5f, -2f), -0.5f);
    assertSameFloat(RealModulo.realModWrap(5.5f, -2f), -0.5f);
  }
}

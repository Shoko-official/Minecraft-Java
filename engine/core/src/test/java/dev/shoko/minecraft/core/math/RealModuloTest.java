package dev.shoko.minecraft.core.math;

import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameDouble;
import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameFloat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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

  // spec: core-math R10 (S): float precision, not double rounded once to float. Computing in
  // double then narrowing gives 0.13833523f and 0.14161503f respectively.
  @Test
  void provisionalFloatOverloadsDoNotComputeInDouble() {
    assertSameFloat(RealModulo.realModFloor(-93950.03f, 0.74358845f), 0.140625f);
    assertSameFloat(RealModulo.realModWrap(0.14161503f, 7.8575983f), 0.14161491f);
  }

  /** The R10 floor form evaluated in pure float, operation by operation. */
  private static float floorFormInFloat(float a, float b) {
    float quotient = a / b;
    float floored = (float) StrictMath.floor(quotient);
    float product = floored * b;
    return a - product;
  }

  /** The R10 wrap form evaluated in pure float. */
  private static float wrapFormInFloat(float a, float b) {
    float remainder = a % b;
    float shifted = remainder + b;
    return shifted % b;
  }

  // spec: core-math R10 (S): dense seeded sample against both formulas written in pure float
  @Test
  @Timeout(20)
  void provisionalFloatOverloadsMatchPureFloatFormulasOnDenseSample() {
    SplittableRandom random = new SplittableRandom(10);
    List<String> mismatches = new ArrayList<>();
    for (int i = 0; i < 500_000 && mismatches.size() < 20; i++) {
      float a = (float) ((random.nextDouble() - 0.5) * Math.scalb(1.0, random.nextInt(-10, 30)));
      float b = (float) ((random.nextDouble() - 0.5) * Math.scalb(1.0, random.nextInt(-4, 12)));
      if (b == 0f) {
        continue;
      }
      float floor = RealModulo.realModFloor(a, b);
      float wrap = RealModulo.realModWrap(a, b);
      if (Float.floatToIntBits(floor) != Float.floatToIntBits(floorFormInFloat(a, b))
          || Float.floatToIntBits(wrap) != Float.floatToIntBits(wrapFormInFloat(a, b))) {
        mismatches.add(a + " mod " + b + ": floor " + floor + ", wrap " + wrap);
      }
    }
    assertThat(mismatches).isEmpty();
  }
}

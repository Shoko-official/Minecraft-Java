package dev.shoko.minecraft.core.math;

import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameDouble;
import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameFloat;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: core-math R10 (S): both literal formulas on special operands and random bit patterns.
class RealModuloAttackTest {

  private static final double[] SPECIAL = {
    0.0,
    -0.0,
    1.0,
    -1.0,
    360.0,
    -360.0,
    -1e-20,
    1e-20,
    5.5,
    -2.0,
    Double.MAX_VALUE,
    -Double.MAX_VALUE,
    Double.MIN_VALUE,
    Double.POSITIVE_INFINITY,
    Double.NEGATIVE_INFINITY,
    Double.NaN
  };

  /** Float form computed through double with a rounding to float after each operation. */
  private static float refFloorFloat(float a, float b) {
    float q = (float) ((double) a / (double) b);
    float f = (float) StrictMath.floor(q);
    float p = (float) ((double) f * (double) b);
    return (float) ((double) a - (double) p);
  }

  @Test
  void specialOperandsFollowBothLiteralFormulas() {
    for (double a : SPECIAL) {
      for (double b : SPECIAL) {
        assertSameDouble(RealModulo.realModFloor(a, b), a - StrictMath.floor(a / b) * b);
        assertSameDouble(RealModulo.realModWrap(a, b), ((a % b) + b) % b);
        float fa = (float) a;
        float fb = (float) b;
        assertSameFloat(RealModulo.realModFloor(fa, fb), refFloorFloat(fa, fb));
        assertSameFloat(RealModulo.realModWrap(fa, fb), ((fa % fb) + fb) % fb);
      }
    }
  }

  @Test
  @Timeout(20)
  void floatFloorFormDenseRandom() {
    SplittableRandom rnd = new SplittableRandom(10);
    for (int i = 0; i < 500_000; i++) {
      float a = Float.intBitsToFloat(rnd.nextInt());
      float b =
          (i & 1) == 0
              ? Float.intBitsToFloat(rnd.nextInt())
              : (float) ((rnd.nextDouble() - 0.5) * 1000);
      assertSameFloat(RealModulo.realModFloor(a, b), refFloorFloat(a, b));
    }
  }
}

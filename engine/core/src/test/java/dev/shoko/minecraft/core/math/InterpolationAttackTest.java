package dev.shoko.minecraft.core.math;

import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameDouble;
import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameFloat;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: core-math R11 to R14, R16 (S): adversarial checks against independent references.
class InterpolationAttackTest {

  private static final double[] SPECIAL = {
    0.0,
    -0.0,
    1.0,
    -1.0,
    0.5,
    2.0,
    -0.5,
    1e20,
    -1e20,
    Double.MAX_VALUE,
    -Double.MAX_VALUE,
    Double.MIN_VALUE,
    Double.POSITIVE_INFINITY,
    Double.NEGATIVE_INFINITY,
    Double.NaN,
    0x1p53,
    0.1
  };

  /** Form A with each operation rounded to float from an exact double computation. */
  private static float refFloat(float t, float a, float b) {
    float d = (float) ((double) b - (double) a);
    float m = (float) ((double) t * (double) d);
    return (float) ((double) a + (double) m);
  }

  /** Form A on finite doubles, each operation computed exactly then rounded once. */
  private static double refDouble(double t, double a, double b) {
    double d = new BigDecimal(b).subtract(new BigDecimal(a)).doubleValue();
    if (Double.isInfinite(d)) {
      return Double.NaN; // not used: callers keep operands small enough
    }
    double m = new BigDecimal(t).multiply(new BigDecimal(d)).doubleValue();
    return new BigDecimal(a).add(new BigDecimal(m), MathContext.UNLIMITED).doubleValue();
  }

  @Test
  @Timeout(20)
  void floatFormADenseRandom() {
    SplittableRandom rnd = new SplittableRandom(11);
    for (int i = 0; i < 500_000; i++) {
      float t = (float) (rnd.nextDouble() * 3 - 1);
      float a = (float) ((rnd.nextDouble() - 0.5) * Math.scalb(1.0, rnd.nextInt(-20, 40)));
      float b = (float) ((rnd.nextDouble() - 0.5) * Math.scalb(1.0, rnd.nextInt(-20, 40)));
      assertSameFloat(Interpolation.interpolate(t, a, b), refFloat(t, a, b));
    }
  }

  @Test
  @Timeout(20)
  void doubleFormADenseRandomAgainstExactArithmetic() {
    SplittableRandom rnd = new SplittableRandom(12);
    for (int i = 0; i < 50_000; i++) {
      double t = rnd.nextDouble() * 3 - 1;
      double a = (rnd.nextDouble() - 0.5) * Math.scalb(1.0, rnd.nextInt(-30, 60));
      double b = (rnd.nextDouble() - 0.5) * Math.scalb(1.0, rnd.nextInt(-30, 60));
      assertSameDouble(Interpolation.interpolate(t, a, b), refDouble(t, a, b));
    }
  }

  @Test
  void specialOperandsFollowTheLiteralFormula() {
    for (double t : SPECIAL) {
      for (double a : SPECIAL) {
        for (double b : SPECIAL) {
          assertSameDouble(Interpolation.interpolate(t, a, b), a + t * (b - a));
          float ft = (float) t;
          float fa = (float) a;
          float fb = (float) b;
          assertSameFloat(Interpolation.interpolate(ft, fa, fb), fa + ft * (fb - fa));
          double clamped = t < 0 ? a : t > 1 ? b : a + t * (b - a);
          assertSameDouble(Interpolation.clampedInterpolate(t, a, b), clamped);
          assertSameDouble(Interpolation.inverseInterpolate(t, a, b), (t - a) / (b - a));
        }
      }
    }
  }

  @Test
  @Timeout(20)
  void bilinearAndTrilinearInterpolateXThenYThenZ() {
    SplittableRandom rnd = new SplittableRandom(13);
    for (int i = 0; i < 100_000; i++) {
      double tx = rnd.nextDouble();
      double ty = rnd.nextDouble();
      double tz = rnd.nextDouble();
      double[] c = new double[8];
      for (int j = 0; j < 8; j++) {
        c[j] = rnd.nextDouble() * 2 - 1;
      }
      double x00 = c[0] + tx * (c[1] - c[0]);
      double x10 = c[2] + tx * (c[3] - c[2]);
      double x01 = c[4] + tx * (c[5] - c[4]);
      double x11 = c[6] + tx * (c[7] - c[6]);
      double y0 = x00 + ty * (x10 - x00);
      double y1 = x01 + ty * (x11 - x01);
      double expected = y0 + tz * (y1 - y0);
      assertSameDouble(
          Interpolation.trilinear(
              tx,
              ty,
              tz,
              new Interpolation.CubeCorners(c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7])),
          expected);
      assertSameDouble(Interpolation.bilinear(tx, ty, c[0], c[1], c[2], c[3]), y0);
    }
  }

  @Test
  @Timeout(20)
  void quinticFadeIsHornerLeftToRight() {
    SplittableRandom rnd = new SplittableRandom(14);
    for (int i = 0; i < 200_000; i++) {
      double t = rnd.nextDouble() * 4 - 1.5;
      double expected = ((t * t) * t) * ((t * ((t * 6.0) - 15.0)) + 10.0);
      assertSameDouble(Interpolation.quinticFade(t), expected);
    }
    assertSameDouble(Interpolation.quinticFade(0.5), 0.5);
    assertSameDouble(Interpolation.quinticFade(1.0), 1.0);
    assertSameDouble(Interpolation.quinticFade(-0.0), -0.0 * -0.0 * -0.0 * 10.0);
  }

  @Test
  @Timeout(20)
  void remapClampedStaysInTargetRangeWhicheverWayItPoints() {
    SplittableRandom rnd = new SplittableRandom(15);
    for (int i = 0; i < 200_000; i++) {
      double v = rnd.nextDouble() * 400 - 200;
      double a0 = rnd.nextDouble() * 200 - 100;
      double a1 = rnd.nextDouble() * 200 - 100;
      double b0 = rnd.nextDouble() * 200 - 100;
      double b1 = rnd.nextDouble() * 200 - 100;
      double r = Interpolation.remapClamped(v, a0, a1, b0, b1);
      assertThat(r).isBetween(Math.min(b0, b1), Math.max(b0, b1));
      double raw = b0 + ((v - a0) / (a1 - a0)) * (b1 - b0);
      assertSameDouble(Interpolation.remap(v, a0, a1, b0, b1), raw);
    }
  }
}

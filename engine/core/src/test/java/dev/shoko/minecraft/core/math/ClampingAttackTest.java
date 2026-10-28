package dev.shoko.minecraft.core.math;

import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameDouble;
import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameFloat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClampingAttackTest {

  private static final double[] D = {
    0.0,
    -0.0,
    1.0,
    -1.0,
    0.5,
    Double.MIN_VALUE,
    -Double.MIN_VALUE,
    Double.MAX_VALUE,
    -Double.MAX_VALUE,
    Double.POSITIVE_INFINITY,
    Double.NEGATIVE_INFINITY,
    Double.NaN
  };

  private static final long[] L = {
    Long.MIN_VALUE,
    Long.MIN_VALUE + 1,
    -1,
    0,
    1,
    Long.MAX_VALUE - 1,
    Long.MAX_VALUE,
    Integer.MIN_VALUE,
    Integer.MAX_VALUE
  };

  /** Form MIX written independently: v < lo ? lo : (v > hi or hi is NaN or signed-zero tie). */
  private static double mix(double v, double lo, double hi) {
    if (v < lo) {
      return lo;
    }
    if (Double.isNaN(v) || Double.isNaN(hi)) {
      return Double.NaN;
    }
    if (v > hi) {
      return hi;
    }
    if (v == hi && v == 0.0) {
      // Math.min(+0.0, -0.0) is -0.0 (Javadoc of Math.min).
      boolean negative = Double.doubleToRawLongBits(v) != 0 || Double.doubleToRawLongBits(hi) != 0;
      return negative ? -0.0 : 0.0;
    }
    return v;
  }

  @Test
  void doubleAndFloatSpecialValuesMatchFormMix() {
    for (double v : D) {
      for (double lo : D) {
        for (double hi : D) {
          assertSameDouble(Clamping.clamp(v, lo, hi), mix(v, lo, hi));
          float fv = (float) v;
          float flo = (float) lo;
          float fhi = (float) hi;
          assertSameFloat(Clamping.clamp(fv, flo, fhi), (float) mix(fv, flo, fhi));
        }
      }
    }
  }

  @Test
  void longAndIntExtremesMatchFormMix() {
    for (long v : L) {
      for (long lo : L) {
        for (long hi : L) {
          long expected = v < lo ? lo : (v > hi ? hi : v);
          assertThat(Clamping.clamp(v, lo, hi)).isEqualTo(expected);
          int iv = (int) v;
          int ilo = (int) lo;
          int ihi = (int) hi;
          int iexpected = iv < ilo ? ilo : (iv > ihi ? ihi : iv);
          assertThat(Clamping.clamp(iv, ilo, ihi)).isEqualTo(iexpected);
        }
      }
    }
  }
}

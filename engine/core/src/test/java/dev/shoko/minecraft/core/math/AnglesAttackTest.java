package dev.shoko.minecraft.core.math;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: core-math R20 (S): adversarial checks of normalizeDegrees against an exact reference.
class AnglesAttackTest {

  private static final BigDecimal TURN = BigDecimal.valueOf(360);
  private static final BigDecimal HALF = BigDecimal.valueOf(180);

  /** Exact reference: the unique value congruent to x modulo 360 in [-180, 180). */
  private static BigDecimal exactNormalized(double x) {
    BigDecimal r = new BigDecimal(x).remainder(TURN);
    if (r.compareTo(HALF) >= 0) {
      r = r.subtract(TURN);
    }
    if (r.compareTo(HALF.negate()) < 0) {
      r = r.add(TURN);
    }
    return r;
  }

  @Test
  @Timeout(20)
  void intNormalizationOverAStrideOfTheIntRange() {
    long mismatches = 0;
    for (long v = Integer.MIN_VALUE; v <= Integer.MAX_VALUE; v += 4099) {
      long expected = Math.floorMod(v + 180, 360L) - 180;
      if (Angles.normalizeDegrees((int) v) != expected) {
        mismatches++;
      }
    }
    for (int v :
        new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE, -180, 180, -181, 179, 540, -540}) {
      assertThat(Angles.normalizeDegrees(v)).isEqualTo((int) (Math.floorMod(v + 180L, 360L) - 180));
    }
    assertThat(mismatches).isZero();
  }

  @Test
  @Timeout(20)
  void floatNormalizationIsExactAndInRange() {
    List<Float> mismatches = new ArrayList<>();
    for (long bits = 0; bits <= 0xFFFF_FFFFL; bits += 65_537) {
      float f = Float.intBitsToFloat((int) bits);
      float n = Angles.normalizeDegrees(f);
      if (Float.isNaN(f) || Float.isInfinite(f)) {
        if (!Float.isNaN(n)) {
          mismatches.add(f);
        }
        continue;
      }
      BigDecimal exact = exactNormalized(f);
      if (new BigDecimal(n).compareTo(exact) != 0 || !(n >= -180f && n < 180f)) {
        mismatches.add(f);
      }
    }
    assertThat(mismatches).isEmpty();
  }

  @Test
  @Timeout(20)
  void doubleNormalizationIsExactAndInRange() {
    SplittableRandom rnd = new SplittableRandom(360);
    List<Double> mismatches = new ArrayList<>();
    for (int i = 0; i < 50_000; i++) {
      double x =
          (i & 1) == 0
              ? Double.longBitsToDouble(rnd.nextLong())
              : (rnd.nextDouble() - 0.5) * Math.scalb(1.0, rnd.nextInt(-10, 80));
      double n = Angles.normalizeDegrees(x);
      if (Double.isNaN(x) || Double.isInfinite(x)) {
        if (!Double.isNaN(n)) {
          mismatches.add(x);
        }
        continue;
      }
      if (new BigDecimal(n).compareTo(exactNormalized(x)) != 0 || !(n >= -180 && n < 180)) {
        mismatches.add(x);
      }
    }
    double[] edges = {
      180,
      -180,
      Math.nextDown(180.0),
      Math.nextUp(-180.0),
      Math.nextDown(-180.0),
      540,
      -540,
      Double.MAX_VALUE,
      -Double.MAX_VALUE,
      Double.MIN_VALUE,
      -Double.MIN_VALUE,
      0x1p53,
      1e300
    };
    for (double x : edges) {
      double n = Angles.normalizeDegrees(x);
      if (new BigDecimal(n).compareTo(exactNormalized(x)) != 0 || !(n >= -180 && n < 180)) {
        mismatches.add(x);
      }
    }
    assertThat(mismatches).isEmpty();
    assertThat(Double.doubleToRawLongBits(Angles.normalizeDegrees(-0.0)))
        .isEqualTo(Double.doubleToRawLongBits(-0.0));
    assertThat(Float.floatToRawIntBits(Angles.normalizeDegrees(-0.0f)))
        .isEqualTo(Float.floatToRawIntBits(-0.0f));
  }
}

package dev.shoko.minecraft.core.math;

import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameDouble;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

// spec: core-math R4 to R7: adversarial checks against exact and independent references.
class RoundingAttackTest {

  /** Exact floor of the real value of x, saturated to int, NaN to 0 (candidate F1). */
  private static int refFloor(double x) {
    if (Double.isNaN(x)) {
      return 0;
    }
    if (Double.isInfinite(x)) {
      return x > 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
    }
    BigDecimal f = new BigDecimal(x).setScale(0, RoundingMode.FLOOR);
    return f.max(BigDecimal.valueOf(Integer.MIN_VALUE))
        .min(BigDecimal.valueOf(Integer.MAX_VALUE))
        .intValueExact();
  }

  private static int refCeil(double x) {
    if (Double.isNaN(x)) {
      return 0;
    }
    if (Double.isInfinite(x)) {
      return x > 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
    }
    BigDecimal f = new BigDecimal(x).setScale(0, RoundingMode.CEILING);
    return f.max(BigDecimal.valueOf(Integer.MIN_VALUE))
        .min(BigDecimal.valueOf(Integer.MAX_VALUE))
        .intValueExact();
  }

  /** Cheap independent floor for the dense loops: truncation through long, then correction. */
  private static int fastRefFloor(double x) {
    if (Double.isNaN(x)) {
      return 0;
    }
    if (x >= 0x1p31) {
      return Integer.MAX_VALUE;
    }
    if (x < -0x1p31) {
      return Integer.MIN_VALUE;
    }
    long t = (long) x;
    return (int) (x < t ? t - 1 : t);
  }

  private static int fastRefCeil(double x) {
    if (Double.isNaN(x)) {
      return 0;
    }
    if (x > 0x1p31 - 1) {
      return Integer.MAX_VALUE;
    }
    if (x <= -0x1p31 - 1) {
      return Integer.MIN_VALUE;
    }
    long t = (long) x;
    return (int) (x > t ? t + 1 : t);
  }

  private static boolean floatMatches(float g) {
    return Rounding.floorToInt(g) == fastRefFloor(g) && Rounding.ceilToInt(g) == fastRefCeil(g);
  }

  @ParameterizedTest(name = "floor/ceil of {0}")
  @ValueSource(
      doubles = {
        0.0,
        -0.0,
        Double.MIN_VALUE,
        -Double.MIN_VALUE,
        Double.MIN_NORMAL,
        -1e-300,
        Double.MAX_VALUE,
        -Double.MAX_VALUE,
        Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY,
        Double.NaN,
        0x1p53,
        0x1p53 + 2,
        -0x1p53,
        0x1p52 + 0.5,
        -0x1p52 - 0.5,
        0x1p31,
        -0x1p31,
        0x1p31 - 1,
        0x1p31 - 0.5,
        -0x1p31 - 0.5,
        -0x1p31 + 0.5,
        2147483647.9999998,
        -2147483648.0000005,
        16777217.0,
        0.49999999999999994,
        -0.49999999999999994,
        0.9999999999999999,
        -0.9999999999999999,
        3.4028234663852886E38,
        1e20,
        -1e20
      })
  void doubleExtremes(double x) {
    assertThat(Rounding.floorToInt(x)).isEqualTo(refFloor(x));
    assertThat(Rounding.ceilToInt(x)).isEqualTo(refCeil(x));
  }

  @Test
  @Timeout(20)
  void floatBitPatternsStrided() {
    List<Float> mismatches = new ArrayList<>();
    for (long bits = 0; bits <= 0xFFFF_FFFFL && mismatches.size() < 20; bits += 4093) {
      float f = Float.intBitsToFloat((int) bits);
      if (!floatMatches(f)) {
        mismatches.add(f);
      }
    }
    assertThat(mismatches).isEmpty();
  }

  // Every 17th integer up to 2^24 in magnitude, with its float neighbours
  @Test
  @Timeout(20)
  void floatNeighboursOfIntegersUpTo2Pow24() {
    List<Float> mismatches = new ArrayList<>();
    for (int i = -(1 << 24) - 2; i <= (1 << 24) + 2 && mismatches.size() < 20; i += 17) {
      float f = i;
      for (float g : new float[] {Math.nextDown(f), f, Math.nextUp(f)}) {
        if (!floatMatches(g)) {
          mismatches.add(g);
        }
      }
    }
    assertThat(mismatches).isEmpty();
  }

  @Test
  @Timeout(20)
  void doubleDenseRandom() {
    SplittableRandom rnd = new SplittableRandom(1);
    List<Double> mismatches = new ArrayList<>();
    for (int i = 0; i < 1_000_000 && mismatches.size() < 20; i++) {
      double x =
          switch (i & 3) {
            case 0 -> Double.longBitsToDouble(rnd.nextLong());
            case 1 -> (rnd.nextDouble() - 0.5) * 6e9;
            case 2 -> {
              double k = rnd.nextLong(-3_000_000_000L, 3_000_000_000L);
              yield rnd.nextBoolean() ? Math.nextUp(k) : Math.nextDown(k);
            }
            default -> (rnd.nextDouble() - 0.5) * Math.scalb(1.0, rnd.nextInt(-60, 70));
          };
      if (Rounding.floorToInt(x) != fastRefFloor(x) || Rounding.ceilToInt(x) != fastRefCeil(x)) {
        mismatches.add(x);
      }
      if (Math.abs(x) < 0x1p62 && Rounding.floorToLong(x) != (long) Math.floor(x)) {
        mismatches.add(x);
      }
    }
    assertThat(mismatches).isEmpty();
  }

  @Test
  void floatVariantFloorsTheRoundedFloat() {
    assertThat(Rounding.floorToInt((float) 0.99999999)).isEqualTo(1);
    assertThat(Rounding.floorToInt(0.99999999)).isEqualTo(0);
    assertThat(Rounding.floorToInt(Math.nextUp(16777216f))).isEqualTo(16777218);
    assertThat(Rounding.floorToInt(Float.MAX_VALUE)).isEqualTo(Integer.MAX_VALUE);
    assertThat(Rounding.floorToInt(-Float.MAX_VALUE)).isEqualTo(Integer.MIN_VALUE);
    assertThat(Rounding.floorToInt(-Float.MIN_VALUE)).isEqualTo(-1);
    assertThat(Rounding.ceilToInt(Float.MIN_VALUE)).isEqualTo(1);
    assertThat(Rounding.floorToInt(Float.NaN)).isZero();
  }

  @Test
  @Timeout(20)
  void fractionalPartInDomainStaysInUnitInterval() {
    SplittableRandom rnd = new SplittableRandom(11);
    for (int i = 0; i < 500_000; i++) {
      double x = (rnd.nextDouble() - 0.5) * Math.scalb(1.0, rnd.nextInt(-80, 63));
      double fp = Rounding.fractionalPart(x);
      assertThat(fp >= 0 && fp <= 1).as("x=%s fp=%s", x, fp).isTrue();
    }
    assertThat(Rounding.fractionalPart(-1e-20)).isEqualTo(1.0);
    assertThat(Rounding.fractionalPart(-0.0)).isEqualTo(0.0);
  }

  // spec: core-math R7 with R4 provisional (S). Known limit: outside the long domain,
  // floorToLong saturates (F1) and x - floorToLong(x) leaves [0, 1]. The formula wins over the
  // "[0, 1]" prose; callers never pass such values. Expected = x - (double) saturated bound.
  @ParameterizedTest(name = "provisional knownLimit fractionalPart({0}) = {1}")
  @CsvSource({
    "-1e19, -7.766279631452242E17",
    "1e19, 7.766279631452242E17",
    "-1.8446744073709552E19, -9.223372036854776E18"
  })
  void provisionalKnownLimitFractionalPartOutsideLongRangeLeavesUnitInterval(
      double x, double expected) {
    assertSameDouble(Rounding.fractionalPart(x), expected);
  }
}

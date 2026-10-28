package dev.shoko.minecraft.core.math;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// spec: core-math R17 (S): adversarial checks of the DL indexing against an independent table.
class TrigTableAttackTest {

  private static final double K = 10430.378350470453;

  /** Independent table: (float) StrictMath.sin(2 pi i / 65536), written in the spec's order. */
  private static final float[] REF = new float[65536];

  static {
    for (int i = 0; i < REF.length; i++) {
      REF[i] = (float) StrictMath.sin(2 * StrictMath.PI * i / 65536);
    }
  }

  @Test
  void factorIsTheSpecFactor() {
    assertThat(65536 / (2 * StrictMath.PI)).isEqualTo(K);
  }

  @Test
  @Timeout(20)
  void denseRandomInputsMatchTheIndependentFormula() {
    SplittableRandom rnd = new SplittableRandom(2026);
    List<Double> mismatches = new ArrayList<>();
    for (int i = 0; i < 500_000 && mismatches.size() < 20; i++) {
      double x =
          (i & 1) == 0
              ? Double.longBitsToDouble(rnd.nextLong())
              : (rnd.nextDouble() - 0.5) * Math.scalb(1.0, rnd.nextInt(-30, 50));
      if (Math.abs(x * K + 16384.0) >= 0x1p63 || Double.isNaN(x)) {
        continue;
      }
      float s = REF[(int) ((long) (x * K) & 65535)];
      float c = REF[(int) ((long) (x * K + 16384.0) & 65535)];
      if (Float.floatToIntBits(s) != Float.floatToIntBits(TrigTable.tableSin(x))
          || Float.floatToIntBits(c) != Float.floatToIntBits(TrigTable.tableCos(x))) {
        mismatches.add(x);
      }
    }
    assertThat(mismatches).isEmpty();
  }

  // spec: core-math R17 (S). Known limit: once |x * K| >= 2^63 (|x| >= ~8.84e14) the long
  // conversion saturates, so every larger positive x reads entry 65535 (Long.MAX_VALUE & 65535)
  // and every larger negative x entry 0 (Long.MIN_VALUE & 65535), sine and cosine alike. The DL
  // formula wins over the "period correct for every finite x" prose; callers stay far below.
  @ParameterizedTest(name = "provisional knownLimit tableSin({0}) saturates to entry 65535")
  @ValueSource(doubles = {9e14, 1e15, 1.2345678e16, 1e300, Double.MAX_VALUE})
  void provisionalKnownLimitPositiveHugeAngleReadsLastEntry(double x) {
    assertThat(Math.abs(x * K)).isGreaterThanOrEqualTo(0x1p63);
    assertThat(Float.floatToIntBits(TrigTable.tableSin(x)))
        .isEqualTo(Float.floatToIntBits(REF[65535]));
    assertThat(Float.floatToIntBits(TrigTable.tableCos(x)))
        .isEqualTo(Float.floatToIntBits(REF[65535]));
  }

  // spec: core-math R17 (S), known limit as above for negative angles: entry 0
  @ParameterizedTest(name = "provisional knownLimit tableSin({0}) saturates to entry 0")
  @ValueSource(doubles = {-9e14, -1e15, -1e300})
  void provisionalKnownLimitNegativeHugeAngleReadsFirstEntry(double x) {
    assertThat(Float.floatToIntBits(TrigTable.tableSin(x))).isEqualTo(Float.floatToIntBits(REF[0]));
    assertThat(Float.floatToIntBits(TrigTable.tableCos(x))).isEqualTo(Float.floatToIntBits(REF[0]));
  }

  @Test
  void specialValues() {
    assertThat(TrigTable.tableSin(Double.NaN)).isEqualTo(0f);
    assertThat(TrigTable.tableCos(Double.NaN)).isEqualTo(0f);
    assertThat(Float.floatToIntBits(TrigTable.tableSin(-0.0))).isEqualTo(0);
    // Smallest positive values stay in entry 0.
    assertThat(TrigTable.tableSin(Double.MIN_VALUE)).isEqualTo(0f);
    assertThat(TrigTable.tableCos(-Double.MIN_VALUE)).isEqualTo(1f);
  }

  @Test
  @Timeout(20)
  void oddSymmetryHoldsExceptAtTheHalfPeriodEntry() {
    SplittableRandom rnd = new SplittableRandom(5);
    for (int i = 0; i < 200_000; i++) {
      double x = (rnd.nextDouble() - 0.5) * 400_000;
      int index = (int) ((long) (x * K) & 65535);
      if (index != 32768 && index != 0) {
        assertThat(TrigTable.tableSin(-x)).as("x=%s", x).isEqualTo(-TrigTable.tableSin(x));
      }
    }
  }
}

package dev.shoko.minecraft.core.math;

import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameFloat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

// spec: core-math R17 is S: table size, factor and indexing DL are provisional until probe P17.
class TrigTableTest {

  private static final int SIZE = 65536;

  // spec: core-math R17, factor K = 65536 / (2 * pi) in double
  private static final double K = 10430.378350470453;

  /** Entry {@code i} of the table as defined by R17: float rounding of sin(2 pi i / 65536). */
  private static float specEntry(int i) {
    return (float) StrictMath.sin(i * StrictMath.PI * 2.0 / SIZE);
  }

  /** An angle whose product by K lies half-way inside index {@code i}, away from both edges. */
  private static double angleInsideIndex(long i) {
    return (i + 0.5) / K;
  }

  // spec: core-math R17 (S), V14 table entries
  @Test
  void provisionalTableEntriesMatchVectors() {
    assertSameFloat(TrigTable.tableSin(angleInsideIndex(0)), 0f);
    assertSameFloat(TrigTable.tableSin(angleInsideIndex(1)), Float.intBitsToFloat(0x38c90fdb));
    assertSameFloat(TrigTable.tableSin(angleInsideIndex(8192)), Float.intBitsToFloat(0x3f3504f3));
    assertSameFloat(TrigTable.tableSin(angleInsideIndex(16384)), 1f);
    assertSameFloat(TrigTable.tableSin(angleInsideIndex(32768)), Float.intBitsToFloat(0x250d3132));
    assertSameFloat(TrigTable.tableSin(angleInsideIndex(49152)), -1f);
  }

  // spec: core-math R17 (S): 65,536 float entries, entry i = (float) sin(2 pi i / 65536)
  @Test
  void provisionalEveryEntryIsTheFloatRoundingOfTheExactSine() {
    List<Integer> mismatches = new ArrayList<>();
    for (int i = 0; i < SIZE; i++) {
      if (Float.floatToIntBits(TrigTable.tableSin(angleInsideIndex(i)))
          != Float.floatToIntBits(specEntry(i))) {
        mismatches.add(i);
      }
    }
    assertThat(mismatches).isEmpty();
  }

  // spec: core-math R17 (S): the index wraps at 65,536 (table size) for any finite x, including
  // beyond |x| = 205,887.416 where indexing via int would saturate (candidate DI rejected)
  @ParameterizedTest(name = "provisional period of 65536 indices, {0} periods ahead")
  @ValueSource(longs = {1, 2, 33, 1000, 1L << 20})
  void provisionalSineIsPeriodicOverTheTableSize(long periods) {
    List<Integer> mismatches = new ArrayList<>();
    for (int i = 0; i < SIZE; i += 7) {
      float base = TrigTable.tableSin(angleInsideIndex(i));
      float shifted = TrigTable.tableSin(angleInsideIndex(i + periods * SIZE));
      if (Float.floatToIntBits(base) != Float.floatToIntBits(shifted)) {
        mismatches.add(i);
      }
    }
    assertThat(mismatches).isEmpty();
  }

  // spec: core-math R17 (S): cosine reads the same table shifted by +16384
  @Test
  void provisionalCosineIsTheSineTableAQuarterAhead() {
    List<Integer> mismatches = new ArrayList<>();
    for (int i = 0; i < SIZE; i += 3) {
      float cos = TrigTable.tableCos(angleInsideIndex(i));
      if (Float.floatToIntBits(cos) != Float.floatToIntBits(specEntry((i + 16384) % SIZE))) {
        mismatches.add(i);
      }
    }
    assertThat(mismatches).isEmpty();
  }

  // spec: core-math R17 (S): tableSin(-x) == -tableSin(x) except at index 32768
  @Test
  void provisionalSineIsOddExceptAtHalfPeriod() {
    List<Integer> mismatches = new ArrayList<>();
    for (int i = 0; i < SIZE; i++) {
      double x = angleInsideIndex(i);
      if (TrigTable.tableSin(-x) != -TrigTable.tableSin(x)) {
        mismatches.add(i);
      }
    }
    assertThat(mismatches).containsExactly(32768);
  }

  // spec: core-math R17 (S), vector V14 column "sin DL (provisional)"
  @ParameterizedTest(name = "provisional DL tableSin({0}) = {1}")
  @CsvSource({
    "0.0, 0.0",
    "-0.0, 0.0",
    "1.5707963267948966, 1.0",
    "-1.5707963267948966, -1.0",
    "3.141592653589793, 1.2246469E-16",
    "1.0, 0.8414514",
    "-1.0, -0.8414514",
    "0.5, 0.47940964",
    "-0.5, -0.47940964",
    "205887.0, -0.40427697",
    "205888.0, 0.55117816",
    "-205888.0, -0.55117816",
    "1e6, -0.35003576",
    "1e10, -0.48755017",
    "NaN, 0.0"
  })
  void provisionalSineUsesDoubleInputAndLongIndexing(double x, float expected) {
    assertSameFloat(TrigTable.tableSin(x), expected);
  }

  // spec: core-math R17 (S), vector V14 column "cos DL (provisional)"
  @ParameterizedTest(name = "provisional DL tableCos({0}) = {1}")
  @CsvSource({
    "0.0, 1.0",
    "-0.0, 1.0",
    "1.5707963267948966, 1.2246469E-16",
    "-1.5707963267948966, 0.0",
    "3.141592653589793, -1.0",
    "1.0, 0.54033285",
    "-1.0, 0.54025215",
    "0.5, 0.87759125",
    "-0.5, 0.8775453",
    "205887.0, 0.9146366",
    "205888.0, 0.8343876",
    "-205888.0, 0.8343876",
    "1e6, 0.93673635",
    "1e10, 0.873095",
    "NaN, 0.0"
  })
  void provisionalCosineUsesDoubleInputAndLongIndexing(double x, float expected) {
    assertSameFloat(TrigTable.tableCos(x), expected);
  }

  // spec: core-math R17 (S): the cosine is not even, tableCos(-1.0) != tableCos(1.0)
  @Test
  void provisionalCosineIsNotEven() {
    assertThat(TrigTable.tableCos(-1.0)).isNotEqualTo(TrigTable.tableCos(1.0));
  }

  // spec: core-math R17 (S), probe P17 vector: yaw 37 degrees, every candidate gives these entries
  @Test
  void provisionalTableValuesForThirtySevenDegrees() {
    double radians = 0.6457718232379019;
    assertSameFloat(TrigTable.tableSin(radians), 0.6017657f);
    assertSameFloat(TrigTable.tableCos(radians), 0.7986727f);
  }
}

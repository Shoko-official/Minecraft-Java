package dev.shoko.minecraft.core.math;

import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameDouble;
import static dev.shoko.minecraft.core.math.ExactAssertions.assertSameFloat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClampingTest {

  // spec: core-math R15, V12 rows where every form agrees (lo <= hi, no NaN, no signed zeros)
  @ParameterizedTest(name = "clamp({0}, {1}, {2}) = {3}")
  @CsvSource({"0.5, 0, 1, 0.5", "-1, 0, 1, 0.0", "2, 0, 1, 1.0", "0, 0, 1, 0.0", "1, 0, 1, 1.0"})
  void doubleClampReturnsBoundOrValue(double v, double lo, double hi, double expected) {
    assertSameDouble(Clamping.clamp(v, lo, hi), expected);
  }

  // spec: core-math R15: NaN value gives NaN in every form
  @Test
  void nanValueStaysNan() {
    assertThat(Clamping.clamp(Double.NaN, 0.0, 1.0)).isNaN();
    assertThat(Clamping.clamp(Float.NaN, 0f, 1f)).isNaN();
  }

  // spec: core-math R15 (S), vector V12 column MIX: provisional v < lo ? lo : min(v, hi)
  @ParameterizedTest(name = "provisional MIX clamp({0}, {1}, {2}) = {3}")
  @CsvSource({
    "-0.0, 0.0, 1, -0.0",
    "0.0, -0.0, 1, 0.0",
    "1.0, 0.0, -0.0, -0.0",
    "0.5, NaN, 1, 0.5",
    "0.5, 0, NaN, NaN"
  })
  void provisionalDoubleClampUsesFormMix(double v, double lo, double hi, double expected) {
    assertSameDouble(Clamping.clamp(v, lo, hi), expected);
  }

  // spec: core-math R15 (S), V12 column MIX transposed to float
  @Test
  void provisionalFloatClampUsesFormMix() {
    assertSameFloat(Clamping.clamp(0.5f, 0f, 1f), 0.5f);
    assertSameFloat(Clamping.clamp(-1f, 0f, 1f), 0f);
    assertSameFloat(Clamping.clamp(2f, 0f, 1f), 1f);
    assertSameFloat(Clamping.clamp(-0.0f, 0.0f, 1f), -0.0f);
    assertSameFloat(Clamping.clamp(0.0f, -0.0f, 1f), 0.0f);
    assertSameFloat(Clamping.clamp(1.0f, 0.0f, -0.0f), -0.0f);
    assertSameFloat(Clamping.clamp(0.5f, Float.NaN, 1f), 0.5f);
    assertThat(Clamping.clamp(0.5f, 0f, Float.NaN)).isNaN();
  }

  // spec: core-math R15, V12 int rows; lo > hi is forbidden, provisional MIX result is pinned
  @ParameterizedTest(name = "int clamp({0}, {1}, {2}) = {3}")
  @CsvSource({
    "5, 0, 10, 5",
    "-1, 0, 10, 0",
    "11, 0, 10, 10",
    "-2147483648, -64, 319, -64",
    "2147483647, -64, 319, 319",
    "5, 10, 0, 10",
    "20, 10, 0, 0",
    "-5, 10, 0, 10"
  })
  void intClampMatchesVectors(int v, int lo, int hi, int expected) {
    assertThat(Clamping.clamp(v, lo, hi)).isEqualTo(expected);
  }

  // spec: core-math R15, long overload; last row is the provisional MIX result for lo > hi
  @ParameterizedTest(name = "long clamp({0}, {1}, {2}) = {3}")
  @CsvSource({
    "5, 0, 10, 5",
    "-9223372036854775808, -1, 1, -1",
    "9223372036854775807, -1, 1, 1",
    "4294967296, 0, 4294967295, 4294967295",
    "20, 10, 0, 0"
  })
  void longClampMatchesVectors(long v, long lo, long hi, long expected) {
    assertThat(Clamping.clamp(v, lo, hi)).isEqualTo(expected);
  }

  // spec: core-math R15: unlike Math.clamp, lo > hi does not throw (provisional MIX value)
  @Test
  void provisionalInvertedBoundsDoNotThrow() {
    assertThat(Clamping.clamp(20.0, 10.0, 0.0)).isEqualTo(0.0);
    assertThat(Clamping.clamp(5.0, 10.0, 0.0)).isEqualTo(10.0);
  }
}

package dev.shoko.minecraft.core.math;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bit-exact comparisons of floating-point results. A plain {@code ==} treats {@code 0.0} and {@code
 * -0.0} as equal and NaN as different from itself, which would hide or invent fidelity gaps (spec
 * core-math R2).
 */
public final class ExactAssertions {

  private ExactAssertions() {}

  /** Asserts that both values have the same IEEE 754 bits, NaN payloads aside. */
  public static void assertSameDouble(double actual, double expected) {
    assertThat(Double.doubleToLongBits(actual))
        .as("expected %s (0x%016x) but was %s", expected, Double.doubleToLongBits(expected), actual)
        .isEqualTo(Double.doubleToLongBits(expected));
  }

  /** Asserts that both values have the same IEEE 754 bits, NaN payloads aside. */
  public static void assertSameFloat(float actual, float expected) {
    assertThat(Float.floatToIntBits(actual))
        .as("expected %s (0x%08x) but was %s", expected, Float.floatToIntBits(expected), actual)
        .isEqualTo(Float.floatToIntBits(expected));
  }
}

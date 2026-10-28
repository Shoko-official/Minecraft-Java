package dev.shoko.minecraft.core.math;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class PowersOfTwoAttackTest {

  /** Independent reference: number of bits of v - 1, i.e. ceil(log2(v)) for v >= 1. */
  private static int refLog2(int v) {
    return 32 - Integer.numberOfLeadingZeros(v - 1);
  }

  private static boolean matches(int v) {
    int log = refLog2(v);
    return PowersOfTwo.ceilingLog2(v) == log && PowersOfTwo.ceilingPowerOfTwo(v) == (1 << log);
  }

  // spec: core-math R21 on its domain [1, 2^30]: a stride over the whole domain plus every
  // power of two and its neighbours
  @Test
  @Timeout(20)
  void domainStrideAndPowerNeighboursMatchReference() {
    long mismatches = 0;
    for (int v = 1; v <= (1 << 30); v += 1021) {
      if (!matches(v)) {
        mismatches++;
      }
    }
    for (int k = 0; k <= 30; k++) {
      int power = 1 << k;
      for (int v : new int[] {power - 1, power, power + 1}) {
        if (v >= 1 && v <= (1 << 30) && !matches(v)) {
          mismatches++;
        }
      }
    }
    assertThat(mismatches).isZero();
    // spec: core-math V16, last row
    assertThat(PowersOfTwo.ceilingLog2((1 << 30) + 1)).isEqualTo(31);
  }
}

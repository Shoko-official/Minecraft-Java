package dev.shoko.minecraft.core.math;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PowersOfTwoTest {

  // spec: core-math R21, vector V16
  @ParameterizedTest(name = "ceilingPowerOfTwo({0}) = {1}, ceilingLog2({0}) = {2}")
  @CsvSource({
    "1, 1, 0",
    "2, 2, 1",
    "3, 4, 2",
    "4, 4, 2",
    "5, 8, 3",
    "16, 16, 4",
    "17, 32, 5",
    "31, 32, 5",
    "32, 32, 5",
    "33, 64, 6",
    "536870913, 1073741824, 30",
    "1073741824, 1073741824, 30"
  })
  void matchesVectors(int v, int power, int log2) {
    assertThat(PowersOfTwo.ceilingPowerOfTwo(v)).isEqualTo(power);
    assertThat(PowersOfTwo.ceilingLog2(v)).isEqualTo(log2);
  }

  // spec: core-math R21: smallest power of two >= v, and ceil(log2(v)) bits, checked against the
  // definition on 1..2^20 and around every power of two up to 2^30
  @Test
  void agreesWithTheDefinitionOverTheDomain() {
    List<Integer> mismatches = new ArrayList<>();
    for (int v = 1; v <= 1 << 20; v++) {
      check(v, mismatches);
    }
    for (int k = 20; k <= 30; k++) {
      check((1 << k) - 1, mismatches);
      check(1 << k, mismatches);
      if (k < 30) {
        check((1 << k) + 1, mismatches);
      }
    }
    assertThat(mismatches).isEmpty();
  }

  private static void check(int v, List<Integer> mismatches) {
    int log2 = 0;
    while ((1L << log2) < v) {
      log2++;
    }
    if (PowersOfTwo.ceilingLog2(v) != log2 || PowersOfTwo.ceilingPowerOfTwo(v) != (1 << log2)) {
      mismatches.add(v);
    }
  }
}

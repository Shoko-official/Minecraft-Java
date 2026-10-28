package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * G48 skip: exactly {@code n} steps, computed in O(log n) (spec core-random R10, R11). The timeout
 * only guards the very large jumps against a linear implementation that would never finish; it is
 * orders of magnitude above the expected cost.
 */
class Lcg48RandomSkipTest {

  private static final long PERIOD = 1L << 48;

  // spec: core-random R10 (skip(n) advances the state exactly as n calls to next(b))
  @ParameterizedTest
  @ValueSource(longs = {0, 1, 2, 3, 7, 10, 100, 1000, 4097, 65539, 1_000_000})
  void skipMatchesTheSameNumberOfSingleSteps(long n) {
    for (long seed : new long[] {0L, -1L, 12345L, 0x5DEECE66DL}) {
      Lcg48Random jumped = Lcg48Random.ofSeed(seed);
      Lcg48Random stepped = Lcg48Random.ofSeed(seed);
      jumped.skip(n);
      for (long i = 0; i < n; i++) {
        stepped.next(32);
      }
      assertThat(jumped.state()).as("seed %d", seed).isEqualTo(stepped.state());
      assertThat(jumped.steps()).isEqualTo(n);
      assertThat(jumped.nextInt()).isEqualTo(stepped.nextInt());
    }
  }

  // spec: core-random R10, vector V-L4
  @Test
  void skipOfOneMillionFromSeedZeroMatchesVectorVL4() {
    Lcg48Random random = Lcg48Random.ofSeed(0L);
    random.skip(1_000_000);
    assertThat(random.steps()).isEqualTo(1_000_000L);
    assertThat(random.nextInt()).isEqualTo(1257967379);
    assertThat(random.steps()).isEqualTo(1_000_001L);
  }

  // spec: core-random R10, vector V-L4 (the closed form and the repetition coincide)
  @Test
  void oneMillionNextIntFromSeedZeroMatchesVectorVL4() {
    Lcg48Random random = Lcg48Random.ofSeed(0L);
    for (int i = 0; i < 1_000_000; i++) {
      random.nextInt();
    }
    assertThat(random.nextInt()).isEqualTo(1257967379);
  }

  // spec: core-random R10, R1 (C: the step has full period 2^48, multiplier = 1 mod 4 and odd
  // increment)
  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  void skipOfTheFullPeriodReturnsToTheSameState() {
    Lcg48Random random = Lcg48Random.ofSeed(2024L);
    long state = random.state();
    random.skip(PERIOD);
    assertThat(random.state()).isEqualTo(state);
    assertThat(random.steps()).isEqualTo(PERIOD);
    random.skip(PERIOD / 2);
    assertThat(random.state()).isNotEqualTo(state);
    random.skip(PERIOD / 2);
    assertThat(random.state()).isEqualTo(state);
  }

  // spec: core-random R10 (very large n: 2^63 - 1 = 2^48 - 1 mod 2^48, one step short of a period)
  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  void skipOfLongMaxValueIsOneStepShortOfAPeriod() {
    Lcg48Random random = Lcg48Random.ofSeed(-77L);
    long state = random.state();
    random.skip(Long.MAX_VALUE);
    assertThat(random.steps()).isEqualTo(Long.MAX_VALUE);
    assertThat(random.state()).isNotEqualTo(state);
    random.setSeed(-77L);
    random.skip(Long.MAX_VALUE);
    random.next(1);
    assertThat(random.state()).isEqualTo(state);
  }

  // spec: core-random R10 (composition of affine steps)
  @ParameterizedTest
  @CsvSource({
    "0, 5",
    "1, 1",
    "123456789012345, 98765432109876",
    "281474976710655, 1",
    "4611686018427387903, 4611686018427387904"
  })
  @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  void consecutiveSkipsAddUp(long first, long second) {
    Lcg48Random split = Lcg48Random.ofSeed(31L);
    split.skip(first);
    split.skip(second);
    Lcg48Random whole = Lcg48Random.ofSeed(31L);
    whole.skip(first + second);
    assertThat(split.state()).isEqualTo(whole.state());
    assertThat(split.steps()).isEqualTo(first + second);
  }

  // RandomSource.skip contract: n < 0 is an error
  @ParameterizedTest
  @ValueSource(longs = {-1, -1000, Long.MIN_VALUE})
  void negativeSkipFailsWithoutChangingTheState(long n) {
    Lcg48Random random = Lcg48Random.ofSeed(8L);
    long state = random.state();
    assertThatThrownBy(() -> random.skip(n)).isInstanceOf(IllegalArgumentException.class);
    assertThat(random.state()).isEqualTo(state);
    assertThat(random.steps()).isZero();
  }
}

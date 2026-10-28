package dev.shoko.minecraft.core.random;

import static dev.shoko.minecraft.core.random.Lcg48RandomTest.INCREMENT;
import static dev.shoko.minecraft.core.random.Lcg48RandomTest.MASK_48;
import static dev.shoko.minecraft.core.random.Lcg48RandomTest.MULTIPLIER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.core.random.Lcg48Vectors.BoundedDraws;
import dev.shoko.minecraft.core.random.Lcg48Vectors.Rejection;
import java.math.BigInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * G48 bounded draws: {@code nextInt(bound)} and its rejections (spec core-random R5, R11), and the
 * inclusive range draw (R12, level S, provisional until probe P4).
 */
class Lcg48RandomBoundedTest {

  private static final long TWO_POW_31 = 1L << 31;

  static Stream<Arguments> vectorVL2() {
    return Lcg48Vectors.V_L2.stream().map(v -> Arguments.of(v.seed(), v.bound(), v));
  }

  static Stream<Arguments> vectorVL3() {
    return Lcg48Vectors.V_L3.stream().map(v -> Arguments.of(v.seed(), v));
  }

  // spec: core-random R5 (bound <= 0: error, no step consumed)
  @ParameterizedTest
  @ValueSource(ints = {0, -1, -16, Integer.MIN_VALUE})
  void nonPositiveBoundFailsWithoutStep(int bound) {
    Lcg48Random random = Lcg48Random.ofSeed(3L);
    long state = random.state();
    assertThatThrownBy(() -> random.nextInt(bound)).isInstanceOf(IllegalArgumentException.class);
    assertThat(random.state()).isEqualTo(state);
    assertThat(random.steps()).isZero();
  }

  static IntStream powersOfTwo() {
    return IntStream.rangeClosed(0, 30).map(k -> 1 << k);
  }

  // spec: core-random R5 (power of two, 1 included: (bound * next(31)) >> 31, one step)
  @ParameterizedTest
  @MethodSource("powersOfTwo")
  void powerOfTwoBoundScalesNext31InOneStep(int bound) {
    Lcg48Random random = Lcg48Random.ofSeed(-123456789L);
    Lcg48Random twin = Lcg48Random.ofSeed(-123456789L);
    for (int i = 0; i < 100; i++) {
      long expected = (bound * (long) twin.next(31)) >> 31;
      assertThat(random.nextInt(bound)).as("draw %d", i).isEqualTo((int) expected);
      assertThat(random.steps()).isEqualTo(i + 1L);
    }
  }

  // spec: core-random R5, edge case nextInt(1), vector V-L2 bound 1
  @Test
  void nextIntOfOneIsZeroButConsumesAStep() {
    Lcg48Random random = Lcg48Random.ofSeed(0L);
    assertThat(random.nextInt(1)).isZero();
    assertThat(random.nextInt(1)).isZero();
    assertThat(random.nextInt(1)).isZero();
    assertThat(random.steps()).isEqualTo(3L);
    assertThat(random.nextInt()).isEqualTo(-1690734402);
  }

  // spec: core-random R5, R11, vectors V-L2
  @ParameterizedTest(name = "seed {0}, bound {1}")
  @MethodSource("vectorVL2")
  void boundedDrawsMatchVectorVL2(long seed, int bound, BoundedDraws expected) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    for (int i = 0; i < 3; i++) {
      assertThat(random.nextInt(bound)).as("#%d", i).isEqualTo(expected.values().get(i));
    }
    assertThat(random.nextInt()).as("nextInt() after").isEqualTo(expected.nextIntAfter());
  }

  // spec: core-random R5, R11, vectors V-L3 (rejections of 2^30 + 1)
  @ParameterizedTest(name = "seed {0}")
  @MethodSource("vectorVL3")
  void rejectionsOfTwoPow30PlusOneMatchVectorVL3(long seed, Rejection expected) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    assertThat(random.nextInt((1 << 30) + 1)).isEqualTo(expected.value());
    assertThat(random.steps()).isEqualTo(expected.steps());
    assertThat(random.nextInt()).isEqualTo(expected.nextIntAfter());
  }

  // spec: core-random R5 (other bound: r = next(31), v = r mod bound, accept when
  // r - v + (bound - 1) < 2^31, one step per attempt), R11
  @ParameterizedTest
  @ValueSource(
      ints = {3, 6, 7, 10, 26, 100, 1_000_003, (1 << 30) + 1, 1_431_655_766, 2_147_483_647})
  void otherBoundRedrawsUntilTheExactAcceptanceTestPasses(int bound) {
    long rejections = 0;
    for (long seed = 0; seed < 64; seed++) {
      Lcg48Random random = Lcg48Random.ofSeed(seed);
      Lcg48Random twin = Lcg48Random.ofSeed(seed);
      for (int i = 0; i < 8; i++) {
        long attempts = 0;
        long r;
        long v;
        do {
          r = twin.next(31);
          v = r % bound;
          attempts++;
        } while (r - v + (bound - 1) >= TWO_POW_31);
        rejections += attempts - 1;
        long stepsBefore = random.steps();
        assertThat(random.nextInt(bound)).as("seed %d draw %d", seed, i).isEqualTo((int) v);
        assertThat(random.steps() - stepsBefore).as("steps").isEqualTo(attempts);
      }
    }
    // Near 2^31 - 1 a rejection is too rare to show up here; 2^30 + 1 and 1431655766 reject
    // about one draw in two and one in three.
    if (bound == (1 << 30) + 1 || bound == 1_431_655_766) {
      assertThat(rejections).as("the rejection path is exercised").isPositive();
    }
  }

  // spec: core-random R5, edge case nextInt(2^31 - 1): rejection only when next(31) = 2^31 - 1
  @Test
  void largestBoundRejectsOnlyTheTopDraw() {
    Lcg48Random top = Lcg48Random.ofSeed(seedWhoseNextStateIs((TWO_POW_31 - 1) << 17));
    top.nextInt(Integer.MAX_VALUE);
    assertThat(top.steps()).isEqualTo(2L);

    Lcg48Random belowTop =
        Lcg48Random.ofSeed(seedWhoseNextStateIs(((TWO_POW_31 - 2) << 17) | 0x1FFFF));
    assertThat(belowTop.nextInt(Integer.MAX_VALUE)).isEqualTo(Integer.MAX_VALUE - 1);
    assertThat(belowTop.steps()).isEqualTo(1L);
  }

  /** Seed whose first step lands on {@code target} (R1, R2: the step is invertible mod 2^48). */
  private static long seedWhoseNextStateIs(long target) {
    BigInteger modulus = BigInteger.ONE.shiftLeft(48);
    BigInteger inverse = BigInteger.valueOf(MULTIPLIER).modInverse(modulus);
    long state = inverse.multiply(BigInteger.valueOf(target - INCREMENT)).mod(modulus).longValue();
    long seed = state ^ MULTIPLIER;
    Lcg48Random check = Lcg48Random.ofSeed(seed);
    check.skip(1);
    assertThat(check.state()).as("crafted seed").isEqualTo(target & MASK_48);
    return seed;
  }

  // spec: core-random R12 (S, provisional until P4): min < max gives min + nextInt(max - min + 1)
  @ParameterizedTest
  @CsvSource({
    "0, 99",
    "-50, 49",
    "1, 6",
    "0, 2147483645",
    "-2147483648, -2",
    "-1, 2147483645",
    "2147483646, 2147483647"
  })
  void provisionalRangeDrawIsMinPlusBoundedDrawOfTheSize(int min, int max) {
    Lcg48Random random = Lcg48Random.ofSeed(12345L);
    Lcg48Random twin = Lcg48Random.ofSeed(12345L);
    for (int i = 0; i < 20; i++) {
      int expected = min + twin.nextInt(max - min + 1);
      int value = random.nextIntBetweenInclusive(min, max);
      assertThat(value).as("draw %d", i).isEqualTo(expected).isBetween(min, max);
      assertThat(random.steps()).isEqualTo(twin.steps());
    }
  }

  // spec: core-random R12 (S, provisional), vector V-L2 seed 0 bound 100
  @Test
  void provisionalRangeDrawMatchesVectorVL2ShiftedByMin() {
    Lcg48Random random = Lcg48Random.ofSeed(0L);
    assertThat(random.nextIntBetweenInclusive(-50, 49)).isEqualTo(60 - 50);
    assertThat(random.nextIntBetweenInclusive(-50, 49)).isEqualTo(48 - 50);
    assertThat(random.nextIntBetweenInclusive(-50, 49)).isEqualTo(29 - 50);
    assertThat(random.nextInt()).isEqualTo(-1690734402);
  }

  // spec: core-random R12 (S, provisional until P4): min >= max returns min without a step
  @ParameterizedTest
  @CsvSource({
    "5, 5",
    "0, 0",
    "-2147483648, -2147483648",
    "2147483647, 2147483647",
    "6, 5",
    "2147483647, -2147483648"
  })
  void provisionalRangeWithMinNotBelowMaxReturnsMinWithoutStep(int min, int max) {
    Lcg48Random random = Lcg48Random.ofSeed(42L);
    long state = random.state();
    assertThat(random.nextIntBetweenInclusive(min, max)).isEqualTo(min);
    assertThat(random.state()).isEqualTo(state);
    assertThat(random.steps()).isZero();
  }

  // spec: core-random R12 (S, provisional, Q4): a size overflowing int fails like nextInt(bound)
  @ParameterizedTest
  @CsvSource({"-2147483648, 2147483647", "0, 2147483647", "-1, 2147483647", "-2147483648, -1"})
  void provisionalRangeWhoseSizeOverflowsFailsWithoutStep(int min, int max) {
    Lcg48Random random = Lcg48Random.ofSeed(42L);
    long state = random.state();
    assertThatThrownBy(() -> random.nextIntBetweenInclusive(min, max))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(random.state()).isEqualTo(state);
    assertThat(random.steps()).isZero();
  }
}

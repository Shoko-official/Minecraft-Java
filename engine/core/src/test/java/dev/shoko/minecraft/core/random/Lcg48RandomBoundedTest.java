package dev.shoko.minecraft.core.random;

import static dev.shoko.minecraft.core.random.Lcg48RandomTest.INCREMENT;
import static dev.shoko.minecraft.core.random.Lcg48RandomTest.MASK_48;
import static dev.shoko.minecraft.core.random.Lcg48RandomTest.MULTIPLIER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.core.random.Lcg48Vectors.BoundedDraws;
import dev.shoko.minecraft.core.random.Lcg48Vectors.Rejection;
import dev.shoko.minecraft.core.random.Lcg48Vectors.StrictRejection;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * G48 bounded draws: {@code nextInt(bound)} and its rejections (spec core-random R5, R11), and the
 * inclusive range draw (R12, level S, provisional until probe P4). The timeout turns a rejection
 * loop that never ends into a failure instead of a blocked build.
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class Lcg48RandomBoundedTest {

  private static final long TWO_POW_31 = 1L << 31;

  /** (2^31 + 1) / 3: with 3, the only bounds where a non-strict acceptance test differs (R5). */
  private static final int DIVISOR_OF_TWO_POW_31_PLUS_ONE = 715_827_883;

  static Stream<Arguments> vectorVL2() {
    return Lcg48Vectors.V_L2.stream().map(v -> Arguments.of(v.seed(), v.bound(), v));
  }

  static Stream<Arguments> vectorVL3() {
    return Lcg48Vectors.V_L3.stream().map(v -> Arguments.of(v.seed(), v));
  }

  static Stream<Arguments> vectorVL3b() {
    return Lcg48Vectors.V_L3B.stream().map(v -> Arguments.of(v.seed(), v));
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
      ints = {
        3,
        6,
        7,
        10,
        26,
        100,
        1_000_003,
        715_827_883,
        (1 << 30) + 1,
        1_431_655_766,
        2_147_483_647
      })
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
    // Near 2^31 - 1 a rejection is too rare to show up here; 2^30 + 1 rejects about one draw in
    // two, 715827883 and 1431655766 about one in three.
    if (bound == (1 << 30) + 1 || bound == 715_827_883 || bound == 1_431_655_766) {
      assertThat(rejections).as("the rejection path is exercised").isPositive();
    }
  }

  // spec: core-random R5 (strict inequality: 715827883 divides 2^31 + 1), R11, vectors V-L3b
  @ParameterizedTest(name = "seed {0}")
  @MethodSource("vectorVL3b")
  void rejectionsOf715827883MatchVectorVL3b(long seed, StrictRejection expected) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    int value = random.nextInt(DIVISOR_OF_TWO_POW_31_PLUS_ONE);
    assertThat(value).isEqualTo(expected.value()).isNotEqualTo(expected.nonStrictValue());
    assertThat(random.steps()).isEqualTo(expected.steps());
    assertThat(random.nextInt()).isEqualTo(expected.nextIntAfter());
  }

  // spec: core-random R5, vectors V-L3b: the first draw of every seed falls in the rejected top
  // third, where a non-strict test would accept it and return r - 2 * 715827883
  @ParameterizedTest(name = "seed {0}")
  @MethodSource("vectorVL3b")
  void firstDrawOfVectorVL3bIsInTheRejectedTopThird(long seed, StrictRejection expected) {
    long first = Lcg48Random.ofSeed(seed).next(31);
    assertThat(first).isGreaterThanOrEqualTo(2L * DIVISOR_OF_TWO_POW_31_PLUS_ONE);
    assertThat(first - 2L * DIVISOR_OF_TWO_POW_31_PLUS_ONE).isEqualTo(expected.nonStrictValue());
  }

  // spec: core-random R5 (strict inequality), vector V-L3b note on bound 3: only r = 2^31 - 2 and
  // r = 2^31 - 1 are rejected, so the test starts from a chosen state
  @ParameterizedTest
  @ValueSource(longs = {(1L << 31) - 2, (1L << 31) - 1})
  void boundThreeRejectsTheTwoTopDraws(long firstDraw) {
    long seed = seedWhoseNextStateIs(firstDraw << 17);
    Lcg48Random twin = Lcg48Random.ofSeed(seed);
    assertThat((long) twin.next(31)).isEqualTo(firstDraw);
    long second = twin.next(31);
    assertThat(second).as("second draw accepted").isLessThan(TWO_POW_31 - 2);

    Lcg48Random random = Lcg48Random.ofSeed(seed);
    assertThat(random.nextInt(3)).isEqualTo((int) (second % 3));
    assertThat(random.steps()).isEqualTo(2L);
  }

  // spec: core-random R5: the largest draw accepted for bound 3 is 2^31 - 3, in one step
  @Test
  void boundThreeAcceptsTheDrawJustBelowTheRejectedOnes() {
    Lcg48Random random = Lcg48Random.ofSeed(seedWhoseNextStateIs((TWO_POW_31 - 3) << 17));
    assertThat(random.nextInt(3)).isEqualTo((int) ((TWO_POW_31 - 3) % 3));
    assertThat(random.steps()).isEqualTo(1L);
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

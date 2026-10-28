package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Attack tests for G48 (spec core-random R1 to R12, interface requirements on the step counter):
 * mixed scripts, skip, step counter, invalid inputs and range extremes. Every expected value comes
 * from an independent reference: {@code java.util.Random} (R1 to R7, level D) wrapped to count its
 * steps, or direct long arithmetic on the 48-bit state. Distribution and concurrency attacks are in
 * {@link Lcg48RandomDistributionAttackTest}.
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class Lcg48RandomAttackTest {

  private static final long MUL = 0x5DEECE66DL;
  private static final long ADD = 0xBL;
  private static final long MASK = (1L << 48) - 1;

  /** java.util.Random with a step counter: every call to next(bits) is one step (R11). */
  private static final class CountingRandom extends Random {
    private static final long serialVersionUID = 1L;
    long steps;

    CountingRandom(long seed) {
      super(seed);
    }

    @Override
    protected int next(int bits) {
      steps++;
      return super.next(bits);
    }

    int bits(int bits) {
      return next(bits);
    }

    @Override
    public synchronized void setSeed(long seed) {
      super.setSeed(seed);
      steps = 0;
    }
  }

  static LongStream extremeSeeds() {
    SplittableRandom enumerator = new SplittableRandom(0xA77ACCL);
    return LongStream.concat(
        LongStream.of(
            0L,
            -1L,
            1L,
            Long.MIN_VALUE,
            Long.MAX_VALUE,
            1L << 48,
            (1L << 48) + 1,
            (1L << 48) - 1,
            MUL,
            ~MUL,
            MUL ^ MASK,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE),
        LongStream.generate(enumerator::nextLong).limit(20));
  }

  private static final int[] BOUNDS = {
    1,
    2,
    3,
    5,
    6,
    7,
    16,
    17,
    100,
    (1 << 30) - 1,
    1 << 30,
    (1 << 30) + 1,
    (1 << 30) + 3,
    Integer.MAX_VALUE - 1,
    Integer.MAX_VALUE,
    3 << 29
  };

  /**
   * Plays the same script of mixed calls (including setSeed while a gaussian is cached) on the
   * engine and on the reference, comparing every result, the step count and the state.
   */
  @ParameterizedTest
  @MethodSource("extremeSeeds")
  void mixedScriptMatchesJavaUtilRandom(long seed) {
    Lcg48Random engine = Lcg48Random.ofSeed(seed);
    CountingRandom reference = new CountingRandom(seed);
    SplittableRandom script = new SplittableRandom(seed ^ 0x5EEDL);
    for (int i = 0; i < 20_000; i++) {
      int op = script.nextInt(12);
      String what = "call " + i + " op " + op;
      switch (op) {
        case 0 -> assertThat(engine.nextInt()).as(what).isEqualTo(reference.nextInt());
        case 1 -> {
          int bound = BOUNDS[script.nextInt(BOUNDS.length)];
          assertThat(engine.nextInt(bound)).as(what).isEqualTo(reference.nextInt(bound));
        }
        case 2 -> assertThat(engine.nextLong()).as(what).isEqualTo(reference.nextLong());
        case 3 -> assertThat(engine.nextBoolean()).as(what).isEqualTo(reference.nextBoolean());
        case 4 ->
            assertThat(Float.floatToRawIntBits(engine.nextFloat()))
                .as(what)
                .isEqualTo(Float.floatToRawIntBits(reference.nextFloat()));
        case 5 ->
            assertThat(Double.doubleToRawLongBits(engine.nextDouble()))
                .as(what)
                .isEqualTo(Double.doubleToRawLongBits(reference.nextDouble()));
        case 6, 7 ->
            assertThat(Double.doubleToRawLongBits(engine.nextGaussian()))
                .as(what)
                .isEqualTo(Double.doubleToRawLongBits(reference.nextGaussian()));
        case 8 -> {
          long newSeed = script.nextLong();
          engine.setSeed(newSeed);
          reference.setSeed(newSeed);
        }
        case 9 -> {
          int n = script.nextInt(50);
          engine.skip(n);
          // skip keeps the cached gaussian, as n next(32) calls on the reference do.
          for (int k = 0; k < n; k++) {
            reference.nextInt();
          }
        }
        case 10 -> {
          int bits = 1 + script.nextInt(32);
          assertThat(engine.next(bits)).as(what).isEqualTo(reference.bits(bits));
        }
        default -> checkRange(engine, reference, script.nextInt(), script.nextInt(), what);
      }
      assertThat(engine.steps()).as(what + " steps").isEqualTo(reference.steps);
    }
  }

  private static void checkRange(
      Lcg48Random engine, CountingRandom reference, int a, int b, String what) {
    int min = Math.min(a, b);
    int max = Math.max(a, b);
    long size = (long) max - min + 1;
    if (min == max) {
      assertThat(engine.nextIntBetweenInclusive(min, max)).as(what).isEqualTo(min);
    } else if (size > Integer.MAX_VALUE) {
      long before = engine.steps();
      assertThatThrownBy(() -> engine.nextIntBetweenInclusive(min, max))
          .as(what)
          .isInstanceOf(IllegalArgumentException.class);
      assertThat(engine.steps()).isEqualTo(before);
    } else {
      assertThat(engine.nextIntBetweenInclusive(min, max))
          .as(what)
          .isEqualTo(min + reference.nextInt((int) size));
    }
  }

  // R4: nextLong sign extension over 10^6 draws against java.util.Random
  @Test
  void oneMillionNextLongMatchJavaUtilRandom() {
    for (long seed : new long[] {0L, -1L, 1L << 48, (1L << 48) + 1}) {
      Lcg48Random engine = Lcg48Random.ofSeed(seed);
      Random reference = new Random(seed);
      for (int i = 0; i < 1_000_000; i++) {
        long expected = reference.nextLong();
        long actual = engine.nextLong();
        if (actual != expected) {
          assertThat(actual).as("seed %d draw %d", seed, i).isEqualTo(expected);
        }
      }
    }
  }

  // R2: seeds congruent mod 2^48 give the same stream, the others differ
  @Test
  void seedsCongruentModulo2Pow48AreIdentical() {
    assertThat(Lcg48Random.ofSeed(1L).state())
        .isEqualTo(Lcg48Random.ofSeed((1L << 48) + 1).state());
    assertThat(Lcg48Random.ofSeed(1L).state())
        .isEqualTo(Lcg48Random.ofSeed(Long.MIN_VALUE + 1).state());
    assertThat(Lcg48Random.ofSeed(-1L).state()).isEqualTo(Lcg48Random.ofSeed(MASK).state());
    assertThat(Lcg48Random.ofSeed(MUL).state()).isZero();
  }

  // R10: skip(n) against an independent loop, n up to 10^7
  @ParameterizedTest
  @ValueSource(
      longs = {0, 1, 2, 3, 4, 63, 64, 65, 1023, 1024, 1025, 999_983, 4_194_304, 10_000_000})
  void skipMatchesAnIndependentLoop(long n) {
    for (long seed : new long[] {0L, Long.MIN_VALUE, (1L << 48) + 1, MUL}) {
      Lcg48Random engine = Lcg48Random.ofSeed(seed);
      long x = (seed ^ MUL) & MASK;
      for (long i = 0; i < n; i++) {
        x = (x * MUL + ADD) & MASK;
      }
      engine.skip(n);
      assertThat(engine.state()).as("seed %d n %d", seed, n).isEqualTo(x);
      assertThat(engine.steps()).isEqualTo(n);
    }
  }

  // R10: skip(a) then skip(b) equals skip(a + b) for random a, b, and 2^48 is the period
  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  void skipComposesAndHasPeriod2Pow48() {
    SplittableRandom script = new SplittableRandom(99L);
    for (int i = 0; i < 2_000; i++) {
      long a = script.nextLong(Long.MAX_VALUE / 2);
      long b = script.nextLong(Long.MAX_VALUE / 2);
      long seed = script.nextLong();
      Lcg48Random split = Lcg48Random.ofSeed(seed);
      split.skip(a);
      split.skip(b);
      Lcg48Random whole = Lcg48Random.ofSeed(seed);
      whole.skip(a + b);
      assertThat(split.state()).isEqualTo(whole.state());
      Lcg48Random reduced = Lcg48Random.ofSeed(seed);
      reduced.skip((a + b) & MASK);
      assertThat(whole.state()).isEqualTo(reduced.state());
    }
  }

  /**
   * Interface requirement (steps() saturates at Long.MAX_VALUE, vector V-L4). Adapted from the
   * original attack, which only required a non-negative counter: the updated spec makes the counter
   * saturate instead of overflowing, while the state stays exact.
   */
  @Test
  void stepCounterSaturatesAfterAHugeSkip() {
    Lcg48Random random = Lcg48Random.ofSeed(-77L);
    random.skip(Long.MAX_VALUE);
    random.nextInt();
    assertThat(random.steps())
        .as("steps after skip(Long.MAX_VALUE) + 1 step")
        .isNotNegative()
        .isEqualTo(Long.MAX_VALUE);
  }

  // Same through two legal skips: the state is right (period 2^48), the counter saturates.
  @Test
  void stepCounterSaturatesAfterTwoHugeSkips() {
    Lcg48Random random = Lcg48Random.ofSeed(5L);
    random.skip(Long.MAX_VALUE);
    random.skip(Long.MAX_VALUE);
    long x = (5L ^ MUL) & MASK;
    Lcg48Random reference = Lcg48Random.ofSeed(5L);
    reference.skip((2 * (Long.MAX_VALUE & MASK)) & MASK);
    assertThat(random.state()).isEqualTo(reference.state()).isNotEqualTo(x);
    assertThat(random.steps()).isEqualTo(Long.MAX_VALUE);
  }

  // next(bits) outside 1..32, and nextInt(bound <= 0): typed error, no step
  @ParameterizedTest
  @ValueSource(ints = {0, -1, 33, 64, Integer.MIN_VALUE, Integer.MAX_VALUE})
  void invalidBitsFailWithoutStep(int bits) {
    Lcg48Random random = Lcg48Random.ofSeed(3L);
    long state = random.state();
    assertThatThrownBy(() -> random.next(bits)).isInstanceOf(IllegalArgumentException.class);
    assertThat(random.state()).isEqualTo(state);
    assertThat(random.steps()).isZero();
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, Integer.MIN_VALUE, -(1 << 30)})
  void invalidBoundsFailWithoutStep(int bound) {
    Lcg48Random random = Lcg48Random.ofSeed(3L);
    long state = random.state();
    assertThatThrownBy(() -> random.nextInt(bound)).isInstanceOf(IllegalArgumentException.class);
    assertThat(random.state()).isEqualTo(state);
    assertThat(random.steps()).isZero();
  }

  // R12 extremes of nextIntBetweenInclusive against a long computation
  @Test
  void inclusiveRangeExtremes() {
    int[][] ranges = {
      {Integer.MIN_VALUE, Integer.MIN_VALUE + 1},
      {Integer.MIN_VALUE, -2},
      {0, Integer.MAX_VALUE - 1},
      {Integer.MAX_VALUE - 1, Integer.MAX_VALUE},
      {-1, Integer.MAX_VALUE - 2},
      {Integer.MIN_VALUE + 1, -1},
    };
    for (int[] range : ranges) {
      Lcg48Random engine = Lcg48Random.ofSeed(55L);
      Random reference = new Random(55L);
      int size = (int) ((long) range[1] - range[0] + 1);
      for (int i = 0; i < 1000; i++) {
        int v = engine.nextIntBetweenInclusive(range[0], range[1]);
        assertThat(v).isEqualTo(range[0] + reference.nextInt(size)).isBetween(range[0], range[1]);
      }
    }
    int[][] overflowing = {
      {Integer.MIN_VALUE, Integer.MAX_VALUE},
      {Integer.MIN_VALUE, -1},
      {0, Integer.MAX_VALUE},
      {-1, Integer.MAX_VALUE - 1},
      {Integer.MIN_VALUE, Integer.MAX_VALUE - 1}
    };
    for (int[] range : overflowing) {
      Lcg48Random engine = Lcg48Random.ofSeed(55L);
      assertThatThrownBy(() -> engine.nextIntBetweenInclusive(range[0], range[1]))
          .isInstanceOf(IllegalArgumentException.class);
      assertThat(engine.steps()).isZero();
    }
    Lcg48Random engine = Lcg48Random.ofSeed(55L);
    assertThat(engine.nextIntBetweenInclusive(Integer.MAX_VALUE, Integer.MIN_VALUE))
        .isEqualTo(Integer.MAX_VALUE);
    assertThat(engine.nextIntBetweenInclusive(5, 5)).isEqualTo(5);
    assertThat(engine.steps()).isZero();
  }
}

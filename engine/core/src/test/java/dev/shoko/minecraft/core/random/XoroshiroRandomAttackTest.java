package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Attack tests for X128 (spec core-random R13 to R21, R24). Expected values come from an
 * independent reference written here from [BV] and the hypotheses H-X1 to H-X5 as the spec states
 * them, and from the JDK's xoroshiro128++ for seeded streams.
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class XoroshiroRandomAttackTest {

  private static final long K_SQRT2 = 0x6A09E667F3BCC909L;
  private static final long K_PHI = 0x9E3779B97F4A7C15L;

  /** Independent reference of R13 and of the derived draws R16 to R20 as the spec words them. */
  private static final class Ref {
    long s0;
    long s1;
    long steps;
    boolean hasCached;
    double cached;

    Ref(long s0, long s1) {
      this.s0 = s0;
      this.s1 = s1;
    }

    static Ref ofSeed(long seed) {
      long lo = seed ^ K_SQRT2;
      long hi = lo + K_PHI;
      return new Ref(stafford13(lo), stafford13(hi));
    }

    long raw() {
      long a = s0;
      long b = s1;
      long r = Long.rotateLeft(a + b, 17) + a;
      long t = b ^ a;
      s0 = Long.rotateLeft(a, 49) ^ t ^ (t << 21);
      s1 = Long.rotateLeft(t, 28);
      steps++;
      return r;
    }

    int nextIntBounded(int bound) {
      long u = raw() & 0xFFFFFFFFL;
      long m = u * bound;
      if (Long.compareUnsigned(m & 0xFFFFFFFFL, bound) < 0) {
        long t = Long.remainderUnsigned((1L << 32) - bound, bound);
        while ((m & 0xFFFFFFFFL) < t) {
          u = raw() & 0xFFFFFFFFL;
          m = u * bound;
        }
      }
      return (int) (m >>> 32);
    }

    double nextDouble() {
      return (double) (raw() >>> 11) / (double) (1L << 53);
    }

    double gaussian() {
      if (hasCached) {
        hasCached = false;
        return cached;
      }
      double v1;
      double v2;
      double s;
      do {
        v1 = 2 * nextDouble() - 1;
        v2 = 2 * nextDouble() - 1;
        s = v1 * v1 + v2 * v2;
      } while (!(s > 0 && s < 1));
      double m = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
      cached = v2 * m;
      hasCached = true;
      return v1 * m;
    }
  }

  private static long stafford13(long z) {
    // SplittableRandom.nextLong() is mix64(seed + gamma) with gamma = K_PHI (R23 recoupement).
    return new SplittableRandom(z - K_PHI).nextLong();
  }

  static Stream<Arguments> extremeStates() {
    return Stream.of(
        Arguments.of(0L, 1L),
        Arguments.of(1L, 0L),
        Arguments.of(Long.MIN_VALUE, 0L),
        Arguments.of(0L, Long.MIN_VALUE),
        Arguments.of(-1L, 0L),
        Arguments.of(0L, -1L),
        Arguments.of(-1L, -1L),
        Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE),
        Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE),
        Arguments.of(0L, 1L << 20));
  }

  private static final int[] BOUNDS = {
    1,
    2,
    3,
    6,
    7,
    16,
    100,
    (1 << 30) + 1,
    1 << 30,
    Integer.MAX_VALUE,
    Integer.MAX_VALUE - 1,
    2147483646,
    (1 << 31) - 3
  };

  @ParameterizedTest
  @MethodSource("extremeStates")
  void mixedScriptFromExtremeRawStatesMatchesTheReference(long s0, long s1) {
    XoroshiroRandom engine = XoroshiroRandom.ofState(new Seed128(s0, s1));
    Ref ref = new Ref(s0, s1);
    SplittableRandom script = new SplittableRandom(s0 * 31 + s1);
    for (int i = 0; i < 50_000; i++) {
      int op = script.nextInt(9);
      String what = "call " + i + " op " + op;
      switch (op) {
        case 0 -> assertThat(engine.nextLong()).as(what).isEqualTo(ref.raw());
        case 1 -> assertThat(engine.nextInt()).as(what).isEqualTo((int) ref.raw());
        case 2 -> {
          int bound = BOUNDS[script.nextInt(BOUNDS.length)];
          assertThat(engine.nextInt(bound)).as(what).isEqualTo(ref.nextIntBounded(bound));
        }
        case 3 -> assertThat(engine.nextBoolean()).as(what).isEqualTo((ref.raw() & 1) != 0);
        case 4 ->
            assertThat(engine.nextFloat())
                .as(what)
                .isEqualTo((float) ((ref.raw() >>> 40) / (double) (1 << 24)));
        case 5 -> assertThat(engine.nextDouble()).as(what).isEqualTo(ref.nextDouble());
        case 6 ->
            assertThat(Double.doubleToRawLongBits(engine.nextGaussian()))
                .as(what)
                .isEqualTo(Double.doubleToRawLongBits(ref.gaussian()));
        case 7 -> {
          int n = script.nextInt(40);
          engine.skip(n);
          for (int k = 0; k < n; k++) {
            ref.raw();
          }
        }
        default -> {
          int a = script.nextInt();
          int b = script.nextInt(3) == 0 ? a + script.nextInt(10) : script.nextInt();
          int min = Math.min(a, b);
          int max = Math.max(a, b);
          long size = (long) max - min + 1;
          if (min == max) {
            assertThat(engine.nextIntBetweenInclusive(min, max)).as(what).isEqualTo(min);
          } else if (size > Integer.MAX_VALUE) {
            assertThatThrownBy(() -> engine.nextIntBetweenInclusive(min, max))
                .isInstanceOf(IllegalArgumentException.class);
          } else {
            assertThat(engine.nextIntBetweenInclusive(min, max))
                .as(what)
                .isEqualTo(min + ref.nextIntBounded((int) size));
          }
        }
      }
      assertThat(engine.steps()).as(what + " steps").isEqualTo(ref.steps);
    }
    assertThat(engine.state()).isEqualTo(new Seed128(ref.s0, ref.s1));
  }

  // R24: extreme seeds, including those whose unmixed words are 0 (lo = 0 or hi = 0 before mix)
  @ParameterizedTest
  @ValueSource(
      longs = {
        0L,
        -1L,
        Long.MIN_VALUE,
        Long.MAX_VALUE,
        1L << 48,
        (1L << 48) + 1,
        K_SQRT2,
        -K_PHI ^ K_SQRT2,
        K_SQRT2 ^ Long.MIN_VALUE
      })
  void extremeSeedsMatchTheJdkAndTheReference(long seed) {
    RandomGenerator jdk =
        RandomGeneratorFactory.<RandomGenerator>of("Xoroshiro128PlusPlus").create(seed);
    XoroshiroRandom engine = XoroshiroRandom.ofSeed(seed);
    Ref ref = Ref.ofSeed(seed);
    assertThat(engine.state()).isEqualTo(new Seed128(ref.s0, ref.s1));
    assertThat(engine.state().lo() | engine.state().hi()).isNotZero();
    for (int i = 0; i < 10_000; i++) {
      long expected = jdk.nextLong();
      assertThat(ref.raw()).isEqualTo(expected);
      assertThat(engine.nextLong()).as("seed %d draw %d", seed, i).isEqualTo(expected);
    }
  }

  // R18: a forced rejection. State (0, 2^20): first output low 32 bits are 0, rejected for bound 6.
  @Test
  void forcedRejectionTakesTwoSteps() {
    XoroshiroRandom engine = XoroshiroRandom.ofState(new Seed128(0L, 1L << 20));
    Ref ref = new Ref(0L, 1L << 20);
    assertThat(new Ref(0L, 1L << 20).raw() & 0xFFFFFFFFL).isZero();
    assertThat(engine.nextInt(6)).isEqualTo(ref.nextIntBounded(6));
    assertThat(engine.steps()).isEqualTo(2L).isEqualTo(ref.steps);
    // Power of two: threshold 0, the zero draw is accepted.
    XoroshiroRandom power = XoroshiroRandom.ofState(new Seed128(0L, 1L << 20));
    assertThat(power.nextInt(16)).isZero();
    assertThat(power.steps()).isEqualTo(1L);
  }

  // R19: the output with every bit set gives the largest float and double, still below 1
  @Test
  void allOnesOutputStaysBelowOne() {
    assertThat(new Ref(0L, -1L).raw()).isEqualTo(-1L);
    assertThat(XoroshiroRandom.ofState(new Seed128(0L, -1L)).nextFloat())
        .isEqualTo(1f - 0x1.0p-24f)
        .isLessThan(1f);
    assertThat(XoroshiroRandom.ofState(new Seed128(0L, -1L)).nextDouble())
        .isEqualTo(1d - 0x1.0p-53)
        .isLessThan(1d);
    assertThat(XoroshiroRandom.ofState(new Seed128(0L, -1L)).nextInt(Integer.MAX_VALUE))
        .isBetween(0, Integer.MAX_VALUE - 1);
  }

  // R13, R15: the state never becomes all-zero, over five million steps from a sparse state
  @Test
  void stateNeverBecomesAllZero() {
    XoroshiroRandom engine = XoroshiroRandom.ofState(new Seed128(0L, 1L));
    for (int i = 0; i < 5_000_000; i++) {
      engine.nextLong();
      Seed128 s = engine.state();
      if (s.lo() == 0 && s.hi() == 0) {
        assertThat(s).as("step %d", i).isNotEqualTo(new Seed128(0L, 0L));
      }
    }
  }

  // state() is a snapshot, not a live view of the generator
  @Test
  void stateIsASnapshot() {
    XoroshiroRandom engine = XoroshiroRandom.ofSeed(1L);
    Seed128 before = engine.state();
    Seed128 copy = new Seed128(before.lo(), before.hi());
    engine.nextLong();
    assertThat(before).isEqualTo(copy);
    assertThat(engine.state()).isNotEqualTo(before);
    Seed128 given = new Seed128(5L, 6L);
    XoroshiroRandom fromGiven = XoroshiroRandom.ofState(given);
    fromGiven.nextLong();
    assertThat(given).isEqualTo(new Seed128(5L, 6L));
  }

  // R20: gaussian cache survives draws and skip, setSeed drops it, against the reference
  @Test
  void gaussianCacheAgainstTheReference() {
    XoroshiroRandom engine = XoroshiroRandom.ofSeed(42L);
    Ref ref = Ref.ofSeed(42L);
    assertThat(engine.nextGaussian()).isEqualTo(ref.gaussian());
    engine.skip(5);
    for (int i = 0; i < 5; i++) {
      ref.raw();
    }
    assertThat(engine.nextInt(7)).isEqualTo(ref.nextIntBounded(7));
    long steps = engine.steps();
    assertThat(engine.nextGaussian()).isEqualTo(ref.gaussian());
    assertThat(engine.steps()).isEqualTo(steps);
    engine.nextGaussian();
    engine.setSeed(42L);
    Ref fresh = Ref.ofSeed(42L);
    assertThat(engine.nextGaussian()).isEqualTo(fresh.gaussian());
    assertThat(engine.steps()).isEqualTo(fresh.steps);
  }

  // R28 (provisional): fork takes one step, child = Init64 of that long; the parent keeps its cache
  @Test
  void forkAgainstTheReference() {
    XoroshiroRandom parent = XoroshiroRandom.ofSeed(-5L);
    Ref ref = Ref.ofSeed(-5L);
    parent.nextGaussian();
    ref.gaussian();
    long steps = parent.steps();
    XoroshiroRandom child = parent.fork();
    assertThat(parent.steps()).isEqualTo(steps + 1);
    Ref childRef = Ref.ofSeed(ref.raw());
    for (int i = 0; i < 1000; i++) {
      assertThat(child.nextLong()).isEqualTo(childRef.raw());
    }
    assertThat(parent.nextGaussian()).isEqualTo(ref.gaussian());
    assertThat(parent.nextLong()).isEqualTo(ref.raw());
  }

  // R18: rough chi-square for small bounds
  @ParameterizedTest
  @ValueSource(ints = {2, 3, 5, 6, 7, 10, 13, 27, 100})
  void smallBoundsAreRoughlyUniform(int bound) {
    XoroshiroRandom random = XoroshiroRandom.ofSeed(20261028L + bound);
    int draws = 200_000 * Math.max(1, bound / 10);
    long[] counts = new long[bound];
    for (int i = 0; i < draws; i++) {
      counts[random.nextInt(bound)]++;
    }
    double expected = (double) draws / bound;
    double chi2 = 0;
    for (long c : counts) {
      chi2 += (c - expected) * (c - expected) / expected;
    }
    int df = bound - 1;
    assertThat(chi2).isLessThan(df + 8 * Math.sqrt(2.0 * df) + 10);
  }

  // R21: rejection rate for a bound close to 2^31 matches bound / 2^32 roughly (t / 2^32 exactly)
  @Test
  void rejectionRateNearTwoPow31() {
    int bound = (1 << 30) + (1 << 29) + 1;
    XoroshiroRandom random = XoroshiroRandom.ofSeed(77L);
    int calls = 2_000_000;
    for (int i = 0; i < calls; i++) {
      random.nextInt(bound);
    }
    double rejectProbability =
        (double) Long.remainderUnsigned((1L << 32) - bound, bound) / (1L << 32);
    double expectedExtra = rejectProbability / (1 - rejectProbability) * calls;
    double extra = random.steps() - calls;
    assertThat(extra)
        .isBetween(
            expectedExtra - 6 * Math.sqrt(expectedExtra),
            expectedExtra + 6 * Math.sqrt(expectedExtra));
  }
}

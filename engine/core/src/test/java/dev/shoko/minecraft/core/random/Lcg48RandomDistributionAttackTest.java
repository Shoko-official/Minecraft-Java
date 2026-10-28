package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Attack tests for G48 (spec core-random R5 to R7, R28): rejection rate, rough uniformity, float
 * and double extremes, gaussian cache, fork, equivalent call sequences and two instances on two
 * threads. References: {@code java.util.Random} (level D) and direct long arithmetic. The
 * statistical tests use fixed seeds and very loose bounds, so they are deterministic.
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class Lcg48RandomDistributionAttackTest {

  private static final long MUL = 0x5DEECE66DL;
  private static final long ADD = 0xBL;
  private static final long MASK = (1L << 48) - 1;

  // R5, R11: rejection frequency of nextInt(2^30 + 1): about 2 steps per call on average
  @Test
  void rejectionRateOfNextIntTwoPow30PlusOneMatchesTheory() {
    int bound = (1 << 30) + 1;
    Lcg48Random random = Lcg48Random.ofSeed(4242L);
    int calls = 1_000_000;
    for (int i = 0; i < calls; i++) {
      assertThat(random.nextInt(bound)).isBetween(0, bound - 1);
    }
    // Acceptance probability p = bound * floor(2^31 / bound) / 2^31, mean steps 1/p.
    double p = (double) bound * Math.floorDiv(1L << 31, bound) / (1L << 31);
    double mean = (double) random.steps() / calls;
    // Geometric: sd per call = sqrt(1 - p) / p, about 1.41; 6 sigma over 10^6 calls.
    double tolerance = 6 * Math.sqrt(1 - p) / p / Math.sqrt(calls);
    assertThat(mean).isCloseTo(1 / p, Offset.offset(tolerance));
  }

  // R5: rough chi-square on small bounds with a fixed seed
  @ParameterizedTest
  @ValueSource(ints = {2, 3, 5, 6, 7, 10, 13, 27, 100})
  void smallBoundsAreRoughlyUniform(int bound) {
    Lcg48Random random = Lcg48Random.ofSeed(20261028L + bound);
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
    // Very loose bound: mean df, sd sqrt(2 df); 8 sd above the mean.
    int df = bound - 1;
    assertThat(chi2).isLessThan(df + 8 * Math.sqrt(2.0 * df) + 10);
  }

  // R6: nextFloat and nextDouble stay in [0, 1) and are exact multiples (reduced from 10^6 to
  // 2 * 10^5 draws, assertions only on failure, to keep the suite fast)
  @Test
  void floatsAndDoublesStayBelowOne() {
    Lcg48Random random = Lcg48Random.ofSeed(-123L);
    for (int i = 0; i < 200_000; i++) {
      float f = random.nextFloat();
      double d = random.nextDouble();
      boolean floatOk = f >= 0f && f < 1f && f * 0x1.0p24f == (float) Math.floor(f * 0x1.0p24f);
      boolean doubleOk = d >= 0d && d < 1d && d * 0x1.0p53 == Math.floor(d * 0x1.0p53);
      if (!floatOk || !doubleOk) {
        assertThat(f).as("float %d", i).isGreaterThanOrEqualTo(0f).isLessThan(1f);
        assertThat(d).as("double %d", i).isGreaterThanOrEqualTo(0d).isLessThan(1d);
        assertThat(f * 0x1.0p24f).isEqualTo((float) Math.floor(f * 0x1.0p24f));
        assertThat(d * 0x1.0p53).isEqualTo(Math.floor(d * 0x1.0p53));
      }
    }
  }

  // R6: largest float, built from a state whose next step has its 24 high bits set
  @Test
  void largestFloatIsBelowOne() {
    long wantedNext = MASK;
    long inverse = modInverse48(MUL);
    long state0 = ((wantedNext - ADD) * inverse) & MASK;
    Lcg48Random random = Lcg48Random.ofSeed(state0 ^ MUL);
    assertThat(random.state()).isEqualTo(state0);
    float f = random.nextFloat();
    assertThat(f).isEqualTo(1f - 0x1.0p-24f).isLessThan(1f);
    Random reference = new Random(state0 ^ MUL);
    assertThat(reference.nextFloat()).isEqualTo(f);
  }

  private static long modInverse48(long a) {
    long x = 1;
    for (int i = 0; i < 6; i++) {
      x = x * (2 - a * x);
    }
    return x & MASK;
  }

  // R7: the cached gaussian survives interleaved draws and skip; setSeed drops it
  @Test
  void gaussianCacheSurvivesDrawsButNotSetSeed() {
    Lcg48Random engine = Lcg48Random.ofSeed(7L);
    Random reference = new Random(7L);
    assertThat(engine.nextGaussian()).isEqualTo(reference.nextGaussian());
    assertThat(engine.nextInt(10)).isEqualTo(reference.nextInt(10));
    engine.skip(3);
    for (int i = 0; i < 3; i++) {
      reference.nextInt();
    }
    long steps = engine.steps();
    assertThat(engine.nextGaussian()).isEqualTo(reference.nextGaussian());
    assertThat(engine.steps()).as("cached gaussian costs nothing").isEqualTo(steps);
    engine.nextGaussian();
    reference.nextGaussian();
    engine.setSeed(7L);
    reference.setSeed(7L);
    assertThat(engine.nextGaussian()).isEqualTo(reference.nextGaussian());
  }

  // R28 (provisional): fork takes 2 steps from the parent, child = new seed from nextLong
  @Test
  void forkMatchesTheReferenceAndLeavesTheParentIndependent() {
    Lcg48Random parent = Lcg48Random.ofSeed(11L);
    Random referenceParent = new Random(11L);
    parent.nextGaussian();
    referenceParent.nextGaussian();
    long steps = parent.steps();
    Lcg48Random child = parent.fork();
    assertThat(parent.steps()).isEqualTo(steps + 2);
    assertThat(child.steps()).isZero();
    Random referenceChild = new Random(referenceParent.nextLong());
    for (int i = 0; i < 1000; i++) {
      assertThat(child.nextInt()).isEqualTo(referenceChild.nextInt());
    }
    // The child does not touch the parent, and the parent keeps its cached gaussian.
    assertThat(parent.steps()).isEqualTo(steps + 2);
    assertThat(parent.nextGaussian()).isEqualTo(referenceParent.nextGaussian());
  }

  // Equivalent call sequences reach the same state
  @Test
  void equivalentCallSequencesReachTheSameState() {
    Lcg48Random a = Lcg48Random.ofSeed(1234L);
    Lcg48Random b = Lcg48Random.ofSeed(1234L);
    a.nextLong();
    a.nextDouble();
    a.nextInt(16);
    a.nextBoolean();
    b.skip(2);
    b.next(26);
    b.next(27);
    b.nextFloat();
    b.next(7);
    assertThat(a.state()).isEqualTo(b.state());
    assertThat(a.steps()).isEqualTo(b.steps());
    assertThat(a.nextInt()).isEqualTo(b.nextInt());
  }

  // Two threads on two instances share nothing
  @Test
  void twoThreadsOnTwoInstancesShareNothing() throws Exception {
    long[] sequential = run(Lcg48Random.ofSeed(9L));
    ExecutorService pool = Executors.newFixedThreadPool(4);
    try {
      List<Future<long[]>> futures = new ArrayList<>();
      for (int t = 0; t < 8; t++) {
        Callable<long[]> task = () -> run(Lcg48Random.ofSeed(9L));
        futures.add(pool.submit(task));
      }
      for (Future<long[]> f : futures) {
        assertThat(f.get(60, TimeUnit.SECONDS)).isEqualTo(sequential);
      }
    } finally {
      pool.shutdownNow();
    }
  }

  private static long[] run(RandomSource random) {
    long[] out = new long[200_000];
    for (int i = 0; i < out.length; i++) {
      out[i] =
          switch (i % 4) {
            case 0 -> random.nextLong();
            case 1 -> Double.doubleToRawLongBits(random.nextGaussian());
            case 2 -> random.nextInt(1_000_003);
            default -> Double.doubleToRawLongBits(random.nextDouble());
          };
    }
    return out;
  }
}

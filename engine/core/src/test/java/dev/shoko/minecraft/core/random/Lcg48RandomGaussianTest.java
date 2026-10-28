package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Vectors.BasicDraws;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * G48 gaussian: Marsaglia's polar method with one cached value (spec core-random R7, R11) and the
 * mixed sequence V-L5. The logarithm is {@link StrictMath#log(double)}: the spec makes it a
 * provisional choice (R9, level S, question Q3), so every test that depends on the exact bits of a
 * gaussian is marked provisional.
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class Lcg48RandomGaussianTest {

  static Stream<Arguments> vectorVL1() {
    return Lcg48Vectors.V_L1.stream().map(v -> Arguments.of(v.seed(), v));
  }

  // spec: core-random R7, R9 (S, provisional StrictMath), vectors V-L1
  @ParameterizedTest(name = "seed {0}")
  @MethodSource("vectorVL1")
  void provisionalStrictMathGaussianMatchesVectorVL1(long seed, BasicDraws expected) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    for (int i = 0; i < expected.gaussians().size(); i++) {
      assertThat(Double.doubleToRawLongBits(random.nextGaussian()))
          .as("nextGaussian #%d", i)
          .isEqualTo(Hex.u64(expected.gaussians().get(i)));
    }
  }

  // spec: core-random R9 (S, provisional until Q3), vector V-L7: seed 42 is a case where
  // Math.log and StrictMath.log differ by one ulp
  @Test
  void provisionalGaussianUsesStrictMathLogarithmNotMathLogarithm() {
    Lcg48Random random = Lcg48Random.ofSeed(42L);
    long first = Double.doubleToRawLongBits(random.nextGaussian());
    long second = Double.doubleToRawLongBits(random.nextGaussian());
    assertThat(Hex.of(first)).isEqualTo("0x3FF2453E82115D86").isNotEqualTo("0x3FF2453E82115D87");
    assertThat(Hex.of(second)).isEqualTo("0x3FED6BCA38120847").isNotEqualTo("0x3FED6BCA38120848");
  }

  static LongStream replaySeeds() {
    return LongStream.concat(
        LongStream.range(0, 40), LongStream.of(-1L, Long.MIN_VALUE, 0x5DEECE66DL));
  }

  // spec: core-random R7 (polar method, four steps per attempt, cache), R9 (provisional
  // StrictMath), R11
  @ParameterizedTest
  @MethodSource("replaySeeds")
  void provisionalGaussianPairFollowsThePolarMethodAndCachesTheSecondValue(long seed) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    Lcg48Random twin = Lcg48Random.ofSeed(seed);
    for (int pair = 0; pair < 10; pair++) {
      double v1;
      double v2;
      double s;
      long attempts = 0;
      do {
        v1 = 2 * twin.nextDouble() - 1;
        v2 = 2 * twin.nextDouble() - 1;
        s = v1 * v1 + v2 * v2;
        attempts++;
      } while (s >= 1 || s == 0);
      double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);

      long before = random.steps();
      assertThat(random.nextGaussian()).as("pair %d first", pair).isEqualTo(v1 * multiplier);
      assertThat(random.steps() - before).as("steps of the first").isEqualTo(4 * attempts);
      assertThat(random.nextGaussian()).as("pair %d cached", pair).isEqualTo(v2 * multiplier);
      assertThat(random.steps() - before)
          .as("the cached value costs nothing")
          .isEqualTo(4 * attempts);
    }
  }

  // spec: core-random R7, edge case "the cached value survives other draws in between"
  @Test
  void cachedGaussianSurvivesInterveningDraws() {
    Lcg48Random fresh = Lcg48Random.ofSeed(7L);
    double first = fresh.nextGaussian();
    double cached = fresh.nextGaussian();

    Lcg48Random random = Lcg48Random.ofSeed(7L);
    assertThat(random.nextGaussian()).isEqualTo(first);
    random.nextInt();
    random.nextLong();
    random.nextInt(10);
    random.nextDouble();
    long steps = random.steps();
    assertThat(random.nextGaussian()).isEqualTo(cached);
    assertThat(random.steps()).isEqualTo(steps);
  }

  // spec: core-random R2, R7 (initialization drops the cached gaussian)
  @Test
  void setSeedDropsTheCachedGaussian() {
    Lcg48Random fresh = Lcg48Random.ofSeed(99L);
    double first = fresh.nextGaussian();
    double cached = fresh.nextGaussian();
    long firstCallSteps = stepsOfFirstGaussian(99L);

    Lcg48Random random = Lcg48Random.ofSeed(99L);
    random.nextGaussian();
    random.setSeed(99L);
    double afterReset = random.nextGaussian();
    assertThat(afterReset).isEqualTo(first).isNotEqualTo(cached);
    assertThat(random.steps()).isEqualTo(firstCallSteps).isPositive();
  }

  private static long stepsOfFirstGaussian(long seed) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    random.nextGaussian();
    return random.steps();
  }

  // RandomSource.skip contract: the cached gaussian, if any, is kept
  @Test
  void skipKeepsTheCachedGaussian() {
    Lcg48Random fresh = Lcg48Random.ofSeed(5L);
    fresh.nextGaussian();
    double cached = fresh.nextGaussian();

    Lcg48Random random = Lcg48Random.ofSeed(5L);
    random.nextGaussian();
    random.skip(1000);
    assertThat(random.nextGaussian()).isEqualTo(cached);
  }

  // spec: core-random R4 to R7, R11, vector V-L5 (13 steps for calls 0 to 8), R9 provisional
  @Test
  void provisionalMixedSequenceMatchesVectorVL5() {
    Lcg48Random random = Lcg48Random.ofSeed(12345L);
    assertThat(random.nextInt()).isEqualTo(1553932502);
    assertThat(random.nextInt(10)).isZero();
    assertThat(random.nextLong()).isEqualTo(-1236052134575208584L);
    assertThat(random.nextBoolean()).isTrue();
    assertThat(Float.floatToRawIntBits(random.nextFloat())).isEqualTo(Hex.u32("0x3D1A4EF0"));
    assertThat(Double.doubleToRawLongBits(random.nextDouble()))
        .isEqualTo(Hex.u64("0x3FD4E4FA91FEA406"));
    assertThat(Double.doubleToRawLongBits(random.nextGaussian()))
        .isEqualTo(Hex.u64("0xBFF39318653B97F7"));
    assertThat(Double.doubleToRawLongBits(random.nextGaussian()))
        .isEqualTo(Hex.u64("0xBFE655B243032927"));
    assertThat(random.nextInt((1 << 30) + 1)).isEqualTo(962239390);
    assertThat(random.steps()).isEqualTo(13L);

    random.setSeed(12345L);
    assertThat(random.steps()).isZero();
    assertThat(random.nextInt()).isEqualTo(1553932502);
  }
}

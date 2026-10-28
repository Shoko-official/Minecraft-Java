package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * G48 against {@code java.util.Random}, the public reference for R1 to R7 (spec core-random, level
 * D; allowed in tests by the spec section on inputs and outputs). Only the legacy single-argument
 * methods are compared: the two-bound methods of {@code java.util.Random} follow other algorithms
 * (R12). The seed list is fixed: a seeded {@link SplittableRandom} only enumerates it.
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class Lcg48RandomReferenceTest {

  private static final int DRAWS = 300;
  private static final int[] BOUNDS = {
    1,
    2,
    3,
    5,
    6,
    7,
    10,
    16,
    26,
    100,
    1000,
    1 << 20,
    715_827_883,
    (1 << 30) + 1,
    1_431_655_766,
    2_147_483_647
  };

  static LongStream seeds() {
    SplittableRandom enumerator = new SplittableRandom(20261028L);
    return LongStream.concat(
        LongStream.of(0L, 1L, -1L, 42L, Long.MIN_VALUE, Long.MAX_VALUE, 0x5DEECE66DL, 1L << 48),
        LongStream.generate(enumerator::nextLong).limit(56));
  }

  // spec: core-random R1 to R4
  @ParameterizedTest
  @MethodSource("seeds")
  void intsLongsAndBooleansMatchJavaUtilRandom(long seed) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    Random reference = new Random(seed);
    for (int i = 0; i < DRAWS; i++) {
      assertThat(random.nextInt()).as("nextInt #%d", i).isEqualTo(reference.nextInt());
      assertThat(random.nextLong()).as("nextLong #%d", i).isEqualTo(reference.nextLong());
      assertThat(random.nextBoolean()).as("nextBoolean #%d", i).isEqualTo(reference.nextBoolean());
    }
  }

  // spec: core-random R6
  @ParameterizedTest
  @MethodSource("seeds")
  void floatsAndDoublesMatchJavaUtilRandomBitForBit(long seed) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    Random reference = new Random(seed);
    for (int i = 0; i < DRAWS; i++) {
      assertThat(Float.floatToRawIntBits(random.nextFloat()))
          .as("nextFloat #%d", i)
          .isEqualTo(Float.floatToRawIntBits(reference.nextFloat()));
      assertThat(Double.doubleToRawLongBits(random.nextDouble()))
          .as("nextDouble #%d", i)
          .isEqualTo(Double.doubleToRawLongBits(reference.nextDouble()));
    }
  }

  // spec: core-random R5
  @ParameterizedTest
  @MethodSource("seeds")
  void boundedIntsMatchJavaUtilRandom(long seed) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    Random reference = new Random(seed);
    for (int i = 0; i < DRAWS; i++) {
      int bound = BOUNDS[i % BOUNDS.length];
      assertThat(random.nextInt(bound))
          .as("nextInt(%d) #%d", bound, i)
          .isEqualTo(reference.nextInt(bound));
    }
    assertThat(random.nextInt()).as("streams still aligned").isEqualTo(reference.nextInt());
  }

  // spec: core-random R7, R9 (S, provisional: java.util.Random uses StrictMath)
  @ParameterizedTest
  @MethodSource("seeds")
  void provisionalGaussiansMatchJavaUtilRandomBitForBit(long seed) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    Random reference = new Random(seed);
    for (int i = 0; i < DRAWS; i++) {
      assertThat(Double.doubleToRawLongBits(random.nextGaussian()))
          .as("nextGaussian #%d", i)
          .isEqualTo(Double.doubleToRawLongBits(reference.nextGaussian()));
    }
  }

  // spec: core-random R2, R4 to R7 (interleaved calls, cache kept across other draws, reseeding)
  @ParameterizedTest
  @MethodSource("seeds")
  void provisionalInterleavedCallsMatchJavaUtilRandom(long seed) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    Random reference = new Random(seed);
    SplittableRandom script = new SplittableRandom(seed);
    for (int i = 0; i < DRAWS; i++) {
      int op = script.nextInt(8);
      String label = "op " + op + " at " + i;
      switch (op) {
        case 0 -> assertThat(random.nextInt()).as(label).isEqualTo(reference.nextInt());
        case 1 -> {
          int bound = BOUNDS[script.nextInt(BOUNDS.length)];
          assertThat(random.nextInt(bound)).as(label).isEqualTo(reference.nextInt(bound));
        }
        case 2 -> assertThat(random.nextLong()).as(label).isEqualTo(reference.nextLong());
        case 3 -> assertThat(random.nextBoolean()).as(label).isEqualTo(reference.nextBoolean());
        case 4 -> assertThat(random.nextFloat()).as(label).isEqualTo(reference.nextFloat());
        case 5 -> assertThat(random.nextDouble()).as(label).isEqualTo(reference.nextDouble());
        case 6 -> assertThat(random.nextGaussian()).as(label).isEqualTo(reference.nextGaussian());
        default -> {
          long newSeed = script.nextLong();
          random.setSeed(newSeed);
          reference.setSeed(newSeed);
        }
      }
    }
  }
}

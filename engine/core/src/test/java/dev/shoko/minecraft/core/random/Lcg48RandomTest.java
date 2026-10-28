package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.core.random.Lcg48Vectors.BasicDraws;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * G48 core: state, seed scrambling, {@code next(bits)}, simple draws and their consumption (spec
 * core-random R1 to R4, R6, R11). The vectors follow the public {@code java.util.Random} algorithm
 * (level D); equality of G48 with the game is R8 (level S, probe P5).
 */
class Lcg48RandomTest {

  static final long MULTIPLIER = 0x5DEECE66DL;
  static final long INCREMENT = 0xBL;
  static final long MASK_48 = (1L << 48) - 1;

  static Stream<Long> seeds() {
    return Stream.of(
        0L,
        1L,
        42L,
        -1L,
        12345L,
        -123456789L,
        Long.MIN_VALUE,
        Long.MAX_VALUE,
        1L << 48,
        MULTIPLIER);
  }

  static Stream<Arguments> vectorVL1() {
    return Lcg48Vectors.V_L1.stream().map(v -> Arguments.of(v.seed(), v));
  }

  // spec: core-random R1
  @ParameterizedTest
  @MethodSource("seeds")
  void eachStepAppliesTheAffineTransitionModulo2Pow48(long seed) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    for (int i = 0; i < 200; i++) {
      long before = random.state();
      random.next(32);
      assertThat(random.state())
          .as("step %d from seed %d", i, seed)
          .isEqualTo((MULTIPLIER * before + INCREMENT) & MASK_48)
          .isBetween(0L, MASK_48);
    }
  }

  // spec: core-random R1, R2 (seed 0x5DEECE66D gives the zero state)
  @Test
  void zeroStateStepsToTheIncrement() {
    Lcg48Random random = Lcg48Random.ofSeed(MULTIPLIER);
    random.next(1);
    assertThat(random.state()).isEqualTo(INCREMENT);
  }

  // spec: core-random R2
  @ParameterizedTest
  @MethodSource("seeds")
  void initialStateIsSeedXorMultiplierModulo2Pow48(long seed) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    assertThat(random.state()).isEqualTo((seed ^ MULTIPLIER) & MASK_48);
    assertThat(random.steps()).isZero();
  }

  static Stream<Arguments> congruentSeeds() {
    return Stream.of(
        Arguments.of(0L, 1L << 48),
        Arguments.of(0L, Long.MIN_VALUE),
        Arguments.of(1L << 48, Long.MIN_VALUE),
        Arguments.of(-1L, Long.MAX_VALUE),
        Arguments.of(12345L, 12345L + (0x7123L << 48)));
  }

  // spec: core-random R2 (only the 48 low bits of the seed matter)
  @ParameterizedTest
  @MethodSource("congruentSeeds")
  void seedsCongruentModulo2Pow48GiveTheSameStream(long first, long second) {
    Lcg48Random a = Lcg48Random.ofSeed(first);
    Lcg48Random b = Lcg48Random.ofSeed(second);
    assertThat(a.state()).isEqualTo(b.state());
    for (int i = 0; i < 50; i++) {
      assertThat(a.nextLong()).as("draw %d", i).isEqualTo(b.nextLong());
    }
  }

  // spec: core-random R2, edge case seed 25214903917
  @Test
  void multiplierSeedGivesZeroStateZeroFirstIntAndZeroFirstFloat() {
    assertThat(Lcg48Random.ofSeed(MULTIPLIER).state()).isZero();
    assertThat(Lcg48Random.ofSeed(MULTIPLIER).nextInt()).isZero();
    float first = Lcg48Random.ofSeed(MULTIPLIER).nextFloat();
    assertThat(Float.floatToRawIntBits(first)).isZero();
  }

  // spec: core-random R2, R11 (initialization consumes nothing and resets the count)
  @Test
  void setSeedReinitializesStateAndStepCount() {
    Lcg48Random random = Lcg48Random.ofSeed(1L);
    random.nextLong();
    random.nextInt(7);
    random.setSeed(42L);
    assertThat(random.state()).isEqualTo(Lcg48Random.ofSeed(42L).state());
    assertThat(random.steps()).isZero();
    assertThat(random.nextInt()).isEqualTo(-1170105035);
  }

  static IntStream bitCounts() {
    return IntStream.rangeClosed(1, 32);
  }

  // spec: core-random R3
  @ParameterizedTest
  @MethodSource("bitCounts")
  void nextBitsReturnsTheHighBitsOfTheNewState(int bits) {
    Lcg48Random random = Lcg48Random.ofSeed(12345L);
    for (int i = 0; i < 50; i++) {
      int value = random.next(bits);
      assertThat(value).as("draw %d", i).isEqualTo((int) (random.state() >>> (48 - bits)));
      if (bits < 32) {
        assertThat((long) value).isNotNegative().isLessThan(1L << bits);
      }
    }
    assertThat(random.steps()).isEqualTo(50L);
  }

  // spec: core-random R3 (32 bits are read as a signed int)
  @Test
  void next32IsSigned() {
    assertThat(Lcg48Random.ofSeed(0L).next(32)).isEqualTo(-1155484576);
  }

  // spec: core-random R3 (b outside 1..32 is an error, no step consumed)
  @ParameterizedTest
  @ValueSource(ints = {0, -1, 33, 64, Integer.MIN_VALUE, Integer.MAX_VALUE})
  void nextBitsOutsideOneToThirtyTwoFailsWithoutStep(int bits) {
    Lcg48Random random = Lcg48Random.ofSeed(7L);
    long state = random.state();
    assertThatThrownBy(() -> random.next(bits)).isInstanceOf(IllegalArgumentException.class);
    assertThat(random.state()).isEqualTo(state);
    assertThat(random.steps()).isZero();
  }

  // spec: core-random R4, vectors V-L1
  @ParameterizedTest(name = "seed {0}")
  @MethodSource("vectorVL1")
  void nextIntMatchesVectorVL1(long seed, BasicDraws expected) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    for (int i = 0; i < expected.ints().size(); i++) {
      assertThat(random.nextInt()).as("nextInt #%d", i).isEqualTo(expected.ints().get(i));
    }
  }

  // spec: core-random R4, vectors V-L1
  @ParameterizedTest(name = "seed {0}")
  @MethodSource("vectorVL1")
  void nextLongMatchesVectorVL1(long seed, BasicDraws expected) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    for (int i = 0; i < expected.longs().size(); i++) {
      assertThat(random.nextLong()).as("nextLong #%d", i).isEqualTo(expected.longs().get(i));
    }
  }

  // spec: core-random R4, vectors V-L1
  @ParameterizedTest(name = "seed {0}")
  @MethodSource("vectorVL1")
  void nextBooleanMatchesVectorVL1(long seed, BasicDraws expected) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    StringBuilder bits = new StringBuilder();
    for (int i = 0; i < 16; i++) {
      bits.append(random.nextBoolean() ? '1' : '0');
    }
    assertThat(bits.toString()).isEqualTo(expected.booleans());
  }

  // spec: core-random R6, vectors V-L1 (exact bits)
  @ParameterizedTest(name = "seed {0}")
  @MethodSource("vectorVL1")
  void nextFloatMatchesVectorVL1(long seed, BasicDraws expected) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    for (int i = 0; i < expected.floats().size(); i++) {
      assertThat(Float.floatToRawIntBits(random.nextFloat()))
          .as("nextFloat #%d", i)
          .isEqualTo(Hex.u32(expected.floats().get(i)));
    }
  }

  // spec: core-random R6, vectors V-L1 (exact bits)
  @ParameterizedTest(name = "seed {0}")
  @MethodSource("vectorVL1")
  void nextDoubleMatchesVectorVL1(long seed, BasicDraws expected) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    for (int i = 0; i < expected.doubles().size(); i++) {
      assertThat(Double.doubleToRawLongBits(random.nextDouble()))
          .as("nextDouble #%d", i)
          .isEqualTo(Hex.u64(expected.doubles().get(i)));
    }
  }

  // spec: core-random R4 (second draw sign-extended before the addition)
  @ParameterizedTest
  @MethodSource("seeds")
  void nextLongAddsTheSignExtendedSecondDrawToTheShiftedFirst(long seed) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    Lcg48Random twin = Lcg48Random.ofSeed(seed);
    for (int i = 0; i < 50; i++) {
      long high = twin.next(32);
      long low = twin.next(32);
      assertThat(random.nextLong()).as("draw %d", i).isEqualTo((high << 32) + low);
    }
  }

  // spec: core-random R4, vector V-L1 seed 0 (negative second draw)
  @Test
  void nextLongHighHalfDiffersFromFirstDrawWhenSecondDrawIsNegative() {
    long value = Lcg48Random.ofSeed(0L).nextLong();
    assertThat((int) (value >>> 32)).isEqualTo(-1155484576 - 1);
  }

  // spec: core-random R4
  @ParameterizedTest
  @MethodSource("seeds")
  void nextBooleanIsNextOneBitNotZero(long seed) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    Lcg48Random twin = Lcg48Random.ofSeed(seed);
    for (int i = 0; i < 64; i++) {
      assertThat(random.nextBoolean()).as("draw %d", i).isEqualTo(twin.next(1) != 0);
    }
  }

  // spec: core-random R6
  @ParameterizedTest
  @MethodSource("seeds")
  void nextFloatIsNext24ScaledBy2PowMinus24(long seed) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    Lcg48Random twin = Lcg48Random.ofSeed(seed);
    for (int i = 0; i < 64; i++) {
      float value = random.nextFloat();
      assertThat(Float.floatToRawIntBits(value))
          .as("draw %d", i)
          .isEqualTo(Float.floatToRawIntBits(twin.next(24) * 0x1.0p-24f));
      assertThat(value).isGreaterThanOrEqualTo(0.0f).isLessThan(1.0f);
    }
  }

  // spec: core-random R6
  @ParameterizedTest
  @MethodSource("seeds")
  void nextDoubleAssemblesNext26AndNext27(long seed) {
    Lcg48Random random = Lcg48Random.ofSeed(seed);
    Lcg48Random twin = Lcg48Random.ofSeed(seed);
    for (int i = 0; i < 64; i++) {
      long high = twin.next(26);
      long low = twin.next(27);
      double value = random.nextDouble();
      assertThat(Double.doubleToRawLongBits(value))
          .as("draw %d", i)
          .isEqualTo(Double.doubleToRawLongBits(((high << 27) + low) * 0x1.0p-53));
      assertThat(value).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
    }
  }

  static Stream<Arguments> singleCalls() {
    return Stream.of(
        call("next(1)", r -> r.next(1), 1),
        call("next(32)", r -> r.next(32), 1),
        call("nextInt()", Lcg48Random::nextInt, 1),
        call("nextBoolean()", Lcg48Random::nextBoolean, 1),
        call("nextFloat()", Lcg48Random::nextFloat, 1),
        call("nextInt(1)", r -> r.nextInt(1), 1),
        call("nextInt(16)", r -> r.nextInt(16), 1),
        call("nextInt(1 << 30)", r -> r.nextInt(1 << 30), 1),
        call("nextLong()", Lcg48Random::nextLong, 2),
        call("nextDouble()", Lcg48Random::nextDouble, 2),
        call("setSeed(5)", r -> r.setSeed(5L), 0));
  }

  private static Arguments call(String name, Consumer<Lcg48Random> call, long steps) {
    return Arguments.of(name, call, steps);
  }

  // spec: core-random R11
  @ParameterizedTest(name = "{0}")
  @MethodSource("singleCalls")
  void eachCallConsumesItsDocumentedSteps(String name, Consumer<Lcg48Random> call, long steps) {
    for (long seed : new long[] {0L, 42L, -1L, MULTIPLIER}) {
      Lcg48Random random = Lcg48Random.ofSeed(seed);
      for (int i = 0; i < 20; i++) {
        call.accept(random);
      }
      assertThat(random.steps()).as("%s x20, seed %d", name, seed).isEqualTo(20 * steps);
    }
  }
}

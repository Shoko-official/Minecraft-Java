package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.LongFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Contract shared by both families (spec core-random, section on inputs and outputs, R11, R21,
 * R28): determinism, no shared state, reseeding, step counter, range draw and the provisional
 * {@code fork}. Thread confinement is not required by the spec, so it is not tested.
 */
class RandomSourceTest {

  static Stream<Arguments> families() {
    LongFunction<RandomSource> lcg48 = Lcg48Random::ofSeed;
    LongFunction<RandomSource> xoroshiro = XoroshiroRandom::ofSeed;
    return Stream.of(Arguments.of("G48", lcg48), Arguments.of("X128", xoroshiro));
  }

  private static void drawEverything(RandomSource random) {
    random.nextInt();
    random.nextInt(7);
    random.nextLong();
    random.nextBoolean();
    random.nextFloat();
    random.nextDouble();
    random.nextGaussian();
    random.nextIntBetweenInclusive(-3, 3);
  }

  // spec: inputs and outputs (a generator is a function of its seed)
  @ParameterizedTest(name = "{0}")
  @MethodSource("families")
  void twoGeneratorsWithTheSameSeedProduceTheSameStream(
      String family, LongFunction<RandomSource> factory) {
    RandomSource a = factory.apply(-31337L);
    RandomSource b = factory.apply(-31337L);
    for (int i = 0; i < 100; i++) {
      assertThat(a.nextLong()).as("%s draw %d", family, i).isEqualTo(b.nextLong());
      assertThat(a.nextGaussian()).isEqualTo(b.nextGaussian());
      assertThat(a.nextInt(1000)).isEqualTo(b.nextInt(1000));
    }
    assertThat(a.steps()).isEqualTo(b.steps());
  }

  // spec: inputs and outputs (no global state): draws on other generators, created or used in
  // between, do not change a generator's stream
  @ParameterizedTest(name = "{0}")
  @MethodSource("families")
  void generatorsShareNoState(String family, LongFunction<RandomSource> factory) {
    RandomSource alone = factory.apply(5L);
    long[] expected = new long[50];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = alone.nextLong();
    }
    RandomSource observed = factory.apply(5L);
    RandomSource other = factory.apply(5L);
    for (int i = 0; i < expected.length; i++) {
      drawEverything(other);
      drawEverything(factory.apply(i));
      other.setSeed(i);
      assertThat(observed.nextLong()).as("%s draw %d", family, i).isEqualTo(expected[i]);
    }
  }

  // spec: R2, R25, inputs and outputs: setSeed resets the stream, the step count and the
  // cached gaussian
  @ParameterizedTest(name = "{0}")
  @MethodSource("families")
  void setSeedMakesTheGeneratorIndistinguishableFromAFreshOne(
      String family, LongFunction<RandomSource> factory) {
    RandomSource random = factory.apply(1L);
    drawEverything(random);
    random.setSeed(77L);
    assertThat(random.steps()).isZero();
    RandomSource fresh = factory.apply(77L);
    for (int i = 0; i < 10; i++) {
      assertThat(random.nextGaussian())
          .as("%s gaussian %d", family, i)
          .isEqualTo(fresh.nextGaussian());
      assertThat(random.nextLong()).isEqualTo(fresh.nextLong());
    }
    assertThat(random.steps()).isEqualTo(fresh.steps());
  }

  // spec: R11, R21 (skip counts its steps), RandomSource.skip
  @ParameterizedTest(name = "{0}")
  @MethodSource("families")
  void skipCountsItsStepsLikeSingleStepDraws(String family, LongFunction<RandomSource> factory) {
    RandomSource jumped = factory.apply(9L);
    RandomSource stepped = factory.apply(9L);
    jumped.nextInt();
    jumped.skip(10);
    stepped.nextInt();
    for (int i = 0; i < 10; i++) {
      stepped.nextInt();
    }
    assertThat(jumped.steps()).as(family).isEqualTo(11L).isEqualTo(stepped.steps());
    assertThat(jumped.nextLong()).isEqualTo(stepped.nextLong());
  }

  // spec: R12 (S, provisional until P4): min >= max returns min without a step, for both families
  @ParameterizedTest(name = "{0}")
  @MethodSource("families")
  void provisionalDegenerateRangeConsumesNothing(
      String family, LongFunction<RandomSource> factory) {
    RandomSource random = factory.apply(4L);
    assertThat(random.nextIntBetweenInclusive(5, 5)).isEqualTo(5);
    assertThat(random.nextIntBetweenInclusive(9, -9)).isEqualTo(9);
    assertThat(random.steps()).as(family).isZero();
    assertThat(random.nextLong()).isEqualTo(factory.apply(4L).nextLong());
  }

  // RandomSource.nextIntBetweenInclusive contract (R12, Q4 open): an overflowing size fails
  // without consuming a step
  @ParameterizedTest(name = "{0}")
  @MethodSource("families")
  void provisionalOverflowingRangeFailsWithoutStep(
      String family, LongFunction<RandomSource> factory) {
    RandomSource random = factory.apply(4L);
    assertThatThrownBy(() -> random.nextIntBetweenInclusive(Integer.MIN_VALUE, Integer.MAX_VALUE))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(random.steps()).as(family).isZero();
  }

  // spec: inputs and outputs (value ranges of each draw)
  @ParameterizedTest(name = "{0}")
  @MethodSource("families")
  void drawsStayWithinTheirDocumentedRanges(String family, LongFunction<RandomSource> factory) {
    RandomSource random = factory.apply(2026L);
    for (int i = 0; i < 2000; i++) {
      assertThat(random.nextInt(13)).as(family).isBetween(0, 12);
      assertThat(random.nextIntBetweenInclusive(-4, 4)).isBetween(-4, 4);
      float f = random.nextFloat();
      assertThat(f).isGreaterThanOrEqualTo(0.0f).isLessThan(1.0f);
      assertThat(f * 0x1.0p24f)
          .as("multiple of 2^-24")
          .isEqualTo((float) Math.floor(f * 0x1.0p24f));
      double d = random.nextDouble();
      assertThat(d).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
      assertThat(d * 0x1.0p53).as("multiple of 2^-53").isEqualTo(Math.floor(d * 0x1.0p53));
    }
  }

  // spec: R28 (provisional, not faithful (Q7)): G48 child is ofSeed of one nextLong, two steps
  @Test
  void provisionalNotFaithfulLcg48ForkSeedsTheChildWithOneNextLong() {
    Lcg48Random parent = Lcg48Random.ofSeed(12345L);
    Lcg48Random twin = Lcg48Random.ofSeed(12345L);
    Lcg48Random child = parent.fork();
    assertThat(parent.steps()).isEqualTo(2L);
    assertThat(child.steps()).isZero();
    assertThat(child.state()).isEqualTo(Lcg48Random.ofSeed(twin.nextLong()).state());
    assertThat(parent.state()).isEqualTo(twin.state());
  }

  // spec: R28 (provisional, not faithful (Q7)): X128 child is ofSeed of one nextLong, one step
  @Test
  void provisionalNotFaithfulXoroshiroForkSeedsTheChildWithOneNextLong() {
    XoroshiroRandom parent = XoroshiroRandom.ofSeed(12345L);
    XoroshiroRandom twin = XoroshiroRandom.ofSeed(12345L);
    XoroshiroRandom child = parent.fork();
    assertThat(parent.steps()).isEqualTo(1L);
    assertThat(child.steps()).isZero();
    assertThat(child.state()).isEqualTo(XoroshiroRandom.ofSeed(twin.nextLong()).state());
    assertThat(parent.state()).isEqualTo(twin.state());
  }

  // spec: R28 contract: the child is a deterministic function of the parent state, the parent
  // advances by a fixed number of steps, and the child is independent afterwards
  @ParameterizedTest(name = "{0}")
  @MethodSource("families")
  void forkIsDeterministicAndIndependentOfItsParent(
      String family, LongFunction<RandomSource> factory) {
    RandomSource parent = factory.apply(600L);
    RandomSource sameParent = factory.apply(600L);
    RandomSource child = parent.fork();
    RandomSource sameChild = sameParent.fork();
    assertThat(child).isNotNull().isNotSameAs(parent);
    assertThat(child.getClass()).isEqualTo(parent.getClass());
    long parentSteps = parent.steps();
    for (int i = 0; i < 20; i++) {
      assertThat(child.nextLong())
          .as("%s child draw %d", family, i)
          .isEqualTo(sameChild.nextLong());
    }
    assertThat(parent.steps())
        .as("drawing from the child leaves the parent")
        .isEqualTo(parentSteps);
    assertThat(parent.fork().steps()).isZero();
    assertThat(parent.steps()).as("each fork costs the same").isEqualTo(2 * parentSteps);
  }
}

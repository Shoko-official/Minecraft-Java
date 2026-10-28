package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * X128 derived draws (spec core-random R17 to R21). Every rule here is level S (hypotheses H-X1 to
 * H-X5, probes P2, P3, P6, questions Q3, Q8): all tests are provisional and will follow the probes.
 * The raw output {@code r} is read through a twin generator's {@code nextLong()} (R16, H-X1).
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class XoroshiroRandomDrawsTest {

  private static final long MASK_32 = 0xFFFFFFFFL;

  /** One V-X5 row, used here as a raw state (the state itself is tested with R31). */
  record NamedCase(
      String name, Seed128 initial, List<Integer> values, Seed128 after, List<Integer> dice) {}

  static Stream<NamedCase> vectorVX5() {
    return Stream.of(
        new NamedCase(
            "N1",
            new Seed128(3847398142028685078L, 7192185014346937746L),
            List.of(2067245245, 390848952, 326786297, 668760460, 1894545760),
            new Seed128(5907017683142997804L, -5217483301452126061L),
            List.of(6, 2, 1, 2, 6, 6, 1, 2, 4, 3)),
        new NamedCase(
            "N2",
            new Seed128(5272463233947570727L, 1927618558350093866L),
            List.of(867282174, 418117060, 412632368, 1434538546, 383715241),
            new Seed128(-1430254592785879641L, -8290170135680512777L),
            List.of(3, 2, 2, 5, 2, 4, 6, 2, 1, 2)),
        new NamedCase(
            "N3",
            new Seed128(-110783831392733308L, 2932223646667407290L),
            List.of(1421574888, 1004810129, 458210092, 2121564838, 466775805),
            new Seed128(2173709032643078312L, -2849544362869967681L),
            List.of(4, 3, 2, 6, 2, 6, 5, 6, 2, 6)),
        new NamedCase(
            "N3z",
            new Seed128(5368716647786959924L, -554802411955493188L),
            List.of(343469287, 117407115, 946035148, 1039241215, 10099746),
            new Seed128(-8264014475489643770L, -6056685171208551996L),
            List.of(1, 1, 3, 3, 1, 3, 3, 1, 5, 1)),
        new NamedCase(
            "N4",
            new Seed128(2153501729715799201L, -2652207934038623535L),
            List.of(1793465186, 323874290, 1138589083, 1914133938, 1261544791),
            new Seed128(6887961208285858452L, -2052858044374788392L),
            List.of(6, 1, 4, 6, 4, 3, 2, 1, 5, 2)),
        new NamedCase(
            "N4le",
            new Seed128(1165417471098110475L, -2632000392545274068L),
            List.of(790826154, 924853721, 1233154537, 1415715827, 15974204),
            new Seed128(5762611463864902182L, 483216415578334690L),
            List.of(3, 3, 4, 4, 1, 2, 4, 3, 3, 1)),
        new NamedCase(
            "N5",
            new Seed128(733019005196230046L, -3494074583369400597L),
            List.of(28592253, 1758037181, 1883544479, 39523540, 1191494888),
            new Seed128(-3380976663154756996L, 7698574065656511185L),
            List.of(1, 5, 6, 1, 4, 5, 2, 4, 2, 6)),
        new NamedCase(
            "N6",
            new Seed128(-8338171165017409289L, 2650172299325186875L),
            List.of(2089840009, 1915533696, 1931485652, 1250613981, 1567712703),
            new Seed128(-4285309943909617478L, 3450026542495718202L),
            List.of(6, 6, 6, 4, 5, 1, 5, 6, 3, 6)),
        new NamedCase(
            "N7",
            new Seed128(-8196287188144295939L, 4607975983393672058L),
            List.of(809584177, 2031271794, 356800533, 1450885541, 2027094301),
            new Seed128(-8776242114209539175L, 4340599208118940372L),
            List.of(3, 6, 1, 5, 6, 5, 3, 4, 1, 6)));
  }

  static Stream<Arguments> namedCases() {
    return vectorVX5().map(c -> Arguments.of(c.name(), c));
  }

  // spec: core-random R18 (H-X2), R21, vectors V-X5 (value column and state after 5 draws)
  @ParameterizedTest(name = "{0}")
  @MethodSource("namedCases")
  void provisionalLemireBoundedDrawsMatchVectorVX5(String name, NamedCase expected) {
    XoroshiroRandom random = XoroshiroRandom.ofState(expected.initial());
    for (int i = 0; i < 5; i++) {
      assertThat(random.nextInt(2147483646)).as("#%d", i).isEqualTo(expected.values().get(i));
    }
    assertThat(random.state()).isEqualTo(expected.after());
    assertThat(random.steps()).isEqualTo(5L);
  }

  // spec: core-random R12, R33, R18 (all S, provisional until P2 and P4), vectors V-X5
  @ParameterizedTest(name = "{0}")
  @MethodSource("namedCases")
  void provisionalRangeDrawsMatchVectorVX5(String name, NamedCase expected) {
    XoroshiroRandom values = XoroshiroRandom.ofState(expected.initial());
    for (int i = 0; i < 5; i++) {
      assertThat(values.nextIntBetweenInclusive(0, 2147483645))
          .as("value 0..2147483645 #%d", i)
          .isEqualTo(expected.values().get(i));
    }
    XoroshiroRandom dice = XoroshiroRandom.ofState(expected.initial());
    for (int i = 0; i < 10; i++) {
      assertThat(dice.nextIntBetweenInclusive(1, 6))
          .as("value 1..6 #%d", i)
          .isEqualTo(expected.dice().get(i));
    }
    assertThat(dice.steps()).isEqualTo(10L);
  }

  private static XoroshiroRandom stateN1() {
    return XoroshiroRandom.ofState(vectorVX5().findFirst().orElseThrow().initial());
  }

  // spec: core-random R18 (H-X2, no special case for powers of two), probe P3, vector V-X5
  @Test
  void provisionalPowerOfTwoBoundUsesTheSameMethodFromN1() {
    XoroshiroRandom random = stateN1();
    List<Integer> expected = List.of(15, 2, 2, 4, 14, 15, 1, 5, 10, 5);
    for (int i = 0; i < expected.size(); i++) {
      assertThat(random.nextInt(16)).as("#%d", i).isEqualTo(expected.get(i));
    }
  }

  // spec: core-random R19 (H-X3), probe P6, vector V-X5 (three nextFloat from N1)
  @Test
  void provisionalNextFloatFromN1MatchesVectorVX5() {
    XoroshiroRandom random = stateN1();
    for (String bits : List.of("0x3E28B290", "0x3F4CBC22", "0x3E809CC8")) {
      assertThat(Float.floatToRawIntBits(random.nextFloat())).isEqualTo(Hex.u32(bits));
    }
  }

  // spec: core-random R19 (H-X4), question Q8, vector V-X5 (three nextDouble from N1)
  @Test
  void provisionalNextDoubleFromN1MatchesVectorVX5() {
    XoroshiroRandom random = stateN1();
    assertThat(random.nextDouble()).isEqualTo(0.16474369376959186);
    assertThat(random.nextDouble()).isEqualTo(0.7997457290026366);
    assertThat(random.nextDouble()).isEqualTo(0.2511961888876212);
  }

  // spec: core-random R17, R19 (H-X1, H-X3 to H-X5), R21 (one step each)
  @ParameterizedTest
  @ValueSource(longs = {0L, -1L, 12345L, Long.MIN_VALUE})
  void provisionalSimpleDrawsReadTheDocumentedBitsOfOneOutput(long seed) {
    XoroshiroRandom random = XoroshiroRandom.ofSeed(seed);
    XoroshiroRandom twin = XoroshiroRandom.ofSeed(seed);
    for (int i = 0; i < 100; i++) {
      assertThat(random.nextInt()).as("nextInt %d", i).isEqualTo((int) twin.nextLong());
      assertThat(random.nextBoolean())
          .as("nextBoolean %d", i)
          .isEqualTo((twin.nextLong() & 1L) != 0);
      float f = random.nextFloat();
      assertThat(Float.floatToRawIntBits(f))
          .as("nextFloat %d", i)
          .isEqualTo(Float.floatToRawIntBits((twin.nextLong() >>> 40) * 0x1.0p-24f));
      assertThat(f).isGreaterThanOrEqualTo(0.0f).isLessThan(1.0f);
      double d = random.nextDouble();
      assertThat(Double.doubleToRawLongBits(d))
          .as("nextDouble %d", i)
          .isEqualTo(Double.doubleToRawLongBits((twin.nextLong() >>> 11) * 0x1.0p-53));
      assertThat(d).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
      assertThat(random.steps()).isEqualTo(4L * (i + 1));
    }
  }

  // spec: core-random R18 (H-X2: Lemire on the 32 low bits, redraw while m mod 2^32 < t), R21
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 6, 16, 100, 1 << 30, (1 << 30) + 1, 2147483646, 2147483647})
  void provisionalBoundedDrawFollowsLemireRejection(int bound) {
    long rejections = 0;
    for (long seed = 0; seed < 64; seed++) {
      XoroshiroRandom random = XoroshiroRandom.ofSeed(seed);
      XoroshiroRandom twin = XoroshiroRandom.ofSeed(seed);
      for (int i = 0; i < 16; i++) {
        long attempts = 1;
        long m = (twin.nextLong() & MASK_32) * bound;
        if ((m & MASK_32) < bound) {
          long threshold = ((1L << 32) - bound) % bound;
          while ((m & MASK_32) < threshold) {
            m = (twin.nextLong() & MASK_32) * bound;
            attempts++;
          }
        }
        rejections += attempts - 1;
        long before = random.steps();
        int value = random.nextInt(bound);
        assertThat(value).as("seed %d draw %d", seed, i).isEqualTo((int) (m >>> 32));
        assertThat(value).isNotNegative().isLessThan(bound);
        assertThat(random.steps() - before).as("steps").isEqualTo(attempts);
      }
    }
    if (bound == (1 << 30) + 1) {
      assertThat(rejections).as("the rejection path is exercised").isPositive();
    }
  }

  // RandomSource.nextInt(int) contract, R18: bound <= 0 is an error without step
  @ParameterizedTest
  @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
  void nonPositiveBoundFailsWithoutStep(int bound) {
    XoroshiroRandom random = XoroshiroRandom.ofSeed(0L);
    Seed128 state = random.state();
    assertThatThrownBy(() -> random.nextInt(bound)).isInstanceOf(IllegalArgumentException.class);
    assertThat(random.state()).isEqualTo(state);
    assertThat(random.steps()).isZero();
  }

  // spec: core-random R20 (S: R7 with the X128 nextDouble, two steps per attempt, cache;
  // provisional StrictMath logarithm, Q3)
  @ParameterizedTest
  @ValueSource(longs = {0L, 1L, -1L, 42L, 12345L})
  void provisionalGaussianFollowsThePolarMethodWithTwoStepsPerAttempt(long seed) {
    XoroshiroRandom random = XoroshiroRandom.ofSeed(seed);
    XoroshiroRandom twin = XoroshiroRandom.ofSeed(seed);
    for (int pair = 0; pair < 20; pair++) {
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
      assertThat(random.nextGaussian()).as("pair %d cached", pair).isEqualTo(v2 * multiplier);
      assertThat(random.steps() - before).as("steps").isEqualTo(2 * attempts);
    }
  }

  // spec: core-random R20, R25 (reinitialization drops the cached gaussian)
  @Test
  void provisionalSetSeedDropsTheCachedGaussian() {
    XoroshiroRandom fresh = XoroshiroRandom.ofSeed(11L);
    double first = fresh.nextGaussian();
    double cached = fresh.nextGaussian();
    XoroshiroRandom random = XoroshiroRandom.ofSeed(11L);
    random.nextGaussian();
    random.setSeed(11L);
    assertThat(random.nextGaussian()).isEqualTo(first).isNotEqualTo(cached);
  }
}

package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * X128 core: raw state, output and transition, zero state, skip and step count, initialization by a
 * 64-bit seed (spec core-random R13, R15, R16, R21, R24, R25). The raw output is only observable
 * through {@code nextLong()}, which R16 (hypothesis H-X1, level S) defines as the output itself.
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class XoroshiroRandomTest {

  static Stream<Arguments> vectorVX1() {
    return Stream.of(
        Arguments.of(
            "0x0000000000000001",
            "0x0000000000000002",
            List.of(
                393217L,
                669327710093319L,
                1732421326133921491L,
                -7051953992050424633L,
                -8891291296936358940L),
            Hex.seed("0x21A3060E10687053", "0xC141020330186810")),
        Arguments.of(
            "0x0123456789ABCDEF",
            "0xFEDCBA9876543210",
            List.of(
                81985529216486894L,
                -6887384918253362076L,
                -3704330611534247336L,
                -6724422987835305309L,
                -251466190376645636L),
            Hex.seed("0xCAAF9AD20231BFEC", "0xD94221A27B82D871")),
        Arguments.of(
            "0x0000000000000000",
            "0x0000000000000001",
            List.of(
                131072L,
                35459252224001L,
                576535519371067973L,
                -8536533096778751167L,
                4866527886176355657L),
            Hex.seed("0x2080830602203021", "0x4000800010082010")),
        Arguments.of(
            "0xFFFFFFFFFFFFFFFF",
            "0xFFFFFFFFFFFFFFFF",
            List.of(-131074L, -2L, -274880004097L, -575937384760344506L, -8682935975298663105L),
            Hex.seed("0xFF0100FFF8181FC0", "0xFFFE00000808003F")));
  }

  // spec: core-random R13, R16 (H-X1), R21, vectors V-X1
  @ParameterizedTest(name = "({0}, {1})")
  @MethodSource("vectorVX1")
  void rawOutputsMatchVectorVX1(String s0, String s1, List<Long> outputs, Seed128 finalState) {
    XoroshiroRandom random = XoroshiroRandom.ofState(Hex.seed(s0, s1));
    assertThat(random.state()).isEqualTo(Hex.seed(s0, s1));
    assertThat(random.steps()).isZero();
    for (int i = 0; i < outputs.size(); i++) {
      assertThat(random.nextLong()).as("output #%d", i).isEqualTo(outputs.get(i));
    }
    assertThat(random.state()).isEqualTo(finalState);
    assertThat(random.steps()).isEqualTo(5L);
  }

  // spec: core-random R13 (output on the state before the transition, then the transition)
  @ParameterizedTest
  @ValueSource(longs = {0L, 1L, -1L, 12345L, Long.MIN_VALUE})
  void outputIsComputedBeforeTheTransition(long seed) {
    XoroshiroRandom random = XoroshiroRandom.ofSeed(seed);
    for (int i = 0; i < 200; i++) {
      long s0 = random.state().lo();
      long s1 = random.state().hi();
      long t = s1 ^ s0;
      assertThat(random.nextLong()).as("output %d", i).isEqualTo(Long.rotateLeft(s0 + s1, 17) + s0);
      assertThat(random.state())
          .as("transition %d", i)
          .isEqualTo(new Seed128(Long.rotateLeft(s0, 49) ^ t ^ (t << 21), Long.rotateLeft(t, 28)));
    }
  }

  // spec: core-random R13, vector V-X1 (1, 2): rotl(1 + 2, 17) + 1
  @Test
  void firstOutputDependsOnlyOnTheInitialState() {
    XoroshiroRandom random = XoroshiroRandom.ofState(new Seed128(1L, 2L));
    assertThat(random.nextLong()).isEqualTo((3L << 17) + 1L);
  }

  // spec: core-random R15 (S, provisional until Q2): an all-zero raw state is refused
  @Test
  void provisionalAllZeroStateIsRefused() {
    assertThatThrownBy(() -> XoroshiroRandom.ofState(new Seed128(0L, 0L)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // spec: core-random R13, edge case: only one zero word is a valid state
  @Test
  void stateWithOneZeroWordIsAccepted() {
    assertThat(XoroshiroRandom.ofState(new Seed128(0L, 1L)).state()).isEqualTo(new Seed128(0L, 1L));
    assertThat(XoroshiroRandom.ofState(new Seed128(1L, 0L)).state()).isEqualTo(new Seed128(1L, 0L));
  }

  // spec: core-random R21 (skip executed step by step: same state as n single steps)
  @ParameterizedTest
  @ValueSource(longs = {0, 1, 2, 5, 64, 1000, 100_003})
  void skipMatchesTheSameNumberOfSingleSteps(long n) {
    XoroshiroRandom jumped = XoroshiroRandom.ofSeed(777L);
    XoroshiroRandom stepped = XoroshiroRandom.ofSeed(777L);
    jumped.skip(n);
    for (long i = 0; i < n; i++) {
      stepped.nextLong();
    }
    assertThat(jumped.state()).isEqualTo(stepped.state());
    assertThat(jumped.steps()).isEqualTo(n);
    assertThat(jumped.nextLong()).isEqualTo(stepped.nextLong());
  }

  // RandomSource.skip contract: n < 0 is an error
  @ParameterizedTest
  @ValueSource(longs = {-1, Long.MIN_VALUE})
  void negativeSkipFailsWithoutChangingTheState(long n) {
    XoroshiroRandom random = XoroshiroRandom.ofSeed(3L);
    Seed128 state = random.state();
    assertThatThrownBy(() -> random.skip(n)).isInstanceOf(IllegalArgumentException.class);
    assertThat(random.state()).isEqualTo(state);
    assertThat(random.steps()).isZero();
  }

  static Stream<Arguments> vectorVX3() {
    return Stream.of(
        Arguments.of(
            0L,
            Hex.seed("0x3564B439CD1E1F16", "0x63CFC62A2B097592"),
            List.of(3038984756725240190L, -3694039286755638414L, 4633751808701151732L)),
        Arguments.of(
            1L,
            Hex.seed("0x492B8D6066C09227", "0x1AC046DDA8E86E2A"),
            List.of(-1033667707219518978L, 6451672561743293322L, -1821890263888393630L)),
        Arguments.of(
            -1L,
            Hex.seed("0xFE766AB13AFAB784", "0x28B159DEFD9C7FBA"),
            List.of(-8676505878415342125L, -868585888688873692L, -6331679347063163302L)),
        Arguments.of(
            12345L,
            Hex.seed("0x0A2C34E6CA54DD9E", "0xCF828DADC78BBEEB"),
            List.of(-8118485274630516485L, 8241557746459281790L, 4143755034716878659L)),
        Arguments.of(
            Long.MIN_VALUE,
            Hex.seed("0xA76C539A9A3F0612", "0x4B9E7F42D2702803"),
            List.of(5559461712809880615L, 4137405319755209525L, -3866801757029155188L)));
  }

  // spec: core-random R24, R25 (S for the game's use, probe P1), vectors V-X3
  @ParameterizedTest(name = "seed {0}")
  @MethodSource("vectorVX3")
  void ofSeedMatchesVectorVX3(long seed, Seed128 state, List<Long> longs) {
    XoroshiroRandom random = XoroshiroRandom.ofSeed(seed);
    assertThat(random.state()).isEqualTo(state);
    assertThat(random.steps()).isZero();
    for (int i = 0; i < longs.size(); i++) {
      assertThat(random.nextLong()).as("nextLong #%d", i).isEqualTo(longs.get(i));
    }
  }

  // spec: core-random R25 (reinitialization by Init64 resets the step count)
  @ParameterizedTest(name = "seed {0}")
  @MethodSource("vectorVX3")
  void setSeedReinitializesByInit64(long seed, Seed128 state, List<Long> longs) {
    XoroshiroRandom random = XoroshiroRandom.ofSeed(seed + 1);
    random.nextLong();
    random.nextInt(6);
    random.setSeed(seed);
    assertThat(random.state()).isEqualTo(state).isEqualTo(SeedExpansion.init64(seed));
    assertThat(random.steps()).isZero();
    assertThat(random.nextLong()).isEqualTo(longs.get(0));
  }

  static LongStream crossCheckSeeds() {
    SplittableRandom enumerator = new SplittableRandom(13L);
    return LongStream.concat(
        LongStream.of(0L, 1L, -1L, 12345L, Long.MIN_VALUE, Long.MAX_VALUE),
        LongStream.generate(enumerator::nextLong).limit(42));
  }

  // spec: core-random R13, R24 (recoupement with the JDK's independent xoroshiro128++ seeded by
  // a 64-bit seed, as the spec did for V-X3)
  @ParameterizedTest
  @MethodSource("crossCheckSeeds")
  void ofSeedStreamMatchesTheJdkXoroshiro128PlusPlus(long seed) {
    RandomGenerator reference =
        RandomGeneratorFactory.<RandomGenerator>of("Xoroshiro128PlusPlus").create(seed);
    XoroshiroRandom random = XoroshiroRandom.ofSeed(seed);
    for (int i = 0; i < 500; i++) {
      assertThat(random.nextLong()).as("nextLong #%d", i).isEqualTo(reference.nextLong());
    }
    assertThat(random.steps()).isEqualTo(500L);
  }

  // spec: core-random R21 (one step per nextLong)
  @Test
  void eachNextLongTakesOneStep() {
    XoroshiroRandom random = XoroshiroRandom.ofState(new Seed128(-1L, -1L));
    for (int i = 1; i <= 100; i++) {
      random.nextLong();
      assertThat(random.steps()).isEqualTo(i);
    }
  }
}

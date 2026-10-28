package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Seed expansion and Stafford's mixer (spec core-random R22 to R24, vectors V-X2 and V-X3). */
class SeedExpansionTest {

  private static final long K_SQRT2 = 0x6A09E667F3BCC909L;
  private static final long K_PHI = 0x9E3779B97F4A7C15L;

  // spec: core-random R23, vectors V-X2
  @ParameterizedTest(name = "mix({0})")
  @CsvSource({
    "0x0000000000000000, 0x0000000000000000",
    "0x0000000000000001, 0x5692161D100B05E5",
    "0x6A09E667F3BCC909, 0x3564B439CD1E1F16",
    "0x084160217307451E, 0x63CFC62A2B097592",
    "0xFFFFFFFFFFFFFFFF, 0xB4D055FCF2CBBD7B"
  })
  void mixMatchesVectorVX2(String input, String output) {
    assertThat(Hex.of(SeedExpansion.mix(Hex.u64(input)))).isEqualTo(output);
  }

  // spec: core-random R23 (recoupement: SplittableRandom(0).nextLong() is the mix of K_PHI)
  @Test
  void mixOfGoldenRatioMatchesSplittableRandomOfZero() {
    assertThat(SeedExpansion.mix(K_PHI))
        .isEqualTo(-2152535657050944081L)
        .isEqualTo(new SplittableRandom(0L).nextLong());
  }

  // spec: core-random R23 (the formula, on many words)
  @Test
  void mixAppliesTheThreeXorShiftMultiplySteps() {
    SplittableRandom words = new SplittableRandom(23L);
    for (int i = 0; i < 1000; i++) {
      long z = words.nextLong();
      long expected = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
      expected = (expected ^ (expected >>> 27)) * 0x94D049BB133111EBL;
      expected = expected ^ (expected >>> 31);
      assertThat(SeedExpansion.mix(z)).as("mix(%s)", Hex.of(z)).isEqualTo(expected);
    }
  }

  // spec: core-random R23 (bijective, zero only at zero): no collision on a structured sample
  @Test
  void mixHasNoCollisionAndIsZeroOnlyAtZero() {
    Set<Long> outputs = new HashSet<>();
    for (long z = -5000; z <= 5000; z++) {
      long mixed = SeedExpansion.mix(z);
      assertThat(outputs.add(mixed)).as("collision at %d", z).isTrue();
      assertThat(mixed == 0).as("mix(%d) is zero", z).isEqualTo(z == 0);
    }
    for (int k = 13; k < 64; k++) {
      long z = 1L << k;
      assertThat(outputs.add(SeedExpansion.mix(z))).as("collision at 2^%d", k).isTrue();
    }
  }

  static Stream<Arguments> vectorVX3() {
    return Stream.of(
        Arguments.of(
            0L,
            Hex.seed("0x6A09E667F3BCC909", "0x084160217307451E"),
            Hex.seed("0x3564B439CD1E1F16", "0x63CFC62A2B097592")),
        Arguments.of(
            1L,
            Hex.seed("0x6A09E667F3BCC908", "0x084160217307451D"),
            Hex.seed("0x492B8D6066C09227", "0x1AC046DDA8E86E2A")),
        Arguments.of(
            -1L,
            Hex.seed("0x95F619980C4336F6", "0x342D93518B8DB30B"),
            Hex.seed("0xFE766AB13AFAB784", "0x28B159DEFD9C7FBA")),
        Arguments.of(
            12345L,
            Hex.seed("0x6A09E667F3BCF930", "0x0841602173077545"),
            Hex.seed("0x0A2C34E6CA54DD9E", "0xCF828DADC78BBEEB")),
        Arguments.of(
            Long.MIN_VALUE,
            Hex.seed("0xEA09E667F3BCC909", "0x884160217307451E"),
            Hex.seed("0xA76C539A9A3F0612", "0x4B9E7F42D2702803")));
  }

  // spec: core-random R22, vectors V-X3 (unmixed words)
  @ParameterizedTest(name = "seed {0}")
  @MethodSource("vectorVX3")
  void expandUnmixedMatchesVectorVX3(long seed, Seed128 unmixed, Seed128 mixed) {
    assertThat(SeedExpansion.expandUnmixed(seed)).isEqualTo(unmixed);
  }

  // spec: core-random R24, vectors V-X3 (state after mixing)
  @ParameterizedTest(name = "seed {0}")
  @MethodSource("vectorVX3")
  void init64MatchesVectorVX3(long seed, Seed128 unmixed, Seed128 mixed) {
    assertThat(SeedExpansion.init64(seed)).isEqualTo(mixed);
  }

  static LongStream values() {
    SplittableRandom enumerator = new SplittableRandom(22L);
    return LongStream.concat(
        LongStream.of(0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, K_SQRT2, K_SQRT2 ^ -K_PHI),
        LongStream.generate(enumerator::nextLong).limit(100));
  }

  // spec: core-random R22 (lo = v XOR K_SQRT2, hi = lo + K_PHI mod 2^64)
  @ParameterizedTest
  @MethodSource("values")
  void expandUnmixedXorsSqrt2ThenAddsGoldenRatio(long value) {
    Seed128 words = SeedExpansion.expandUnmixed(value);
    assertThat(words.lo()).isEqualTo(value ^ K_SQRT2);
    assertThat(words.hi()).isEqualTo(words.lo() + K_PHI);
    assertThat(words).isNotEqualTo(new Seed128(0L, 0L));
  }

  // spec: core-random R24 (Init64 = mix of each unmixed word), edge case "mix(0) = 0 yet seed 0
  // gives a non-zero state"
  @ParameterizedTest
  @MethodSource("values")
  void init64MixesEachUnmixedWord(long seed) {
    Seed128 words = SeedExpansion.expandUnmixed(seed);
    Seed128 state = SeedExpansion.init64(seed);
    assertThat(state)
        .isEqualTo(new Seed128(SeedExpansion.mix(words.lo()), SeedExpansion.mix(words.hi())));
    assertThat(state).isNotEqualTo(new Seed128(0L, 0L));
  }

  // spec: core-random R22 edge: the words can be zero one at a time, never both
  @Test
  void expandUnmixedHasAZeroWordOnlyOneAtATime() {
    assertThat(SeedExpansion.expandUnmixed(K_SQRT2)).isEqualTo(new Seed128(0L, K_PHI));
    assertThat(SeedExpansion.expandUnmixed(K_SQRT2 ^ -K_PHI)).isEqualTo(new Seed128(-K_PHI, 0L));
  }
}

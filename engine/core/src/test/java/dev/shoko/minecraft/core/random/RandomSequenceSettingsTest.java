package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Named sequence settings and initial state (spec core-random R30, R31, R25, R26). Every rule here
 * is level S (questions Q5, Q6, probe P1): the expected states are the V-X5 predictions, all
 * provisional until the oracle fills O-1.
 */
class RandomSequenceSettingsTest {

  private static final String PROBE_A = "probe:a";

  // spec: core-random R30 (S, provisional): defaults salt 0, both flags true
  @Test
  void provisionalDefaultsAreSaltZeroAndBothFlagsTrue() {
    assertThat(RandomSequenceSettings.DEFAULTS)
        .isEqualTo(new RandomSequenceSettings(0, true, true));
  }

  static Stream<Arguments> vectorVX5() {
    return Stream.of(
        Arguments.of(
            "N1",
            0L,
            new RandomSequenceSettings(0, false, false),
            PROBE_A,
            new Seed128(3847398142028685078L, 7192185014346937746L)),
        Arguments.of(
            "N2",
            0L,
            new RandomSequenceSettings(1, false, false),
            PROBE_A,
            new Seed128(5272463233947570727L, 1927618558350093866L)),
        Arguments.of(
            "N3",
            0L,
            new RandomSequenceSettings(-1, false, false),
            PROBE_A,
            new Seed128(-110783831392733308L, 2932223646667407290L)),
        Arguments.of(
            "N4",
            0L,
            new RandomSequenceSettings(0, false, true),
            PROBE_A,
            new Seed128(2153501729715799201L, -2652207934038623535L)),
        Arguments.of(
            "N5",
            12345L,
            new RandomSequenceSettings(0, true, false),
            PROBE_A,
            new Seed128(733019005196230046L, -3494074583369400597L)),
        Arguments.of(
            "N6",
            12345L,
            new RandomSequenceSettings(0, true, true),
            PROBE_A,
            new Seed128(-8338171165017409289L, 2650172299325186875L)),
        Arguments.of(
            "N7",
            12345L,
            new RandomSequenceSettings(7, true, true),
            "minecraft:test",
            new Seed128(-8196287188144295939L, 4607975983393672058L)));
  }

  // spec: core-random R31 (S, provisional until P1), vectors V-X5 (initial state column)
  @ParameterizedTest(name = "{0}")
  @MethodSource("vectorVX5")
  void provisionalInitialStateMatchesVectorVX5(
      String name, long worldSeed, RandomSequenceSettings settings, String id, Seed128 expected) {
    assertThat(settings.initialState(worldSeed, id)).as(name).isEqualTo(expected);
  }

  // spec: core-random R31 step 1 (S, provisional): the counter-hypothesis N3z (salt extended by
  // zeros) is not used
  @Test
  void provisionalSaltIsSignExtendedNotZeroExtended() {
    Seed128 state = new RandomSequenceSettings(-1, false, false).initialState(0L, PROBE_A);
    assertThat(state).isNotEqualTo(new Seed128(5368716647786959924L, -554802411955493188L));
    assertThat(state)
        .isEqualTo(SeedExpansion.init64(0xFFFFFFFFFFFFFFFFL))
        .isNotEqualTo(SeedExpansion.init64(0x00000000FFFFFFFFL));
  }

  // spec: core-random R26, R31 step 3 (S, provisional): the counter-hypothesis N4le
  // (little-endian MD5) is not used
  @Test
  void provisionalFingerprintIsNotReadLittleEndian() {
    Seed128 state = new RandomSequenceSettings(0, false, true).initialState(0L, PROBE_A);
    assertThat(state).isNotEqualTo(new Seed128(1165417471098110475L, -2632000392545274068L));
  }

  // spec: core-random R31, R26 (S, provisional): N7 played with "minecraft:test", "test", ":test"
  @Test
  void provisionalIdentifierWithoutNamespaceGivesTheSameStateAsMinecraftNamespace() {
    RandomSequenceSettings settings = new RandomSequenceSettings(7, true, true);
    assertThat(settings.initialState(12345L, "test"))
        .isEqualTo(settings.initialState(12345L, ":test"))
        .isEqualTo(settings.initialState(12345L, "minecraft:test"))
        .isEqualTo(new Seed128(-8196287188144295939L, 4607975983393672058L));
  }

  // spec: core-random R31 steps 1 to 4 (S, provisional): composition of R22, R26 and R23, the
  // world seed and the salt combined by XOR, not by addition (edge case N7)
  @Test
  void provisionalInitialStateComposesTheFourSteps() {
    long worldSeed = -987654321987654321L;
    int salt = -123456;
    Seed128 unmixed = SeedExpansion.expandUnmixed(worldSeed ^ salt);
    Seed128 fingerprint = IdentifierFingerprint.of("minecraft:chests/simple_dungeon");
    Seed128 expected =
        new Seed128(
            SeedExpansion.mix(unmixed.lo() ^ fingerprint.lo()),
            SeedExpansion.mix(unmixed.hi() ^ fingerprint.hi()));
    RandomSequenceSettings settings = new RandomSequenceSettings(salt, true, true);
    assertThat(settings.initialState(worldSeed, "minecraft:chests/simple_dungeon"))
        .isEqualTo(expected);
  }

  // spec: core-random R31 step 1 (S, provisional): the world seed is ignored when excluded
  @Test
  void provisionalWorldSeedIsIgnoredWhenExcluded() {
    RandomSequenceSettings settings = new RandomSequenceSettings(0, false, false);
    assertThat(settings.initialState(12345L, PROBE_A))
        .isEqualTo(settings.initialState(0L, PROBE_A))
        .isEqualTo(new Seed128(3847398142028685078L, 7192185014346937746L));
  }

  // spec: core-random R31 step 3 (S, provisional): the identifier is ignored when excluded
  @Test
  void provisionalIdentifierIsIgnoredWhenExcluded() {
    RandomSequenceSettings settings = new RandomSequenceSettings(0, true, false);
    assertThat(settings.initialState(12345L, PROBE_A))
        .isEqualTo(settings.initialState(12345L, "minecraft:other"))
        .isEqualTo(new Seed128(733019005196230046L, -3494074583369400597L));
  }

  // spec: core-random R25 (S, probe P1): N1 is exactly Init64(0)
  @Test
  void provisionalCaseN1IsInit64OfZero() {
    assertThat(new RandomSequenceSettings(0, false, false).initialState(0L, PROBE_A))
        .isEqualTo(SeedExpansion.init64(0L));
  }

  // spec: core-random R31, R32 (S, provisional): a sequence starts its generator on that state
  @Test
  void provisionalGeneratorOnTheInitialStateDrawsTheFirstN6Value() {
    Seed128 state = new RandomSequenceSettings(0, true, true).initialState(12345L, PROBE_A);
    XoroshiroRandom random = XoroshiroRandom.ofState(state);
    assertThat(random.nextIntBetweenInclusive(0, 2147483645)).isEqualTo(2089840009);
  }

  // record semantics (generated members): equal settings are interchangeable
  @Test
  void settingsWithTheSameComponentsAreEqual() {
    assertThat(new RandomSequenceSettings(7, true, false))
        .isEqualTo(new RandomSequenceSettings(7, true, false))
        .hasSameHashCodeAs(new RandomSequenceSettings(7, true, false))
        .isNotEqualTo(new RandomSequenceSettings(7, false, false));
  }
}

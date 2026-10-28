package dev.shoko.minecraft.core.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Sources derived from a name or a position (spec core-random R27, R29). Provisional, not faithful
 * (Q1): the game's construction is unknown, so these tests only pin the engine's provisional
 * construction (vectors V-X6, not golden) and the verifiable contract (determinism, distinct
 * streams, every int coordinate, no division). They prove nothing about the game.
 */
class DerivedSourcesTest {

  // spec: core-random R29 (provisional, not faithful (Q1)), vectors V-X6 (hp column)
  @ParameterizedTest(name = "hp({0}, {1}, {2})")
  @CsvSource({
    "0, 0, 0, 0x0000000000000000",
    "1, 2, 3, 0xA55BDF37C08724B5",
    "-1, -64, -1, 0xA0C6E26C8ED33FBB",
    "2147483647, 319, -2147483648, 0x32B5375958F68299"
  })
  void provisionalNotFaithfulPositionHashMatchesVectorVX6(int x, int y, int z, String hash) {
    assertThat(Hex.of(DerivedSources.positionHash(x, y, z))).isEqualTo(hash);
  }

  // spec: core-random R29 (provisional, not faithful (Q1)), vectors V-X6 (first nextLong column)
  @ParameterizedTest(name = "d={0} ({1}, {2}, {3})")
  @CsvSource({
    "0, 0, 0, 0, 3038984756725240190",
    "0, 1, 2, 3, 1595374214425681131",
    "0, -1, -64, -1, 2798568289328956981",
    "0, 2147483647, 319, -2147483648, 2052514311196887087",
    "12345, 0, 0, 0, -8118485274630516485",
    "12345, 1, 2, 3, -1563368128130346438",
    "12345, -1, -64, -1, -4312487232099831939",
    "12345, 2147483647, 319, -2147483648, 1896549530065401711"
  })
  void provisionalNotFaithfulPositionSourceMatchesVectorVX6(
      long derivation, int x, int y, int z, long firstLong) {
    XoroshiroRandom random = DerivedSources.atPosition(derivation, x, y, z);
    assertThat(random.steps()).isZero();
    assertThat(random.nextLong()).isEqualTo(firstLong);
  }

  // spec: core-random R29 (provisional, not faithful (Q1)): hp = mix(mix(mix(x) XOR y) XOR z),
  // coordinates sign-extended, and the source is Init64(d XOR hp)
  @ParameterizedTest(name = "({0}, {1}, {2})")
  @CsvSource({
    "0, 0, 0",
    "-1, -1, -1",
    "-2147483648, -2147483648, -2147483648",
    "2147483647, 2147483647, 2147483647",
    "-30000000, -64, 29999999",
    "7, -7, 7"
  })
  void provisionalNotFaithfulPositionSourceIsInit64OfSeedXorHash(int x, int y, int z) {
    long hash = SeedExpansion.mix(SeedExpansion.mix(SeedExpansion.mix(x) ^ y) ^ z);
    assertThat(DerivedSources.positionHash(x, y, z)).isEqualTo(hash);
    long derivation = -4242L;
    assertThat(DerivedSources.atPosition(derivation, x, y, z).state())
        .isEqualTo(SeedExpansion.init64(derivation ^ hash))
        .isEqualTo(XoroshiroRandom.ofSeed(derivation ^ hash).state());
  }

  // spec: core-random R29 contract (provisional, not faithful (Q1)): determinism
  @Test
  void provisionalNotFaithfulPositionSourceIsDeterministic() {
    XoroshiroRandom a = DerivedSources.atPosition(99L, -5, 70, -123456);
    XoroshiroRandom b = DerivedSources.atPosition(99L, -5, 70, -123456);
    for (int i = 0; i < 20; i++) {
      assertThat(a.nextLong()).isEqualTo(b.nextLong());
    }
  }

  // spec: core-random R29 contract (provisional, not faithful (Q1)): neighbouring positions,
  // negative ones included, give distinct streams
  @Test
  void provisionalNotFaithfulNeighbouringPositionsGiveDistinctStreams() {
    Set<Long> firstLongs = new HashSet<>();
    int count = 0;
    for (int x = -3; x <= 2; x++) {
      for (int y = -3; y <= 2; y++) {
        for (int z = -3; z <= 2; z++) {
          firstLongs.add(DerivedSources.atPosition(12345L, x, y, z).nextLong());
          count++;
        }
      }
    }
    assertThat(firstLongs).hasSize(count);
  }

  // spec: core-random R29 contract (provisional, not faithful (Q1)): no division, so a
  // coordinate and its negation, or Integer.MIN_VALUE, need no special case
  @Test
  void provisionalNotFaithfulPositionHashDistinguishesSignsAndExtremes() {
    assertThat(DerivedSources.positionHash(-1, 0, 0))
        .isNotEqualTo(DerivedSources.positionHash(1, 0, 0));
    assertThat(DerivedSources.positionHash(0, 0, Integer.MIN_VALUE))
        .isNotEqualTo(DerivedSources.positionHash(0, 0, Integer.MAX_VALUE))
        .isEqualTo(SeedExpansion.mix(Integer.MIN_VALUE));
  }

  // spec: core-random R27 (provisional, not faithful (Q1)): XOR each derivation word with the
  // fingerprint, then mix each word (R31 steps 3 and 4)
  @ParameterizedTest(name = "{0}")
  @CsvSource({"probe:a", "minecraft:test", "minecraft:aquifer", "x:y"})
  void provisionalNotFaithfulNamedSourceXorsTheFingerprintThenMixes(String identifier) {
    Seed128 derivation = new Seed128(0x0123456789ABCDEFL, -42L);
    Seed128 fingerprint = IdentifierFingerprint.of(identifier);
    XoroshiroRandom random = DerivedSources.byName(derivation, identifier);
    assertThat(random.steps()).isZero();
    assertThat(random.state())
        .isEqualTo(
            new Seed128(
                SeedExpansion.mix(derivation.lo() ^ fingerprint.lo()),
                SeedExpansion.mix(derivation.hi() ^ fingerprint.hi())));
  }

  // spec: core-random R27 (provisional, not faithful (Q1)) applied to the unmixed expansion of 0
  // is R31 for case N4 (vectors V-X5)
  @Test
  void provisionalNotFaithfulNamedSourceOnExpansionOfZeroIsCaseN4() {
    XoroshiroRandom random = DerivedSources.byName(SeedExpansion.expandUnmixed(0L), "probe:a");
    assertThat(random.state()).isEqualTo(new Seed128(2153501729715799201L, -2652207934038623535L));
  }

  // spec: core-random R27 contract (provisional, not faithful (Q1)): deterministic, and two
  // distinct identifiers give distinct streams
  @Test
  void provisionalNotFaithfulNamedSourcesAreDeterministicAndDistinct() {
    Seed128 derivation = SeedExpansion.init64(12345L);
    assertThat(DerivedSources.byName(derivation, "minecraft:ore").nextLong())
        .isEqualTo(DerivedSources.byName(derivation, "minecraft:ore").nextLong())
        .isNotEqualTo(DerivedSources.byName(derivation, "minecraft:aquifer").nextLong());
  }

  // spec: core-random R29 known limit of the provisional, not faithful (Q1) hash, vectors V-X6:
  // (23901, 0, z) and (52826, 683136096, z) collide for every z. Allowed by the contract (only
  // neighbours must differ); pinned so that a faithful hash, once Q1 is lifted, shows up here.
  @ParameterizedTest(name = "z = {0}")
  @CsvSource({"0, -1813262085031591903", "5, -949738680630853989", "-7, 1788741283831798574"})
  void provisionalNotFaithfulKnownLimitDistantPositionsShareTheirHash(int z, long hash) {
    assertThat(DerivedSources.positionHash(23901, 0, z)).isEqualTo(hash);
    assertThat(DerivedSources.positionHash(52826, 683136096, z)).isEqualTo(hash);
  }

  // spec: core-random R29 known limit (provisional, not faithful (Q1)), vectors V-X6: the shared
  // hash gives the same stream for every derivation seed
  @ParameterizedTest
  @ValueSource(longs = {0L, 12345L, -1L, Long.MIN_VALUE})
  void provisionalNotFaithfulKnownLimitDistantPositionsShareTheirStream(long derivation) {
    XoroshiroRandom first = DerivedSources.atPosition(derivation, 23901, 0, 42);
    XoroshiroRandom second = DerivedSources.atPosition(derivation, 52826, 683136096, 42);
    for (int i = 0; i < 10; i++) {
      assertThat(first.nextLong()).as("draw %d", i).isEqualTo(second.nextLong());
    }
  }

  // spec: core-random R23, R29 (C): the collision comes from mix(23901) = mix(52826) XOR 683136096
  @Test
  void provisionalNotFaithfulKnownLimitComesFromTheIntermediateStage() {
    assertThat(SeedExpansion.mix(23901L)).isEqualTo(SeedExpansion.mix(52826L) ^ 683136096L);
  }
}

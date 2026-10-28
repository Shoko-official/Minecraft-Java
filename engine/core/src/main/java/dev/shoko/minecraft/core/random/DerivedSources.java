package dev.shoko.minecraft.core.random;

/**
 * Provisional, not faithful: generators derived from a name or a position (R27, R29). Pure
 * functions.
 *
 * <p>The game's construction of these sources is unknown (spec Q1). This class only fulfils the
 * verifiable contract (determinism, distinct streams for distinct inputs) with a provisional
 * construction that keeps the rest of the engine testable. No golden vector can be derived from it;
 * the faithful construction will come from the worldgen spec. The position source is excluded from
 * M1 fidelity (R29).
 */
public final class DerivedSources {

  private DerivedSources() {}

  /**
   * Provisional, not faithful (R27): the generator derived from a derivation seed and an
   * identifier. Construction: XOR each word of {@code derivation} with the matching word of {@link
   * IdentifierFingerprint#of(String)}, then {@link SeedExpansion#mix(long)} each word (R31 steps 3
   * and 4).
   *
   * @param derivation the two words of the derivation seed
   * @param identifier the identifier, hashed as in {@link IdentifierFingerprint#of(String)}
   * @return a new generator with a step count of 0
   * @throws IllegalArgumentException when {@code derivation} equals the fingerprint, the only input
   *     that gives the all-zero state
   */
  public static XoroshiroRandom byName(Seed128 derivation, String identifier) {
    return XoroshiroRandom.ofState(SeedExpansion.xorFingerprintThenMix(derivation, identifier));
  }

  /**
   * Provisional, not faithful (R29): position hash {@code hp(x, y, z) = mix(mix(mix(x) XOR y) XOR
   * z)}, each coordinate sign-extended to 64 bits, {@code mix} being {@link
   * SeedExpansion#mix(long)}. Defined for every int, without division (vectors V-X6, not golden).
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @param z the z coordinate
   * @return the hash; 0 for {@code (0, 0, 0)}
   */
  public static long positionHash(int x, int y, int z) {
    // Each int widens to long with sign extension before the XOR.
    return SeedExpansion.mix(SeedExpansion.mix(SeedExpansion.mix(x) ^ y) ^ z);
  }

  /**
   * Provisional, not faithful (R29): the generator derived from a derivation seed {@code d} and a
   * position, {@link XoroshiroRandom#ofSeed(long)} of {@code d XOR }{@link #positionHash(int, int,
   * int)} (vectors V-X6, not golden).
   *
   * @param derivationSeed the derivation seed {@code d}
   * @param x the x coordinate
   * @param y the y coordinate
   * @param z the z coordinate
   * @return a new generator with a step count of 0
   */
  public static XoroshiroRandom atPosition(long derivationSeed, int x, int y, int z) {
    return XoroshiroRandom.ofSeed(derivationSeed ^ positionHash(x, y, z));
  }
}

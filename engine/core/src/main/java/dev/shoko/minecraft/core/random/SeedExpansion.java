package dev.shoko.minecraft.core.random;

/**
 * Expansion of a 64-bit value into a 128-bit {@link XoroshiroRandom} state, and the bit mixer it
 * uses (R22 to R24). Pure functions, arithmetic mod {@code 2^64}.
 */
public final class SeedExpansion {

  /** {@code K_SQRT2} (R22): the odd integer after {@code frac(sqrt 2) * 2^64}. */
  private static final long SQRT2_FRACTION = 0x6A09E667F3BCC909L;

  /** {@code K_PHI} (R22): {@code floor(2^64 / phi)}, phi the golden ratio. */
  private static final long GOLDEN_RATIO = 0x9E3779B97F4A7C15L;

  /** First multiplier of Stafford's variant 13 (R23). */
  private static final long MIX_MULTIPLIER_1 = 0xBF58476D1CE4E5B9L;

  /** Second multiplier of Stafford's variant 13 (R23). */
  private static final long MIX_MULTIPLIER_2 = 0x94D049BB133111EBL;

  private SeedExpansion() {}

  /**
   * Stafford's mixer, variant 13, the SplitMix64 finalizer (R23): {@code z = (z XOR (z >>> 30)) *
   * 0xBF58476D1CE4E5B9}, {@code z = (z XOR (z >>> 27)) * 0x94D049BB133111EB}, result {@code z XOR
   * (z >>> 31)}. It is a bijection and maps 0 to 0 (vectors V-X2).
   *
   * @param z the word to mix
   * @return the mixed word
   */
  public static long mix(long z) {
    long mixed = (z ^ (z >>> 30)) * MIX_MULTIPLIER_1;
    mixed = (mixed ^ (mixed >>> 27)) * MIX_MULTIPLIER_2;
    return mixed ^ (mixed >>> 31);
  }

  /**
   * Unmixed expansion {@code E(v)} (R22): {@code lo = v XOR 0x6A09E667F3BCC909}, {@code hi = lo +
   * 0x9E3779B97F4A7C15}. Never all-zero.
   *
   * @param value the 64-bit value
   * @return the two words before mixing
   */
  public static Seed128 expandUnmixed(long value) {
    long lo = value ^ SQRT2_FRACTION;
    return new Seed128(lo, lo + GOLDEN_RATIO);
  }

  /**
   * {@code Init64(seed)} (R24): {@link #mix(long)} applied to each word of {@link
   * #expandUnmixed(long)} (vectors V-X3). Never all-zero.
   *
   * @param seed the 64-bit seed
   * @return the generator state
   */
  public static Seed128 init64(long seed) {
    return mixWords(expandUnmixed(seed));
  }

  /** Returns {@link #mix(long)} of each word, as in R24 and R31 step 4. */
  static Seed128 mixWords(Seed128 words) {
    return new Seed128(mix(words.lo()), mix(words.hi()));
  }

  /** XORs each word with the identifier fingerprint, then mixes each word (R31 steps 3 and 4). */
  static Seed128 xorFingerprintThenMix(Seed128 words, String identifier) {
    Seed128 fingerprint = IdentifierFingerprint.of(identifier);
    return mixWords(new Seed128(words.lo() ^ fingerprint.lo(), words.hi() ^ fingerprint.hi()));
  }
}

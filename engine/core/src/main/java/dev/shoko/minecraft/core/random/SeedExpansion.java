package dev.shoko.minecraft.core.random;

/**
 * Expansion of a 64-bit value into a 128-bit {@link XoroshiroRandom} state, and the bit mixer it
 * uses (R22 to R24). Pure functions, arithmetic mod {@code 2^64}.
 */
public final class SeedExpansion {

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
    return Skeleton.throwNotImplemented();
  }

  /**
   * Unmixed expansion {@code E(v)} (R22): {@code lo = v XOR 0x6A09E667F3BCC909}, {@code hi = lo +
   * 0x9E3779B97F4A7C15}. Never all-zero.
   *
   * @param value the 64-bit value
   * @return the two words before mixing
   */
  public static Seed128 expandUnmixed(long value) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * {@code Init64(seed)} (R24): {@link #mix(long)} applied to each word of {@link
   * #expandUnmixed(long)} (vectors V-X3). Never all-zero.
   *
   * @param seed the 64-bit seed
   * @return the generator state
   */
  public static Seed128 init64(long seed) {
    return Skeleton.throwNotImplemented();
  }
}

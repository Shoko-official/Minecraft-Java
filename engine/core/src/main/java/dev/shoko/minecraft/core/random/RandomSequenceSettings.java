package dev.shoko.minecraft.core.random;

/**
 * World settings of the named random sequences (R30), and the initial state of a sequence they give
 * (R31). Immutable.
 *
 * @param salt the salt, a signed 32-bit int (R30)
 * @param includeWorldSeed whether the world seed takes part in the initial state (R30, R31 step 1)
 * @param includeSequenceId whether the sequence identifier takes part in the initial state (R30,
 *     R31 step 3)
 */
public record RandomSequenceSettings(
    int salt, boolean includeWorldSeed, boolean includeSequenceId) {

  /** Settings of a world that never changed them: salt 0, both flags true (R30, level S). */
  public static final RandomSequenceSettings DEFAULTS = new RandomSequenceSettings(0, true, true);

  /**
   * Returns the initial {@link XoroshiroRandom} state of a sequence (R31, level S, spec Q5):
   *
   * <ol>
   *   <li>{@code v = (includeWorldSeed ? worldSeed : 0) XOR salt}, the salt sign-extended to 64
   *       bits;
   *   <li>{@code (lo, hi) = }{@link SeedExpansion#expandUnmixed(long)} of {@code v} (R22);
   *   <li>if {@code includeSequenceId}, XOR each word with the matching word of {@link
   *       IdentifierFingerprint#of(String)} (R26);
   *   <li>{@link SeedExpansion#mix(long)} applied to each word (R23).
   * </ol>
   *
   * <p>Vectors V-X5, initial state column. The result is the value {@code source} holds right after
   * a reset (R32).
   *
   * @param worldSeed the world seed
   * @param sequenceId the sequence identifier; only hashed when {@code includeSequenceId}
   * @return the initial generator state
   */
  public Seed128 initialState(long worldSeed, String sequenceId) {
    return Skeleton.throwNotImplemented();
  }
}

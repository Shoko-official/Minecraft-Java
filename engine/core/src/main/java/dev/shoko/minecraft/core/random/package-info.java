/**
 * Seeded pseudo-random generators of the engine and the seed functions that initialize them.
 *
 * <p>Spec: {@code docs/specs/core-random.md}; rule numbers Rn in the Javadoc refer to it, vectors
 * to {@code docs/specs/core-random-vectors.md}.
 *
 * <ul>
 *   <li>Common contract: {@link dev.shoko.minecraft.core.random.RandomSource}, with a readable step
 *       counter and {@code skip}.
 *   <li>Families: {@link dev.shoko.minecraft.core.random.Lcg48Random} (48-bit congruential
 *       generator, G48 in the spec, R1 to R12) and {@link
 *       dev.shoko.minecraft.core.random.XoroshiroRandom} (xoroshiro128++, X128 in the spec, R13 to
 *       R21).
 *   <li>Seed functions: {@link dev.shoko.minecraft.core.random.SeedExpansion} (R22 to R24), {@link
 *       dev.shoko.minecraft.core.random.IdentifierFingerprint} (R26), {@link
 *       dev.shoko.minecraft.core.random.RandomSequenceSettings} (named sequences, R30, R31), all on
 *       the 128-bit value {@link dev.shoko.minecraft.core.random.Seed128}.
 *   <li>Provisional, not faithful: {@link dev.shoko.minecraft.core.random.DerivedSources} (sources
 *       by name and by position, R27, R29) and {@link
 *       dev.shoko.minecraft.core.random.RandomSource#fork()} (R28).
 * </ul>
 *
 * <p>No class here extends {@code java.util.Random} or implements {@code
 * java.util.random.RandomGenerator}: their bounded-range methods use other algorithms and other
 * consumption (R12), and the engine determinism rule forbids them. Generators are mutable and not
 * thread-safe; seed functions and values are immutable. No method accepts or returns {@code null}:
 * a {@code null} argument throws {@link NullPointerException}, even where the value would be
 * unused.
 */
package dev.shoko.minecraft.core.random;

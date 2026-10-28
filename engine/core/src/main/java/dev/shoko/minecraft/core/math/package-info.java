/**
 * Numeric helpers whose exact rounding decides the fidelity of world generation.
 *
 * <p>Spec: {@code docs/specs/core-math.md}; rule numbers Rn in the Javadoc refer to it. Rules
 * marked provisional (level S in the spec) describe the engine's current contract, which the blind
 * tests target until an oracle probe settles the game's behavior.
 *
 * <ul>
 *   <li>Rounding to integers: {@link dev.shoko.minecraft.core.math.Rounding} (R4 to R7).
 *   <li>Real modulo: {@link dev.shoko.minecraft.core.math.RealModulo} (R10).
 *   <li>Interpolation and quintic fade: {@link dev.shoko.minecraft.core.math.Interpolation} (R11 to
 *       R14, R16).
 *   <li>Clamping: {@link dev.shoko.minecraft.core.math.Clamping} (R15).
 *   <li>Table sine and cosine: {@link dev.shoko.minecraft.core.math.TrigTable} (R17).
 *   <li>Arc tangent and angle normalization: {@link dev.shoko.minecraft.core.math.Angles} (R18,
 *       R20).
 *   <li>Powers of two: {@link dev.shoko.minecraft.core.math.PowersOfTwo} (R21).
 *   <li>Weighted selection: {@link dev.shoko.minecraft.core.math.WeightedSelection} (R23).
 * </ul>
 *
 * <p>Not provided here, on purpose: integer floor division and modulo (R8, R9) are {@link
 * java.lang.Math#floorDiv(int, int)}, {@link java.lang.Math#floorMod(int, int)} and the arithmetic
 * shift {@code >>}; never {@code /} or {@code %} on block coordinates. There is no fast inverse
 * square root (R19). Bounded integer draws (R22) and the position hash (R24) belong to the {@code
 * core-random} spec. Any value that affects world state uses {@link java.lang.StrictMath}
 * transcendental functions (R3) and keeps the {@code float} or {@code double} precision and the
 * operation order of its formula (R1, R2).
 *
 * <p>All classes are stateless and safe to share between threads. No method accepts or returns
 * {@code null}.
 */
package dev.shoko.minecraft.core.math;

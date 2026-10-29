/**
 * Choice of the random source of a draw and observation of the order in which a draw consumes it.
 *
 * <p>Spec: {@code docs/specs/loot-tables.md} (LT-R63 to LT-R67, hypothesis H-draw) and {@code
 * docs/specs/core-random.md} (R14, R30 to R35). The engine never creates a generator by itself
 * except the 48-bit one of an explicit seed (LT-R63); a test injects a scripted {@link
 * dev.shoko.minecraft.core.random.RandomSource} and reads {@link
 * dev.shoko.minecraft.loot.random.DrawEvent}s to check the order of consumption.
 */
package dev.shoko.minecraft.loot.random;

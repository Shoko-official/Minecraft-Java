/**
 * The datapack loader of milestone M2 and the data it produces (K2 of {@code
 * docs/specs/data-datapack-loading.md}, ADR 0005).
 *
 * <p>The loader validates what it can without knowing any game type: pack metadata, file
 * attribution, JSON syntax of every file, tag files and their references. It leaves the typed
 * decoding of elements to the modules that own the types. It never stops at the first problem; the
 * caller (engine/game) applies the severity policy of R55. Results are immutable and ordered
 * (determinism). No method accepts or returns {@code null}.
 */
package dev.shoko.minecraft.data.loader;

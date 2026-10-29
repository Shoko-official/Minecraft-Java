/**
 * Where a problem is, what it is, and the result of an operation that reports problems instead of
 * stopping (R54 and the output list of {@code docs/specs/data-datapack-loading.md}).
 *
 * <p>The loader never stops at the first diagnostic: the caller (engine/game) applies the severity
 * policy of R55. The codes and their severities are the closed table of R61. Types here are
 * immutable. No method accepts or returns {@code null}.
 */
package dev.shoko.minecraft.data.diag;

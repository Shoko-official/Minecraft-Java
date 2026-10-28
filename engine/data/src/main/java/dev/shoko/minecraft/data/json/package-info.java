/**
 * The JSON tree and its strict reader (R49 to R54, R57 to R61, ADR 0006).
 *
 * <p>Spec: {@code docs/specs/data-datapack-loading.md}. Every node knows its file and its path in
 * it, for diagnostics. Objects keep the order of the file (determinism). Trees are immutable and
 * safe to share between threads. This is the only package allowed to use the Gson library, and no
 * Gson type appears in a signature.
 */
package dev.shoko.minecraft.data.json;

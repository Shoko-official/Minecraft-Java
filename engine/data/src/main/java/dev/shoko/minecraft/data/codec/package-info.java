/**
 * Decoders from the JSON tree to typed values, and their combinators (K1 of {@code
 * docs/specs/data-datapack-loading.md}, ADR 0005).
 *
 * <p>The module that owns a type writes its codec from these pieces and registers it; {@code data}
 * knows no game type. Decoding only: nothing here writes JSON (R65); codecs are values that cannot
 * be implemented outside this package. Following R56, a missing required field is an error with the
 * JSON path, a missing optional field takes its default, and a present field of the wrong type is
 * an error, never the default. All the errors of an object are collected (R62 g). Unknown fields
 * are ignored unless the object is declared strict (R63). Codecs are immutable and safe to share
 * between threads. No method accepts or returns {@code null}.
 */
package dev.shoko.minecraft.data.codec;

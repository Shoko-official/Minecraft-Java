/**
 * Tag files, merging across packs and resolution (R9, R31 to R39).
 *
 * <p>Spec: {@code docs/specs/data-datapack-loading.md}. Tags are merged on raw entries in pack
 * priority order, then resolved against the identifiers of their registry, without decoding any
 * element. Results are ordered and free of duplicates (determinism). Types are immutable. No method
 * accepts or returns {@code null}.
 */
package dev.shoko.minecraft.data.tag;

/**
 * Registries and references to their entries (ADR 0005, R26 to R30).
 *
 * <p>Spec: {@code docs/specs/data-datapack-loading.md}. A registry maps identifiers to values of a
 * type owned by another module and gives every entry a raw numeric id. {@code data} never defines
 * those types: the owning module builds its typed registries from the loaded data. References
 * between entries are holders, resolved by identifier, or sets of holders written as a tag or an
 * inline list. Types are immutable and safe to share between threads once built. No method accepts
 * or returns {@code null}.
 */
package dev.shoko.minecraft.data.registry;

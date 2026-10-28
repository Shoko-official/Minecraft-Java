/**
 * Datapack loading infrastructure: sources, JSON trees, registries, codecs and tags (ADR 0005).
 *
 * <p>Specs: {@code docs/specs/data-datapack-loading.md} and {@code
 * docs/specs/data-registries-inventory.md}; rule numbers Rn in the Javadoc refer to the first one.
 * Packages, from the lowest layer to the highest, each depending only on the ones before it:
 *
 * <ul>
 *   <li>{@code id}: namespaced identifiers.
 *   <li>{@code diag}: locations, diagnostics and the {@code Outcome} of an operation.
 *   <li>{@code json}: the JSON tree and its strict reader (ADR 0006).
 *   <li>{@code pack.meta}: {@code pack.mcmeta}, format versions and feature flags.
 *   <li>{@code pack}: pack files, packs and their priority stack.
 *   <li>{@code registry}: registries, holders and references by identifier or tag.
 *   <li>{@code tag}: tag files, merging across packs and resolution.
 *   <li>{@code codec}: JSON decoders and their combinators, for the modules that own a type.
 *   <li>{@code catalog}: registry folders, static registries and file attribution.
 *   <li>{@code loader}: the datapack loader and its result.
 * </ul>
 *
 * <p>This module knows no game type: typed decoding belongs to the module that owns the type.
 */
package dev.shoko.minecraft.data;

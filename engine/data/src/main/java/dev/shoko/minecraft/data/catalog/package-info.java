/**
 * What the extracted reference reports say about registries and where each pack file belongs: the
 * folders of the loadable registries, the static registries with their entries, and the attribution
 * of a file path to a registry element or tag (R8 to R16, R24 to R28).
 *
 * <p>Spec: {@code docs/specs/data-datapack-loading.md} and {@code
 * docs/specs/data-registries-inventory.md}. The reports are inputs read at run time from the
 * extracted data, never copied into the code (ADR 0002, ADR 0003). Types are immutable. No method
 * accepts or returns {@code null}.
 */
package dev.shoko.minecraft.data.catalog;

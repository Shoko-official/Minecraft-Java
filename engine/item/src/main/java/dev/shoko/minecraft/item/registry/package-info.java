/**
 * The item registry and the prototype components of each item.
 *
 * <p>Spec: {@code docs/specs/item-components-inventory.md} (IC-R1 to IC-R5). The prototypes come
 * from the extracted reports, not from code (IC-R2). As in {@code world}, the module defines a port
 * ({@link dev.shoko.minecraft.item.registry.ItemReportSource}) instead of reading the loaded data
 * itself: the caller adapts whatever {@code data} offers. Registries and catalogs are immutable and
 * safe to share between threads. No method accepts or returns {@code null}.
 */
package dev.shoko.minecraft.item.registry;

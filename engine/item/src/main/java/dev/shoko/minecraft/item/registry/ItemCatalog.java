package dev.shoko.minecraft.item.registry;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.Registry;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.component.ComponentCatalog;
import java.util.Optional;

/**
 * All items and their prototypes, loaded from the reports (IC-R1 to IC-R5). The reference data has
 * 1658 items, each with a prototype table. Immutable once loaded.
 */
public final class ItemCatalog {

  /** Key of the {@code minecraft:item} registry, for {@code Holder} and {@code HolderSet}. */
  public static final RegistryKey<ItemType> REGISTRY =
      new RegistryKey<>(Identifier.minecraft("item"));

  private ItemCatalog() {}

  /**
   * Loads and verifies the catalog. Each prototype table is decoded with the component registry of
   * the source; a table that fails to decode fails the load, with the item named in the path of the
   * finding.
   *
   * @param source the reports
   * @return the catalog, or the failure when an invariant is broken: the default entry is not the
   *     first item or is missing, an identifier is duplicated, a prototype breaks IC-R5 ({@code
   *     max_damage} present requires {@code max_stack_size} 1, {@code damage} requires {@code
   *     max_damage}), all as {@code INCONSISTENT_PROTOTYPES}
   */
  public static Decoding<ItemCatalog> load(ItemReportSource source) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the component types.
   *
   * @return the catalog of the 122 component types
   */
  public ComponentCatalog components() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the items as a {@code data} registry, for the references of ingredients (the default
   * entry is air).
   *
   * @return the registry
   */
  public Registry<ItemType> registry() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Finds an item by identifier.
   *
   * @param id an item identifier
   * @return the item, or empty when it is not registered
   */
  public Optional<ItemType> find(Identifier id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the default entry (IC-R1).
   *
   * @return {@code minecraft:air}
   */
  public ItemType air() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of items.
   *
   * @return the size, 1658 for the reference data
   */
  public int size() {
    return Skeleton.throwNotImplemented();
  }
}

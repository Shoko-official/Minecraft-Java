package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.List;
import java.util.Optional;

/**
 * The registered component types, the acceptance list of the decoding (IC-R9, IC-R10). Immutable.
 */
public final class ComponentCatalog {

  private ComponentCatalog() {}

  /**
   * Builds the catalog from the registry listing.
   *
   * @param idsInRegistryOrder the identifiers in the order of the registry: entry {@code i} has raw
   *     id {@code i}
   * @return the catalog
   * @throws IllegalArgumentException when an identifier appears twice
   */
  public static ComponentCatalog of(List<Identifier> idsInRegistryOrder) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of types.
   *
   * @return the size, 122 for the reference data
   */
  public int size() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the types in registry order.
   *
   * @return the types, unmodifiable
   */
  public List<ComponentType> types() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Finds a type by identifier.
   *
   * @param id a component identifier
   * @return the type, or empty when the identifier is not registered
   */
  public Optional<ComponentType> find(Identifier id) {
    return Skeleton.throwNotImplemented();
  }
}

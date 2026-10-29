package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The registered component types, the acceptance list of the decoding (IC-R9, IC-R10). Immutable.
 */
public final class ComponentCatalog {

  private final List<ComponentType> types;
  // Lookup only: never iterated, so its order is not observable.
  private final Map<Identifier, ComponentType> byId;

  private ComponentCatalog(List<ComponentType> types, Map<Identifier, ComponentType> byId) {
    this.types = types;
    this.byId = byId;
  }

  /**
   * Builds the catalog from the registry listing.
   *
   * @param idsInRegistryOrder the identifiers in the order of the registry: entry {@code i} has raw
   *     id {@code i}
   * @return the catalog
   * @throws IllegalArgumentException when an identifier appears twice
   */
  public static ComponentCatalog of(List<Identifier> idsInRegistryOrder) {
    List<ComponentType> types = new ArrayList<>(idsInRegistryOrder.size());
    Map<Identifier, ComponentType> byId = new HashMap<>();
    for (Identifier id : idsInRegistryOrder) {
      ComponentType type = new ComponentType(id, types.size());
      if (byId.put(id, type) != null) {
        throw new IllegalArgumentException("duplicated component type: " + id);
      }
      types.add(type);
    }
    return new ComponentCatalog(List.copyOf(types), byId);
  }

  /**
   * Returns the number of types.
   *
   * @return the size, 122 for the reference data
   */
  public int size() {
    return types.size();
  }

  /**
   * Returns the types in registry order.
   *
   * @return the types, unmodifiable
   */
  public List<ComponentType> types() {
    return types;
  }

  /**
   * Finds a type by identifier.
   *
   * @param id a component identifier
   * @return the type, or empty when the identifier is not registered
   */
  public Optional<ComponentType> find(Identifier id) {
    return Optional.ofNullable(byId.get(id));
  }
}

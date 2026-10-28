package dev.shoko.minecraft.data.loader;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * The elements of one registry loaded from files, kept as JSON trees until the owning module
 * decodes them. For each identifier only the version of the pack of highest priority is kept, in
 * full (R41); the origin of each tree tells which pack it comes from. Immutable.
 */
public final class ElementRegistryData {

  private ElementRegistryData() {}

  /**
   * Returns the name of the registry.
   *
   * @return the identifier, such as {@code minecraft:loot_table}
   */
  public Identifier registry() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the elements in code point order of their identifiers (R30, provisional). The map is
   * empty for a registry that no pack fills (R28).
   *
   * @return an unmodifiable map; a file that was invalid is absent (R54)
   */
  public SequencedMap<Identifier, JsonNode> elements() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns one element.
   *
   * @param id the identifier of the element
   * @return the tree, or empty when absent
   */
  public Optional<JsonNode> get(Identifier id) {
    return Skeleton.throwNotImplemented();
  }
}

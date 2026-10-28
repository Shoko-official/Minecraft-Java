package dev.shoko.minecraft.data.registry;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.List;
import java.util.Optional;

/**
 * Read access to what is loaded, by registry. It answers existence questions from identifiers alone
 * (the universe of R33), so that references can be checked before the typed values exist (K4 of the
 * spec), and gives the typed registry once its owning module has built it.
 */
public interface RegistryAccess {

  /**
   * Returns an access that knows nothing: no registry, no tag.
   *
   * @return the empty access
   */
  static RegistryAccess empty() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether an entry exists, typed or not.
   *
   * @param registry the registry
   * @param id the identifier of the entry
   * @return whether the registry lists the identifier
   */
  boolean contains(RegistryKey<?> registry, Identifier id);

  /**
   * Returns the typed registry.
   *
   * @param key the name of the registry
   * @param <T> the type of the entries
   * @return the registry, or empty when its owning module has not built it
   */
  <T> Optional<Registry<T>> registry(RegistryKey<T> key);

  /**
   * Returns the identifiers a tag resolved to, in order (R35).
   *
   * @param tag the tag
   * @return the members, or empty when the tag does not exist or failed to load (R36)
   */
  Optional<List<Identifier>> tagMembers(TagKey<?> tag);
}

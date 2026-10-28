package dev.shoko.minecraft.data.registry;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SequencedMap;

/**
 * An immutable, ordered map from identifiers to values, with a raw numeric id per entry. The raw id
 * of an entry is its position: for a static registry, its {@code protocol_id} (R26); for a registry
 * loaded from files, its rank in the code point order of the identifiers (R30, provisional).
 *
 * @param <T> the type of the values
 */
public final class Registry<T> {

  private Registry() {}

  /**
   * Builds a registry without default entry.
   *
   * @param key the name of the registry
   * @param values the entries; their iteration order gives the raw ids 0 to n-1
   * @param <T> the type of the values
   * @return the registry; later changes to {@code values} do not affect it
   */
  public static <T> Registry<T> of(RegistryKey<T> key, SequencedMap<Identifier, T> values) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Builds a registry with a default entry, as 10 static registries have (R26).
   *
   * @param key the name of the registry
   * @param values the entries; their iteration order gives the raw ids 0 to n-1
   * @param defaultId the identifier of the default entry, which must be in {@code values}
   * @param <T> the type of the values
   * @return the registry; later changes to {@code values} do not affect it
   * @throws IllegalArgumentException when {@code defaultId} is not in {@code values}
   */
  public static <T> Registry<T> of(
      RegistryKey<T> key, SequencedMap<Identifier, T> values, Identifier defaultId) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the name of the registry.
   *
   * @return the key
   */
  public RegistryKey<T> key() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of entries.
   *
   * @return the size, 0 for an empty registry (R28)
   */
  public int size() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the identifiers in raw id order.
   *
   * @return an unmodifiable list
   */
  public List<Identifier> ids() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether an identifier is registered.
   *
   * @param id the identifier
   * @return whether the registry has the entry
   */
  public boolean contains(Identifier id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the value of an entry.
   *
   * @param id the identifier
   * @return the value, or empty when absent
   */
  public Optional<T> get(Identifier id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns an entry as a holder bound to its value.
   *
   * @param id the identifier
   * @return the holder, or empty when absent
   */
  public Optional<Holder<T>> holder(Identifier id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the entry with a raw id.
   *
   * @param rawId the position, 0 to {@code size() - 1}
   * @return the holder, or empty when out of range
   */
  public Optional<Holder<T>> byRawId(int rawId) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the raw id of an entry.
   *
   * @param id the identifier
   * @return the raw id, or empty when absent
   */
  public OptionalInt rawIdOf(Identifier id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the identifier of the default entry (R26).
   *
   * @return the identifier, or empty when the registry has none
   */
  public Optional<Identifier> defaultId() {
    return Skeleton.throwNotImplemented();
  }
}

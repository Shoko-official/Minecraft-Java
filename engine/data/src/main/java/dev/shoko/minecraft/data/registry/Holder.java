package dev.shoko.minecraft.data.registry;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.Objects;
import java.util.Optional;

/**
 * A reference to a value: either a registry entry designated by identifier, or a value written in
 * place. A reference is resolved on first use, so that entries may refer to each other, even in a
 * loop, whatever the order in which registries are decoded (K4 of the spec). Immutable.
 *
 * @param <T> the type of the value
 */
public final class Holder<T> {

  // Null for a value written in place.
  private final RegistryKey<T> registry;
  private final Identifier id;
  // Null once the value is known.
  private final RegistryAccess access;
  // Set once, from the registry entry or at construction; the same value whoever resolves it.
  private volatile T value;

  private Holder(RegistryKey<T> registry, Identifier id, RegistryAccess access, T value) {
    this.registry = registry;
    this.id = id;
    this.access = access;
    this.value = value;
  }

  /**
   * Designates a registry entry; nothing is looked up until {@link #value()} is called.
   *
   * @param registry the registry of the entry
   * @param id the identifier of the entry
   * @param access where the registry is found when the reference is resolved
   * @param <T> the type of the value
   * @return the reference
   */
  public static <T> Holder<T> reference(
      RegistryKey<T> registry, Identifier id, RegistryAccess access) {
    return new Holder<>(registry, id, access, null);
  }

  /**
   * Wraps a value written in place.
   *
   * @param value the value, not {@code null}
   * @param <T> the type of the value
   * @return the holder, without identifier
   */
  public static <T> Holder<T> direct(T value) {
    return new Holder<>(null, null, null, Objects.requireNonNull(value));
  }

  static <T> Holder<T> bound(RegistryKey<T> registry, Identifier id, T value) {
    return new Holder<>(registry, id, null, Objects.requireNonNull(value));
  }

  /**
   * Returns the value, resolving the reference when needed.
   *
   * @return the value
   * @throws IllegalStateException when the entry does not exist
   */
  public T value() {
    T known = value;
    if (known != null) {
      return known;
    }
    T resolved =
        access
            .registry(registry)
            .flatMap(entries -> entries.get(id))
            .orElseThrow(
                () -> new IllegalStateException("no entry " + id + " in " + registry.name()));
    value = resolved;
    return resolved;
  }

  /**
   * Returns the identifier of the entry.
   *
   * @return the identifier, or empty for a value written in place
   */
  public Optional<Identifier> id() {
    return Optional.ofNullable(id);
  }

  /**
   * Compares what the holders designate: same registry and identifier for references, equal values
   * for values in place.
   *
   * @param other any object
   * @return whether {@code other} is a holder designating the same thing
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Holder<?> that)) {
      return false;
    }
    if (registry != null) {
      return registry.equals(that.registry) && id.equals(that.id);
    }
    return that.registry == null && value.equals(that.value);
  }

  /**
   * Returns a hash consistent with {@link #equals(Object)}.
   *
   * @return the hash
   */
  @Override
  public int hashCode() {
    return registry != null ? 31 * registry.hashCode() + id.hashCode() : value.hashCode();
  }
}

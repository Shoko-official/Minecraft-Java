package dev.shoko.minecraft.data.registry;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.Optional;

/**
 * A reference to a value: either a registry entry designated by identifier, or a value written in
 * place. A reference is resolved on first use, so that entries may refer to each other, even in a
 * loop, whatever the order in which registries are decoded (K4 of the spec). Immutable.
 *
 * @param <T> the type of the value
 */
public final class Holder<T> {

  private Holder() {}

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
    return Skeleton.throwNotImplemented();
  }

  /**
   * Wraps a value written in place.
   *
   * @param value the value, not {@code null}
   * @param <T> the type of the value
   * @return the holder, without identifier
   */
  public static <T> Holder<T> direct(T value) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the value, resolving the reference when needed.
   *
   * @return the value
   * @throws IllegalStateException when the entry does not exist
   */
  public T value() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the identifier of the entry.
   *
   * @return the identifier, or empty for a value written in place
   */
  public Optional<Identifier> id() {
    return Skeleton.throwNotImplemented();
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
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a hash consistent with {@link #equals(Object)}.
   *
   * @return the hash
   */
  @Override
  public int hashCode() {
    return Skeleton.throwNotImplemented();
  }
}

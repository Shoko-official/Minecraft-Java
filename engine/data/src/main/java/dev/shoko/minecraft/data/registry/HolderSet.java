package dev.shoko.minecraft.data.registry;

import java.util.List;
import java.util.Optional;

/**
 * A set of holders written either as a {@code #tag} or as an inline list (K1 of the spec). The
 * members of a tag are those of its resolved form (R35), in that order. Immutable.
 *
 * @param <T> the type of the values
 */
public final class HolderSet<T> {

  private HolderSet() {}

  /**
   * Designates a tag.
   *
   * @param tag the tag
   * @param access where the tag is found when the members are first requested
   * @param <T> the type of the values
   * @return the set; nothing is looked up until {@link #holders()} is called
   */
  public static <T> HolderSet<T> tag(TagKey<T> tag, RegistryAccess access) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Wraps an inline list.
   *
   * @param holders the members, kept in order
   * @param <T> the type of the values
   * @return the set; later changes to {@code holders} do not affect it
   */
  public static <T> HolderSet<T> list(List<Holder<T>> holders) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the tag this set designates.
   *
   * @return the tag, or empty for an inline list
   */
  public Optional<TagKey<T>> tagKey() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the members.
   *
   * @return the holders in order, without duplicates for a tag (R35)
   * @throws IllegalStateException when the tag does not exist or failed to load (R36)
   */
  public List<Holder<T>> holders() {
    return Skeleton.throwNotImplemented();
  }
}

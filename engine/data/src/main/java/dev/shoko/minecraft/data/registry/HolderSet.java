package dev.shoko.minecraft.data.registry;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A set of holders written either as a {@code #tag} or as an inline list (K1 of the spec). The
 * members of a tag are those of its resolved form (R35), in that order. Immutable.
 *
 * @param <T> the type of the values
 */
public final class HolderSet<T> {

  // Exactly one of tag and inline is set.
  private final TagKey<T> tag;
  private final RegistryAccess access;
  private final List<Holder<T>> inline;

  private HolderSet(TagKey<T> tag, RegistryAccess access, List<Holder<T>> inline) {
    this.tag = tag;
    this.access = access;
    this.inline = inline;
  }

  /**
   * Designates a tag.
   *
   * @param tag the tag
   * @param access where the tag is found when the members are first requested
   * @param <T> the type of the values
   * @return the set; nothing is looked up until {@link #holders()} is called
   */
  public static <T> HolderSet<T> tag(TagKey<T> tag, RegistryAccess access) {
    return new HolderSet<>(tag, access, null);
  }

  /**
   * Wraps an inline list.
   *
   * @param holders the members, kept in order
   * @param <T> the type of the values
   * @return the set; later changes to {@code holders} do not affect it
   */
  public static <T> HolderSet<T> list(List<Holder<T>> holders) {
    return new HolderSet<>(null, null, List.copyOf(holders));
  }

  /**
   * Returns the tag this set designates.
   *
   * @return the tag, or empty for an inline list
   */
  public Optional<TagKey<T>> tagKey() {
    return Optional.ofNullable(tag);
  }

  /**
   * Returns the members.
   *
   * @return the holders in order, without duplicates for a tag (R35)
   * @throws IllegalStateException when the tag does not exist or failed to load (R36)
   */
  public List<Holder<T>> holders() {
    if (tag == null) {
      return inline;
    }
    List<Identifier> members =
        access
            .tagMembers(tag)
            .orElseThrow(() -> new IllegalStateException("no tag #" + tag.id() + " loaded"));
    List<Holder<T>> holders = new ArrayList<>(members.size());
    for (Identifier member : members) {
      holders.add(Holder.reference(tag.registry(), member, access));
    }
    return List.copyOf(holders);
  }
}

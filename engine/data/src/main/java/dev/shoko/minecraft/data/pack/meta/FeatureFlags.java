package dev.shoko.minecraft.data.pack.meta;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.TreeSet;

/**
 * A set of feature flags, such as {@code minecraft:trade_rebalance} (R44). Immutable; iteration
 * follows the code point order of the identifiers (determinism).
 *
 * <p>The flags of a world are {@link #vanilla()} plus the flags enabled by the packs active at
 * creation (R45, provisional). The game module computes and keeps that set; the loader only
 * receives it as input (R66 a).
 */
public final class FeatureFlags {

  private static final FeatureFlags VANILLA =
      new FeatureFlags(List.of(Identifier.minecraft("vanilla")));

  private final List<Identifier> sorted;
  private final Set<Identifier> members;

  private FeatureFlags(List<Identifier> sorted) {
    this.sorted = sorted;
    this.members = new HashSet<>(sorted);
  }

  /**
   * Returns the flags a world always has: {@code minecraft:vanilla} (R45, provisional).
   *
   * @return the set holding only the vanilla flag
   */
  public static FeatureFlags vanilla() {
    return VANILLA;
  }

  /**
   * Returns a set of flags.
   *
   * @param flags the flags; duplicates are merged
   * @return the set; later changes to {@code flags} do not affect it
   */
  public static FeatureFlags of(Collection<Identifier> flags) {
    return new FeatureFlags(List.copyOf(new TreeSet<>(flags)));
  }

  /**
   * Returns the flags in code point order.
   *
   * @return an unmodifiable collection
   */
  public SequencedCollection<Identifier> flags() {
    return sorted;
  }

  /**
   * Tells whether a flag is in the set.
   *
   * @param flag the flag
   * @return whether the set holds it
   */
  public boolean contains(Identifier flag) {
    return members.contains(flag);
  }

  /**
   * Tells whether every flag of another set is in this one, as needed to enable a pack (R45).
   *
   * @param required the flags to look for
   * @return whether this set holds all of them
   */
  public boolean containsAll(FeatureFlags required) {
    return members.containsAll(required.members);
  }

  /**
   * Returns the union of two sets.
   *
   * @param other the other set
   * @return the set holding the flags of both
   */
  public FeatureFlags union(FeatureFlags other) {
    TreeSet<Identifier> all = new TreeSet<>(sorted);
    all.addAll(other.sorted);
    return new FeatureFlags(List.copyOf(all));
  }

  /**
   * Compares the flags.
   *
   * @param other any object
   * @return whether {@code other} is a set with the same flags
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof FeatureFlags that && sorted.equals(that.sorted);
  }

  /**
   * Returns a hash consistent with {@link #equals(Object)}.
   *
   * @return the hash
   */
  @Override
  public int hashCode() {
    return sorted.hashCode();
  }

  @Override
  public String toString() {
    return sorted.toString();
  }
}

package dev.shoko.minecraft.data.pack.meta;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.Collection;
import java.util.SequencedCollection;

/**
 * A set of feature flags, such as {@code minecraft:trade_rebalance} (R44). Immutable; iteration
 * follows the code point order of the identifiers (determinism).
 *
 * <p>The flags of a world are {@link #vanilla()} plus the flags enabled by the packs active at
 * creation (R45, provisional).
 */
public final class FeatureFlags {

  private FeatureFlags() {}

  /**
   * Returns the flags a world always has: {@code minecraft:vanilla} (R45, provisional).
   *
   * @return the set holding only the vanilla flag
   */
  public static FeatureFlags vanilla() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a set of flags.
   *
   * @param flags the flags; duplicates are merged
   * @return the set; later changes to {@code flags} do not affect it
   */
  public static FeatureFlags of(Collection<Identifier> flags) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the flags in code point order.
   *
   * @return an unmodifiable collection
   */
  public SequencedCollection<Identifier> flags() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether a flag is in the set.
   *
   * @param flag the flag
   * @return whether the set holds it
   */
  public boolean contains(Identifier flag) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether every flag of another set is in this one, as needed to enable a pack (R45).
   *
   * @param required the flags to look for
   * @return whether this set holds all of them
   */
  public boolean containsAll(FeatureFlags required) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the union of two sets.
   *
   * @param other the other set
   * @return the set holding the flags of both
   */
  public FeatureFlags union(FeatureFlags other) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Compares the flags.
   *
   * @param other any object
   * @return whether {@code other} is a set with the same flags
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

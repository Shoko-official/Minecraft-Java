package dev.shoko.minecraft.data.tag;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;

/**
 * The tags of one registry after merging and resolution. A tag is either resolved, possibly to an
 * empty list (R38), or failed, in which case it does not exist (R36). Immutable.
 */
public final class TagResolution {

  private TagResolution() {}

  /**
   * Returns the identifiers of the resolved tags in code point order (R30).
   *
   * @return the tag identifiers
   */
  public SequencedCollection<Identifier> tags() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the members of a tag.
   *
   * @param tag the identifier of the tag, without the {@code #}
   * @return the element identifiers in resolved order, without duplicates (R35), or empty when the
   *     tag does not exist or failed; an existing empty tag gives a present, empty list
   */
  public Optional<List<Identifier>> members(Identifier tag) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the identifiers of the tags that failed, in code point order (R36, R37).
   *
   * @return the failed tags
   */
  public SequencedCollection<Identifier> failed() {
    return Skeleton.throwNotImplemented();
  }
}

package dev.shoko.minecraft.core.nbt;

import java.util.Objects;

/**
 * Root of a file: a value with the name written before it (R5, R14). Reference files use the empty
 * name; any other name is kept as read so that rewriting gives the same bytes (R15, R30). Neither
 * component may be {@code null} (constructing with {@code null} throws {@link
 * NullPointerException}).
 *
 * @param name the root name, usually empty
 * @param tag the root value, of any type (a file root is a compound, R14)
 */
public record NamedTag(String name, NbtTag tag) {

  public NamedTag {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(tag, "tag");
  }
}

package dev.shoko.minecraft.data.id;

import java.util.Optional;

/**
 * A namespaced identifier {@code namespace:path} (R20). Immutable; equality is equality of both
 * strings (R25). No normalization is ever applied: {@code foo:Bar} is invalid, not rewritten.
 *
 * <p>Namespace characters are {@code a-z 0-9 _ - .}; path characters are the same plus {@code /}
 * (R20). The namespace {@code ..} is invalid. An empty path is syntactically valid and matches no
 * element (R22, provisional).
 */
public final class Identifier implements Comparable<Identifier> {

  /** Namespace assumed when a value written in JSON has none (R22). */
  public static final String DEFAULT_NAMESPACE = "minecraft";

  private Identifier() {}

  /**
   * Builds an identifier from its two parts, which must already be valid (R20).
   *
   * @param namespace the namespace
   * @param path the path
   * @return the identifier
   * @throws IllegalArgumentException when a part uses a forbidden character, or the namespace is
   *     {@code ..}
   */
  public static Identifier of(String namespace, String path) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Builds {@code minecraft:path}.
   *
   * @param path the path
   * @return the identifier
   * @throws IllegalArgumentException when the path is invalid
   */
  public static Identifier minecraft(String path) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads an identifier written as a JSON value (R22): without {@code :} or with a leading {@code
   * :}, the namespace is {@link #DEFAULT_NAMESPACE}; at most one {@code :} is allowed. A leading
   * {@code #} is not accepted here (R23, a tag reference is read by the tag package).
   *
   * @param text the text, such as {@code stone}, {@code :stone} or {@code minecraft:stone}
   * @return the identifier
   * @throws IllegalArgumentException when the text is not valid (R20), such as {@code Foo:bar},
   *     {@code a:b:c} or {@code ..:x}
   */
  public static Identifier parse(String text) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Same as {@link #parse(String)} but reports invalid text as an empty result.
   *
   * @param text the text
   * @return the identifier, or empty when the text is not valid
   */
  public static Optional<Identifier> tryParse(String text) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether a string is a valid namespace (R20).
   *
   * @param namespace the candidate
   * @return whether it only uses allowed characters and is not {@code ..}
   */
  public static boolean isValidNamespace(String namespace) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether a string is a valid path (R20); the empty path is valid (R22).
   *
   * @param path the candidate
   * @return whether it only uses allowed characters
   */
  public static boolean isValidPath(String path) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the namespace.
   *
   * @return the namespace
   */
  public String namespace() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the path, possibly with {@code /} separators.
   *
   * @return the path
   */
  public String path() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Orders by namespace then by path, comparing Unicode code points (R30). Independent of locale.
   *
   * @param other the identifier to compare with
   * @return a negative, zero or positive number
   */
  @Override
  public int compareTo(Identifier other) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Compares namespace and path (R25).
   *
   * @param other any object
   * @return whether {@code other} is an identifier with the same namespace and path
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

  /**
   * Returns the full form {@code namespace:path}, always with the namespace.
   *
   * @return the text
   */
  @Override
  public String toString() {
    return Skeleton.throwNotImplemented();
  }
}

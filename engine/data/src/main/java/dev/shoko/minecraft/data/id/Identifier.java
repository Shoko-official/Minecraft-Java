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

  private final String namespace;
  private final String path;

  private Identifier(String namespace, String path) {
    this.namespace = namespace;
    this.path = path;
  }

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
    if (!isValidNamespace(namespace)) {
      throw new IllegalArgumentException("invalid namespace: " + namespace);
    }
    if (!isValidPath(path)) {
      throw new IllegalArgumentException("invalid path: " + path);
    }
    return new Identifier(namespace, path);
  }

  /**
   * Builds {@code minecraft:path}.
   *
   * @param path the path
   * @return the identifier
   * @throws IllegalArgumentException when the path is invalid
   */
  public static Identifier minecraft(String path) {
    return of(DEFAULT_NAMESPACE, path);
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
    int colon = text.indexOf(':');
    if (colon < 0) {
      return of(DEFAULT_NAMESPACE, text);
    }
    String namespace = colon == 0 ? DEFAULT_NAMESPACE : text.substring(0, colon);
    return of(namespace, text.substring(colon + 1));
  }

  /**
   * Same as {@link #parse(String)} but reports invalid text as an empty result.
   *
   * @param text the text
   * @return the identifier, or empty when the text is not valid
   */
  public static Optional<Identifier> tryParse(String text) {
    int colon = text.indexOf(':');
    String namespace = colon <= 0 ? DEFAULT_NAMESPACE : text.substring(0, colon);
    String path = colon < 0 ? text : text.substring(colon + 1);
    if (!isValidNamespace(namespace) || !isValidPath(path)) {
      return Optional.empty();
    }
    return Optional.of(new Identifier(namespace, path));
  }

  /**
   * Tells whether a string is a valid namespace (R20).
   *
   * @param namespace the candidate
   * @return whether it only uses allowed characters and is not {@code ..}
   */
  public static boolean isValidNamespace(String namespace) {
    return !namespace.isEmpty() && !namespace.equals("..") && onlyAllowed(namespace, false);
  }

  /**
   * Tells whether a string is a valid path (R20); the empty path is valid (R22).
   *
   * @param path the candidate
   * @return whether it only uses allowed characters
   */
  public static boolean isValidPath(String path) {
    return onlyAllowed(path, true);
  }

  /**
   * Returns the namespace.
   *
   * @return the namespace
   */
  public String namespace() {
    return namespace;
  }

  /**
   * Returns the path, possibly with {@code /} separators.
   *
   * @return the path
   */
  public String path() {
    return path;
  }

  /**
   * Orders by namespace then by path, comparing Unicode code points (R30). Independent of locale.
   *
   * @param other the identifier to compare with
   * @return a negative, zero or positive number
   */
  @Override
  public int compareTo(Identifier other) {
    // Valid characters are ASCII, so UTF-16 order and code point order agree.
    int byNamespace = namespace.compareTo(other.namespace);
    return byNamespace != 0 ? byNamespace : path.compareTo(other.path);
  }

  /**
   * Compares namespace and path (R25).
   *
   * @param other any object
   * @return whether {@code other} is an identifier with the same namespace and path
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof Identifier id && namespace.equals(id.namespace) && path.equals(id.path);
  }

  /**
   * Returns a hash consistent with {@link #equals(Object)}.
   *
   * @return the hash
   */
  @Override
  public int hashCode() {
    return 31 * namespace.hashCode() + path.hashCode();
  }

  /**
   * Returns the full form {@code namespace:path}, always with the namespace.
   *
   * @return the text
   */
  @Override
  public String toString() {
    return namespace + ":" + path;
  }

  private static boolean onlyAllowed(String text, boolean allowSlash) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      boolean allowed =
          (c >= 'a' && c <= 'z')
              || (c >= '0' && c <= '9')
              || c == '_'
              || c == '-'
              || c == '.'
              || (allowSlash && c == '/');
      if (!allowed) {
        return false;
      }
    }
    return true;
  }
}

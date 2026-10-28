package dev.shoko.minecraft.data.diag;

/**
 * A path inside a JSON document: the root, then object keys and array indices. Immutable. Its text
 * form starts with {@code $}, then {@code .key} for a key and {@code [n]} for an index, such as
 * {@code $.values[2]}.
 */
public final class JsonPath {

  private JsonPath() {}

  /**
   * Returns the path of the document root.
   *
   * @return the root path, whose text is {@code $}
   */
  public static JsonPath root() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the path of an object member.
   *
   * @param key the member name
   * @return the child path
   */
  public JsonPath field(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the path of an array element.
   *
   * @param index the position, 0 or more
   * @return the child path
   * @throws IllegalArgumentException when {@code index} is negative
   */
  public JsonPath index(int index) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Compares the sequence of keys and indices.
   *
   * @param other any object
   * @return whether {@code other} is a path with the same steps
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
   * Returns the text form.
   *
   * @return the text
   */
  @Override
  public String toString() {
    return Skeleton.throwNotImplemented();
  }
}

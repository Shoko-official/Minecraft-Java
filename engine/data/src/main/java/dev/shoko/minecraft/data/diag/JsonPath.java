package dev.shoko.minecraft.data.diag;

/**
 * A path inside a JSON document: the root, then object keys and array indices. Immutable. Its text
 * form (R61) is {@code $} for the root, {@code .name} for an object member and {@code [i]} for an
 * array element, {@code i} counting from 0, such as {@code $}, {@code $.a.b} or {@code
 * $.values[2].id}. A member name is written as decoded, without quotes or escaping, so a name that
 * contains {@code .} or {@code [} gives a text that several paths share; {@link #equals(Object)}
 * compares the steps, not the text.
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
   * Returns the text form of R61.
   *
   * @return the text, {@code $} for the root
   */
  @Override
  public String toString() {
    return Skeleton.throwNotImplemented();
  }
}

package dev.shoko.minecraft.data.diag;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * A path inside a JSON document: the root, then object keys and array indices. Immutable. Its text
 * form (R61) is {@code $} for the root, {@code .name} for an object member and {@code [i]} for an
 * array element, {@code i} counting from 0, such as {@code $}, {@code $.a.b} or {@code
 * $.values[2].id}. A member name is written as decoded, without quotes or escaping, so a name that
 * contains {@code .} or {@code [} gives a text that several paths share; {@link #equals(Object)}
 * compares the steps, not the text.
 */
public final class JsonPath {

  private static final JsonPath ROOT = new JsonPath(null, null, -1);

  private final JsonPath parent;
  // Exactly one of key (non-null) and index (non-negative) describes the last step; the root has
  // neither.
  private final String key;
  private final int index;
  private final int depth;
  private final int hash;

  private JsonPath(JsonPath parent, String key, int index) {
    this.parent = parent;
    this.key = key;
    this.index = index;
    this.depth = parent == null ? 0 : parent.depth + 1;
    this.hash = parent == null ? 0 : 31 * parent.hash + (key != null ? key.hashCode() : ~index);
  }

  /**
   * Returns the path of the document root.
   *
   * @return the root path, whose text is {@code $}
   */
  public static JsonPath root() {
    return ROOT;
  }

  /**
   * Returns the path of an object member.
   *
   * @param key the member name
   * @return the child path
   */
  public JsonPath field(String key) {
    return new JsonPath(this, Objects.requireNonNull(key), -1);
  }

  /**
   * Returns the path of an array element.
   *
   * @param index the position, 0 or more
   * @return the child path
   * @throws IllegalArgumentException when {@code index} is negative
   */
  public JsonPath index(int index) {
    if (index < 0) {
      throw new IllegalArgumentException("negative index: " + index);
    }
    return new JsonPath(this, null, index);
  }

  /**
   * Compares the sequence of keys and indices.
   *
   * @param other any object
   * @return whether {@code other} is a path with the same steps
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof JsonPath that) || depth != that.depth || hash != that.hash) {
      return false;
    }
    JsonPath a = this;
    JsonPath b = that;
    while (a.parent != null) {
      if (a.index != b.index || !Objects.equals(a.key, b.key)) {
        return false;
      }
      a = a.parent;
      b = b.parent;
    }
    return true;
  }

  /**
   * Returns a hash consistent with {@link #equals(Object)}.
   *
   * @return the hash
   */
  @Override
  public int hashCode() {
    return hash;
  }

  /**
   * Returns the text form of R61.
   *
   * @return the text, {@code $} for the root
   */
  @Override
  public String toString() {
    Deque<JsonPath> steps = new ArrayDeque<>();
    for (JsonPath p = this; p.parent != null; p = p.parent) {
      steps.push(p);
    }
    StringBuilder text = new StringBuilder("$");
    for (JsonPath step : steps) {
      if (step.key != null) {
        text.append('.').append(step.key);
      } else {
        text.append('[').append(step.index).append(']');
      }
    }
    return text.toString();
  }
}

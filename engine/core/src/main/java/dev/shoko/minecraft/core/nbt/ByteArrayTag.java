package dev.shoko.minecraft.core.nbt;

import java.util.Arrays;

/**
 * Byte_Array value: signed bytes behind a signed 32-bit element count (R2, R9). Immutable: values
 * are copied in and out.
 */
public final class ByteArrayTag implements NbtTag {

  private final byte[] values;

  private ByteArrayTag(byte[] values) {
    this.values = values;
  }

  /**
   * Returns an array tag holding a copy of these values (R2, R9).
   *
   * @param values the elements, possibly none
   * @return the tag; later changes to {@code values} do not affect it
   */
  public static ByteArrayTag of(byte... values) {
    return new ByteArrayTag(values.clone());
  }

  /**
   * Returns a copy of the elements (R2, R9).
   *
   * @return a new array; changing it does not affect this tag
   */
  public byte[] toArray() {
    return values.clone();
  }

  /**
   * Returns the number of elements (R2, R9).
   *
   * @return the element count, 0 or more
   */
  public int size() {
    return values.length;
  }

  /**
   * Returns one element without copying the array (R2, R9).
   *
   * @param index position, from 0 to {@link #size()} - 1
   * @return the element
   * @throws IndexOutOfBoundsException when {@code index} is out of range
   */
  public byte get(int index) {
    return values[index];
  }

  /**
   * Returns {@link TagType#BYTE_ARRAY} (R2).
   *
   * @return the type
   */
  @Override
  public TagType type() {
    return TagType.BYTE_ARRAY;
  }

  /**
   * Compares element by element; an array of another type is never equal (R37).
   *
   * @param other any object
   * @return whether {@code other} is a {@code ByteArrayTag} with the same elements in the same
   *     order
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof ByteArrayTag tag && Arrays.equals(values, tag.values);
  }

  /**
   * Returns a hash consistent with {@link #equals(Object)} (R37).
   *
   * @return the hash of the elements
   */
  @Override
  public int hashCode() {
    return Arrays.hashCode(values);
  }
}

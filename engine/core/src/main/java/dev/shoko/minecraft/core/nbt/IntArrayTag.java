package dev.shoko.minecraft.core.nbt;

/**
 * Int_Array value: signed 32-bit integers behind a signed 32-bit element count (R1, R8). A UUID is
 * an Int_Array of 4 elements, most significant first (R8). Immutable: values are copied in and out.
 */
public final class IntArrayTag implements NbtTag {

  private IntArrayTag() {}

  /**
   * Returns an array tag holding a copy of these values (R1, R8).
   *
   * @param values the elements, possibly none
   * @return the tag; later changes to {@code values} do not affect it
   */
  public static IntArrayTag of(int... values) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a copy of the elements (R1, R8).
   *
   * @return a new array; changing it does not affect this tag
   */
  public int[] toArray() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of elements (R1, R8).
   *
   * @return the element count, 0 or more
   */
  public int size() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns one element without copying the array (R1, R8).
   *
   * @param index position, from 0 to {@link #size()} - 1
   * @return the element
   * @throws IndexOutOfBoundsException when {@code index} is out of range
   */
  public int get(int index) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@link TagType#INT_ARRAY} (R1).
   *
   * @return the type
   */
  @Override
  public TagType type() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Compares element by element; an array of another type is never equal (R37).
   *
   * @param other any object
   * @return whether {@code other} is a {@code IntArrayTag} with the same elements in the same order
   */
  @Override
  public boolean equals(Object other) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a hash consistent with {@link #equals(Object)} (R37).
   *
   * @return the hash of the elements
   */
  @Override
  public int hashCode() {
    return Skeleton.throwNotImplemented();
  }
}

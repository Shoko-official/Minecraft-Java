package dev.shoko.minecraft.core.nbt;

import java.util.List;

/**
 * List value: elements in order (R7). Immutable.
 *
 * <p>An empty list has no element type of its own: its element type is always {@link TagType#END},
 * which is how it is written (R21, R22), whatever type a file declared for it (R23, R25).
 *
 * <p>Elements of different types are allowed (R47): such a heterogeneous list keeps its elements
 * unwrapped in memory and in SNBT (R56), and is written in binary as a list of compounds, each
 * holding one element under the empty key. Reading that binary form back gives the list of
 * compounds, not the heterogeneous list.
 */
public final class ListTag implements NbtTag {

  private static final ListTag EMPTY = new ListTag(List.of(), TagType.END);

  private final List<NbtTag> elements;
  private final TagType elementType;

  private ListTag(List<NbtTag> elements, TagType elementType) {
    this.elements = elements;
    this.elementType = elementType;
  }

  // A heterogeneous list is written as compounds wrapping each element (R47).
  private static TagType commonType(List<NbtTag> elements) {
    TagType first = elements.getFirst().type();
    for (NbtTag element : elements) {
      if (element.type() != first) {
        return TagType.COMPOUND;
      }
    }
    return first;
  }

  /**
   * Returns a list holding these elements in this order (R7, R47).
   *
   * @param elements the elements, of one or several types, none {@code null}
   * @return the list; later changes to {@code elements} do not affect it
   * @throws NullPointerException when {@code elements} or one of its elements is {@code null}
   */
  public static ListTag of(List<? extends NbtTag> elements) {
    List<NbtTag> copy = List.copyOf(elements);
    return copy.isEmpty() ? EMPTY : new ListTag(copy, commonType(copy));
  }

  /**
   * Returns the empty list, of element type {@link TagType#END} (R22); also the default value of
   * {@link CompoundTag#getList(String)} (R38).
   *
   * @return the empty list
   */
  public static ListTag empty() {
    return EMPTY;
  }

  /**
   * Returns the element type written in binary (R7, R22, R47): {@link TagType#END} when empty, the
   * common type of the elements when they share one, {@link TagType#COMPOUND} when they do not.
   *
   * @return the element type
   */
  public TagType elementType() {
    return elementType;
  }

  /**
   * Returns the elements in order (R7).
   *
   * @return an unmodifiable list
   */
  public List<NbtTag> elements() {
    return elements;
  }

  /**
   * Returns {@link TagType#LIST} (R1).
   *
   * @return the type
   */
  @Override
  public TagType type() {
    return TagType.LIST;
  }

  /**
   * Compares elements in order; two empty lists are equal (R37).
   *
   * @param other any object
   * @return whether {@code other} is a list with equal elements in the same order
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof ListTag list && elements.equals(list.elements);
  }

  /**
   * Returns a hash consistent with {@link #equals(Object)} (R37).
   *
   * @return the hash of the elements
   */
  @Override
  public int hashCode() {
    return elements.hashCode();
  }
}

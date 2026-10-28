package dev.shoko.minecraft.world.palette;

import java.util.List;

/**
 * A fixed number of positions, each holding a value, stored through a palette of distinct values
 * (CF-R6, CF-R12). The block states of a section use 4096 positions in the order of CF-R4; its
 * biomes use 64 in the order of CF-R5.
 *
 * <p>Values are compared with {@code equals} to merge palette entries. Mutable, confined to the
 * thread that ticks the world. The internal representation is free (CF-R12).
 *
 * @param <T> the value type
 */
public final class PalettedContainer<T> {

  private PalettedContainer() {}

  /**
   * Creates a container where every position holds the same value, a single-entry palette.
   *
   * @param <T> the value type
   * @param size number of positions, positive
   * @param value the value of every position
   * @return the container
   */
  public static <T> PalettedContainer<T> filled(int size, T value) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of positions.
   *
   * @return the size
   */
  public int size() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads one position.
   *
   * @param index position in {@code [0, size())}
   * @return the value
   * @throws IndexOutOfBoundsException if out of range
   */
  public T get(int index) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes one position.
   *
   * @param index position in {@code [0, size())}
   * @param value the new value
   * @return the previous value
   * @throws IndexOutOfBoundsException if out of range
   */
  public T set(int index, T value) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the distinct values currently held, in order of first occurrence by position. The disk
   * form uses exactly these as palette, so no entry is unused (chunk writing).
   *
   * @return an unmodifiable list, never empty
   */
  public List<T> distinctValues() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether every position holds the same value (CF-R6, palette of one entry).
   *
   * @return {@code true} for a single distinct value
   */
  public boolean isUniform() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns an independent container with the same values.
   *
   * @return the copy
   */
  public PalettedContainer<T> copy() {
    return Skeleton.throwNotImplemented();
  }
}

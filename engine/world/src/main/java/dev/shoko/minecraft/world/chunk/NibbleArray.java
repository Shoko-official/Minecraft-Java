package dev.shoko.minecraft.world.chunk;

/**
 * 4096 values from 0 to 15, two per byte, in the block order of a section (CF-R14): an even index
 * is in the low 4 bits of byte {@code index >> 1}, an odd index in the high 4 bits. Mutable,
 * confined to the thread that ticks the world.
 */
public final class NibbleArray {

  /** Length of the byte form, 4096 values at 4 bits (CF-R14). */
  public static final int BYTE_LENGTH = 2048;

  private NibbleArray() {}

  /**
   * Creates an array of zeros.
   *
   * @return the array
   */
  public static NibbleArray zeros() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Creates an array with the same value everywhere.
   *
   * @param value from 0 to 15
   * @return the array
   * @throws IllegalArgumentException if the value is out of range
   */
  public static NibbleArray filled(int value) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Creates an array from its byte form, copied.
   *
   * @param bytes exactly {@link #BYTE_LENGTH} bytes
   * @return the array
   * @throws IllegalArgumentException if the length differs
   */
  public static NibbleArray ofBytes(byte[] bytes) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads one value.
   *
   * @param index from 0 to 4095, as {@code LocalIndex.sectionStateIndex}
   * @return the value from 0 to 15
   * @throws IndexOutOfBoundsException if out of range
   */
  public int get(int index) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes one value.
   *
   * @param index from 0 to 4095
   * @param value from 0 to 15
   * @throws IndexOutOfBoundsException if the index is out of range
   * @throws IllegalArgumentException if the value is out of range
   */
  public void set(int index, int value) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Returns the byte form (CF-R14).
   *
   * @return a new array of {@link #BYTE_LENGTH} bytes
   */
  public byte[] toBytes() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns an independent array with the same values.
   *
   * @return the copy
   */
  public NibbleArray copy() {
    return Skeleton.throwNotImplemented();
  }
}

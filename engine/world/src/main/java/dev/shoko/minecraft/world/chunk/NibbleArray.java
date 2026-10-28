package dev.shoko.minecraft.world.chunk;

import java.util.Arrays;

/**
 * 4096 values from 0 to 15, two per byte, in the block order of a section (CF-R14): an even index
 * is in the low 4 bits of byte {@code index >> 1}, an odd index in the high 4 bits. Mutable,
 * confined to the thread that ticks the world.
 */
public final class NibbleArray {

  /** Length of the byte form, 4096 values at 4 bits (CF-R14). */
  public static final int BYTE_LENGTH = 2048;

  private final byte[] bytes;

  private NibbleArray(byte[] bytes) {
    this.bytes = bytes;
  }

  /**
   * Creates an array of zeros.
   *
   * @return the array
   */
  public static NibbleArray zeros() {
    return new NibbleArray(new byte[BYTE_LENGTH]);
  }

  /**
   * Creates an array with the same value everywhere.
   *
   * @param value from 0 to 15
   * @return the array
   * @throws IllegalArgumentException if the value is out of range
   */
  public static NibbleArray filled(int value) {
    checkValue(value);
    byte[] bytes = new byte[BYTE_LENGTH];
    Arrays.fill(bytes, (byte) (value | (value << 4)));
    return new NibbleArray(bytes);
  }

  /**
   * Creates an array from its byte form, copied.
   *
   * @param bytes exactly {@link #BYTE_LENGTH} bytes
   * @return the array
   * @throws IllegalArgumentException if the length differs
   */
  public static NibbleArray ofBytes(byte[] bytes) {
    if (bytes.length != BYTE_LENGTH) {
      throw new IllegalArgumentException("expected " + BYTE_LENGTH + " bytes, got " + bytes.length);
    }
    return new NibbleArray(bytes.clone());
  }

  /**
   * Reads one value.
   *
   * @param index from 0 to 4095, as {@code LocalIndex.sectionStateIndex}
   * @return the value from 0 to 15
   * @throws IndexOutOfBoundsException if out of range
   */
  public int get(int index) {
    checkIndex(index);
    int packed = bytes[index >> 1];
    return (index & 1) == 0 ? packed & 0xF : (packed >> 4) & 0xF;
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
    checkIndex(index);
    checkValue(value);
    int packed = bytes[index >> 1];
    bytes[index >> 1] =
        (byte) ((index & 1) == 0 ? (packed & 0xF0) | value : (packed & 0x0F) | (value << 4));
  }

  /**
   * Returns the byte form (CF-R14).
   *
   * @return a new array of {@link #BYTE_LENGTH} bytes
   */
  public byte[] toBytes() {
    return bytes.clone();
  }

  /**
   * Returns an independent array with the same values.
   *
   * @return the copy
   */
  public NibbleArray copy() {
    return new NibbleArray(bytes.clone());
  }

  private static void checkIndex(int index) {
    if (index < 0 || index >= BYTE_LENGTH * 2) {
      throw new IndexOutOfBoundsException("index " + index + " of " + BYTE_LENGTH * 2);
    }
  }

  private static void checkValue(int value) {
    if (value < 0 || value > 15) {
      throw new IllegalArgumentException("value must be from 0 to 15: " + value);
    }
  }
}

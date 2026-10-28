package dev.shoko.minecraft.core.nbt;

import java.util.Optional;
import java.util.SequencedMap;

/**
 * Compound value: unique string keys mapped to values (R6, R26). Immutable.
 *
 * <p>Keys keep their storage order: the order of the map given to {@link #of(SequencedMap)}, which
 * is the read order for a decoded compound and the write order (R30). The order is not part of
 * equality (R28, R37). Reproducing the order observed in reference files (R29) is not required.
 *
 * <p>The typed getters read leniently (R38): a number of another numeric type is converted as
 * described by {@link NumericTag}; an absent key or a value of an incompatible type gives the
 * default value (0, {@code false}, empty string, empty array, empty list, empty compound).
 */
public final class CompoundTag implements NbtTag {

  private CompoundTag() {}

  /**
   * Returns a compound holding these entries in their iteration order (R6, R30).
   *
   * @param entries keys and values, none {@code null}
   * @return the compound; later changes to {@code entries} do not affect it
   * @throws NullPointerException when {@code entries}, a key or a value is {@code null}
   */
  public static CompoundTag of(SequencedMap<String, ? extends NbtTag> entries) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the compound without entries (R6); also the default value of {@link
   * #getCompound(String)} (R38).
   *
   * @return the empty compound
   */
  public static CompoundTag empty() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the entries in storage order (R30).
   *
   * @return an unmodifiable map
   */
  public SequencedMap<String, NbtTag> entries() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the value stored under a key, whatever its type.
   *
   * @param key the key
   * @return the value, or empty when the key is absent
   */
  public Optional<NbtTag> get(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a byte (R38).
   *
   * @param key the key
   * @return the numeric value converted to byte, or 0
   */
  public byte getByte(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a short (R38).
   *
   * @param key the key
   * @return the numeric value converted to short, or 0
   */
  public short getShort(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads an int (R38).
   *
   * @param key the key
   * @return the numeric value converted to int, or 0
   */
  public int getInt(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a long (R38).
   *
   * @param key the key
   * @return the numeric value converted to long, or 0
   */
  public long getLong(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a float (R38).
   *
   * @param key the key
   * @return the numeric value converted to float, or 0
   */
  public float getFloat(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a double (R38).
   *
   * @param key the key
   * @return the numeric value converted to double, or 0
   */
  public double getDouble(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a boolean stored as a Byte (R36, R38): {@code getByte(key) != 0}, so any non-zero byte is
   * true (provisional).
   *
   * @param key the key
   * @return the boolean, or {@code false}
   */
  public boolean getBoolean(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a String value; no other type is converted to a string (R38).
   *
   * @param key the key
   * @return the text, or the empty string
   */
  public String getString(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a Byte_Array value; no other type is converted (R38).
   *
   * @param key the key
   * @return a copy of the elements, or an empty array
   */
  public byte[] getByteArray(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads an Int_Array value; no other type is converted (R38).
   *
   * @param key the key
   * @return a copy of the elements, or an empty array
   */
  public int[] getIntArray(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a Long_Array value; no other type is converted (R38).
   *
   * @param key the key
   * @return a copy of the elements, or an empty array
   */
  public long[] getLongArray(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a List value of any element type (R38).
   *
   * @param key the key
   * @return the list, or {@link ListTag#empty()}
   */
  public ListTag getList(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a Compound value (R38).
   *
   * @param key the key
   * @return the compound, or {@link #empty()}
   */
  public CompoundTag getCompound(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@link TagType#COMPOUND} (R1).
   *
   * @return the type
   */
  @Override
  public TagType type() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Compares keys and values, ignoring key order (R28, R37).
   *
   * @param other any object
   * @return whether {@code other} is a compound with the same keys mapped to equal values
   */
  @Override
  public boolean equals(Object other) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a hash consistent with {@link #equals(Object)}, independent of key order (R28, R37).
   *
   * @return the hash of the entries
   */
  @Override
  public int hashCode() {
    return Skeleton.throwNotImplemented();
  }
}

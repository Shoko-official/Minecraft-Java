package dev.shoko.minecraft.core.nbt;

/**
 * Byte value: signed 8-bit integer, 1 byte in binary (R1, R3, R4). There is no boolean type: a
 * boolean is stored as a Byte 0 or 1 (R36).
 *
 * @param value the value, -128 to 127
 */
public record ByteTag(byte value) implements NumericTag {

  /**
   * Returns {@link TagType#BYTE} (R1).
   *
   * @return the type
   */
  @Override
  public TagType type() {
    return Skeleton.throwNotImplemented();
  }

  @Override
  public byte byteValue() {
    return Skeleton.throwNotImplemented();
  }

  @Override
  public short shortValue() {
    return Skeleton.throwNotImplemented();
  }

  @Override
  public int intValue() {
    return Skeleton.throwNotImplemented();
  }

  @Override
  public long longValue() {
    return Skeleton.throwNotImplemented();
  }

  @Override
  public float floatValue() {
    return Skeleton.throwNotImplemented();
  }

  @Override
  public double doubleValue() {
    return Skeleton.throwNotImplemented();
  }
}

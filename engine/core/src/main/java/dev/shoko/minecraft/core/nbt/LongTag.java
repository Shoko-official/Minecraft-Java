package dev.shoko.minecraft.core.nbt;

/**
 * Long value: signed 64-bit integer, 8 bytes big-endian in binary (R1, R3, R4).
 *
 * @param value the value
 */
public record LongTag(long value) implements NumericTag {

  /**
   * Returns {@link TagType#LONG} (R1).
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

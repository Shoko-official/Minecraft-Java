package dev.shoko.minecraft.core.nbt;

/**
 * Int value: signed 32-bit integer, 4 bytes big-endian in binary (R1, R3, R4).
 *
 * @param value the value
 */
public record IntTag(int value) implements NumericTag {

  /**
   * Returns {@link TagType#INT} (R1).
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

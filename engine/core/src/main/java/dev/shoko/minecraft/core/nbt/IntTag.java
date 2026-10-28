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
    return TagType.INT;
  }

  @Override
  public byte byteValue() {
    return (byte) value;
  }

  @Override
  public short shortValue() {
    return (short) value;
  }

  @Override
  public int intValue() {
    return value;
  }

  @Override
  public long longValue() {
    return value;
  }

  @Override
  public float floatValue() {
    return value;
  }

  @Override
  public double doubleValue() {
    return value;
  }
}

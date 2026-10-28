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
    return TagType.LONG;
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
    return (int) value;
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

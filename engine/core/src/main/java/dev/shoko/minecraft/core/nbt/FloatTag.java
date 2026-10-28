package dev.shoko.minecraft.core.nbt;

/**
 * Float value kept as its exact IEEE 754 binary32 bit pattern (R1, R3, R4, R35). The canonical
 * constructor keeps any pattern, including -0.0, infinities and NaN payloads read from a file; the
 * generated {@code equals} therefore compares bit patterns, as R37 requires.
 *
 * @param bits the raw bit pattern, as given by {@link Float#floatToRawIntBits(float)}
 */
public record FloatTag(int bits) implements NumericTag {

  /**
   * Returns the tag of a computed value (R35, provisional): -0.0 and infinities keep their bit
   * pattern, every NaN becomes the canonical pattern {@code 7fc00000}.
   *
   * @param value the value
   * @return the tag
   */
  public static FloatTag of(float value) {
    return new FloatTag(Float.floatToIntBits(value));
  }

  /**
   * Returns {@link TagType#FLOAT} (R1).
   *
   * @return the type
   */
  @Override
  public TagType type() {
    return TagType.FLOAT;
  }

  @Override
  public byte byteValue() {
    return (byte) floatValue();
  }

  @Override
  public short shortValue() {
    return (short) floatValue();
  }

  @Override
  public int intValue() {
    return (int) floatValue();
  }

  @Override
  public long longValue() {
    return (long) floatValue();
  }

  @Override
  public float floatValue() {
    return Float.intBitsToFloat(bits);
  }

  @Override
  public double doubleValue() {
    return floatValue();
  }
}

package dev.shoko.minecraft.core.nbt;

/**
 * Double value kept as its exact IEEE 754 binary64 bit pattern (R1, R3, R4, R35). The canonical
 * constructor keeps any pattern, including -0.0, infinities and NaN payloads read from a file; the
 * generated {@code equals} therefore compares bit patterns, as R37 requires.
 *
 * @param bits the raw bit pattern, as given by {@link Double#doubleToRawLongBits(double)}
 */
public record DoubleTag(long bits) implements NumericTag {

  /**
   * Returns the tag of a computed value (R35, provisional): -0.0 and infinities keep their bit
   * pattern, every NaN becomes the canonical pattern {@code 7ff8000000000000}.
   *
   * @param value the value
   * @return the tag
   */
  public static DoubleTag of(double value) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@link TagType#DOUBLE} (R1).
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

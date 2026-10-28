package dev.shoko.minecraft.core.nbt;

/**
 * A numeric value readable as any numeric type (R38). The conversions are the primitive conversions
 * of the Java language, provisional until probe PS2:
 *
 * <ul>
 *   <li>integer to a wider integer: exact;
 *   <li>integer to a narrower integer: low-order bits kept ({@code Long 3000000000} read as int
 *       gives {@code -1294967296});
 *   <li>float or double to int or long: truncation toward zero, NaN gives 0, saturation at the
 *       bounds;
 *   <li>float or double to byte or short: first to int as above, then low-order bits;
 *   <li>integer to float or double: nearest value; double to float: IEEE round to nearest.
 * </ul>
 */
public sealed interface NumericTag extends NbtTag
    permits ByteTag, ShortTag, IntTag, LongTag, FloatTag, DoubleTag {

  /**
   * Returns this value converted to a byte (R38).
   *
   * @return the converted value
   */
  byte byteValue();

  /**
   * Returns this value converted to a short (R38).
   *
   * @return the converted value
   */
  short shortValue();

  /**
   * Returns this value converted to an int (R38).
   *
   * @return the converted value
   */
  int intValue();

  /**
   * Returns this value converted to a long (R38).
   *
   * @return the converted value
   */
  long longValue();

  /**
   * Returns this value converted to a float (R38).
   *
   * @return the converted value
   */
  float floatValue();

  /**
   * Returns this value converted to a double (R38).
   *
   * @return the converted value
   */
  double doubleValue();
}

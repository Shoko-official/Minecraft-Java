package dev.shoko.minecraft.tools.oracle.nbt;

/**
 * Decodes the "modified UTF-8" of Java SE ({@code java.io.DataInput}) as core-nbt R11 and R12
 * require: one, two or three byte groups only. A raw 00 byte, a lead byte of a 4-byte sequence, a
 * stray continuation byte or a truncated group is refused; surrogates, paired or not, are kept as
 * they are, and so are overlong forms such as {@code c0 80} for U+0000.
 */
final class ModifiedUtf8 {

  private ModifiedUtf8() {}

  /**
   * Decodes a whole string.
   *
   * @throws IllegalArgumentException naming the offending byte and its offset in the string
   */
  static String decode(byte[] bytes) {
    StringBuilder text = new StringBuilder(bytes.length);
    int i = 0;
    while (i < bytes.length) {
      int lead = bytes[i] & 0xff;
      if (lead == 0) {
        throw invalid(bytes, i, "raw 00 byte");
      }
      if (lead < 0x80) {
        text.append((char) lead);
        i++;
      } else if ((lead & 0xe0) == 0xc0) {
        text.append((char) (((lead & 0x1f) << 6) | continuation(bytes, i, 1)));
        i += 2;
      } else if ((lead & 0xf0) == 0xe0) {
        int middle = continuation(bytes, i, 1);
        int last = continuation(bytes, i, 2);
        text.append((char) (((lead & 0x0f) << 12) | (middle << 6) | last));
        i += 3;
      } else {
        throw invalid(bytes, i, "byte not allowed as the start of a group");
      }
    }
    return text.toString();
  }

  private static int continuation(byte[] bytes, int start, int offset) {
    int index = start + offset;
    if (index >= bytes.length) {
      throw invalid(bytes, start, "truncated group");
    }
    int value = bytes[index] & 0xff;
    if ((value & 0xc0) != 0x80) {
      throw invalid(bytes, index, "expected a continuation byte");
    }
    return value & 0x3f;
  }

  private static IllegalArgumentException invalid(byte[] bytes, int index, String problem) {
    return new IllegalArgumentException(
        String.format("%s, byte 0x%02x at offset %d", problem, bytes[index] & 0xff, index));
  }
}

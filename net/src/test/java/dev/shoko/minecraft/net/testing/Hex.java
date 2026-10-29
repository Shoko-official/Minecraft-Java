package dev.shoko.minecraft.net.testing;

import java.util.Arrays;
import java.util.HexFormat;

/** Byte helpers for test vectors written as readable hexadecimal strings ("01 00 01 62 ff"). */
public final class Hex {

  private static final HexFormat SPACED = HexFormat.ofDelimiter(" ");

  private Hex() {}

  /** Parses hexadecimal byte pairs, with or without separating spaces. */
  public static byte[] bytes(String hex) {
    return HexFormat.of().parseHex(hex.replace(" ", ""));
  }

  /** Formats bytes as lower-case pairs separated by one space, for readable failures. */
  public static String format(byte[] data) {
    return SPACED.formatHex(data);
  }

  /** Concatenates byte arrays. */
  public static byte[] concat(byte[]... parts) {
    int length = 0;
    for (byte[] part : parts) {
      length += part.length;
    }
    byte[] out = new byte[length];
    int position = 0;
    for (byte[] part : parts) {
      System.arraycopy(part, 0, out, position, part.length);
      position += part.length;
    }
    return out;
  }

  /** Returns {@code count} copies of one byte value. */
  public static byte[] filled(int value, int count) {
    byte[] out = new byte[count];
    Arrays.fill(out, (byte) value);
    return out;
  }

  /** Returns {@code count} bytes {@code 0, 1, 2, ...} wrapping at 256. */
  public static byte[] ramp(int count) {
    byte[] out = new byte[count];
    for (int i = 0; i < count; i++) {
      out[i] = (byte) i;
    }
    return out;
  }
}

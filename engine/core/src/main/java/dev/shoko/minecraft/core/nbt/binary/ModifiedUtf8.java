package dev.shoko.minecraft.core.nbt.binary;

import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;

/**
 * The "modified UTF-8" of Java SE {@code java.io.DataInput}, as NBT strings use it (R11). Works on
 * UTF-16 code units: a supplementary character is two 3-byte surrogates, an unpaired surrogate is
 * one (R12), U+0000 is {@code c0 80}.
 */
final class ModifiedUtf8 {

  /** Largest encoded length: the length prefix is an unsigned 16-bit integer (R10). */
  static final int MAX_ENCODED_LENGTH = 0xFFFF;

  private ModifiedUtf8() {}

  /**
   * Decodes string bytes; overlong 2-byte forms other than {@code c0 80} are accepted like it.
   *
   * @param raw the encoded bytes, without the length prefix
   * @param offset the position of {@code raw[0]} in the input, for errors
   * @throws NbtException INVALID_STRING at the first byte of a raw {@code 00}, a 4-byte form, a
   *     stray continuation byte or a truncated sequence (R12)
   */
  static String decode(byte[] raw, long offset) throws NbtException {
    char[] chars = new char[raw.length];
    int count = 0;
    int i = 0;
    while (i < raw.length) {
      int lead = raw[i] & 0xFF;
      if (lead != 0 && lead < 0x80) {
        chars[count++] = (char) lead;
        i++;
      } else if ((lead & 0xE0) == 0xC0 && continuation(raw, i + 1)) {
        chars[count++] = (char) (((lead & 0x1F) << 6) | (raw[i + 1] & 0x3F));
        i += 2;
      } else if ((lead & 0xF0) == 0xE0 && continuation(raw, i + 1) && continuation(raw, i + 2)) {
        chars[count++] =
            (char) (((lead & 0x0F) << 12) | ((raw[i + 1] & 0x3F) << 6) | (raw[i + 2] & 0x3F));
        i += 3;
      } else {
        throw new NbtException(
            Reason.INVALID_STRING,
            offset + i,
            String.format("byte %02x does not start a modified UTF-8 sequence", lead));
      }
    }
    return new String(chars, 0, count);
  }

  /**
   * Encodes a string.
   *
   * @throws NbtException STRING_TOO_LONG when the encoding exceeds 65535 bytes (R13)
   */
  static byte[] encode(String text) throws NbtException {
    long length = 0;
    for (int k = 0; k < text.length(); k++) {
      length += encodedLength(text.charAt(k));
    }
    if (length > MAX_ENCODED_LENGTH) {
      throw new NbtException(
          Reason.STRING_TOO_LONG, "string encodes to " + length + " bytes, more than 65535");
    }
    byte[] out = new byte[(int) length];
    int p = 0;
    for (int k = 0; k < text.length(); k++) {
      char c = text.charAt(k);
      if (c != 0 && c < 0x80) {
        out[p++] = (byte) c;
      } else if (c < 0x800) {
        out[p++] = (byte) (0xC0 | (c >> 6));
        out[p++] = (byte) (0x80 | (c & 0x3F));
      } else {
        out[p++] = (byte) (0xE0 | (c >> 12));
        out[p++] = (byte) (0x80 | ((c >> 6) & 0x3F));
        out[p++] = (byte) (0x80 | (c & 0x3F));
      }
    }
    return out;
  }

  private static int encodedLength(char c) {
    if (c != 0 && c < 0x80) {
      return 1;
    }
    return c < 0x800 ? 2 : 3;
  }

  private static boolean continuation(byte[] raw, int index) {
    return index < raw.length && (raw[index] & 0xC0) == 0x80;
  }
}

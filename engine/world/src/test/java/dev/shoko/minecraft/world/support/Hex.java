package dev.shoko.minecraft.world.support;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Byte helpers for test vectors written as hexadecimal strings. */
public final class Hex {

  private static final HexFormat SPACED = HexFormat.ofDelimiter(" ");

  private Hex() {}

  /** Parses hexadecimal byte pairs, with or without separating spaces. */
  public static byte[] bytes(String hex) {
    return HexFormat.of().parseHex(hex.replace(" ", ""));
  }

  /** Formats bytes as lower-case pairs separated by one space. */
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

  /** Returns the SHA-256 digest in lower-case hexadecimal. */
  public static String sha256(byte[] data) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required on every Java platform", e);
    }
  }
}

package dev.shoko.minecraft.core.nbt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Byte helpers for test vectors written as readable hexadecimal strings ("01 00 01 62 ff"). */
public final class Hex {

  private static final HexFormat SPACED = HexFormat.ofDelimiter(" ");

  private Hex() {}

  /** Parses hexadecimal byte pairs, with or without separating spaces. */
  public static byte[] bytes(String hex) {
    return HexFormat.of().parseHex(hex.replace(" ", ""));
  }

  /** Formats bytes as lower-case pairs separated by one space, for readable assertion failures. */
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

  /** Returns {@code times} copies of {@code part}, one after the other. */
  public static byte[] repeat(byte[] part, int times) {
    byte[] out = new byte[part.length * times];
    for (int k = 0; k < times; k++) {
      System.arraycopy(part, 0, out, k * part.length, part.length);
    }
    return out;
  }

  /** Returns the SHA-256 digest of the bytes, in lower-case hexadecimal without separators. */
  public static String sha256(byte[] data) {
    return sha256(new ByteArrayInputStream(data));
  }

  /** Returns the SHA-256 digest of everything the stream yields, in lower-case hexadecimal. */
  public static String sha256(InputStream in) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] buffer = new byte[1 << 16];
      int read;
      while ((read = in.read(buffer)) != -1) {
        digest.update(buffer, 0, read);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required on every Java platform", e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

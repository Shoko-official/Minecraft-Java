package dev.shoko.minecraft.net.testing;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * A reference model of the wire format, written from the rules of the spec (the sub-commands of the
 * reference script of the vectors file) and independent of the module under test.
 */
public final class Wire {

  private Wire() {}

  /** VarInt of the rule R6. */
  public static byte[] varInt(int value) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int v = value;
    while ((v & ~0x7f) != 0) {
      out.write((v & 0x7f) | 0x80);
      v >>>= 7;
    }
    out.write(v);
    return out.toByteArray();
  }

  /** VarLong of the rule R7. */
  public static byte[] varLong(long value) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    long v = value;
    while ((v & ~0x7fL) != 0) {
      out.write((int) ((v & 0x7f) | 0x80));
      v >>>= 7;
    }
    out.write((int) v);
    return out.toByteArray();
  }

  /** String of the rule R9: byte length then UTF-8. */
  public static byte[] string(String value) {
    byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
    return Hex.concat(varInt(utf8.length), utf8);
  }

  /** Big-endian long. */
  public static byte[] longBytes(long value) {
    return ByteBuffer.allocate(8).putLong(value).array();
  }

  /** Big-endian int. */
  public static byte[] intBytes(int value) {
    return ByteBuffer.allocate(4).putInt(value).array();
  }

  /** UUID of the rule R11. */
  public static byte[] uuid(UUID value) {
    return ByteBuffer.allocate(16)
        .putLong(value.getMostSignificantBits())
        .putLong(value.getLeastSignificantBits())
        .array();
  }

  /** Prefixed byte array of the rule R14. */
  public static byte[] prefixedBytes(byte[] value) {
    return Hex.concat(varInt(value.length), value);
  }

  /** Frame without compression (rule R27): length, id, payload. */
  public static byte[] frame(int id, byte[] payload) {
    byte[] body = Hex.concat(varInt(id), payload);
    return Hex.concat(varInt(body.length), body);
  }

  /** Frame with compression (rule R29) and a threshold of 0 or more, zlib at the default level. */
  public static byte[] compressedFrame(int id, byte[] payload, int threshold) {
    byte[] body = Hex.concat(varInt(id), payload);
    byte[] inner =
        body.length >= threshold
            ? Hex.concat(varInt(body.length), zlib(body))
            : Hex.concat(varInt(0), body);
    return Hex.concat(varInt(inner.length), inner);
  }

  /** Zlib stream (RFC 1950) of the bytes. */
  public static byte[] zlib(byte[] data) {
    Deflater deflater = new Deflater();
    try {
      deflater.setInput(data);
      deflater.finish();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] chunk = new byte[4096];
      while (!deflater.finished()) {
        out.write(chunk, 0, deflater.deflate(chunk));
      }
      return out.toByteArray();
    } finally {
      deflater.end();
    }
  }

  /** Inflates a zlib stream that must hold exactly {@code expectedSize} bytes. */
  public static byte[] inflate(byte[] stream, int offset, int expectedSize) {
    Inflater inflater = new Inflater();
    try {
      inflater.setInput(stream, offset, stream.length - offset);
      byte[] out = new byte[expectedSize];
      int total = 0;
      while (total < expectedSize && !inflater.finished()) {
        int read = inflater.inflate(out, total, expectedSize - total);
        if (read == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
          break;
        }
        total += read;
      }
      if (total != expectedSize || !inflater.finished()) {
        throw new IllegalStateException("stream does not hold exactly " + expectedSize + " bytes");
      }
      return out;
    } catch (DataFormatException e) {
      throw new IllegalStateException(e);
    } finally {
      inflater.end();
    }
  }
}

package dev.shoko.minecraft.tools.oracle.nbt;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Decompresses one gzip member (RFC 1952) or one zlib stream (RFC 1950) completely, so that the
 * trailer is always checked: CRC-32 and ISIZE for gzip, Adler-32 for zlib (checked by the inflater
 * itself). A missing or wrong trailer fails; bytes after the member or stream are ignored.
 */
final class Containers {

  private static final int GZIP_ID1 = 0x1f;
  private static final int GZIP_ID2 = 0x8b;
  private static final int GZIP_DEFLATE = 8;
  private static final int FHCRC = 0x02;
  private static final int FEXTRA = 0x04;
  private static final int FNAME = 0x08;
  private static final int FCOMMENT = 0x10;
  private static final int FLAG_RESERVED = 0xe0;
  private static final int GZIP_FIXED_HEADER_REST = 6;
  private static final int GZIP_TRAILER = 8;
  private static final int BUFFER = 8192;

  private Containers() {}

  static byte[] gunzip(InputStream in, long maxBytes) throws IOException {
    if (in.read() != GZIP_ID1 || in.read() != GZIP_ID2) {
      throw new IOException("gzip: bad magic number");
    }
    if (in.read() != GZIP_DEFLATE) {
      throw new IOException("gzip: compression method is not deflate");
    }
    int flags = requireByte(in, "header");
    if ((flags & FLAG_RESERVED) != 0) {
      throw new IOException("gzip: reserved header flags set");
    }
    readExactly(in, GZIP_FIXED_HEADER_REST, "header");
    if ((flags & FEXTRA) != 0) {
      int low = requireByte(in, "extra field");
      int length = low | (requireByte(in, "extra field") << 8);
      readExactly(in, length, "extra field");
    }
    if ((flags & FNAME) != 0) {
      skipZeroTerminated(in, "file name");
    }
    if ((flags & FCOMMENT) != 0) {
      skipZeroTerminated(in, "comment");
    }
    if ((flags & FHCRC) != 0) {
      readExactly(in, 2, "header CRC");
    }
    Inflater inflater = new Inflater(true);
    try {
      Inflated inflated = inflate(inflater, in, maxBytes, "gzip");
      byte[] trailer = new byte[GZIP_TRAILER];
      int fromBuffer = Math.min(inflated.leftover().length, GZIP_TRAILER);
      System.arraycopy(inflated.leftover(), 0, trailer, 0, fromBuffer);
      int rest = in.readNBytes(trailer, fromBuffer, GZIP_TRAILER - fromBuffer);
      if (fromBuffer + rest < GZIP_TRAILER) {
        throw new IOException("gzip: trailer missing or truncated");
      }
      CRC32 crc = new CRC32();
      crc.update(inflated.data());
      if (littleEndian(trailer, 0) != crc.getValue()) {
        throw new IOException("gzip: CRC-32 mismatch in trailer");
      }
      if (littleEndian(trailer, 4) != (inflated.data().length & 0xffffffffL)) {
        throw new IOException("gzip: ISIZE mismatch in trailer");
      }
      return inflated.data();
    } finally {
      inflater.end();
    }
  }

  static byte[] inflateZlib(InputStream in, long maxBytes) throws IOException {
    Inflater inflater = new Inflater(false);
    try {
      return inflate(inflater, in, maxBytes, "zlib").data();
    } finally {
      inflater.end();
    }
  }

  /** Decompressed bytes and the compressed bytes read past the end of the deflate stream. */
  private static final class Inflated {
    private final byte[] data;
    private final byte[] leftover;

    Inflated(byte[] data, byte[] leftover) {
      this.data = data;
      this.leftover = leftover;
    }

    byte[] data() {
      return data;
    }

    byte[] leftover() {
      return leftover;
    }
  }

  private static Inflated inflate(Inflater inflater, InputStream in, long maxBytes, String format)
      throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] input = new byte[BUFFER];
    byte[] output = new byte[BUFFER];
    int inputLength = 0;
    try {
      while (!inflater.finished()) {
        if (inflater.needsInput()) {
          inputLength = in.read(input);
          if (inputLength < 0) {
            throw new IOException(format + ": stream truncated before its end and trailer");
          }
          inflater.setInput(input, 0, inputLength);
        }
        if (inflater.needsDictionary()) {
          throw new IOException(format + ": preset dictionary is not supported");
        }
        int produced = inflater.inflate(output);
        if (out.size() + (long) produced > maxBytes) {
          throw new IOException(format + ": decompressed data exceeds " + maxBytes + " bytes");
        }
        out.write(output, 0, produced);
      }
    } catch (DataFormatException e) {
      throw new IOException(format + ": " + e.getMessage(), e);
    }
    int remaining = inflater.getRemaining();
    byte[] leftover = new byte[remaining];
    System.arraycopy(input, inputLength - remaining, leftover, 0, remaining);
    return new Inflated(out.toByteArray(), leftover);
  }

  private static int requireByte(InputStream in, String what) throws IOException {
    int value = in.read();
    if (value < 0) {
      throw new EOFException("gzip: " + what + " truncated");
    }
    return value;
  }

  private static void readExactly(InputStream in, int count, String what) throws IOException {
    if (in.readNBytes(count).length != count) {
      throw new EOFException("gzip: " + what + " truncated");
    }
  }

  private static void skipZeroTerminated(InputStream in, String what) throws IOException {
    int value;
    do {
      value = requireByte(in, what);
    } while (value != 0);
  }

  private static long littleEndian(byte[] bytes, int offset) {
    return (bytes[offset] & 0xffL)
        | ((bytes[offset + 1] & 0xffL) << 8)
        | ((bytes[offset + 2] & 0xffL) << 16)
        | ((bytes[offset + 3] & 0xffL) << 24);
  }
}

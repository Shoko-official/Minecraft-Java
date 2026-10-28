package dev.shoko.minecraft.core.nbt.binary;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

/**
 * Decompresses one gzip member (RFC 1952) or one zlib stream (RFC 1950) and checks its trailer when
 * the compressed data ends (R61): CRC-32 and ISIZE for gzip, Adler-32 for zlib (checked by the
 * inflater itself). A wrong, missing or truncated trailer and an unterminated DEFLATE stream fail
 * with an {@link IOException} that is not a failure of the source. Bytes after the member or stream
 * are not read; a second gzip member is not part of the data.
 */
final class InflatingInputStream extends InputStream {

  private static final int FHCRC = 0x02;
  private static final int FEXTRA = 0x04;
  private static final int FNAME = 0x08;
  private static final int FCOMMENT = 0x10;

  private final InputStream source;
  private final Inflater inflater;
  private final CRC32 crc;
  private final byte[] input = new byte[8192];
  private int inputLength;
  private boolean done;

  private InflatingInputStream(InputStream source, boolean gzip) {
    this.source = source;
    this.inflater = new Inflater(gzip);
    this.crc = gzip ? new CRC32() : null;
  }

  /** Reads the gzip header from {@code source} (R62), then returns the decompressed member. */
  static InflatingInputStream gzip(InputStream source) throws IOException {
    readGzipHeader(source);
    return new InflatingInputStream(source, true);
  }

  /** Returns the decompressed zlib stream of {@code source} (R18). */
  static InflatingInputStream zlib(InputStream source) {
    return new InflatingInputStream(source, false);
  }

  @Override
  public int read() throws IOException {
    byte[] one = new byte[1];
    return read(one, 0, 1) < 0 ? -1 : one[0] & 0xFF;
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    if (length == 0) {
      return 0;
    }
    while (!done) {
      int count = inflate(buffer, offset, length);
      if (count > 0) {
        if (crc != null) {
          crc.update(buffer, offset, count);
        }
        return count;
      }
      if (inflater.finished()) {
        if (crc != null) {
          checkGzipTrailer();
        }
        done = true;
      } else if (inflater.needsDictionary()) {
        throw new ZipException("zlib preset dictionary is not supported");
      } else if (inflater.needsInput()) {
        inputLength = source.read(input);
        if (inputLength < 0) {
          throw new EOFException("container ends before the end of the compressed data");
        }
        inflater.setInput(input, 0, inputLength);
      }
    }
    return -1;
  }

  @Override
  public void close() {
    inflater.end();
  }

  private int inflate(byte[] buffer, int offset, int length) throws ZipException {
    try {
      return inflater.inflate(buffer, offset, length);
    } catch (DataFormatException e) {
      var failure = new ZipException("invalid compressed data: " + e.getMessage());
      failure.initCause(e);
      throw failure;
    }
  }

  /** CRC-32 then ISIZE, little-endian, right after the DEFLATE data (RFC 1952). */
  private void checkGzipTrailer() throws IOException {
    int remaining = inflater.getRemaining();
    int next = inputLength - remaining;
    long[] fields = new long[2];
    for (int k = 0; k < 8; k++) {
      int value = remaining > 0 ? input[next++] & 0xFF : source.read();
      remaining--;
      if (value < 0) {
        throw new EOFException("gzip trailer missing or truncated");
      }
      fields[k / 4] |= (long) value << (8 * (k % 4));
    }
    if (fields[0] != crc.getValue()) {
      throw new ZipException("gzip CRC-32 mismatch");
    }
    // ISIZE is the decompressed length modulo 2^32.
    if (fields[1] != (inflater.getBytesWritten() & 0xFFFFFFFFL)) {
      throw new ZipException("gzip ISIZE mismatch");
    }
  }

  /** Magic, method 8, flags; optional fields skipped, header CRC not checked (R62). */
  private static void readGzipHeader(InputStream source) throws IOException {
    if (readByte(source) != 0x1f || readByte(source) != 0x8b) {
      throw new ZipException("not in gzip format");
    }
    if (readByte(source) != 8) {
      throw new ZipException("unsupported gzip compression method");
    }
    int flags = readByte(source);
    // Reserved flag bits are accepted and ignored (R62, provisional).
    skip(source, 6);
    if ((flags & FEXTRA) != 0) {
      skip(source, readByte(source) | (readByte(source) << 8));
    }
    if ((flags & FNAME) != 0) {
      skipZeroTerminated(source);
    }
    if ((flags & FCOMMENT) != 0) {
      skipZeroTerminated(source);
    }
    if ((flags & FHCRC) != 0) {
      skip(source, 2);
    }
  }

  private static int readByte(InputStream source) throws IOException {
    int value = source.read();
    if (value < 0) {
      throw new EOFException("gzip header truncated");
    }
    return value;
  }

  private static void skip(InputStream source, int count) throws IOException {
    for (int k = 0; k < count; k++) {
      readByte(source);
    }
  }

  private static void skipZeroTerminated(InputStream source) throws IOException {
    int value;
    do {
      value = readByte(source);
    } while (value != 0);
  }
}

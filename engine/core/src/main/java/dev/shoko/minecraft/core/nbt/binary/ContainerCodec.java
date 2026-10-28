package dev.shoko.minecraft.core.nbt.binary;

import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Opens and produces the gzip (RFC 1952) and zlib (RFC 1950) containers (R16 to R19), and tells a
 * failure of the caller's stream from an undecodable container.
 */
final class ContainerCodec {

  /** Gzip header of the reference files: no flags, time 0, XFL 0, OS 255 (R16). */
  private static final byte[] GZIP_HEADER = {0x1f, (byte) 0x8b, 8, 0, 0, 0, 0, 0, 0, (byte) 0xff};

  private static final int CHUNK = 8192;

  private ContainerCodec() {}

  /**
   * Returns a stream of the decompressed bytes. Closing it releases the inflater and leaves {@code
   * source} open.
   *
   * @throws IOException when the gzip header is invalid or {@code source} fails
   */
  static InputStream open(InputStream source, NbtCompression compression) throws IOException {
    InputStream guarded = new GuardedSource(source);
    return switch (compression) {
      case NONE -> guarded;
      case GZIP -> new GZIPInputStream(guarded, CHUNK);
      case ZLIB -> new InflaterInputStream(guarded);
    };
  }

  /** Returns the container bytes holding {@code raw}. */
  static byte[] pack(byte[] raw, NbtCompression compression) {
    return switch (compression) {
      case NONE -> raw;
      case ZLIB -> deflate(raw, false);
      case GZIP -> gzip(raw);
    };
  }

  /**
   * Classifies a read failure: IO_FAILURE when the caller's stream failed, CORRUPT_CONTAINER when
   * the gzip or zlib data could not be decoded.
   */
  static NbtException readFailure(IOException e) {
    if (e instanceof SourceFailure failure) {
      return new NbtException(Reason.IO_FAILURE, "input stream failed", failure.getCause());
    }
    return new NbtException(Reason.CORRUPT_CONTAINER, "container cannot be decoded", e);
  }

  private static byte[] gzip(byte[] raw) {
    var crc = new CRC32();
    crc.update(raw);
    byte[] body = deflate(raw, true);
    var out = new ByteArrayOutputStream(GZIP_HEADER.length + body.length + 8);
    out.writeBytes(GZIP_HEADER);
    out.writeBytes(body);
    writeIntLittleEndian(out, (int) crc.getValue());
    // ISIZE is the input length modulo 2^32 (RFC 1952).
    writeIntLittleEndian(out, raw.length);
    return out.toByteArray();
  }

  private static byte[] deflate(byte[] raw, boolean nowrap) {
    var deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, nowrap);
    try {
      deflater.setInput(raw);
      deflater.finish();
      var out = new ByteArrayOutputStream();
      byte[] chunk = new byte[CHUNK];
      while (!deflater.finished()) {
        out.write(chunk, 0, deflater.deflate(chunk));
      }
      return out.toByteArray();
    } finally {
      deflater.end();
    }
  }

  private static void writeIntLittleEndian(ByteArrayOutputStream out, int value) {
    out.write(value);
    out.write(value >>> 8);
    out.write(value >>> 16);
    out.write(value >>> 24);
  }

  /** Marks the failures of the caller's stream, so that they are not taken for corrupt data. */
  private static final class SourceFailure extends IOException {

    @Serial private static final long serialVersionUID = 1L;

    SourceFailure(IOException cause) {
      super(cause);
    }
  }

  /** The caller's stream: failures are marked, closing is ignored (the caller owns it). */
  private static final class GuardedSource extends FilterInputStream {

    GuardedSource(InputStream source) {
      super(source);
    }

    @Override
    public int read() throws IOException {
      try {
        return in.read();
      } catch (IOException e) {
        throw new SourceFailure(e);
      }
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      try {
        return in.read(buffer, offset, length);
      } catch (IOException e) {
        throw new SourceFailure(e);
      }
    }

    @Override
    public long skip(long count) throws IOException {
      try {
        return in.skip(count);
      } catch (IOException e) {
        throw new SourceFailure(e);
      }
    }

    @Override
    public int available() throws IOException {
      try {
        return in.available();
      } catch (IOException e) {
        throw new SourceFailure(e);
      }
    }

    @Override
    public void close() {
      // The caller owns the stream: reading must not close it.
    }
  }
}

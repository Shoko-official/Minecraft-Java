package dev.shoko.minecraft.core.nbt.binary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Independent gzip and zlib coding with {@code java.util.zip}, to build container inputs and to
 * check container outputs without relying on the code under test. Also stream doubles.
 */
final class Containers {

  private Containers() {}

  static byte[] gzip(byte[] data) {
    var out = new ByteArrayOutputStream();
    try (var gzip = new GZIPOutputStream(out)) {
      gzip.write(data);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return out.toByteArray();
  }

  static byte[] gunzip(byte[] data) {
    try (var in = new GZIPInputStream(new ByteArrayInputStream(data))) {
      return in.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static byte[] zlib(byte[] data) {
    var out = new ByteArrayOutputStream();
    try (var zlib = new DeflaterOutputStream(out)) {
      zlib.write(data);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return out.toByteArray();
  }

  static byte[] inflate(byte[] data) {
    try (var in = new InflaterInputStream(new ByteArrayInputStream(data))) {
      return in.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Gzip of everything the stream yields, compressed while streaming (for large inputs). */
  static byte[] gzip(InputStream data) {
    var out = new ByteArrayOutputStream();
    try (var gzip = new GZIPOutputStream(out)) {
      data.transferTo(gzip);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return out.toByteArray();
  }

  /**
   * Raw DEFLATE stream of stored blocks only, as {@code stored_deflate} of {@code nbt_ref.py}:
   * deterministic, no compressor involved.
   */
  static byte[] storedDeflate(byte[] data) {
    var out = new ByteArrayOutputStream();
    int position = 0;
    do {
      int length = Math.min(65535, data.length - position);
      boolean last = position + length == data.length;
      out.write(last ? 1 : 0);
      out.write(length & 0xff);
      out.write(length >>> 8);
      out.write(~length & 0xff);
      out.write((~length >>> 8) & 0xff);
      out.write(data, position, length);
      position += length;
    } while (position < data.length);
    return out.toByteArray();
  }

  /** Gzip member with the given 10-byte header (and optional header fields), stored blocks. */
  static byte[] gzipStored(byte[] header, byte[] data) {
    var crc = new CRC32();
    crc.update(data);
    ByteBuffer trailer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    trailer.putInt((int) crc.getValue()).putInt(data.length);
    return concat(header, storedDeflate(data), trailer.array());
  }

  /** Gzip member with the header of R16 ({@code 1f 8b 08 00 00 00 00 00 00 ff}), stored blocks. */
  static byte[] gzipStored(byte[] data) {
    return gzipStored(new byte[] {0x1f, (byte) 0x8b, 8, 0, 0, 0, 0, 0, 0, (byte) 0xff}, data);
  }

  /** Zlib stream with header {@code 78 01}, stored blocks and the Adler-32 of the data. */
  static byte[] zlibStored(byte[] data) {
    var adler = new Adler32();
    adler.update(data);
    byte[] checksum = ByteBuffer.allocate(4).putInt((int) adler.getValue()).array();
    return concat(new byte[] {0x78, 0x01}, storedDeflate(data), checksum);
  }

  /** Complements the bytes from {@code from} (inclusive) to {@code to} (exclusive). */
  static byte[] invert(byte[] data, int from, int to) {
    byte[] out = data.clone();
    for (int i = from; i < to; i++) {
      out[i] = (byte) ~out[i];
    }
    return out;
  }

  private static byte[] concat(byte[]... parts) {
    var out = new ByteArrayOutputStream();
    for (byte[] part : parts) {
      out.write(part, 0, part.length);
    }
    return out.toByteArray();
  }

  /** An input stream that records whether it was closed. */
  static final class CloseTrackingInputStream extends FilterInputStream {

    private boolean closed;

    CloseTrackingInputStream(byte[] data) {
      super(new ByteArrayInputStream(data));
    }

    @Override
    public void close() throws IOException {
      closed = true;
      super.close();
    }

    boolean closed() {
      return closed;
    }
  }

  /** An output stream that records whether it was closed. */
  static final class CloseTrackingOutputStream extends FilterOutputStream {

    private boolean closed;

    CloseTrackingOutputStream(OutputStream target) {
      super(target);
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
      out.write(buffer, offset, length);
    }

    @Override
    public void close() throws IOException {
      closed = true;
      super.close();
    }

    boolean closed() {
      return closed;
    }
  }

  /** An input stream whose every read fails. */
  static final class FailingInputStream extends InputStream {

    @Override
    public int read() throws IOException {
      throw new IOException("read failure for the test");
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      throw new IOException("read failure for the test");
    }
  }

  /** An output stream whose every write fails. */
  static final class FailingOutputStream extends OutputStream {

    @Override
    public void write(int value) throws IOException {
      throw new IOException("write failure for the test");
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
      throw new IOException("write failure for the test");
    }
  }
}

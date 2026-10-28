package dev.shoko.minecraft.core.nbt.binary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
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

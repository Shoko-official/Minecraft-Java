package dev.shoko.minecraft.net.frame;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/** Inflates the zlib stream of a compressed frame into exactly the announced size (R29, R30). */
final class FrameInflater {

  /** First output allocation, so that a large announcement costs nothing until data is produced. */
  private static final int INITIAL_CAPACITY = 8192;

  private FrameInflater() {}

  /**
   * Inflates a zlib stream.
   *
   * @param data the array holding the stream
   * @param offset index of the first stream byte
   * @param length number of stream bytes
   * @param expectedSize the announced decompressed size, already checked against the limit
   * @return exactly {@code expectedSize} bytes
   * @throws ProtocolException with {@code DECOMPRESSED_SIZE_MISMATCH} when the stream yields a
   *     different number of bytes or ends early, or {@code CORRUPT_COMPRESSION} when it is not
   *     valid zlib or bytes follow its end
   */
  static byte[] inflate(byte[] data, int offset, int length, int expectedSize)
      throws ProtocolException {
    Inflater inflater = new Inflater();
    try {
      inflater.setInput(data, offset, length);
      byte[] out = inflateInto(inflater, expectedSize);
      if (inflater.needsDictionary()) {
        throw new ProtocolException(Reason.CORRUPT_COMPRESSION, "zlib stream needs a dictionary");
      }
      if (out == null || !inflater.finished()) {
        throw new ProtocolException(
            Reason.DECOMPRESSED_SIZE_MISMATCH,
            "announced " + expectedSize + " bytes, the stream does not yield exactly that");
      }
      if (inflater.getRemaining() != 0) {
        throw new ProtocolException(
            Reason.CORRUPT_COMPRESSION, "bytes after the end of the zlib stream");
      }
      return out;
    } catch (DataFormatException e) {
      throw new ProtocolException(Reason.CORRUPT_COMPRESSION, "invalid zlib stream", e);
    } finally {
      inflater.end();
    }
  }

  /**
   * Inflates until the stream ends or {@code expectedSize} bytes are out, growing the output with
   * the data actually produced. Returns null when the stream holds a different number of bytes.
   */
  private static byte[] inflateInto(Inflater inflater, int expectedSize)
      throws DataFormatException {
    byte[] out = new byte[Math.min(expectedSize, INITIAL_CAPACITY)];
    int total = 0;
    while (total < expectedSize && !inflater.finished()) {
      if (total == out.length) {
        out = Arrays.copyOf(out, (int) Math.min(expectedSize, 2L * out.length));
      }
      int produced = inflater.inflate(out, total, out.length - total);
      total += produced;
      if (produced == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
        return total == expectedSize ? out : null;
      }
    }
    if (total != expectedSize) {
      return null;
    }
    // The stream may still hold bytes or only its checksum: one probe byte tells which.
    byte[] probe = new byte[1];
    if (!inflater.finished() && inflater.inflate(probe) > 0) {
      return null;
    }
    return out;
  }
}

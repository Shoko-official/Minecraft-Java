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
   *     different number of bytes, or {@code CORRUPT_COMPRESSION} when it is not valid zlib
   */
  static byte[] inflate(byte[] data, int offset, int length, int expectedSize)
      throws ProtocolException {
    Inflater inflater = new Inflater();
    try {
      inflater.setInput(data, offset, length);
      // One byte of room beyond the announced size detects a stream that holds more.
      long limit = (long) expectedSize + 1;
      byte[] out = new byte[(int) Math.min(limit, INITIAL_CAPACITY)];
      int total = 0;
      while (!inflater.finished() && total < limit) {
        if (total == out.length) {
          out = Arrays.copyOf(out, (int) Math.min(limit, 2L * out.length));
        }
        int produced = inflater.inflate(out, total, out.length - total);
        total += produced;
        if (produced == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
          break;
        }
      }
      if (inflater.needsDictionary()) {
        throw new ProtocolException(Reason.CORRUPT_COMPRESSION, "zlib stream needs a dictionary");
      }
      if (total != expectedSize || !inflater.finished()) {
        throw new ProtocolException(
            Reason.DECOMPRESSED_SIZE_MISMATCH,
            "announced " + expectedSize + " bytes, the stream does not yield exactly that");
      }
      return out.length == expectedSize ? out : Arrays.copyOf(out, expectedSize);
    } catch (DataFormatException e) {
      throw new ProtocolException(Reason.CORRUPT_COMPRESSION, "invalid zlib stream", e);
    } finally {
      inflater.end();
    }
  }
}

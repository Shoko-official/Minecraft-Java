package dev.shoko.minecraft.net.frame;

/**
 * One direction of the connection cipher: a continuous byte stream transformed in place (R31). The
 * cipher keeps its state between calls, across packets and across connection states, and is never
 * restarted. Splitting a stream into chunks at any position gives the same bytes as one call over
 * the whole stream (vector CH02). Not thread-safe.
 */
public interface StreamCipher {

  /**
   * Transforms bytes in place, continuing the stream.
   *
   * @param data the array holding the bytes
   * @param offset index of the first byte to transform
   * @param length number of bytes to transform
   * @throws IndexOutOfBoundsException when the range is not inside {@code data}
   */
  void apply(byte[] data, int offset, int length);
}

package dev.shoko.minecraft.net.frame;

import dev.shoko.minecraft.net.ProtocolException;
import java.util.List;

/**
 * Turns the bytes received on a connection into frames: optional decryption, length, optional
 * decompression (R27 to R33). Incremental: bytes may arrive in any chunking, a frame may be split
 * over several calls and several frames may arrive in one call, with the same result (test plan
 * L1). Starts with compression and decryption off. Not thread-safe: one decoder per direction of
 * one connection.
 *
 * <p>Rules applied, with the typed failure of each:
 *
 * <ul>
 *   <li>The length is a VarInt of at most 3 bytes between 1 and the maximum frame length; a fourth
 *       byte, a length of 0 or a larger value is {@code FRAME_LENGTH_INVALID} (R27, vectors I01,
 *       I02), raised as soon as the length is known and before any body byte is waited for.
 *   <li>With compression, a data length of 0 means a plain body, accepted whatever its size (R30 a,
 *       vector I03); a data length from 1 to threshold - 1 is {@code DATA_LENGTH_BELOW_THRESHOLD}
 *       (vector I05); a data length above the decompressed limit is {@code DECOMPRESSED_TOO_LARGE},
 *       before any allocation (R30 d); otherwise the zlib stream must yield exactly that many
 *       bytes, else {@code DECOMPRESSED_SIZE_MISMATCH} (vector I04), and an invalid stream is
 *       {@code CORRUPT_COMPRESSION} with the zlib failure as cause.
 *   <li>A frame announced but not completed stays buffered, with no error and no frame (vector
 *       I11); the read timeout belongs to the connection.
 * </ul>
 *
 * After any exception the decoder is unusable and every later call throws {@link
 * IllegalStateException}.
 */
public final class FrameDecoder {

  private FrameDecoder() {}

  /**
   * Creates a decoder with the default limits ({@link FrameLimits#DEFAULT}).
   *
   * @return a new decoder
   */
  public static FrameDecoder create() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Creates a decoder with explicit limits.
   *
   * @param limits the size limits
   * @return a new decoder
   */
  public static FrameDecoder create(FrameLimits limits) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Sets the compression threshold for the frames decoded from now on (R28).
   *
   * @param threshold 0 or more to expect the compressed format; negative to switch it off
   */
  public void setCompressionThreshold(int threshold) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Switches decryption on for the bytes fed from now on (R31, R32).
   *
   * @param cipher the decryptor; kept and continued for the rest of the connection
   * @throws IllegalStateException when decryption is already on
   */
  public void enableDecryption(StreamCipher cipher) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Feeds received bytes and returns every frame they complete.
   *
   * @param data the array holding the bytes; not modified, not kept
   * @param offset index of the first byte
   * @param length number of bytes
   * @return the completed frames in order, possibly empty
   * @throws ProtocolException on a malformed frame, see the class description; frames completed
   *     before the fault in the same call are lost with it
   * @throws IndexOutOfBoundsException when the range is not inside {@code data}
   */
  public List<Frame> feed(byte[] data, int offset, int length) throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of bytes received but not yet part of a returned frame.
   *
   * @return the byte count, 0 or more
   */
  public int bufferedBytes() {
    return Skeleton.throwNotImplemented();
  }
}

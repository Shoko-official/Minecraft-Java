package dev.shoko.minecraft.net.frame;

import dev.shoko.minecraft.net.ProtocolException;

/**
 * Turns frames into the bytes of the connection for one direction: length, optional compression,
 * then optional encryption (R27 to R32). Starts with compression and encryption off. Not
 * thread-safe: one encoder per direction of one connection.
 *
 * <p>Without compression a frame is {@code VarInt length, VarInt id, payload} (R27). With a
 * threshold of 0 or more it is {@code VarInt length, VarInt data length, body} (R29): a body of
 * {@code id + payload} at or above the threshold is zlib-compressed and the data length is its
 * uncompressed size; a smaller body is sent as it is with a data length of 0. The zlib bytes
 * produced are not specified (vector F10 is an example); only the header bytes are. Encryption
 * covers every byte of the result (R31).
 */
public final class FrameEncoder {

  private FrameEncoder() {}

  /**
   * Creates an encoder with the default limits ({@link FrameLimits#DEFAULT}).
   *
   * @return a new encoder
   */
  public static FrameEncoder create() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Creates an encoder with explicit limits.
   *
   * @param limits the size limits
   * @return a new encoder
   */
  public static FrameEncoder create(FrameLimits limits) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Sets the compression threshold for the frames encoded from now on (R28).
   *
   * @param threshold 0 or more to compress bodies of at least that size, 0 meaning every body;
   *     negative to switch compression off (vector F14)
   */
  public void setCompressionThreshold(int threshold) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Switches encryption on for the frames encoded from now on (R31, R32).
   *
   * @param cipher the encryptor; kept and continued for the rest of the connection
   * @throws IllegalStateException when encryption is already on
   */
  public void enableEncryption(StreamCipher cipher) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Encodes one frame.
   *
   * @param frame the frame
   * @return the bytes to send
   * @throws ProtocolException with {@code FRAME_LENGTH_INVALID} when the length field would exceed
   *     the maximum frame length (R27)
   */
  public byte[] encode(Frame frame) throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }
}

package dev.shoko.minecraft.net.frame;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import java.io.ByteArrayOutputStream;
import java.util.Objects;
import java.util.zip.Deflater;

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

  private static final int DEFLATE_CHUNK = 4096;

  private final FrameLimits limits;
  private int threshold = -1;
  private StreamCipher cipher;

  private FrameEncoder(FrameLimits limits) {
    this.limits = Objects.requireNonNull(limits, "limits");
  }

  /**
   * Creates an encoder with the default limits ({@link FrameLimits#DEFAULT}).
   *
   * @return a new encoder
   */
  public static FrameEncoder create() {
    return new FrameEncoder(FrameLimits.DEFAULT);
  }

  /**
   * Creates an encoder with explicit limits.
   *
   * @param limits the size limits
   * @return a new encoder
   */
  public static FrameEncoder create(FrameLimits limits) {
    return new FrameEncoder(limits);
  }

  /**
   * Sets the compression threshold for the frames encoded from now on (R28).
   *
   * @param threshold 0 or more to compress bodies of at least that size, 0 meaning every body;
   *     negative to switch compression off (vector F14)
   */
  public void setCompressionThreshold(int threshold) {
    this.threshold = threshold;
  }

  /**
   * Switches encryption on for the frames encoded from now on (R31, R32).
   *
   * @param cipher the encryptor; kept and continued for the rest of the connection
   * @throws IllegalStateException when encryption is already on
   */
  public void enableEncryption(StreamCipher cipher) {
    if (this.cipher != null) {
      throw new IllegalStateException("encryption is already on");
    }
    this.cipher = Objects.requireNonNull(cipher, "cipher");
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
    byte[] body = idAndPayload(frame);
    byte[] inner = threshold < 0 ? body : withDataLength(body);
    if (inner.length > limits.maxFrameLength()) {
      throw new ProtocolException(
          Reason.FRAME_LENGTH_INVALID,
          "frame length " + inner.length + " above " + limits.maxFrameLength());
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream(inner.length + 3);
    writeVarInt(out, inner.length);
    out.writeBytes(inner);
    byte[] bytes = out.toByteArray();
    if (cipher != null) {
      cipher.apply(bytes, 0, bytes.length);
    }
    return bytes;
  }

  private static byte[] idAndPayload(Frame frame) {
    byte[] payload = frame.payload();
    ByteArrayOutputStream out = new ByteArrayOutputStream(payload.length + 5);
    writeVarInt(out, frame.packetId());
    out.writeBytes(payload);
    return out.toByteArray();
  }

  private byte[] withDataLength(byte[] body) {
    ByteArrayOutputStream out = new ByteArrayOutputStream(body.length + 5);
    if (body.length < threshold) {
      writeVarInt(out, 0);
      out.writeBytes(body);
    } else {
      writeVarInt(out, body.length);
      out.writeBytes(deflate(body));
    }
    return out.toByteArray();
  }

  private static byte[] deflate(byte[] body) {
    Deflater deflater = new Deflater();
    try {
      deflater.setInput(body);
      deflater.finish();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] chunk = new byte[DEFLATE_CHUNK];
      while (!deflater.finished()) {
        out.write(chunk, 0, deflater.deflate(chunk));
      }
      return out.toByteArray();
    } finally {
      deflater.end();
    }
  }

  private static void writeVarInt(ByteArrayOutputStream out, int value) {
    int rest = value;
    while ((rest & ~0x7f) != 0) {
      out.write((rest & 0x7f) | 0x80);
      rest >>>= 7;
    }
    out.write(rest);
  }
}

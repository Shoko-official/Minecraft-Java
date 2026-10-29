package dev.shoko.minecraft.net.frame;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.buffer.PacketReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

  private static final int MAX_LENGTH_BYTES = 3;
  private static final int INITIAL_CAPACITY = 256;

  private final FrameLimits limits;
  private byte[] buffer = new byte[INITIAL_CAPACITY];
  private int start;
  private int end;
  private int threshold = -1;
  private StreamCipher cipher;
  private boolean failed;
  private int lengthFieldBytes;

  private FrameDecoder(FrameLimits limits) {
    this.limits = Objects.requireNonNull(limits, "limits");
  }

  /**
   * Creates a decoder with the default limits ({@link FrameLimits#DEFAULT}).
   *
   * @return a new decoder
   */
  public static FrameDecoder create() {
    return new FrameDecoder(FrameLimits.DEFAULT);
  }

  /**
   * Creates a decoder with explicit limits.
   *
   * @param limits the size limits
   * @return a new decoder
   */
  public static FrameDecoder create(FrameLimits limits) {
    return new FrameDecoder(limits);
  }

  /**
   * Sets the compression threshold for the frames decoded from now on (R28).
   *
   * @param threshold 0 or more to expect the compressed format; negative to switch it off
   */
  public void setCompressionThreshold(int threshold) {
    this.threshold = threshold;
  }

  /**
   * Switches decryption on for the bytes fed from now on (R31, R32).
   *
   * @param cipher the decryptor; kept and continued for the rest of the connection
   * @throws IllegalStateException when decryption is already on
   */
  public void enableDecryption(StreamCipher cipher) {
    if (this.cipher != null) {
      throw new IllegalStateException("decryption is already on");
    }
    this.cipher = Objects.requireNonNull(cipher, "cipher");
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
    if (failed) {
      throw new IllegalStateException("the decoder failed and cannot be used again");
    }
    Objects.checkFromIndexSize(offset, length, data.length);
    try {
      append(data, offset, length);
      return drain();
    } catch (ProtocolException e) {
      failed = true;
      throw e;
    }
  }

  /**
   * Returns the number of bytes received but not yet part of a returned frame.
   *
   * @return the byte count, 0 or more
   */
  public int bufferedBytes() {
    return end - start;
  }

  /** Copies received bytes into the buffer and decrypts the copy, never the caller's array. */
  private void append(byte[] data, int offset, int length) {
    if (start == end) {
      start = 0;
      end = 0;
    }
    if (end + length > buffer.length) {
      int live = end - start;
      long needed = (long) live + length;
      byte[] target =
          needed <= buffer.length ? buffer : new byte[(int) Math.max(needed, 2L * live)];
      System.arraycopy(buffer, start, target, 0, live);
      buffer = target;
      start = 0;
      end = live;
    }
    System.arraycopy(data, offset, buffer, end, length);
    if (cipher != null) {
      cipher.apply(buffer, end, length);
    }
    end += length;
  }

  private List<Frame> drain() throws ProtocolException {
    List<Frame> frames = new ArrayList<>();
    while (true) {
      int length = readLength();
      if (length < 0 || end - start < lengthFieldBytes + length) {
        return frames;
      }
      frames.add(decodeBody(start + lengthFieldBytes, length));
      start += lengthFieldBytes + length;
    }
  }

  /**
   * Reads the frame length at the start of the buffer. Returns -1 while the length is incomplete;
   * fails as soon as it is known to be invalid.
   */
  private int readLength() throws ProtocolException {
    int value = 0;
    for (int i = 0; i < MAX_LENGTH_BYTES; i++) {
      if (start + i >= end) {
        return -1;
      }
      int group = buffer[start + i] & 0xff;
      value |= (group & 0x7f) << (7 * i);
      if ((group & 0x80) == 0) {
        lengthFieldBytes = i + 1;
        return checkLength(value);
      }
    }
    throw new ProtocolException(Reason.FRAME_LENGTH_INVALID, "frame length of more than 3 bytes");
  }

  private int checkLength(int value) throws ProtocolException {
    if (value < 1 || value > limits.maxFrameLength()) {
      throw new ProtocolException(
          Reason.FRAME_LENGTH_INVALID,
          "frame length " + value + " outside 1 to " + limits.maxFrameLength());
    }
    return value;
  }

  private Frame decodeBody(int bodyStart, int bodyLength) throws ProtocolException {
    if (threshold < 0) {
      return parseIdAndPayload(buffer, bodyStart, bodyLength);
    }
    PacketReader in = PacketReader.wrap(buffer, bodyStart, bodyLength);
    int dataLength = in.readVarInt();
    int streamStart = bodyStart + bodyLength - in.remaining();
    if (dataLength == 0) {
      return parseIdAndPayload(buffer, streamStart, in.remaining());
    }
    checkDataLength(dataLength);
    byte[] inflated = FrameInflater.inflate(buffer, streamStart, in.remaining(), dataLength);
    return parseIdAndPayload(inflated, 0, inflated.length);
  }

  private void checkDataLength(int dataLength) throws ProtocolException {
    if (dataLength < 0) {
      throw new ProtocolException(Reason.NEGATIVE_LENGTH, "negative data length " + dataLength);
    }
    if (dataLength < threshold) {
      throw new ProtocolException(
          Reason.DATA_LENGTH_BELOW_THRESHOLD,
          "data length " + dataLength + " below threshold " + threshold);
    }
    if (dataLength > limits.maxDecompressedLength()) {
      throw new ProtocolException(
          Reason.DECOMPRESSED_TOO_LARGE,
          "announced " + dataLength + " bytes, limit " + limits.maxDecompressedLength());
    }
  }

  private static Frame parseIdAndPayload(byte[] source, int offset, int length)
      throws ProtocolException {
    PacketReader in = PacketReader.wrap(source, offset, length);
    int id = in.readVarInt();
    int payloadStart = offset + length - in.remaining();
    return Frame.adopt(id, Arrays.copyOfRange(source, payloadStart, offset + length));
  }
}

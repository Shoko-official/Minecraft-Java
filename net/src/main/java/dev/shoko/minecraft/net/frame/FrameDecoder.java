package dev.shoko.minecraft.net.frame;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.buffer.PacketReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Turns the bytes received on a connection into frames: optional decryption, length, optional
 * decompression (R27 to R33). Incremental: bytes may arrive in any chunking, a frame may be split
 * over several calls and several frames may arrive in one call, with the same result (test plan
 * L1). Starts with compression and decryption off. Not thread-safe: one decoder per direction of
 * one connection.
 *
 * <p>Two ways to read. {@link #append} stores received bytes untouched and {@link #poll} returns
 * one frame at a time, reading it with the compression threshold and the decryption in force at
 * that moment: a connection that sees {@code login_compression} or {@code key} in a frame sets the
 * new mode before polling the next one, and the bytes that followed in the same read are still
 * decoded in the new mode. {@link #feed} is the same thing in one call and is only safe when no
 * packet of the connection changes the reading mode; do not use it around {@code login_compression}
 * and {@code key}.
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
 *       before any allocation (R30 d); otherwise the zlib stream must yield exactly that many bytes
 *       with nothing after it, else {@code DECOMPRESSED_SIZE_MISMATCH} (vector I04) or {@code
 *       CORRUPT_COMPRESSION}, and an invalid stream is {@code CORRUPT_COMPRESSION} with the zlib
 *       failure as cause.
 *   <li>A packet id that is negative once read is {@code UNKNOWN_PACKET_ID}.
 *   <li>A frame announced but not completed stays buffered, with no error and no frame (vector
 *       I11); the read timeout belongs to the connection.
 * </ul>
 *
 * After any exception the decoder is unusable and every later call throws {@link
 * IllegalStateException}.
 */
public final class FrameDecoder {

  /**
   * Most payload bytes one {@link #feed} call returns (project choice, R91): the call stops once
   * the frames it has produced reach this total, the frame that crosses it included, so that a
   * burst of maximal decompressed frames is never held in memory at once. The rest stays buffered.
   */
  public static final int MAX_FEED_PAYLOAD = 1 << 20;

  private static final int MAX_LENGTH_BYTES = 3;
  private static final int INITIAL_CAPACITY = 256;

  private final FrameLimits limits;
  private byte[] buffer = new byte[INITIAL_CAPACITY];
  private int start;
  private int end;

  /** End of the bytes already decrypted; meaningful only while a cipher is set. */
  private int decryptedEnd;

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
   * Sets the compression threshold for the frames polled from now on (R28).
   *
   * @param threshold 0 or more to expect the compressed format; negative to switch it off
   */
  public void setCompressionThreshold(int threshold) {
    this.threshold = threshold;
  }

  /**
   * Switches decryption on for every byte not yet read as part of a frame (R31, R32). Bytes already
   * appended after the last returned frame are treated as encrypted.
   *
   * @param cipher the decryptor; kept and continued for the rest of the connection
   * @throws IllegalStateException when decryption is already on
   */
  public void enableDecryption(StreamCipher cipher) {
    if (this.cipher != null) {
      throw new IllegalStateException("decryption is already on");
    }
    this.cipher = Objects.requireNonNull(cipher, "cipher");
    decryptedEnd = start;
  }

  /**
   * Stores received bytes without decrypting or decompressing them. They are read, with the mode in
   * force then, by {@link #poll()}.
   *
   * @param data the array holding the bytes; not modified, not kept
   * @param offset index of the first byte
   * @param length number of bytes
   * @throws IndexOutOfBoundsException when the range is not inside {@code data}
   */
  public void append(byte[] data, int offset, int length) {
    checkUsable();
    Objects.checkFromIndexSize(offset, length, data.length);
    if (start == end) {
      start = 0;
      end = 0;
      decryptedEnd = 0;
    }
    if (end + length > buffer.length) {
      makeRoom(length);
    }
    System.arraycopy(data, offset, buffer, end, length);
    end += length;
  }

  /**
   * Returns the next complete frame, read with the compression threshold and the decryption set at
   * this moment.
   *
   * @return the frame, or empty when the buffered bytes do not complete one (no error: the read
   *     timeout belongs to the connection)
   * @throws ProtocolException on a malformed frame, see the class description
   */
  public Optional<Frame> poll() throws ProtocolException {
    checkUsable();
    try {
      int length = readLength();
      if (length < 0 || end - start < lengthFieldBytes + length) {
        return Optional.empty();
      }
      int bodyStart = start + lengthFieldBytes;
      decryptUpTo(bodyStart + length);
      Frame frame = decodeBody(bodyStart, length);
      start = bodyStart + length;
      return Optional.of(frame);
    } catch (ProtocolException e) {
      failed = true;
      throw e;
    }
  }

  /**
   * Appends received bytes and returns the frames they complete, polled one after the other with
   * the mode in force at this call. At most about {@link #MAX_FEED_PAYLOAD} payload bytes are
   * returned: when more frames are complete, the call returns after the frame that crosses the cap
   * and the others stay buffered for the next call (a call with no bytes is enough) or for {@link
   * #poll()}. Do not use this method around {@code login_compression} and {@code key}: the frames
   * of one call all share the mode set before it.
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
    append(data, offset, length);
    List<Frame> frames = new ArrayList<>();
    long payload = 0;
    while (payload < MAX_FEED_PAYLOAD) {
      Optional<Frame> frame = poll();
      if (frame.isEmpty()) {
        break;
      }
      frames.add(frame.get());
      payload += frame.get().payloadLength();
    }
    return frames;
  }

  /**
   * Returns the number of bytes received but not yet part of a returned frame.
   *
   * @return the byte count, 0 or more
   */
  public int bufferedBytes() {
    return end - start;
  }

  private void checkUsable() {
    if (failed) {
      throw new IllegalStateException("the decoder failed and cannot be used again");
    }
  }

  /** Makes room for {@code length} more bytes, dropping the bytes already returned. */
  private void makeRoom(int length) {
    int live = end - start;
    long needed = (long) live + length;
    byte[] target = needed <= buffer.length ? buffer : new byte[(int) Math.max(needed, 2L * live)];
    System.arraycopy(buffer, start, target, 0, live);
    decryptedEnd = Math.max(0, decryptedEnd - start);
    buffer = target;
    start = 0;
    end = live;
  }

  /** Decrypts, in place and in stream order, the buffered bytes up to {@code upTo}. */
  private void decryptUpTo(int upTo) {
    if (cipher != null && decryptedEnd < upTo) {
      cipher.apply(buffer, decryptedEnd, upTo - decryptedEnd);
      decryptedEnd = upTo;
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
      decryptUpTo(start + i + 1);
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
    if (id < 0) {
      throw new ProtocolException(Reason.UNKNOWN_PACKET_ID, "negative packet id " + id);
    }
    int payloadStart = offset + length - in.remaining();
    return Frame.adopt(id, Arrays.copyOfRange(source, payloadStart, offset + length));
  }
}

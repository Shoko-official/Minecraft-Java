package dev.shoko.minecraft.net;

import java.io.Serial;
import java.util.Objects;

/**
 * The single checked failure of reading, writing, framing and decoding on the wire. A failed
 * operation gives no partial result. Messages are for humans and not specified; callers and tests
 * rely on {@link #reason()} and {@link #getCause()}. How the connection reacts to each reason
 * (silent close or disconnect message) is provisional (R49, question Q7) and is not part of this
 * type.
 */
public final class ProtocolException extends Exception {

  @Serial private static final long serialVersionUID = 1L;

  /** Why an operation failed. */
  public enum Reason {
    /** A VarInt of more than 5 bytes, or a VarLong of more than 10 bytes (R8). */
    VAR_INT_TOO_BIG,
    /** A read goes past the end of the bounded input, such as a truncated payload (R90). */
    UNEXPECTED_END,
    /** Bytes remain in the payload after the last field of a packet (R90). */
    TRAILING_BYTES,
    /**
     * A string whose length in UTF-16 code units exceeds its maximum, or whose encoded size exceeds
     * 3n + 3 bytes (R9). Also raised when writing such a string.
     */
    STRING_TOO_LONG,
    /** A string whose bytes are not valid UTF-8 (R9). */
    INVALID_UTF8,
    /** A writer that would grow past its bound. */
    PAYLOAD_TOO_LARGE,
    /** A negative announced length for a string, array or byte array (R9, R14). */
    NEGATIVE_LENGTH,
    /** A prefixed array with more elements than its maximum (R14). Also raised when writing. */
    ARRAY_TOO_LONG,
    /** A prefixed byte array longer than its maximum (R14). Also raised when writing. */
    BYTES_TOO_LONG,
    /** An identifier that does not follow the namespace and path grammar (R10). */
    INVALID_IDENTIFIER,
    /**
     * A network NBT value that cannot be read or written; the cause is the {@code NbtException}.
     */
    INVALID_NBT,
    /**
     * A value outside the range of its wire type: a position coordinate that does not fit its field
     * (R12), a bit set longer than its fixed size (R16), or an enumeration value that is not
     * defined.
     */
    VALUE_OUT_OF_RANGE,
    /** A handshake intention other than 1, 2 or 3 (R34, R35). */
    UNKNOWN_INTENTION,
    /**
     * A frame length field of 0, of more than 3 bytes, or above the maximum of 2 097 151 (R27).
     * Also raised when encoding a frame that would exceed that maximum.
     */
    FRAME_LENGTH_INVALID,
    /** A decompressed size above the limit of R30 (d), detected before any allocation. */
    DECOMPRESSED_TOO_LARGE,
    /** A data length between 1 and threshold - 1 (R30 b). */
    DATA_LENGTH_BELOW_THRESHOLD,
    /** A data length different from the actual decompressed size (R30 c). */
    DECOMPRESSED_SIZE_MISMATCH,
    /** A compressed body that is not a valid zlib stream; the cause is the zlib failure (R29). */
    CORRUPT_COMPRESSION,
    /** A packet id that is not in the table of the current state and direction (R2, R49). */
    UNKNOWN_PACKET_ID,
    /** A packet that is not allowed in the current connection state (R34, R49). */
    PACKET_NOT_ALLOWED,
    /**
     * A packet id of the table whose fields this module does not decode yet; the play packets come
     * in a second batch (R2, R49).
     */
    UNSUPPORTED_PACKET
  }

  private final Reason reason;

  /**
   * Creates a failure.
   *
   * @param reason why the operation failed
   * @param message a description for humans
   */
  public ProtocolException(Reason reason, String message) {
    super(message);
    this.reason = Objects.requireNonNull(reason, "reason");
  }

  /**
   * Creates a failure caused by another one, such as {@link Reason#INVALID_NBT} or {@link
   * Reason#CORRUPT_COMPRESSION}.
   *
   * @param reason why the operation failed
   * @param message a description for humans
   * @param cause the underlying failure
   */
  public ProtocolException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = Objects.requireNonNull(reason, "reason");
  }

  /**
   * Returns why the operation failed.
   *
   * @return the reason
   */
  public Reason reason() {
    return reason;
  }
}

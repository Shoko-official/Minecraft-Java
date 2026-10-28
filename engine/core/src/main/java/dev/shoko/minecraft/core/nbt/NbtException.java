package dev.shoko.minecraft.core.nbt;

import java.io.Serial;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * The single checked failure of binary reading and writing and of SNBT parsing and writing. A
 * failed read or parse gives no partial result (R33, R51); a failed write leaves nothing written
 * (R13). Messages are for humans and not specified (R51); callers and tests rely on {@link
 * #reason()} and {@link #offset()}.
 */
public final class NbtException extends Exception {

  @Serial private static final long serialVersionUID = 1L;

  private static final long NO_OFFSET = -1;

  private final Reason reason;
  private final long offset;

  /**
   * Why an operation failed. Binary offsets count decompressed bytes from the first byte of the
   * root; SNBT offsets count UTF-16 chars from the start of the text.
   */
  public enum Reason {
    /**
     * A type identifier of 13 or more, as a root, compound entry or list element type (R33).
     * Offset: the identifier byte.
     */
    UNKNOWN_TYPE,
    /**
     * Data ends before the root is complete, including a compound without its End byte (R33).
     * Offset: the number of decompressed bytes available, one past the last byte.
     */
    UNEXPECTED_END,
    /**
     * A string whose bytes are not modified UTF-8: raw {@code 00}, 4-byte sequence, truncated or
     * malformed sequence (R11, R12). Offset: the first byte of the invalid sequence.
     */
    INVALID_STRING,
    /**
     * A list of element type End with a length of 1 or more (R24). Offset: the element type byte.
     */
    END_LIST_NOT_EMPTY,
    /**
     * A Byte_Array, Int_Array or Long_Array with a negative length (R25). Offset: the first byte of
     * the length.
     */
    NEGATIVE_ARRAY_LENGTH,
    /**
     * More than 512 nesting levels, root included. Binary reading (R31): lists and compounds;
     * offset: the first payload byte of the first container beyond the limit. SNBT reading (R58):
     * compounds, lists and typed arrays; offset: the UTF-16 index of the character opening level
     * 513. Binary writing (R57, wrappers of heterogeneous lists included) and SNBT writing (R59):
     * no offset, nothing written.
     */
    TOO_DEEP,
    /**
     * The estimated memory of a read exceeds the quota of the reader (R32, R60). The quota is in
     * memory units, not in bytes; the offset is a byte position in the decompressed payload, where
     * the excess was found, and is not specified further.
     */
    TOO_LARGE,
    /**
     * The gzip or zlib container cannot be decoded, or its trailer is wrong, missing or truncated
     * (R16, R18, R61). No offset.
     */
    CORRUPT_CONTAINER,
    /** The underlying stream failed; the {@link java.io.IOException} is the cause. No offset. */
    IO_FAILURE,
    /** A string to write encodes to more than 65535 bytes (R10, R13). No offset. */
    STRING_TOO_LONG,
    /**
     * SNBT text outside the grammar: unexpected or missing character, malformed number or suffix,
     * unquoted {@code :}, text after the value (R41 to R49). Offset: the faulty character.
     */
    SNBT_SYNTAX,
    /**
     * A well-formed SNBT number outside the range of its type, or a decimal that overflows to
     * infinity (R46). Offset: the first character of the number.
     */
    SNBT_NUMBER_OUT_OF_RANGE,
    /**
     * An unknown escape, or a code point above U+10FFFF or an unknown Unicode name in an escape
     * (R45). Offset: the backslash.
     */
    SNBT_INVALID_ESCAPE,
    /**
     * A typed array element of a type the array does not accept (R48). Offset: the first character
     * of the element.
     */
    SNBT_INVALID_ARRAY_ELEMENT,
    /**
     * An operation argument of the wrong kind, such as {@code bool("x")} or a malformed UUID (R50).
     * Offset: the first character of the argument.
     */
    SNBT_INVALID_OPERATION,
    /**
     * A value that is not a compound where a compound is required (R40). Offset: the first
     * character of the value.
     */
    SNBT_NOT_A_COMPOUND
  }

  /**
   * Creates a failure located in the input (R33, R51).
   *
   * @param reason why the operation failed
   * @param offset the position described by {@code reason}, 0 or more
   * @param message a description for humans
   */
  public NbtException(Reason reason, long offset, String message) {
    super(message);
    if (offset < 0) {
      throw new IllegalArgumentException("negative offset " + offset);
    }
    this.reason = Objects.requireNonNull(reason, "reason");
    this.offset = offset;
  }

  /**
   * Creates a failure without position, such as {@link Reason#STRING_TOO_LONG} (R13).
   *
   * @param reason why the operation failed
   * @param message a description for humans
   */
  public NbtException(Reason reason, String message) {
    super(message);
    this.reason = Objects.requireNonNull(reason, "reason");
    this.offset = NO_OFFSET;
  }

  /**
   * Creates a failure without position caused by another one, such as {@link Reason#IO_FAILURE} or
   * {@link Reason#CORRUPT_CONTAINER}.
   *
   * @param reason why the operation failed
   * @param message a description for humans
   * @param cause the underlying failure
   */
  public NbtException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = Objects.requireNonNull(reason, "reason");
    this.offset = NO_OFFSET;
  }

  /**
   * Returns why the operation failed (R33, R51).
   *
   * @return the reason
   */
  public Reason reason() {
    return reason;
  }

  /**
   * Returns the position of the fault, as described by each {@link Reason} (R33).
   *
   * @return the offset, or empty for the reasons that have none
   */
  public OptionalLong offset() {
    return offset == NO_OFFSET ? OptionalLong.empty() : OptionalLong.of(offset);
  }
}

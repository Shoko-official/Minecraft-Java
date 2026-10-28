package dev.shoko.minecraft.core.nbt.binary;

import dev.shoko.minecraft.core.nbt.NamedTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtTag;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * Decodes the binary form (R1 to R11) of one root, inside the container chosen at creation.
 *
 * <p>Behavior common to every read method:
 *
 * <ul>
 *   <li>A root of type End, the single byte {@code 00}, is an absent root: the result is empty
 *       (R20, vector V01).
 *   <li>A list of another element type than End but of length 0, or of any element type and a
 *       negative length, reads as the empty list (R23, R25).
 *   <li>A duplicate key in a compound: the last value replaces the earlier one (R27, provisional).
 *   <li>Keys keep their read order (R30); floats and doubles keep their exact bit pattern (R35).
 *   <li>Nesting of lists and compounds is limited to {@link #MAX_DEPTH}, root included (R31).
 *   <li>Estimated memory is limited by a quota (R32, R60): 1 unit per decompressed byte consumed
 *       from the root type to its last byte, plus 24 per tag object (the root, each compound value,
 *       each list element; array elements count for their bytes only). An array or list header is
 *       checked before its elements are read or allocated. Beyond the quota: {@link
 *       NbtException.Reason#TOO_LARGE}.
 *   <li>Payload bytes after the root are ignored and not counted (R34). In gzip and zlib mode the
 *       container is still read to its end and its trailer checked (R61); a single gzip member is
 *       read and the bytes after the container are ignored.
 *   <li>The stream is not closed. It may be read ahead: its position after a read, successful or
 *       not, is undefined. A framed use reads from a byte array or a stream bounded to the frame.
 *   <li>A malformed input, an exceeded limit or a failing stream gives an {@link NbtException} and
 *       no partial result (R33); a {@code null} argument gives a {@link NullPointerException}.
 * </ul>
 */
public final class NbtReader {

  /**
   * Maximum number of nested lists and compounds, root included: 512 are read, 513 fail with {@link
   * NbtException.Reason#TOO_DEEP} (R31, provisional; vectors E09a and E09b).
   */
  public static final int MAX_DEPTH = 512;

  /** Quota of a reader created without one: 134 217 728 units, 128 Mio (R32, provisional). */
  public static final long DEFAULT_QUOTA = 128L << 20;

  private final NbtCompression compression;
  private final long quota;

  private NbtReader(NbtCompression compression, long quota) {
    this.compression = Objects.requireNonNull(compression, "compression");
    this.quota = quota;
  }

  /**
   * Returns a reader with the default quota of {@link #DEFAULT_QUOTA} units (R32, R60).
   *
   * @param compression the container of the bytes to read
   * @return the reader
   */
  public static NbtReader of(NbtCompression compression) {
    return new NbtReader(compression, DEFAULT_QUOTA);
  }

  /**
   * Returns a reader with an explicit quota of estimated memory, in the units of R60. A read whose
   * running total exceeds it fails with {@link NbtException.Reason#TOO_LARGE} (R32).
   *
   * @param compression the container of the bytes to read
   * @param quota the largest total of units accepted, 1 or more; {@link Long#MAX_VALUE} means no
   *     limit
   * @return the reader
   * @throws IllegalArgumentException when {@code quota} is less than 1
   */
  public static NbtReader of(NbtCompression compression, long quota) {
    if (quota < 1) {
      throw new IllegalArgumentException("quota must be 1 or more: " + quota);
    }
    return new NbtReader(compression, quota);
  }

  /**
   * Reads a named root: type, name, payload (R5, R14, R15).
   *
   * @param in the stream, positioned at the container start
   * @return the root with its name as read, or empty for an absent root
   * @throws NbtException when the bytes are not a valid root
   */
  public Optional<NamedTag> readNamed(InputStream in) throws NbtException {
    return read(in, TagDecoder::readNamed);
  }

  /**
   * Reads a named root from a byte array; same as {@link #readNamed(InputStream)}.
   *
   * @param data the container bytes
   * @return the root with its name as read, or empty for an absent root
   * @throws NbtException when the bytes are not a valid root
   */
  public Optional<NamedTag> readNamed(byte[] data) throws NbtException {
    return readNamed(new ByteArrayInputStream(data));
  }

  /**
   * Reads an unnamed root, as sent over the network: type then payload, no name (R20).
   *
   * @param in the stream, positioned at the container start
   * @return the root value, or empty for an absent root
   * @throws NbtException when the bytes are not a valid root
   */
  public Optional<NbtTag> readUnnamed(InputStream in) throws NbtException {
    return read(in, TagDecoder::readUnnamed);
  }

  /**
   * Reads an unnamed root from a byte array; same as {@link #readUnnamed(InputStream)}.
   *
   * @param data the container bytes
   * @return the root value, or empty for an absent root
   * @throws NbtException when the bytes are not a valid root
   */
  public Optional<NbtTag> readUnnamed(byte[] data) throws NbtException {
    return readUnnamed(new ByteArrayInputStream(data));
  }

  private <T> T read(InputStream source, Root<T> root) throws NbtException {
    Objects.requireNonNull(source, "source");
    try (InputStream in = ContainerCodec.open(source, compression)) {
      var input = new BinaryInput(in, quota);
      T result = root.read(new TagDecoder(input));
      if (compression != NbtCompression.NONE) {
        input.drain();
      }
      return result;
    } catch (IOException e) {
      throw ContainerCodec.readFailure(e);
    }
  }

  /** The root form to decode: named (files) or unnamed (network). */
  @FunctionalInterface
  private interface Root<T> {
    T read(TagDecoder decoder) throws NbtException;
  }
}

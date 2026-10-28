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
 *   <li>Bytes after the root are ignored (R34). The stream is not closed.
 *   <li>Every failure is an {@link NbtException} and gives no partial result (R33).
 * </ul>
 */
public final class NbtReader {

  /**
   * Maximum number of nested lists and compounds, root included: 512 are read, 513 fail with {@link
   * NbtException.Reason#TOO_DEEP} (R31, provisional; vectors E09a and E09b).
   */
  public static final int MAX_DEPTH = 512;

  private final NbtCompression compression;
  private final long maxBytes;

  private NbtReader(NbtCompression compression, long maxBytes) {
    this.compression = Objects.requireNonNull(compression, "compression");
    this.maxBytes = maxBytes;
  }

  /**
   * Returns a reader without size limit, as used for files (R32).
   *
   * @param compression the container of the bytes to read
   * @return the reader
   */
  public static NbtReader of(NbtCompression compression) {
    return new NbtReader(compression, Long.MAX_VALUE);
  }

  /**
   * Returns a reader that fails with {@link NbtException.Reason#TOO_LARGE} when a read needs more
   * than {@code maxBytes} decompressed bytes (R32, configurable limit for the network).
   *
   * @param compression the container of the bytes to read
   * @param maxBytes the largest number of decompressed bytes a read may consume, 1 or more
   * @return the reader
   * @throws IllegalArgumentException when {@code maxBytes} is less than 1
   */
  public static NbtReader of(NbtCompression compression, long maxBytes) {
    if (maxBytes < 1) {
      throw new IllegalArgumentException("maxBytes must be 1 or more: " + maxBytes);
    }
    return new NbtReader(compression, maxBytes);
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
      return root.read(new TagDecoder(new BinaryInput(in, maxBytes)));
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

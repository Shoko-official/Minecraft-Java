package dev.shoko.minecraft.core.nbt.binary;

import dev.shoko.minecraft.core.nbt.NamedTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtTag;
import java.io.OutputStream;

/**
 * Encodes one root in the binary form (R1 to R11), inside the container chosen at creation. It is
 * the exact inverse of {@link NbtReader} (R30), with these rules:
 *
 * <ul>
 *   <li>Compound keys are written in storage order (R30).
 *   <li>An empty list is written with element type End, {@code 00 00 00 00 00} (R22).
 *   <li>A heterogeneous list is written as a list of compounds, each holding one element under the
 *       empty key (R47).
 *   <li>Floats and doubles are written with their exact bit pattern (R35).
 *   <li>A string encoding to more than 65535 bytes fails with {@link
 *       NbtException.Reason#STRING_TOO_LONG}, and then nothing at all is written to the stream
 *       (R13).
 *   <li>The container is finished, the stream is not closed.
 * </ul>
 */
public final class NbtWriter {

  private NbtWriter() {}

  /**
   * Returns a writer for this container (R16, R18, R19).
   *
   * @param compression the container to produce
   * @return the writer
   */
  public static NbtWriter of(NbtCompression compression) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes a named root: type, name, payload (R5, R14).
   *
   * @param root the root and its name
   * @param out the destination
   * @throws NbtException on a string too long (nothing written) or a stream failure
   */
  public void writeNamed(NamedTag root, OutputStream out) throws NbtException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Returns the container bytes of a named root; same as {@link #writeNamed(NamedTag,
   * OutputStream)}.
   *
   * @param root the root and its name
   * @return the bytes
   * @throws NbtException on a string too long
   */
  public byte[] writeNamed(NamedTag root) throws NbtException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes an unnamed root, as sent over the network: type then payload (R20).
   *
   * @param root the root value
   * @param out the destination
   * @throws NbtException on a string too long (nothing written) or a stream failure
   */
  public void writeUnnamed(NbtTag root, OutputStream out) throws NbtException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Returns the container bytes of an unnamed root; same as {@link #writeUnnamed(NbtTag,
   * OutputStream)}.
   *
   * @param root the root value
   * @return the bytes
   * @throws NbtException on a string too long
   */
  public byte[] writeUnnamed(NbtTag root) throws NbtException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes an absent root, the single End byte {@code 00} inside the container (R20, vector V01).
   *
   * @param out the destination
   * @throws NbtException on a stream failure
   */
  public void writeAbsentRoot(OutputStream out) throws NbtException {
    Skeleton.throwNotImplemented();
  }
}

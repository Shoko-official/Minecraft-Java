package dev.shoko.minecraft.core.nbt.binary;

import dev.shoko.minecraft.core.nbt.NamedTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.TagType;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Encodes one root in the binary form (R1 to R11), inside the container chosen at creation. It is
 * the exact inverse of {@link NbtReader} (R30), with these rules:
 *
 * <ul>
 *   <li>Compound keys are written in storage order (R30).
 *   <li>An empty list is written with element type End, {@code 00 00 00 00 00} (R22).
 *   <li>A heterogeneous list is written as a list of compounds, each holding one element under the
 *       empty key (R47, R63); reading it back gives that list of compounds.
 *   <li>A tree whose binary form nests more than {@link NbtReader#MAX_DEPTH} lists and compounds,
 *       root and wrappers included, fails with {@link NbtException.Reason#TOO_DEEP}, without
 *       offset, before anything is written (R57): what is written can always be read.
 *   <li>Floats and doubles are written with their exact bit pattern (R35).
 *   <li>A string encoding to more than 65535 bytes fails with {@link
 *       NbtException.Reason#STRING_TOO_LONG}, and then nothing at all is written to the stream
 *       (R13).
 *   <li>The container is finished, the stream is not closed.
 * </ul>
 */
public final class NbtWriter {

  private final NbtCompression compression;

  private NbtWriter(NbtCompression compression) {
    this.compression = Objects.requireNonNull(compression, "compression");
  }

  /**
   * Returns a writer for this container (R16, R18, R19).
   *
   * @param compression the container to produce
   * @return the writer
   */
  public static NbtWriter of(NbtCompression compression) {
    return new NbtWriter(compression);
  }

  /**
   * Writes a named root: type, name, payload (R5, R14).
   *
   * @param root the root and its name
   * @param out the destination
   * @throws NbtException on a string too long (nothing written) or a stream failure
   */
  public void writeNamed(NamedTag root, OutputStream out) throws NbtException {
    emit(writeNamed(root), out);
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
    return ContainerCodec.pack(TagEncoder.named(root), compression);
  }

  /**
   * Writes an unnamed root, as sent over the network: type then payload (R20).
   *
   * @param root the root value
   * @param out the destination
   * @throws NbtException on a string too long (nothing written) or a stream failure
   */
  public void writeUnnamed(NbtTag root, OutputStream out) throws NbtException {
    emit(writeUnnamed(root), out);
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
    return ContainerCodec.pack(TagEncoder.unnamed(root), compression);
  }

  /**
   * Writes an absent root, the single End byte {@code 00} inside the container (R20, vector V01).
   *
   * @param out the destination
   * @throws NbtException on a stream failure
   */
  public void writeAbsentRoot(OutputStream out) throws NbtException {
    emit(ContainerCodec.pack(new byte[] {(byte) TagType.END.id()}, compression), out);
  }

  // The bytes are complete before the first write: a failed encoding leaves the stream untouched.
  private static void emit(byte[] bytes, OutputStream out) throws NbtException {
    Objects.requireNonNull(out, "out");
    try {
      out.write(bytes);
      out.flush();
    } catch (IOException e) {
      throw new NbtException(Reason.IO_FAILURE, "output stream failed", e);
    }
  }
}

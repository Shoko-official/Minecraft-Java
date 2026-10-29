package dev.shoko.minecraft.net.buffer;

import dev.shoko.minecraft.net.ProtocolException;

/**
 * Writes one element of a prefixed array or of a prefixed optional.
 *
 * @param <T> the element type
 */
@FunctionalInterface
public interface ElementWriter<T> {

  /**
   * Writes one element.
   *
   * @param out the writer positioned after the previous element
   * @param element the element to write
   * @throws ProtocolException when the element cannot be written
   */
  void write(PacketWriter out, T element) throws ProtocolException;
}

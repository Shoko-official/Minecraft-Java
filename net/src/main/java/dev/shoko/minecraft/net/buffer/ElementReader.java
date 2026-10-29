package dev.shoko.minecraft.net.buffer;

import dev.shoko.minecraft.net.ProtocolException;

/**
 * Reads one element of a prefixed array or of a prefixed optional.
 *
 * @param <T> the element type
 */
@FunctionalInterface
public interface ElementReader<T> {

  /**
   * Reads one element.
   *
   * @param in the reader positioned at the element
   * @return the element
   * @throws ProtocolException when the element is malformed
   */
  T read(PacketReader in) throws ProtocolException;
}

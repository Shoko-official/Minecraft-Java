package dev.shoko.minecraft.net.packet;

import dev.shoko.minecraft.net.registry.PacketType;

/**
 * A decoded packet. Implemented by one record per packet, in the subpackage of its state; the
 * fields follow the table of the spec (R3). Immutable.
 */
public interface Packet {

  /**
   * Returns the packet identity, which gives its id for encoding.
   *
   * @return the type
   */
  PacketType type();
}

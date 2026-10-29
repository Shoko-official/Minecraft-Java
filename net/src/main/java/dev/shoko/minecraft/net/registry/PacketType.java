package dev.shoko.minecraft.net.registry;

import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;

/**
 * Identity of one packet: its state, its direction, its id and its name. Implemented by the nine
 * enums of this package, one per existing (state, direction) pair, which hold the 260 packets of
 * protocol 777 (R2). Ids are contiguous from 0 in each pair.
 */
public interface PacketType {

  /**
   * Returns the state in which the packet is valid.
   *
   * @return the state
   */
  ConnectionState state();

  /**
   * Returns the direction of the packet.
   *
   * @return the direction
   */
  PacketDirection direction();

  /**
   * Returns the VarInt id written after the frame length.
   *
   * @return the id, 0 or more
   */
  int id();

  /**
   * Returns the name of the packet in the extracted report, without the {@code minecraft:} prefix,
   * such as {@code status_response} or {@code debug/block_value}.
   *
   * @return the name
   */
  String packetName();
}

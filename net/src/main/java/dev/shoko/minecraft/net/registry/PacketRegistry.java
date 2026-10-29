package dev.shoko.minecraft.net.registry;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import java.util.List;
import java.util.Optional;

/**
 * Lookup in the 260 packets of protocol 777 (R2): handshake 1 serverbound; status 2 and 2; login 6
 * and 5; configuration 21 and 10; play 144 and 69 (clientbound and serverbound).
 */
public final class PacketRegistry {

  private PacketRegistry() {}

  /**
   * Returns the packet with an id, as a decoder does when it reads a frame.
   *
   * @param state the state of the connection
   * @param direction the direction of the frame
   * @param id the packet id read from the frame
   * @return the packet type
   * @throws ProtocolException with {@code UNKNOWN_PACKET_ID} when the pair has no such id,
   *     including every id of the clientbound handshake, which has no packet (R49, vector I06)
   */
  public static PacketType lookup(ConnectionState state, PacketDirection direction, int id)
      throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the packet with a name.
   *
   * @param state the state of the connection
   * @param direction the direction of the packet
   * @param packetName the name without the {@code minecraft:} prefix
   * @return the packet type, or empty when the pair has no packet of this name
   */
  public static Optional<PacketType> byName(
      ConnectionState state, PacketDirection direction, String packetName) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns every packet of a pair, ordered by id from 0.
   *
   * @param state the state of the connection
   * @param direction the direction of the packets
   * @return the packets, empty for the clientbound handshake
   */
  public static List<PacketType> all(ConnectionState state, PacketDirection direction) {
    return Skeleton.throwNotImplemented();
  }
}

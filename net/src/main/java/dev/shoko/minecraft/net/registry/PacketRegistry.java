package dev.shoko.minecraft.net.registry;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import java.util.List;
import java.util.Optional;

/**
 * Lookup in the 260 packets of protocol 777 (R2): handshake 1 serverbound; status 2 and 2; login 6
 * and 5; configuration 21 and 10; play 144 and 69 (clientbound and serverbound).
 */
public final class PacketRegistry {

  private static final List<PacketType> HANDSHAKE_SERVERBOUND =
      List.of(HandshakeServerbound.values());
  private static final List<PacketType> STATUS_CLIENTBOUND = List.of(StatusClientbound.values());
  private static final List<PacketType> STATUS_SERVERBOUND = List.of(StatusServerbound.values());
  private static final List<PacketType> LOGIN_CLIENTBOUND = List.of(LoginClientbound.values());
  private static final List<PacketType> LOGIN_SERVERBOUND = List.of(LoginServerbound.values());
  private static final List<PacketType> CONFIGURATION_CLIENTBOUND =
      List.of(ConfigurationClientbound.values());
  private static final List<PacketType> CONFIGURATION_SERVERBOUND =
      List.of(ConfigurationServerbound.values());
  private static final List<PacketType> PLAY_CLIENTBOUND = List.of(PlayClientbound.values());
  private static final List<PacketType> PLAY_SERVERBOUND = List.of(PlayServerbound.values());

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
    List<PacketType> packets = all(state, direction);
    if (id < 0 || id >= packets.size()) {
      throw new ProtocolException(
          Reason.UNKNOWN_PACKET_ID, "no " + state + " " + direction + " packet with id " + id);
    }
    return packets.get(id);
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
    return all(state, direction).stream()
        .filter(type -> type.packetName().equals(packetName))
        .findFirst();
  }

  /**
   * Returns every packet of a pair, ordered by id from 0.
   *
   * @param state the state of the connection
   * @param direction the direction of the packets
   * @return the packets, empty for the clientbound handshake
   */
  public static List<PacketType> all(ConnectionState state, PacketDirection direction) {
    return switch (state) {
      case HANDSHAKE ->
          direction == PacketDirection.SERVERBOUND ? HANDSHAKE_SERVERBOUND : List.of();
      case STATUS ->
          direction == PacketDirection.CLIENTBOUND ? STATUS_CLIENTBOUND : STATUS_SERVERBOUND;
      case LOGIN ->
          direction == PacketDirection.CLIENTBOUND ? LOGIN_CLIENTBOUND : LOGIN_SERVERBOUND;
      case CONFIGURATION ->
          direction == PacketDirection.CLIENTBOUND
              ? CONFIGURATION_CLIENTBOUND
              : CONFIGURATION_SERVERBOUND;
      case PLAY -> direction == PacketDirection.CLIENTBOUND ? PLAY_CLIENTBOUND : PLAY_SERVERBOUND;
    };
  }
}

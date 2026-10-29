package dev.shoko.minecraft.net.state;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.packet.Packet;

/**
 * Tracks the state of one connection from the packets that cross it. The same machine serves a
 * server or a client: it is fed every packet of the connection in order, in both directions.
 *
 * <p>Transitions (R34, R39, R45, R93):
 *
 * <ul>
 *   <li>{@code HANDSHAKE} to {@code STATUS} or {@code LOGIN} with the handshake packet, by its
 *       intention;
 *   <li>{@code LOGIN} to {@code CONFIGURATION} with the serverbound {@code login_acknowledged};
 *   <li>{@code CONFIGURATION} to {@code PLAY} with the serverbound {@code finish_configuration},
 *       the acknowledgement, and not with the clientbound one (R93);
 *   <li>{@code PLAY} to {@code CONFIGURATION} with the serverbound {@code
 *       configuration_acknowledged} (R45, play packets come in the second batch).
 * </ul>
 *
 * Any other packet keeps the state. Not thread-safe: one connection, one thread.
 */
public final class ConnectionStateMachine {

  private ConnectionStateMachine() {}

  /**
   * Creates the machine of a new connection.
   *
   * @return a machine in {@link ConnectionState#HANDSHAKE}
   */
  public static ConnectionStateMachine start() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the current state.
   *
   * @return the state
   */
  public ConnectionState state() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Accounts for one packet and returns the state after it.
   *
   * @param packet a packet sent or received by this connection
   * @return the state after the packet
   * @throws ProtocolException with {@code PACKET_NOT_ALLOWED} when the packet does not belong to
   *     the current state (R49); the state is then unchanged
   */
  public ConnectionState advance(Packet packet) throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }
}

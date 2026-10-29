package dev.shoko.minecraft.net.state;

import dev.shoko.minecraft.net.ProtocolException;

/** The intention field of the handshake, which selects the next state (R34). */
public enum ConnectionIntent {
  /** Server list ping; leads to {@link ConnectionState#STATUS}. */
  STATUS(1, ConnectionState.STATUS),
  /** Normal login; leads to {@link ConnectionState#LOGIN}. */
  LOGIN(2, ConnectionState.LOGIN),
  /** Login after a transfer from another server; leads to {@link ConnectionState#LOGIN} (R46). */
  TRANSFER(3, ConnectionState.LOGIN);

  private final int id;
  private final ConnectionState nextState;

  ConnectionIntent(int id, ConnectionState nextState) {
    this.id = id;
    this.nextState = nextState;
  }

  /**
   * Returns the VarInt written on the wire.
   *
   * @return 1, 2 or 3
   */
  public int id() {
    return id;
  }

  /**
   * Returns the state a connection enters after the handshake with this intention.
   *
   * @return the next state
   */
  public ConnectionState nextState() {
    return nextState;
  }

  /**
   * Returns the intention of a wire value.
   *
   * @param id the VarInt read from the handshake
   * @return the intention
   * @throws ProtocolException with {@code UNKNOWN_INTENTION} when {@code id} is not 1, 2 or 3 (R35,
   *     vector I07)
   */
  public static ConnectionIntent fromId(int id) throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }
}

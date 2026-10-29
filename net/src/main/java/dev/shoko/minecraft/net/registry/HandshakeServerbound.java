package dev.shoko.minecraft.net.registry;

import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;

/**
 * The 1 handshake serverbound packet of protocol 777, ids 0 to 0 (R2). Names and ids are the
 * extracted packet report (data); the fields of each packet are in the packet package.
 */
public enum HandshakeServerbound implements PacketType {
  INTENTION(0, "intention");

  private final int id;
  private final String packetName;

  HandshakeServerbound(int id, String packetName) {
    this.id = id;
    this.packetName = packetName;
  }

  @Override
  public ConnectionState state() {
    return ConnectionState.HANDSHAKE;
  }

  @Override
  public PacketDirection direction() {
    return PacketDirection.SERVERBOUND;
  }

  @Override
  public int id() {
    return id;
  }

  @Override
  public String packetName() {
    return packetName;
  }
}

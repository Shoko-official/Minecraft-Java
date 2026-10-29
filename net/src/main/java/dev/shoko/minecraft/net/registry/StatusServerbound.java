package dev.shoko.minecraft.net.registry;

import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;

/**
 * The 2 status serverbound packets of protocol 777, ids 0 to 1 (R2). Names and ids are the
 * extracted packet report (data); the fields of each packet are in the packet package.
 */
public enum StatusServerbound implements PacketType {
  STATUS_REQUEST(0, "status_request"),
  PING_REQUEST(1, "ping_request");

  private final int id;
  private final String packetName;

  StatusServerbound(int id, String packetName) {
    this.id = id;
    this.packetName = packetName;
  }

  @Override
  public ConnectionState state() {
    return ConnectionState.STATUS;
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

package dev.shoko.minecraft.net.registry;

import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;

/**
 * The 6 login clientbound packets of protocol 777, ids 0 to 5 (R2). Names and ids are the extracted
 * packet report (data); the fields of each packet are in the packet package.
 */
public enum LoginClientbound implements PacketType {
  LOGIN_DISCONNECT(0, "login_disconnect"),
  HELLO(1, "hello"),
  LOGIN_FINISHED(2, "login_finished"),
  LOGIN_COMPRESSION(3, "login_compression"),
  CUSTOM_QUERY(4, "custom_query"),
  COOKIE_REQUEST(5, "cookie_request");

  private final int id;
  private final String packetName;

  LoginClientbound(int id, String packetName) {
    this.id = id;
    this.packetName = packetName;
  }

  @Override
  public ConnectionState state() {
    return ConnectionState.LOGIN;
  }

  @Override
  public PacketDirection direction() {
    return PacketDirection.CLIENTBOUND;
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

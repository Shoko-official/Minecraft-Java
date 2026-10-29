package dev.shoko.minecraft.net.registry;

import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;

/**
 * The 5 login serverbound packets of protocol 777, ids 0 to 4 (R2). Names and ids are the extracted
 * packet report (data); the fields of each packet are in the packet package.
 */
public enum LoginServerbound implements PacketType {
  HELLO(0, "hello"),
  KEY(1, "key"),
  CUSTOM_QUERY_ANSWER(2, "custom_query_answer"),
  LOGIN_ACKNOWLEDGED(3, "login_acknowledged"),
  COOKIE_RESPONSE(4, "cookie_response");

  private final int id;
  private final String packetName;

  LoginServerbound(int id, String packetName) {
    this.id = id;
    this.packetName = packetName;
  }

  @Override
  public ConnectionState state() {
    return ConnectionState.LOGIN;
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

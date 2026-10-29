package dev.shoko.minecraft.net.registry;

import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;

/**
 * The 10 configuration serverbound packets of protocol 777, ids 0 to 9 (R2). Names and ids are the
 * extracted packet report (data); the fields of each packet are in the packet package.
 */
public enum ConfigurationServerbound implements PacketType {
  CLIENT_INFORMATION(0, "client_information"),
  COOKIE_RESPONSE(1, "cookie_response"),
  CUSTOM_PAYLOAD(2, "custom_payload"),
  FINISH_CONFIGURATION(3, "finish_configuration"),
  KEEP_ALIVE(4, "keep_alive"),
  PONG(5, "pong"),
  RESOURCE_PACK(6, "resource_pack"),
  SELECT_KNOWN_PACKS(7, "select_known_packs"),
  CUSTOM_CLICK_ACTION(8, "custom_click_action"),
  ACCEPT_CODE_OF_CONDUCT(9, "accept_code_of_conduct");

  private final int id;
  private final String packetName;

  ConfigurationServerbound(int id, String packetName) {
    this.id = id;
    this.packetName = packetName;
  }

  @Override
  public ConnectionState state() {
    return ConnectionState.CONFIGURATION;
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

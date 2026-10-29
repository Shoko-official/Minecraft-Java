package dev.shoko.minecraft.net.registry;

import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;

/**
 * The 21 configuration clientbound packets of protocol 777, ids 0 to 20 (R2). Names and ids are the
 * extracted packet report (data); the fields of each packet are in the packet package.
 */
public enum ConfigurationClientbound implements PacketType {
  COOKIE_REQUEST(0, "cookie_request"),
  CUSTOM_PAYLOAD(1, "custom_payload"),
  DISCONNECT(2, "disconnect"),
  FINISH_CONFIGURATION(3, "finish_configuration"),
  KEEP_ALIVE(4, "keep_alive"),
  PING(5, "ping"),
  RESET_CHAT(6, "reset_chat"),
  REGISTRY_DATA(7, "registry_data"),
  RESOURCE_PACK_POP(8, "resource_pack_pop"),
  RESOURCE_PACK_PUSH(9, "resource_pack_push"),
  POST_EFFECTS(10, "post_effects"),
  STORE_COOKIE(11, "store_cookie"),
  TRANSFER(12, "transfer"),
  UPDATE_ENABLED_FEATURES(13, "update_enabled_features"),
  UPDATE_TAGS(14, "update_tags"),
  SELECT_KNOWN_PACKS(15, "select_known_packs"),
  CUSTOM_REPORT_DETAILS(16, "custom_report_details"),
  SERVER_LINKS(17, "server_links"),
  CLEAR_DIALOG(18, "clear_dialog"),
  SHOW_DIALOG(19, "show_dialog"),
  CODE_OF_CONDUCT(20, "code_of_conduct");

  private final int id;
  private final String packetName;

  ConfigurationClientbound(int id, String packetName) {
    this.id = id;
    this.packetName = packetName;
  }

  @Override
  public ConnectionState state() {
    return ConnectionState.CONFIGURATION;
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

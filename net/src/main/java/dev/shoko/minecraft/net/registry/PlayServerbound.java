package dev.shoko.minecraft.net.registry;

import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;

/**
 * The 69 play serverbound packets of protocol 777, ids 0 to 68 (R2). Names and ids are the
 * extracted packet report (data); the fields of each packet are in the packet package.
 */
public enum PlayServerbound implements PacketType {
  ACCEPT_TELEPORTATION(0, "accept_teleportation"),
  ATTACK(1, "attack"),
  BLOCK_ENTITY_TAG_QUERY(2, "block_entity_tag_query"),
  BUNDLE_ITEM_SELECTED(3, "bundle_item_selected"),
  CHANGE_DIFFICULTY(4, "change_difficulty"),
  CHANGE_GAME_MODE(5, "change_game_mode"),
  CHAT_ACK(6, "chat_ack"),
  CHAT_COMMAND(7, "chat_command"),
  CHAT_COMMAND_SIGNED(8, "chat_command_signed"),
  CHAT(9, "chat"),
  CHAT_SESSION_UPDATE(10, "chat_session_update"),
  CHUNK_BATCH_RECEIVED(11, "chunk_batch_received"),
  CLIENT_COMMAND(12, "client_command"),
  CLIENT_TICK_END(13, "client_tick_end"),
  CLIENT_INFORMATION(14, "client_information"),
  COMMAND_SUGGESTION(15, "command_suggestion"),
  CONFIGURATION_ACKNOWLEDGED(16, "configuration_acknowledged"),
  CONTAINER_BUTTON_CLICK(17, "container_button_click"),
  CONTAINER_CLICK(18, "container_click"),
  CONTAINER_CLOSE(19, "container_close"),
  CONTAINER_SLOT_STATE_CHANGED(20, "container_slot_state_changed"),
  COOKIE_RESPONSE(21, "cookie_response"),
  CUSTOM_PAYLOAD(22, "custom_payload"),
  DEBUG_SUBSCRIPTION_REQUEST(23, "debug_subscription_request"),
  EDIT_BOOK(24, "edit_book"),
  ENTITY_TAG_QUERY(25, "entity_tag_query"),
  INTERACT(26, "interact"),
  JIGSAW_GENERATE(27, "jigsaw_generate"),
  KEEP_ALIVE(28, "keep_alive"),
  LOCK_DIFFICULTY(29, "lock_difficulty"),
  MOVE_PLAYER_POS(30, "move_player_pos"),
  MOVE_PLAYER_POS_ROT(31, "move_player_pos_rot"),
  MOVE_PLAYER_ROT(32, "move_player_rot"),
  MOVE_PLAYER_STATUS_ONLY(33, "move_player_status_only"),
  MOVE_VEHICLE(34, "move_vehicle"),
  PADDLE_BOAT(35, "paddle_boat"),
  PICK_ITEM_FROM_BLOCK(36, "pick_item_from_block"),
  PICK_ITEM_FROM_ENTITY(37, "pick_item_from_entity"),
  PING_REQUEST(38, "ping_request"),
  PLACE_RECIPE(39, "place_recipe"),
  PLAYER_ABILITIES(40, "player_abilities"),
  PLAYER_ACTION(41, "player_action"),
  PLAYER_COMMAND(42, "player_command"),
  PLAYER_INPUT(43, "player_input"),
  PLAYER_LOADED(44, "player_loaded"),
  PONG(45, "pong"),
  PUNCH(46, "punch"),
  RECIPE_BOOK_CHANGE_SETTINGS(47, "recipe_book_change_settings"),
  RECIPE_BOOK_SEEN_RECIPE(48, "recipe_book_seen_recipe"),
  RENAME_ITEM(49, "rename_item"),
  RESOURCE_PACK(50, "resource_pack"),
  SEEN_ADVANCEMENTS(51, "seen_advancements"),
  SELECT_TRADE(52, "select_trade"),
  SET_BEACON(53, "set_beacon"),
  SET_CARRIED_ITEM(54, "set_carried_item"),
  SET_COMMAND_BLOCK(55, "set_command_block"),
  SET_COMMAND_MINECART(56, "set_command_minecart"),
  SET_CREATIVE_MODE_SLOT(57, "set_creative_mode_slot"),
  SET_GAME_RULE(58, "set_game_rule"),
  SET_JIGSAW_BLOCK(59, "set_jigsaw_block"),
  SET_STRUCTURE_BLOCK(60, "set_structure_block"),
  SET_TEST_BLOCK(61, "set_test_block"),
  SIGN_UPDATE(62, "sign_update"),
  SPECTATOR_ACTION(63, "spectator_action"),
  TELEPORT_TO_ENTITY(64, "teleport_to_entity"),
  TEST_INSTANCE_BLOCK_ACTION(65, "test_instance_block_action"),
  USE_ITEM_ON(66, "use_item_on"),
  USE_ITEM(67, "use_item"),
  CUSTOM_CLICK_ACTION(68, "custom_click_action");

  private final int id;
  private final String packetName;

  PlayServerbound(int id, String packetName) {
    this.id = id;
    this.packetName = packetName;
  }

  @Override
  public ConnectionState state() {
    return ConnectionState.PLAY;
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

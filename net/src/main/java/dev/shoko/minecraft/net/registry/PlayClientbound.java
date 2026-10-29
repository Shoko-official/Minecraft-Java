package dev.shoko.minecraft.net.registry;

import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;

/**
 * The 144 play clientbound packets of protocol 777, ids 0 to 143 (R2). Names and ids are the
 * extracted packet report (data); the fields of each packet are in the packet package.
 */
public enum PlayClientbound implements PacketType {
  BUNDLE_DELIMITER(0, "bundle_delimiter"),
  ADD_ENTITY(1, "add_entity"),
  ANIMATE(2, "animate"),
  AWARD_STATS(3, "award_stats"),
  BLOCK_CHANGED_ACK(4, "block_changed_ack"),
  BLOCK_DESTRUCTION(5, "block_destruction"),
  BLOCK_ENTITY_DATA(6, "block_entity_data"),
  BLOCK_EVENT(7, "block_event"),
  BLOCK_UPDATE(8, "block_update"),
  BOSS_EVENT(9, "boss_event"),
  CHANGE_DIFFICULTY(10, "change_difficulty"),
  CHUNK_BATCH_FINISHED(11, "chunk_batch_finished"),
  CHUNK_BATCH_START(12, "chunk_batch_start"),
  CHUNKS_BIOMES(13, "chunks_biomes"),
  CLEAR_TITLES(14, "clear_titles"),
  COMMAND_SUGGESTIONS(15, "command_suggestions"),
  COMMANDS(16, "commands"),
  CONTAINER_CLOSE(17, "container_close"),
  CONTAINER_SET_CONTENT(18, "container_set_content"),
  CONTAINER_SET_DATA(19, "container_set_data"),
  CONTAINER_SET_SLOT(20, "container_set_slot"),
  COOKIE_REQUEST(21, "cookie_request"),
  COOLDOWN(22, "cooldown"),
  CUSTOM_CHAT_COMPLETIONS(23, "custom_chat_completions"),
  CUSTOM_PAYLOAD(24, "custom_payload"),
  DAMAGE_EVENT(25, "damage_event"),
  DEBUG_BLOCK_VALUE(26, "debug/block_value"),
  DEBUG_CHUNK_VALUE(27, "debug/chunk_value"),
  DEBUG_ENTITY_VALUE(28, "debug/entity_value"),
  DEBUG_EVENT(29, "debug/event"),
  DEBUG_SAMPLE(30, "debug_sample"),
  DELETE_CHAT(31, "delete_chat"),
  DISCONNECT(32, "disconnect"),
  DISGUISED_CHAT(33, "disguised_chat"),
  ENTITY_EVENT(34, "entity_event"),
  ENTITY_POSITION_SYNC(35, "entity_position_sync"),
  EXPLODE(36, "explode"),
  ADD_TRANSIENT_BLOCK(37, "add_transient_block"),
  FORGET_LEVEL_CHUNK(38, "forget_level_chunk"),
  GAME_EVENT(39, "game_event"),
  GAME_RULE_VALUES(40, "game_rule_values"),
  GAME_TEST_HIGHLIGHT_POS(41, "game_test_highlight_pos"),
  MOUNT_SCREEN_OPEN(42, "mount_screen_open"),
  HURT_ANIMATION(43, "hurt_animation"),
  INITIALIZE_BORDER(44, "initialize_border"),
  KEEP_ALIVE(45, "keep_alive"),
  LEVEL_CHUNK_WITH_LIGHT(46, "level_chunk_with_light"),
  LEVEL_EVENT(47, "level_event"),
  LEVEL_PARTICLES(48, "level_particles"),
  LIGHT_UPDATE(49, "light_update"),
  LOGIN(50, "login"),
  LOW_DISK_SPACE_WARNING(51, "low_disk_space_warning"),
  MAP_ITEM_DATA(52, "map_item_data"),
  MERCHANT_OFFERS(53, "merchant_offers"),
  MOVE_ENTITY_POS(54, "move_entity_pos"),
  MOVE_ENTITY_POS_ROT(55, "move_entity_pos_rot"),
  MOVE_MINECART_ALONG_TRACK(56, "move_minecart_along_track"),
  MOVE_ENTITY_ROT(57, "move_entity_rot"),
  MOVE_VEHICLE(58, "move_vehicle"),
  OPEN_BOOK(59, "open_book"),
  OPEN_SCREEN(60, "open_screen"),
  OPEN_SIGN_EDITOR(61, "open_sign_editor"),
  PING(62, "ping"),
  PONG_RESPONSE(63, "pong_response"),
  PLACE_GHOST_RECIPE(64, "place_ghost_recipe"),
  PLAYER_ABILITIES(65, "player_abilities"),
  PLAYER_CHAT(66, "player_chat"),
  PLAYER_COMBAT_END(67, "player_combat_end"),
  PLAYER_COMBAT_ENTER(68, "player_combat_enter"),
  PLAYER_COMBAT_KILL(69, "player_combat_kill"),
  PLAYER_INFO_REMOVE(70, "player_info_remove"),
  PLAYER_INFO_UPDATE(71, "player_info_update"),
  PLAYER_LOOK_AT(72, "player_look_at"),
  PLAYER_POSITION(73, "player_position"),
  PLAYER_ROTATION(74, "player_rotation"),
  RECIPE_BOOK_ADD(75, "recipe_book_add"),
  RECIPE_BOOK_REMOVE(76, "recipe_book_remove"),
  RECIPE_BOOK_SETTINGS(77, "recipe_book_settings"),
  REMOVE_ENTITIES(78, "remove_entities"),
  REMOVE_MOB_EFFECT(79, "remove_mob_effect"),
  RESET_SCORE(80, "reset_score"),
  RESOURCE_PACK_POP(81, "resource_pack_pop"),
  RESOURCE_PACK_PUSH(82, "resource_pack_push"),
  POST_EFFECTS(83, "post_effects"),
  RESPAWN(84, "respawn"),
  ROTATE_HEAD(85, "rotate_head"),
  SECTION_BLOCKS_UPDATE(86, "section_blocks_update"),
  SELECT_ADVANCEMENTS_TAB(87, "select_advancements_tab"),
  SERVER_DATA(88, "server_data"),
  SET_ACTION_BAR_TEXT(89, "set_action_bar_text"),
  SET_BORDER_CENTER(90, "set_border_center"),
  SET_BORDER_LERP_SIZE(91, "set_border_lerp_size"),
  SET_BORDER_SIZE(92, "set_border_size"),
  SET_BORDER_WARNING_DELAY(93, "set_border_warning_delay"),
  SET_BORDER_WARNING_DISTANCE(94, "set_border_warning_distance"),
  SET_CAMERA(95, "set_camera"),
  SET_CHUNK_CACHE_CENTER(96, "set_chunk_cache_center"),
  SET_CHUNK_CACHE_RADIUS(97, "set_chunk_cache_radius"),
  SET_CURSOR_ITEM(98, "set_cursor_item"),
  SET_DEFAULT_SPAWN_POSITION(99, "set_default_spawn_position"),
  SET_DISPLAY_OBJECTIVE(100, "set_display_objective"),
  SET_ENTITY_DATA(101, "set_entity_data"),
  SET_ENTITY_LINK(102, "set_entity_link"),
  SET_ENTITY_MOTION(103, "set_entity_motion"),
  SET_EQUIPMENT(104, "set_equipment"),
  SET_EXPERIENCE(105, "set_experience"),
  SET_HEALTH(106, "set_health"),
  SET_HELD_SLOT(107, "set_held_slot"),
  SET_OBJECTIVE(108, "set_objective"),
  SET_PASSENGERS(109, "set_passengers"),
  SET_PLAYER_INVENTORY(110, "set_player_inventory"),
  SET_PLAYER_TEAM(111, "set_player_team"),
  SET_SCORE(112, "set_score"),
  SET_SIMULATION_DISTANCE(113, "set_simulation_distance"),
  SET_SUBTITLE_TEXT(114, "set_subtitle_text"),
  SET_TIME(115, "set_time"),
  SET_TITLE_TEXT(116, "set_title_text"),
  SET_TITLES_ANIMATION(117, "set_titles_animation"),
  SOUND_ENTITY(118, "sound_entity"),
  SOUND(119, "sound"),
  START_CONFIGURATION(120, "start_configuration"),
  STOP_SOUND(121, "stop_sound"),
  STORE_COOKIE(122, "store_cookie"),
  SWING_ANIMATION(123, "swing_animation"),
  SYSTEM_CHAT(124, "system_chat"),
  TAB_LIST(125, "tab_list"),
  TAG_QUERY(126, "tag_query"),
  TAKE_ITEM_ENTITY(127, "take_item_entity"),
  TELEPORT_ENTITY(128, "teleport_entity"),
  TEST_INSTANCE_BLOCK_STATUS(129, "test_instance_block_status"),
  TICKING_STATE(130, "ticking_state"),
  TICKING_STEP(131, "ticking_step"),
  TRANSFER(132, "transfer"),
  UPDATE_ADVANCEMENTS(133, "update_advancements"),
  UPDATE_ATTRIBUTES(134, "update_attributes"),
  UPDATE_MOB_EFFECT(135, "update_mob_effect"),
  UPDATE_RECIPES(136, "update_recipes"),
  UPDATE_TAGS(137, "update_tags"),
  PROJECTILE_POWER(138, "projectile_power"),
  CUSTOM_REPORT_DETAILS(139, "custom_report_details"),
  SERVER_LINKS(140, "server_links"),
  WAYPOINT(141, "waypoint"),
  CLEAR_DIALOG(142, "clear_dialog"),
  SHOW_DIALOG(143, "show_dialog");

  private final int id;
  private final String packetName;

  PlayClientbound(int id, String packetName) {
    this.id = id;
    this.packetName = packetName;
  }

  @Override
  public ConnectionState state() {
    return ConnectionState.PLAY;
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

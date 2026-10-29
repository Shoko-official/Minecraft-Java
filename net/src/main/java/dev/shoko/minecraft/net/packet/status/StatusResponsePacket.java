package dev.shoko.minecraft.net.packet.status;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.PacketType;
import dev.shoko.minecraft.net.registry.StatusClientbound;

/**
 * The server status as JSON text (R37, R38, vector F16). The JSON is kept as text: its keys are the
 * concern of the server, not of the codec.
 *
 * @param json the JSON document, String(32767)
 */
public record StatusResponsePacket(String json) implements Packet {

  @Override
  public PacketType type() {
    return StatusClientbound.STATUS_RESPONSE;
  }
}

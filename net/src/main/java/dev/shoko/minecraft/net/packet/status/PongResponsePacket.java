package dev.shoko.minecraft.net.packet.status;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.PacketType;
import dev.shoko.minecraft.net.registry.StatusClientbound;

/**
 * Echo of a ping request (R37).
 *
 * @param timestamp the value of the request, Long
 */
public record PongResponsePacket(long timestamp) implements Packet {

  @Override
  public PacketType type() {
    return StatusClientbound.PONG_RESPONSE;
  }
}

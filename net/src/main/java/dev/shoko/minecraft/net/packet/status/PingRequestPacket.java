package dev.shoko.minecraft.net.packet.status;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.PacketType;
import dev.shoko.minecraft.net.registry.StatusServerbound;

/**
 * Asks the server to echo a value (R37, vector F03).
 *
 * @param timestamp the value to echo, Long
 */
public record PingRequestPacket(long timestamp) implements Packet {

  @Override
  public PacketType type() {
    return StatusServerbound.PING_REQUEST;
  }
}

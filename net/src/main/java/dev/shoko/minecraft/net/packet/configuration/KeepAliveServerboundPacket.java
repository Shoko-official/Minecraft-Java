package dev.shoko.minecraft.net.packet.configuration;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.ConfigurationServerbound;
import dev.shoko.minecraft.net.registry.PacketType;

/**
 * Answer to a keep-alive (R47).
 *
 * @param id the value received
 */
public record KeepAliveServerboundPacket(long id) implements Packet {

  @Override
  public PacketType type() {
    return ConfigurationServerbound.KEEP_ALIVE;
  }
}

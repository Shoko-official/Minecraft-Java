package dev.shoko.minecraft.net.packet.configuration;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.ConfigurationClientbound;
import dev.shoko.minecraft.net.registry.PacketType;

/**
 * Liveness probe from the server (R47, vector F15).
 *
 * @param id the value the client must return
 */
public record KeepAliveClientboundPacket(long id) implements Packet {

  @Override
  public PacketType type() {
    return ConfigurationClientbound.KEEP_ALIVE;
  }
}

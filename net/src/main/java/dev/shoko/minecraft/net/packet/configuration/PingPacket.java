package dev.shoko.minecraft.net.packet.configuration;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.ConfigurationClientbound;
import dev.shoko.minecraft.net.registry.PacketType;

/**
 * Round-trip probe from the server (R48).
 *
 * @param id the value the client must return, Int
 */
public record PingPacket(int id) implements Packet {

  @Override
  public PacketType type() {
    return ConfigurationClientbound.PING;
  }
}

package dev.shoko.minecraft.net.packet.configuration;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.ConfigurationServerbound;
import dev.shoko.minecraft.net.registry.PacketType;

/**
 * Answer to a ping (R48).
 *
 * @param id the value received, Int
 */
public record PongPacket(int id) implements Packet {

  @Override
  public PacketType type() {
    return ConfigurationServerbound.PONG;
  }
}

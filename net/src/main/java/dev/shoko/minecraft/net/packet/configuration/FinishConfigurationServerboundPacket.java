package dev.shoko.minecraft.net.packet.configuration;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.ConfigurationServerbound;
import dev.shoko.minecraft.net.registry.PacketType;

/**
 * Acknowledges the end of configuration, serverbound; no field. The connection enters the play
 * state (R93).
 */
public record FinishConfigurationServerboundPacket() implements Packet {

  @Override
  public PacketType type() {
    return ConfigurationServerbound.FINISH_CONFIGURATION;
  }
}

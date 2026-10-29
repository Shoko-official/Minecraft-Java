package dev.shoko.minecraft.net.packet.configuration;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.ConfigurationClientbound;
import dev.shoko.minecraft.net.registry.PacketType;

/** Tells the client that configuration is over, clientbound; no field (R93, vector F08). */
public record FinishConfigurationClientboundPacket() implements Packet {

  @Override
  public PacketType type() {
    return ConfigurationClientbound.FINISH_CONFIGURATION;
  }
}

package dev.shoko.minecraft.net.packet.configuration;

import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.ConfigurationClientbound;
import dev.shoko.minecraft.net.registry.PacketType;

/**
 * Disconnects the client during configuration, clientbound (R49).
 *
 * @param reason the reason as a text component in network NBT: a String tag for plain text, else a
 *     Compound (R17)
 */
public record DisconnectPacket(NbtTag reason) implements Packet {

  @Override
  public PacketType type() {
    return ConfigurationClientbound.DISCONNECT;
  }
}

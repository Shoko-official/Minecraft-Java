package dev.shoko.minecraft.net.packet.configuration;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.ConfigurationClientbound;
import dev.shoko.minecraft.net.registry.PacketType;
import java.util.List;

/**
 * Sends the tags of several registries, clientbound (R66, R92).
 *
 * @param registries the registries with tags, prefixed array
 */
public record UpdateTagsPacket(List<RegistryTags> registries) implements Packet {

  @Override
  public PacketType type() {
    return ConfigurationClientbound.UPDATE_TAGS;
  }
}

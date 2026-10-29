package dev.shoko.minecraft.net.packet.configuration;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.ConfigurationClientbound;
import dev.shoko.minecraft.net.registry.PacketType;
import java.util.List;

/**
 * Synchronizes one registry, clientbound (R62, R63). The position of an entry in the list is its
 * numeric id.
 *
 * @param registry the registry identifier, such as minecraft:dimension_type
 * @param entries the entries in id order, prefixed array
 */
public record RegistryDataPacket(Identifier registry, List<RegistryEntry> entries)
    implements Packet {

  @Override
  public PacketType type() {
    return ConfigurationClientbound.REGISTRY_DATA;
  }
}

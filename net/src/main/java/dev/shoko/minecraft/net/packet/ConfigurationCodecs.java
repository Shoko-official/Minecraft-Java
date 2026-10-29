package dev.shoko.minecraft.net.packet;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.buffer.PacketWriter;
import dev.shoko.minecraft.net.packet.configuration.DisconnectPacket;
import dev.shoko.minecraft.net.packet.configuration.FinishConfigurationClientboundPacket;
import dev.shoko.minecraft.net.packet.configuration.FinishConfigurationServerboundPacket;
import dev.shoko.minecraft.net.packet.configuration.KeepAliveClientboundPacket;
import dev.shoko.minecraft.net.packet.configuration.KeepAliveServerboundPacket;
import dev.shoko.minecraft.net.packet.configuration.PingPacket;
import dev.shoko.minecraft.net.packet.configuration.PongPacket;
import dev.shoko.minecraft.net.packet.configuration.RegistryDataPacket;
import dev.shoko.minecraft.net.packet.configuration.UpdateTagsPacket;
import dev.shoko.minecraft.net.registry.ConfigurationClientbound;
import dev.shoko.minecraft.net.registry.ConfigurationServerbound;
import dev.shoko.minecraft.net.registry.PacketType;
import java.util.Map;

/** Fields of the configuration packets (R43, R47, R48, R49, R93). */
final class ConfigurationCodecs {

  private ConfigurationCodecs() {}

  static Map<PacketType, FieldDecoder> decoders() {
    return Map.of(
        ConfigurationClientbound.DISCONNECT,
        in -> new DisconnectPacket(in.readNbt()),
        ConfigurationClientbound.FINISH_CONFIGURATION,
        in -> new FinishConfigurationClientboundPacket(),
        ConfigurationClientbound.KEEP_ALIVE,
        in -> new KeepAliveClientboundPacket(in.readLong()),
        ConfigurationClientbound.PING,
        in -> new PingPacket(in.readInt()),
        ConfigurationClientbound.REGISTRY_DATA,
        RegistryCodecs::readRegistryData,
        ConfigurationClientbound.UPDATE_TAGS,
        RegistryCodecs::readUpdateTags,
        ConfigurationServerbound.FINISH_CONFIGURATION,
        in -> new FinishConfigurationServerboundPacket(),
        ConfigurationServerbound.KEEP_ALIVE,
        in -> new KeepAliveServerboundPacket(in.readLong()),
        ConfigurationServerbound.PONG,
        in -> new PongPacket(in.readInt()));
  }

  static void encode(Packet packet, PacketWriter out) throws ProtocolException {
    switch (packet) {
      case DisconnectPacket disconnect -> out.writeNbt(disconnect.reason());
      case FinishConfigurationClientboundPacket finish -> {}
      case KeepAliveClientboundPacket keepAlive -> out.writeLong(keepAlive.id());
      case PingPacket ping -> out.writeInt(ping.id());
      case RegistryDataPacket registryData -> RegistryCodecs.write(out, registryData);
      case UpdateTagsPacket updateTags -> RegistryCodecs.write(out, updateTags);
      case FinishConfigurationServerboundPacket finish -> {}
      case KeepAliveServerboundPacket keepAlive -> out.writeLong(keepAlive.id());
      case PongPacket pong -> out.writeInt(pong.id());
      default -> throw new IllegalArgumentException("not a configuration packet: " + packet.type());
    }
  }
}

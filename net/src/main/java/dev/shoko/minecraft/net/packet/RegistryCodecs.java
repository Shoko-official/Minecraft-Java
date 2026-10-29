package dev.shoko.minecraft.net.packet;

import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.buffer.PacketReader;
import dev.shoko.minecraft.net.buffer.PacketWriter;
import dev.shoko.minecraft.net.packet.configuration.RegistryDataPacket;
import dev.shoko.minecraft.net.packet.configuration.RegistryEntry;
import dev.shoko.minecraft.net.packet.configuration.RegistryTags;
import dev.shoko.minecraft.net.packet.configuration.TagIds;
import dev.shoko.minecraft.net.packet.configuration.UpdateTagsPacket;
import java.util.Optional;

/** Fields of registry_data and update_tags (R62 to R66, R92). */
final class RegistryCodecs {

  private RegistryCodecs() {}

  static Packet readRegistryData(PacketReader in) throws ProtocolException {
    Identifier registry = in.readIdentifier();
    return new RegistryDataPacket(
        registry, in.readPrefixedArray(FieldLimits.ARRAY, RegistryCodecs::readEntry));
  }

  static Packet readUpdateTags(PacketReader in) throws ProtocolException {
    return new UpdateTagsPacket(
        in.readPrefixedArray(FieldLimits.ARRAY, RegistryCodecs::readRegistryTags));
  }

  static void write(PacketWriter out, RegistryDataPacket packet) throws ProtocolException {
    out.writeIdentifier(packet.registry());
    out.writePrefixedArray(packet.entries(), FieldLimits.ARRAY, RegistryCodecs::writeEntry);
  }

  static void write(PacketWriter out, UpdateTagsPacket packet) throws ProtocolException {
    out.writePrefixedArray(
        packet.registries(), FieldLimits.ARRAY, RegistryCodecs::writeRegistryTags);
  }

  private static RegistryEntry readEntry(PacketReader in) throws ProtocolException {
    Identifier id = in.readIdentifier();
    Optional<NbtTag> data = in.readPrefixedOptional(PacketReader::readNbt);
    return data.<RegistryEntry>map(tag -> new RegistryEntry.WithData(id, tag))
        .orElseGet(() -> new RegistryEntry.WithoutData(id));
  }

  private static void writeEntry(PacketWriter out, RegistryEntry entry) throws ProtocolException {
    out.writeIdentifier(entry.id());
    switch (entry) {
      case RegistryEntry.WithData withData ->
          out.writePresent(withData.data(), PacketWriter::writeNbt);
      case RegistryEntry.WithoutData withoutData -> out.writeAbsent();
    }
  }

  private static RegistryTags readRegistryTags(PacketReader in) throws ProtocolException {
    Identifier registry = in.readIdentifier();
    return new RegistryTags(
        registry, in.readPrefixedArray(FieldLimits.ARRAY, RegistryCodecs::readTag));
  }

  private static void writeRegistryTags(PacketWriter out, RegistryTags registryTags)
      throws ProtocolException {
    out.writeIdentifier(registryTags.registry());
    out.writePrefixedArray(registryTags.tags(), FieldLimits.ARRAY, RegistryCodecs::writeTag);
  }

  private static TagIds readTag(PacketReader in) throws ProtocolException {
    Identifier name = in.readIdentifier();
    return new TagIds(name, in.readPrefixedArray(FieldLimits.ARRAY, PacketReader::readVarInt));
  }

  private static void writeTag(PacketWriter out, TagIds tag) throws ProtocolException {
    out.writeIdentifier(tag.name());
    out.writePrefixedArray(tag.entries(), FieldLimits.ARRAY, PacketWriter::writeVarInt);
  }
}

package dev.shoko.minecraft.net.packet;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.buffer.PacketReader;
import dev.shoko.minecraft.net.buffer.PacketWriter;
import dev.shoko.minecraft.net.type.GameProfile;
import dev.shoko.minecraft.net.type.ProfileProperty;
import java.util.UUID;

/** The game profile of R20: UUID, name, then at most 16 properties. */
final class ProfileCodec {

  private static final int NAME_CHARS = 16;
  private static final int MAX_PROPERTIES = 16;
  private static final int PROPERTY_NAME_CHARS = 64;
  private static final int SIGNATURE_CHARS = 1024;

  private ProfileCodec() {}

  static GameProfile read(PacketReader in) throws ProtocolException {
    UUID id = in.readUuid();
    String name = in.readString(NAME_CHARS);
    return new GameProfile(id, name, in.readPrefixedArray(MAX_PROPERTIES, ProfileCodec::property));
  }

  static void write(PacketWriter out, GameProfile profile) throws ProtocolException {
    out.writeUuid(profile.id());
    out.writeString(profile.name(), NAME_CHARS);
    out.writePrefixedArray(profile.properties(), MAX_PROPERTIES, ProfileCodec::writeProperty);
  }

  private static ProfileProperty property(PacketReader in) throws ProtocolException {
    String name = in.readString(PROPERTY_NAME_CHARS);
    String value = in.readString(FieldLimits.STRING);
    if (in.readBoolean()) {
      return new ProfileProperty.Signed(name, value, in.readString(SIGNATURE_CHARS));
    }
    return new ProfileProperty.Unsigned(name, value);
  }

  private static void writeProperty(PacketWriter out, ProfileProperty property)
      throws ProtocolException {
    out.writeString(property.name(), PROPERTY_NAME_CHARS);
    out.writeString(property.value(), FieldLimits.STRING);
    switch (property) {
      case ProfileProperty.Signed signed -> {
        out.writeBoolean(true);
        out.writeString(signed.signature(), SIGNATURE_CHARS);
      }
      case ProfileProperty.Unsigned unsigned -> out.writeBoolean(false);
    }
  }
}

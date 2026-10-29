package dev.shoko.minecraft.net.packet;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.buffer.ByteBlob;
import dev.shoko.minecraft.net.frame.Frame;
import dev.shoko.minecraft.net.packet.configuration.DisconnectPacket;
import dev.shoko.minecraft.net.packet.configuration.RegistryDataPacket;
import dev.shoko.minecraft.net.packet.configuration.RegistryEntry;
import dev.shoko.minecraft.net.packet.configuration.RegistryTags;
import dev.shoko.minecraft.net.packet.configuration.TagIds;
import dev.shoko.minecraft.net.packet.configuration.UpdateTagsPacket;
import dev.shoko.minecraft.net.packet.handshake.IntentionPacket;
import dev.shoko.minecraft.net.packet.login.HelloClientboundPacket;
import dev.shoko.minecraft.net.packet.login.HelloServerboundPacket;
import dev.shoko.minecraft.net.packet.login.KeyPacket;
import dev.shoko.minecraft.net.packet.login.LoginDisconnectPacket;
import dev.shoko.minecraft.net.packet.login.LoginFinishedPacket;
import dev.shoko.minecraft.net.packet.status.StatusResponsePacket;
import dev.shoko.minecraft.net.state.ConnectionIntent;
import dev.shoko.minecraft.net.type.GameProfile;
import dev.shoko.minecraft.net.type.ProfileProperty;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.SequencedMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: net-protocol-777 test plan L1 (round trip for every packet of the table with generated
// fields, fixed seed), R9, R14, R20 (limits respected by the generator)
class PacketCodecsRoundTripTest {

  private static final int[] CODE_POINT_STARTS = {0x20, 0xA1, 0x4E00, 0x1F300};
  private static final int[] CODE_POINT_SPANS = {0x5E, 0x100, 0x500, 0x100};

  /** A string of at most {@code maxUnits} UTF-16 code units, valid Unicode. */
  private static String string(Random random, int maxUnits) {
    StringBuilder text = new StringBuilder();
    int target = random.nextInt(maxUnits + 1);
    while (text.length() < target) {
      int range = random.nextInt(CODE_POINT_STARTS.length);
      int codePoint = CODE_POINT_STARTS[range] + random.nextInt(CODE_POINT_SPANS[range]);
      if (text.length() + Character.charCount(codePoint) <= target) {
        text.appendCodePoint(codePoint);
      } else {
        text.append('a');
      }
    }
    return text.toString();
  }

  private static Identifier identifier(Random random) {
    String chars = "abcdefghijklmnopqrstuvwxyz0123456789_-.";
    StringBuilder path = new StringBuilder();
    int length = 1 + random.nextInt(12);
    for (int i = 0; i < length; i++) {
      path.append(chars.charAt(random.nextInt(chars.length())));
      if (random.nextInt(8) == 0) {
        path.append('/');
      }
    }
    return Identifier.of(
        random.nextBoolean() ? "minecraft" : "mod_" + random.nextInt(9), path.toString());
  }

  private static UUID uuid(Random random) {
    return new UUID(random.nextLong(), random.nextLong());
  }

  private static ByteBlob blob(Random random, int maxLength) {
    byte[] bytes = new byte[random.nextInt(maxLength + 1)];
    random.nextBytes(bytes);
    return ByteBlob.of(bytes);
  }

  private static NbtTag text(Random random) {
    if (random.nextBoolean()) {
      return new StringTag(string(random, 40));
    }
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    entries.put("text", new StringTag(string(random, 20)));
    entries.put("n", new IntTag(random.nextInt()));
    return CompoundTag.of(entries);
  }

  private static ProfileProperty property(Random random) {
    String name = string(random, 64);
    String value = string(random, 200);
    return random.nextBoolean()
        ? new ProfileProperty.Unsigned(name, value)
        : new ProfileProperty.Signed(name, value, string(random, 1024));
  }

  private static List<Packet> randomPackets(Random random) {
    List<ProfileProperty> properties = new ArrayList<>();
    int propertyCount = random.nextInt(17);
    for (int i = 0; i < propertyCount; i++) {
      properties.add(property(random));
    }
    List<RegistryEntry> entries = new ArrayList<>();
    int entryCount = random.nextInt(6);
    for (int i = 0; i < entryCount; i++) {
      entries.add(
          random.nextBoolean()
              ? new RegistryEntry.WithData(identifier(random), text(random))
              : new RegistryEntry.WithoutData(identifier(random)));
    }
    List<RegistryTags> registries = new ArrayList<>();
    int registryCount = random.nextInt(4);
    for (int i = 0; i < registryCount; i++) {
      List<TagIds> tags = new ArrayList<>();
      int tagCount = random.nextInt(4);
      for (int j = 0; j < tagCount; j++) {
        List<Integer> ids = new ArrayList<>();
        int idCount = random.nextInt(8);
        for (int k = 0; k < idCount; k++) {
          ids.add(random.nextInt(35_723));
        }
        tags.add(new TagIds(identifier(random), ids));
      }
      registries.add(new RegistryTags(identifier(random), tags));
    }
    ConnectionIntent[] intents = ConnectionIntent.values();
    return List.of(
        new IntentionPacket(
            random.nextInt(),
            string(random, 255),
            random.nextInt(65_536),
            intents[random.nextInt(3)]),
        new StatusResponsePacket(string(random, 500)),
        new LoginDisconnectPacket(string(random, 300)),
        new HelloServerboundPacket(string(random, 16), uuid(random)),
        new HelloClientboundPacket(
            string(random, 20), blob(random, 162), blob(random, 8), random.nextBoolean()),
        new KeyPacket(blob(random, 128), blob(random, 128)),
        new LoginFinishedPacket(
            new GameProfile(uuid(random), string(random, 16), properties), uuid(random)),
        new DisconnectPacket(text(random)),
        new RegistryDataPacket(identifier(random), entries),
        new UpdateTagsPacket(registries));
  }

  @Test
  @Timeout(60)
  void generatedPacketsSurviveEncodingAndDecoding() throws ProtocolException {
    // spec: test plan L1 (round trip), R9, R14, R20
    Random random = new Random(0xC0DECL);
    for (int round = 0; round < 300; round++) {
      for (Packet packet : randomPackets(random)) {
        Frame frame = PacketCodecs.encode(packet);
        Packet decoded =
            PacketCodecs.decode(packet.type().state(), packet.type().direction(), frame);
        assertThat(decoded).as("round %d, %s", round, packet.type().packetName()).isEqualTo(packet);
      }
    }
  }

  @Test
  @Timeout(60)
  void theSamePacketAlwaysEncodesToTheSameFrame() throws ProtocolException {
    // spec: ARCHITECTURE determinism
    Random random = new Random(0xDE7EL);
    for (int round = 0; round < 100; round++) {
      for (Packet packet : randomPackets(random)) {
        assertThat(PacketCodecs.encode(packet)).isEqualTo(PacketCodecs.encode(packet));
      }
    }
  }
}

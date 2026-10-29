package dev.shoko.minecraft.net.packet;

import static dev.shoko.minecraft.net.testing.Codecs.assertWire;
import static dev.shoko.minecraft.net.testing.Codecs.decode;
import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static dev.shoko.minecraft.net.testing.Failures.failure;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.packet.configuration.RegistryDataPacket;
import dev.shoko.minecraft.net.packet.configuration.RegistryEntry;
import dev.shoko.minecraft.net.packet.configuration.RegistryTags;
import dev.shoko.minecraft.net.packet.configuration.TagIds;
import dev.shoko.minecraft.net.packet.configuration.UpdateTagsPacket;
import dev.shoko.minecraft.net.registry.ConfigurationClientbound;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Samples;
import dev.shoko.minecraft.net.testing.Wire;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R62 to R66, R92 (registry_data id 7, update_tags id 14)
class RegistryCodecTest {

  private static final ConnectionState STATE = ConnectionState.CONFIGURATION;
  private static final PacketDirection CLIENTBOUND = PacketDirection.CLIENTBOUND;

  private static Identifier id(String path) {
    return Identifier.of("minecraft", path);
  }

  private static String hex(byte[]... parts) {
    return Hex.format(Hex.concat(parts));
  }

  /** A count of 2^31 - 1 over a few bytes: refused as too long or as more than the bytes left. */
  private static void assertRefusedHugeCount(byte[] payload, int id) {
    assertThat(failure(() -> decode(STATE, CLIENTBOUND, id, payload)).reason())
        .isIn(Reason.UNEXPECTED_END, Reason.ARRAY_TOO_LONG);
  }

  @Test
  void aRegistryIsItsIdentifierThenTheEntriesInIdOrder() throws ProtocolException {
    // spec: R63 (the position of an entry is its numeric id), R17 (NBT without root name)
    RegistryDataPacket packet = Samples.registryData();
    String expected =
        hex(
            Wire.string("minecraft:dimension_type"),
            Wire.varInt(2),
            Wire.string("minecraft:overworld"),
            Hex.bytes("01"),
            Hex.bytes("0a 01 00 01 78 01 00"),
            Wire.string("minecraft:the_end"),
            Hex.bytes("00"));
    assertWire(STATE, CLIENTBOUND, 7, packet, expected);
    assertThat(packet.type()).isEqualTo(ConfigurationClientbound.REGISTRY_DATA);
  }

  @Test
  void anEntryWithDataHasATrueFlagAndAnEntryWithoutDataAFalseFlag() throws ProtocolException {
    // spec: R63 (an entry without data is taken from a known pack)
    RegistryDataPacket packet =
        new RegistryDataPacket(
            id("chat_type"),
            List.of(
                new RegistryEntry.WithData(id("chat"), new StringTag("x")),
                new RegistryEntry.WithoutData(id("say_command"))));
    String expected =
        hex(
            Wire.string("minecraft:chat_type"),
            Wire.varInt(2),
            Wire.string("minecraft:chat"),
            Hex.bytes("01 08 00 01 78"),
            Wire.string("minecraft:say_command"),
            Hex.bytes("00"));
    assertWire(STATE, CLIENTBOUND, 7, packet, expected);
  }

  @Test
  void aRegistryWithoutEntriesIsTheIdentifierAndAZero() throws ProtocolException {
    // spec: R14 (an empty array is 00)
    assertWire(
        STATE,
        CLIENTBOUND,
        7,
        new RegistryDataPacket(id("timeline"), List.of()),
        hex(Wire.string("minecraft:timeline"), Hex.bytes("00")));
  }

  @Test
  void aLargeRegistryKeepsEveryEntryInOrder() throws ProtocolException {
    // spec: R62 (worldgen/biome has 67 entries), R63 (no arbitrary limit below the frame size)
    List<RegistryEntry> entries = new ArrayList<>();
    for (int i = 0; i < 2000; i++) {
      entries.add(new RegistryEntry.WithoutData(Identifier.of("minecraft", "entry_" + i)));
    }
    RegistryDataPacket packet = new RegistryDataPacket(id("worldgen/biome"), entries);
    assertThat(decode(STATE, CLIENTBOUND, 7, PacketCodecs.encode(packet).payload()))
        .isEqualTo(packet);
  }

  @Test
  void provisionalAnInvalidIdentifierIsRefused() {
    // spec: R10 (an invalid identifier is a protocol error)
    byte[] badRegistry = Hex.concat(Wire.string("Minecraft:Bad"), Hex.bytes("00"));
    assertFails(() -> decode(STATE, CLIENTBOUND, 7, badRegistry), Reason.INVALID_IDENTIFIER);
    byte[] badEntry =
        Hex.concat(
            Wire.string("minecraft:chat_type"),
            Wire.varInt(1),
            Wire.string("minecraft:UPPER"),
            Hex.bytes("00"));
    assertFails(() -> decode(STATE, CLIENTBOUND, 7, badEntry), Reason.INVALID_IDENTIFIER);
  }

  @Test
  void provisionalAnnouncingMoreEntriesThanBytesLeftIsRefusedWithoutAllocatingThem() {
    // spec: R90 (a count is compared with the bytes left before any allocation)
    byte[] huge = Hex.concat(Wire.string("minecraft:chat_type"), Hex.bytes("ff ff ff ff 07"));
    assertRefusedHugeCount(huge, 7);
  }

  @Test
  void provisionalTruncatedAndPaddedRegistryPayloadsAreRefused() throws ProtocolException {
    // spec: R90
    byte[] payload = PacketCodecs.encode(Samples.registryData()).payload();
    assertFails(
        () -> decode(STATE, CLIENTBOUND, 7, java.util.Arrays.copyOf(payload, payload.length - 1)),
        Reason.UNEXPECTED_END);
    assertFails(
        () -> decode(STATE, CLIENTBOUND, 7, Hex.concat(payload, new byte[] {0})),
        Reason.TRAILING_BYTES);
  }

  @Test
  void updateTagsListsRegistriesThenTagsThenMemberIds() throws ProtocolException {
    // spec: R66 (per registry, tags of {name, numeric ids})
    UpdateTagsPacket packet = Samples.updateTags();
    String expected =
        hex(
            Wire.varInt(2),
            Wire.string("minecraft:block"),
            Wire.varInt(2),
            Wire.string("minecraft:logs"),
            Wire.varInt(3),
            Wire.varInt(1),
            Wire.varInt(300),
            Wire.varInt(35722),
            Wire.string("minecraft:empty"),
            Wire.varInt(0),
            Wire.string("minecraft:item"),
            Wire.varInt(0));
    assertWire(STATE, CLIENTBOUND, 14, packet, expected);
    assertThat(packet.type()).isEqualTo(ConfigurationClientbound.UPDATE_TAGS);
  }

  @Test
  void anUpdateTagsWithoutRegistriesIsASingleZero() throws ProtocolException {
    // spec: R14
    assertWire(STATE, CLIENTBOUND, 14, new UpdateTagsPacket(List.of()), "00");
  }

  @Test
  void theMemberIdsOfALargeTagSurviveTheRoundTrip() throws ProtocolException {
    // spec: R61 (35 723 block states), R66 (ids are VarInts)
    List<Integer> ids = new ArrayList<>();
    for (int i = 0; i < 5000; i++) {
      ids.add(i * 7);
    }
    UpdateTagsPacket packet =
        new UpdateTagsPacket(
            List.of(new RegistryTags(id("block"), List.of(new TagIds(id("big"), ids)))));
    assertThat(decode(STATE, CLIENTBOUND, 14, PacketCodecs.encode(packet).payload()))
        .isEqualTo(packet);
  }

  @Test
  void provisionalAnnouncingMoreTagsThanBytesLeftIsRefused() {
    // spec: R90
    byte[] hugeRegistries = Hex.bytes("ff ff ff ff 07");
    assertRefusedHugeCount(hugeRegistries, 14);
    byte[] hugeTags =
        Hex.concat(Wire.varInt(1), Wire.string("minecraft:block"), Hex.bytes("ff ff ff ff 07"));
    assertRefusedHugeCount(hugeTags, 14);
    byte[] hugeIds =
        Hex.concat(
            Wire.varInt(1),
            Wire.string("minecraft:block"),
            Wire.varInt(1),
            Wire.string("minecraft:logs"),
            Hex.bytes("ff ff ff ff 07"));
    assertRefusedHugeCount(hugeIds, 14);
  }

  @Test
  void provisionalPaddedUpdateTagsAreRefused() throws ProtocolException {
    // spec: R90
    byte[] payload = PacketCodecs.encode(Samples.updateTags()).payload();
    assertFails(
        () -> decode(STATE, CLIENTBOUND, 14, Hex.concat(payload, new byte[] {1})),
        Reason.TRAILING_BYTES);
  }
}

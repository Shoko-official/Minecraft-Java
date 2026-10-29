package dev.shoko.minecraft.net.packet;

import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static dev.shoko.minecraft.net.testing.Failures.assertThrows;
import static dev.shoko.minecraft.net.testing.Failures.failure;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.frame.Frame;
import dev.shoko.minecraft.net.registry.PacketRegistry;
import dev.shoko.minecraft.net.registry.PacketType;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import dev.shoko.minecraft.net.testing.Golden;
import dev.shoko.minecraft.net.testing.Golden.PacketRow;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Samples;
import dev.shoko.minecraft.net.testing.Samples.Sample;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

// spec: net-protocol-777 R2, R49, R90 (frame <-> packet for the first batch: handshake, status,
// login and configuration packets)
class PacketCodecsTest {

  static Stream<Sample> samples() {
    return Samples.all().stream();
  }

  private static String key(ConnectionState state, PacketDirection direction, String name) {
    return state + "/" + direction + "/" + name;
  }

  private static PacketType registered(Sample sample) {
    return PacketRegistry.byName(sample.state(), sample.direction(), sample.name())
        .orElseThrow(() -> new AssertionError("not in the registry: " + sample));
  }

  private static Packet unsupportedPacket(String name, ConnectionState state, PacketDirection dir) {
    PacketType type = PacketRegistry.byName(state, dir, name).orElseThrow();
    return () -> type;
  }

  @Test
  void theFirstBatchIsTwentyOnePackets() {
    // spec: packages handshake (1), status (4), login (7) and configuration (9) of the module
    assertThat(Samples.all()).hasSize(21);
  }

  @ParameterizedTest
  @MethodSource("samples")
  void aSampleIsTheTypeThatTheSpecTableNames(Sample sample) {
    // spec: R2 (state, direction, name and id of each packet)
    PacketType type = sample.packet().type();
    assertThat(type.state()).isEqualTo(sample.state());
    assertThat(type.direction()).isEqualTo(sample.direction());
    assertThat(type.packetName()).isEqualTo(sample.name());
    assertThat(type).isEqualTo(registered(sample));
  }

  @Test
  void exactlyThePacketsOfTheFirstBatchAreSupported() {
    // spec: API contract of isSupported (first batch only)
    Set<String> supported = new HashSet<>();
    for (Sample sample : Samples.all()) {
      supported.add(key(sample.state(), sample.direction(), sample.name()));
    }
    int count = 0;
    for (PacketRow row : Golden.packets()) {
      PacketType type =
          PacketRegistry.byName(row.state(), row.direction(), row.name()).orElseThrow();
      boolean expected = supported.contains(key(row.state(), row.direction(), row.name()));
      assertThat(PacketCodecs.isSupported(type)).as(row.toString()).isEqualTo(expected);
      count += expected ? 1 : 0;
    }
    assertThat(count).isEqualTo(21);
  }

  @Test
  void packetsOutsideTheBatchAreNotSupported() {
    // spec: API contract of isSupported (cookies, custom payloads and queries, client
    // information, known packs, enabled features and every play packet)
    String[][] outside = {
      {"CONFIGURATION", "CLIENTBOUND", "cookie_request"},
      {"CONFIGURATION", "CLIENTBOUND", "custom_payload"},
      {"CONFIGURATION", "CLIENTBOUND", "update_enabled_features"},
      {"CONFIGURATION", "CLIENTBOUND", "select_known_packs"},
      {"CONFIGURATION", "SERVERBOUND", "client_information"},
      {"CONFIGURATION", "SERVERBOUND", "cookie_response"},
      {"CONFIGURATION", "SERVERBOUND", "custom_payload"},
      {"CONFIGURATION", "SERVERBOUND", "select_known_packs"},
      {"LOGIN", "CLIENTBOUND", "custom_query"},
      {"LOGIN", "CLIENTBOUND", "cookie_request"},
      {"LOGIN", "SERVERBOUND", "custom_query_answer"},
      {"LOGIN", "SERVERBOUND", "cookie_response"},
      {"PLAY", "CLIENTBOUND", "bundle_delimiter"},
      {"PLAY", "CLIENTBOUND", "login"},
      {"PLAY", "SERVERBOUND", "accept_teleportation"}
    };
    for (String[] row : outside) {
      PacketType type =
          PacketRegistry.byName(
                  ConnectionState.valueOf(row[0]), PacketDirection.valueOf(row[1]), row[2])
              .orElseThrow();
      assertThat(PacketCodecs.isSupported(type)).as(String.join(" ", row)).isFalse();
    }
  }

  @ParameterizedTest
  @MethodSource("samples")
  void encodingGivesTheIdOfTheType(Sample sample) throws ProtocolException {
    // spec: R2, API contract of encode ("with the id of its type")
    assertThat(PacketCodecs.encode(sample.packet()).packetId())
        .isEqualTo(sample.packet().type().id());
  }

  @ParameterizedTest
  @MethodSource("samples")
  void decodingTheEncodedFrameGivesAnEqualPacket(Sample sample) throws ProtocolException {
    // spec: R90, test plan L1 (round trip for every packet of the table)
    Frame frame = PacketCodecs.encode(sample.packet());
    Packet decoded = PacketCodecs.decode(sample.state(), sample.direction(), frame);
    assertThat(decoded).isEqualTo(sample.packet());
    assertThat(decoded.type()).isEqualTo(sample.packet().type());
  }

  @ParameterizedTest
  @MethodSource("samples")
  void encodingIsDeterministic(Sample sample) throws ProtocolException {
    // spec: ARCHITECTURE determinism
    assertThat(PacketCodecs.encode(sample.packet()))
        .isEqualTo(PacketCodecs.encode(sample.packet()));
  }

  @ParameterizedTest
  @MethodSource("samples")
  void provisionalBytesAfterTheLastFieldAreRefused(Sample sample) throws ProtocolException {
    // spec: R90 (a decoder that leaves bytes unread signals a protocol error)
    byte[] payload = PacketCodecs.encode(sample.packet()).payload();
    Frame padded = Frame.of(sample.packet().type().id(), Hex.concat(payload, new byte[] {0}));
    assertFails(
        () -> PacketCodecs.decode(sample.state(), sample.direction(), padded),
        Reason.TRAILING_BYTES);
  }

  @ParameterizedTest
  @MethodSource("samples")
  void provisionalAPayloadCutShortIsRefused(Sample sample) throws ProtocolException {
    // spec: R90 (a read past the frame is an error, never a partial packet)
    byte[] payload = PacketCodecs.encode(sample.packet()).payload();
    if (payload.length == 0) {
      return;
    }
    Frame cut =
        Frame.of(sample.packet().type().id(), java.util.Arrays.copyOf(payload, payload.length - 1));
    ProtocolException failure =
        failure(() -> PacketCodecs.decode(sample.state(), sample.direction(), cut));
    assertThat(failure.reason()).isIn(Reason.UNEXPECTED_END, Reason.INVALID_NBT);
  }

  @Test
  void provisionalAnIdOutsideTheTableOfAPairIsUnknownEvenInPlay() {
    // spec: R49 (unknown id), R2 (counts per pair): the id just after the table, and 255
    for (ConnectionState state : ConnectionState.values()) {
      for (PacketDirection direction : PacketDirection.values()) {
        int count = PacketRegistry.all(state, direction).size();
        for (int id : new int[] {count, 255}) {
          Frame beyond = Frame.of(id, new byte[0]);
          assertFails(
              () -> PacketCodecs.decode(state, direction, beyond), Reason.UNKNOWN_PACKET_ID);
        }
      }
    }
  }

  @Test
  void provisionalTheClientboundHandshakeHasNoPacketToDecode() {
    // spec: R2, R34
    assertFails(
        () ->
            PacketCodecs.decode(
                ConnectionState.HANDSHAKE, PacketDirection.CLIENTBOUND, Frame.of(0, new byte[0])),
        Reason.UNKNOWN_PACKET_ID);
  }

  @Test
  void aKnownButUnsupportedPacketIsAnIllegalArgumentNotAProtocolError() {
    // spec: API contract of decode (IllegalArgumentException when the type exists but is not
    // supported)
    for (PacketRow row : Golden.packets()) {
      PacketType type =
          PacketRegistry.byName(row.state(), row.direction(), row.name()).orElseThrow();
      if (PacketCodecs.isSupported(type)) {
        continue;
      }
      Frame frame = Frame.of(row.id(), new byte[0]);
      assertThrows(
          IllegalArgumentException.class,
          () -> PacketCodecs.decode(row.state(), row.direction(), frame));
    }
  }

  @Test
  void encodingAnUnsupportedPacketIsAnIllegalArgument() {
    // spec: API contract of encode
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PacketCodecs.encode(
                unsupportedPacket(
                    "custom_payload", ConnectionState.CONFIGURATION, PacketDirection.CLIENTBOUND)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PacketCodecs.encode(
                unsupportedPacket(
                    "bundle_delimiter", ConnectionState.PLAY, PacketDirection.CLIENTBOUND)));
  }

  @Test
  void theSameBytesDecodeDifferentlyUnderDifferentPairs() throws ProtocolException {
    // spec: R2 (ids are numbered per state and direction)
    byte[] eight = Hex.bytes("00 00 00 00 00 00 00 09");
    Packet clientbound =
        PacketCodecs.decode(
            ConnectionState.CONFIGURATION, PacketDirection.CLIENTBOUND, Frame.of(4, eight));
    Packet serverbound =
        PacketCodecs.decode(
            ConnectionState.CONFIGURATION, PacketDirection.SERVERBOUND, Frame.of(4, eight));
    assertThat(clientbound).isNotEqualTo(serverbound);
    assertThat(clientbound.type().direction()).isEqualTo(PacketDirection.CLIENTBOUND);
    assertThat(serverbound.type().direction()).isEqualTo(PacketDirection.SERVERBOUND);
  }
}

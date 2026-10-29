package dev.shoko.minecraft.net.packet;

import static dev.shoko.minecraft.net.testing.Codecs.assertWire;
import static dev.shoko.minecraft.net.testing.Codecs.decode;
import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.packet.status.PingRequestPacket;
import dev.shoko.minecraft.net.packet.status.PongResponsePacket;
import dev.shoko.minecraft.net.packet.status.StatusRequestPacket;
import dev.shoko.minecraft.net.packet.status.StatusResponsePacket;
import dev.shoko.minecraft.net.registry.StatusClientbound;
import dev.shoko.minecraft.net.registry.StatusServerbound;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Wire;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R37, R38, vectors F02, F03, F16
class StatusCodecTest {

  private static final ConnectionState STATE = ConnectionState.STATUS;
  private static final PacketDirection CLIENTBOUND = PacketDirection.CLIENTBOUND;
  private static final PacketDirection SERVERBOUND = PacketDirection.SERVERBOUND;

  private static final String JSON = "{\"version\":{\"name\":\"26.3\",\"protocol\":777}}";

  @Test
  void statusRequestHasNoFieldAndIdZero() throws ProtocolException {
    // spec: R37, vector F02
    assertWire(STATE, SERVERBOUND, 0, new StatusRequestPacket(), "");
    assertThat(new StatusRequestPacket().type()).isEqualTo(StatusServerbound.STATUS_REQUEST);
  }

  @Test
  void pingRequestIsALongWithIdOne() throws ProtocolException {
    // spec: R37, vector F03
    assertWire(
        STATE,
        SERVERBOUND,
        1,
        new PingRequestPacket(0x0123456789abcdefL),
        "01 23 45 67 89 ab cd ef");
    assertThat(new PingRequestPacket(1).type()).isEqualTo(StatusServerbound.PING_REQUEST);
  }

  @Test
  void pongResponseEchoesTheLongWithIdOne() throws ProtocolException {
    // spec: R37
    assertWire(
        STATE,
        CLIENTBOUND,
        1,
        new PongResponsePacket(0x0123456789abcdefL),
        "01 23 45 67 89 ab cd ef");
    assertThat(new PongResponsePacket(1).type()).isEqualTo(StatusClientbound.PONG_RESPONSE);
  }

  @Test
  void theLongOfAPingIsSignedAndKeepsItsExtremes() throws ProtocolException {
    // spec: R5 (Long), R37
    assertWire(STATE, SERVERBOUND, 1, new PingRequestPacket(-1L), "ff ff ff ff ff ff ff ff");
    assertWire(
        STATE, SERVERBOUND, 1, new PingRequestPacket(Long.MIN_VALUE), "80 00 00 00 00 00 00 00");
    assertWire(
        STATE, CLIENTBOUND, 1, new PongResponsePacket(Long.MAX_VALUE), "7f ff ff ff ff ff ff ff");
  }

  @Test
  void statusResponseIsAStringWithIdZero() throws ProtocolException {
    // spec: R37, R38, vector F16
    StatusResponsePacket packet = new StatusResponsePacket(JSON);
    assertWire(
        STATE,
        CLIENTBOUND,
        0,
        packet,
        "2a 7b 22 76 65 72 73 69 6f 6e 22 3a 7b 22 6e 61 6d 65 22 3a 22 32 36 2e 33 22 2c 22 70 72"
            + " 6f 74 6f 63 6f 6c 22 3a 37 37 37 7d 7d");
    assertThat(packet.type()).isEqualTo(StatusClientbound.STATUS_RESPONSE);
  }

  @Test
  void theJsonKeepsItsUnicodeText() throws ProtocolException {
    // spec: R9, R38 (description is UTF-8 text)
    String json =
        "{\"description\":\"caf"
            + (char) 0xe9
            + " "
            + (char) 0x4e2d
            + " "
            + new String(Character.toChars(0x1F600))
            + "\"}";
    StatusResponsePacket packet = new StatusResponsePacket(json);
    assertThat(decode(STATE, CLIENTBOUND, 0, PacketCodecs.encode(packet).payload()))
        .isEqualTo(packet);
  }

  @Test
  void provisionalAJsonOf32767CharactersIsAcceptedAnd32768IsTooLong() throws ProtocolException {
    // spec: R37 (json String(32767)), R14 (a case at n and a case at n + 1)
    String atLimit = "a".repeat(32767);
    StatusResponsePacket packet = new StatusResponsePacket(atLimit);
    assertThat(decode(STATE, CLIENTBOUND, 0, PacketCodecs.encode(packet).payload()))
        .isEqualTo(packet);
    StatusResponsePacket over = new StatusResponsePacket(atLimit + "a");
    assertFails(() -> PacketCodecs.encode(over), Reason.STRING_TOO_LONG);
    assertFails(
        () -> decode(STATE, CLIENTBOUND, 0, Wire.string(atLimit + "a")), Reason.STRING_TOO_LONG);
  }

  @Test
  void provisionalBytesAfterTheLastFieldAreRefused() {
    // spec: R90
    assertFails(() -> decode(STATE, SERVERBOUND, 0, "00"), Reason.TRAILING_BYTES);
    assertFails(
        () -> decode(STATE, SERVERBOUND, 1, "01 23 45 67 89 ab cd ef 00"), Reason.TRAILING_BYTES);
    assertFails(
        () -> decode(STATE, CLIENTBOUND, 1, "01 23 45 67 89 ab cd ef 00"), Reason.TRAILING_BYTES);
    assertFails(
        () -> decode(STATE, CLIENTBOUND, 0, Hex.concat(Wire.string(JSON), new byte[] {0})),
        Reason.TRAILING_BYTES);
  }

  @Test
  void provisionalTruncatedPayloadsAreAnUnexpectedEnd() {
    // spec: R90
    assertFails(() -> decode(STATE, SERVERBOUND, 1, "01 23 45 67 89 ab cd"), Reason.UNEXPECTED_END);
    assertFails(() -> decode(STATE, SERVERBOUND, 1, ""), Reason.UNEXPECTED_END);
    assertFails(() -> decode(STATE, CLIENTBOUND, 1, "01"), Reason.UNEXPECTED_END);
    assertFails(() -> decode(STATE, CLIENTBOUND, 0, ""), Reason.UNEXPECTED_END);
    assertFails(() -> decode(STATE, CLIENTBOUND, 0, "05 61 62"), Reason.UNEXPECTED_END);
  }

  @Test
  void provisionalAnInvalidUtf8JsonIsRefused() {
    // spec: R9
    assertFails(() -> decode(STATE, CLIENTBOUND, 0, "01 ff"), Reason.INVALID_UTF8);
  }

  @Test
  void provisionalIdsOutsideTheTablesAreUnknown() {
    // spec: R2 (status: 2 clientbound, 2 serverbound), R49
    assertFails(() -> decode(STATE, SERVERBOUND, 2, ""), Reason.UNKNOWN_PACKET_ID);
    assertFails(() -> decode(STATE, CLIENTBOUND, 2, ""), Reason.UNKNOWN_PACKET_ID);
    assertFails(() -> decode(STATE, CLIENTBOUND, -1, ""), Reason.UNKNOWN_PACKET_ID);
  }

  @Test
  void theSameIdInTheOtherDirectionIsAnotherPacket() throws ProtocolException {
    // spec: R2 (ids are numbered separately for each direction)
    assertThat(decode(STATE, SERVERBOUND, 1, "00 00 00 00 00 00 00 05"))
        .isEqualTo(new PingRequestPacket(5));
    assertThat(decode(STATE, CLIENTBOUND, 1, "00 00 00 00 00 00 00 05"))
        .isEqualTo(new PongResponsePacket(5));
  }
}

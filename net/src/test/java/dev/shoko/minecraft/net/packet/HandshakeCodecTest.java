package dev.shoko.minecraft.net.packet;

import static dev.shoko.minecraft.net.testing.Codecs.assertWire;
import static dev.shoko.minecraft.net.testing.Codecs.decode;
import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.frame.Frame;
import dev.shoko.minecraft.net.packet.handshake.IntentionPacket;
import dev.shoko.minecraft.net.registry.HandshakeServerbound;
import dev.shoko.minecraft.net.state.ConnectionIntent;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Wire;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R34 (intention, id 0), R35, R36, vectors F01, F04, I07 to I09
class HandshakeCodecTest {

  private static final ConnectionState STATE = ConnectionState.HANDSHAKE;
  private static final PacketDirection SERVERBOUND = PacketDirection.SERVERBOUND;
  private static final String E_ACUTE = String.valueOf((char) 0xe9);
  private static final String LOCALHOST = "09 6c 6f 63 61 6c 68 6f 73 74";

  private static byte[] payload(int version, String address, int port, int intent) {
    return Hex.concat(
        Wire.varInt(version),
        Wire.string(address),
        new byte[] {(byte) (port >> 8), (byte) port},
        Wire.varInt(intent));
  }

  @Test
  void theStatusHandshakeOfVectorF01() throws ProtocolException {
    // spec: R34, vector F01 (protocol 777, localhost, port 25565, intention 1)
    assertWire(
        STATE,
        SERVERBOUND,
        0,
        new IntentionPacket(777, "localhost", 25565, ConnectionIntent.STATUS),
        "89 06 " + LOCALHOST + " 63 dd 01");
  }

  @Test
  void theLoginHandshakeOfVectorF04() throws ProtocolException {
    // spec: R34, vector F04 (intention 2)
    assertWire(
        STATE,
        SERVERBOUND,
        0,
        new IntentionPacket(777, "localhost", 25565, ConnectionIntent.LOGIN),
        "89 06 " + LOCALHOST + " 63 dd 02");
  }

  @Test
  void theTransferIntentionIsThree() throws ProtocolException {
    // spec: R34, R46
    assertWire(
        STATE,
        SERVERBOUND,
        0,
        new IntentionPacket(777, "localhost", 25565, ConnectionIntent.TRANSFER),
        "89 06 " + LOCALHOST + " 63 dd 03");
  }

  @Test
  void theIntentionPacketTypeIsTheHandshakeIntention() {
    // spec: R2 (handshake, id 0, intention)
    IntentionPacket packet = new IntentionPacket(777, "h", 1, ConnectionIntent.STATUS);
    assertThat(packet.type()).isEqualTo(HandshakeServerbound.INTENTION);
    assertThat(packet.type().id()).isZero();
    assertThat(packet.type().packetName()).isEqualTo("intention");
  }

  @Test
  void provisionalAProtocolVersionOtherThan777IsStillDecoded() throws ProtocolException {
    // spec: R36, vectors I08 (776, intention 2) and I09 (-1, intention 1): the version is not a
    // codec rule, the server answers
    assertThat(decode(STATE, SERVERBOUND, 0, payload(776, "localhost", 25565, 2)))
        .isEqualTo(new IntentionPacket(776, "localhost", 25565, ConnectionIntent.LOGIN));
    assertThat(decode(STATE, SERVERBOUND, 0, payload(-1, "localhost", 25565, 1)))
        .isEqualTo(new IntentionPacket(-1, "localhost", 25565, ConnectionIntent.STATUS));
    assertThat(decode(STATE, SERVERBOUND, 0, "ff ff ff ff 0f " + LOCALHOST + " 63 dd 01"))
        .isEqualTo(new IntentionPacket(-1, "localhost", 25565, ConnectionIntent.STATUS));
  }

  @Test
  void theBordersOfThePortAreKept() throws ProtocolException {
    // spec: R34 (unsigned short), "Cas limites" (port 0 and 65535)
    assertWire(
        STATE,
        SERVERBOUND,
        0,
        new IntentionPacket(777, "localhost", 0, ConnectionIntent.STATUS),
        "89 06 " + LOCALHOST + " 00 00 01");
    assertWire(
        STATE,
        SERVERBOUND,
        0,
        new IntentionPacket(777, "localhost", 65535, ConnectionIntent.STATUS),
        "89 06 " + LOCALHOST + " ff ff 01");
  }

  @Test
  void provisionalAnAddressOf255CharactersIsAcceptedAnd256IsTooLong() throws ProtocolException {
    // spec: R34 (server_address String(255)), R14 (a case at n and a case at n + 1)
    String atLimit = "a".repeat(255);
    IntentionPacket packet = new IntentionPacket(777, atLimit, 25565, ConnectionIntent.LOGIN);
    Frame frame = PacketCodecs.encode(packet);
    assertThat(decode(STATE, SERVERBOUND, 0, frame.payload())).isEqualTo(packet);
    IntentionPacket tooLong =
        new IntentionPacket(777, atLimit + "a", 25565, ConnectionIntent.LOGIN);
    assertFails(() -> PacketCodecs.encode(tooLong), Reason.STRING_TOO_LONG);
    assertFails(
        () -> decode(STATE, SERVERBOUND, 0, payload(777, atLimit + "a", 25565, 2)),
        Reason.STRING_TOO_LONG);
  }

  @Test
  void provisionalTheAddressLimitCountsUtf16UnitsNotBytes() throws ProtocolException {
    // spec: R9, R34 (255 characters of two bytes each are fine)
    String accented = E_ACUTE.repeat(255);
    IntentionPacket packet = new IntentionPacket(777, accented, 25565, ConnectionIntent.STATUS);
    assertThat(decode(STATE, SERVERBOUND, 0, PacketCodecs.encode(packet).payload()))
        .isEqualTo(packet);
    assertFails(
        () -> decode(STATE, SERVERBOUND, 0, payload(777, E_ACUTE.repeat(256), 25565, 1)),
        Reason.STRING_TOO_LONG);
  }

  @Test
  void provisionalAnUnknownIntentionIsRefused() {
    // spec: R35, vector I07 (intention 4)
    for (int intent : new int[] {0, 4, 5, 100, -1}) {
      assertFails(
          () -> decode(STATE, SERVERBOUND, 0, payload(777, "localhost", 25565, intent)),
          Reason.UNKNOWN_INTENTION);
    }
  }

  @Test
  void provisionalEveryTruncatedPayloadIsAnUnexpectedEnd() {
    // spec: R90 (a read past the end of the frame)
    byte[] full = payload(777, "localhost", 25565, 1);
    for (int length = 0; length < full.length; length++) {
      byte[] cut = java.util.Arrays.copyOf(full, length);
      assertFails(() -> decode(STATE, SERVERBOUND, 0, cut), Reason.UNEXPECTED_END);
    }
  }

  @Test
  void provisionalBytesAfterTheLastFieldAreRefused() {
    // spec: R90
    byte[] full = Hex.concat(payload(777, "localhost", 25565, 1), new byte[] {0});
    assertFails(() -> decode(STATE, SERVERBOUND, 0, full), Reason.TRAILING_BYTES);
  }

  @Test
  void provisionalTheHandshakeKnowsOnlyTheIdZeroServerbound() {
    // spec: R34 (the handshake accepts only the intention), R49, vector I06 (id 5)
    byte[] body = payload(777, "localhost", 25565, 1);
    assertFails(() -> decode(STATE, SERVERBOUND, 5, body), Reason.UNKNOWN_PACKET_ID);
    assertFails(() -> decode(STATE, SERVERBOUND, 1, body), Reason.UNKNOWN_PACKET_ID);
    assertFails(
        () -> decode(STATE, PacketDirection.CLIENTBOUND, 0, body), Reason.UNKNOWN_PACKET_ID);
  }

  @Test
  void theSameBytesInAnotherStateAreAnotherPacket() {
    // spec: R2, R90 (id 0 of status serverbound is status_request: its payload is empty)
    byte[] body = payload(777, "localhost", 25565, 1);
    assertFails(() -> decode(ConnectionState.STATUS, SERVERBOUND, 0, body), Reason.TRAILING_BYTES);
  }
}

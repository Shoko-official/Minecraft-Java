package dev.shoko.minecraft.net.packet;

import static dev.shoko.minecraft.net.testing.Codecs.assertWire;
import static dev.shoko.minecraft.net.testing.Codecs.decode;
import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.buffer.ByteBlob;
import dev.shoko.minecraft.net.packet.login.HelloClientboundPacket;
import dev.shoko.minecraft.net.packet.login.HelloServerboundPacket;
import dev.shoko.minecraft.net.packet.login.KeyPacket;
import dev.shoko.minecraft.net.packet.login.LoginAcknowledgedPacket;
import dev.shoko.minecraft.net.packet.login.LoginCompressionPacket;
import dev.shoko.minecraft.net.packet.login.LoginDisconnectPacket;
import dev.shoko.minecraft.net.registry.LoginClientbound;
import dev.shoko.minecraft.net.registry.LoginServerbound;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Samples;
import dev.shoko.minecraft.net.testing.Wire;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R36, R39, R40, R50, R51, R57, R28 (login packets), vectors F05 to F07, I10
class LoginCodecTest {

  private static final ConnectionState STATE = ConnectionState.LOGIN;
  private static final PacketDirection CLIENTBOUND = PacketDirection.CLIENTBOUND;
  private static final PacketDirection SERVERBOUND = PacketDirection.SERVERBOUND;

  @Test
  void theOfflineHelloOfVectorF05() throws ProtocolException {
    // spec: R39, R57, vector F05 (name Notch and the offline UUID of vector U01)
    HelloServerboundPacket packet = new HelloServerboundPacket("Notch", Samples.NOTCH);
    assertWire(
        STATE,
        SERVERBOUND,
        0,
        packet,
        "05 4e 6f 74 63 68 b5 0a d3 85 82 9d 31 41 a2 16 7e 7d 75 39 ba 7f");
    assertThat(packet.type()).isEqualTo(LoginServerbound.HELLO);
  }

  @Test
  void provisionalANameOf16CharactersIsAcceptedAnd17IsTooLong() throws ProtocolException {
    // spec: R39 (name String(16)), R59, vector I10 (17 characters)
    UUID uuid = new UUID(5, 6);
    HelloServerboundPacket atLimit = new HelloServerboundPacket("A".repeat(16), uuid);
    assertThat(decode(STATE, SERVERBOUND, 0, PacketCodecs.encode(atLimit).payload()))
        .isEqualTo(atLimit);
    HelloServerboundPacket over = new HelloServerboundPacket("A".repeat(17), uuid);
    assertFails(() -> PacketCodecs.encode(over), Reason.STRING_TOO_LONG);
    byte[] wire = Hex.concat(Wire.string("A".repeat(17)), Wire.uuid(uuid));
    assertFails(() -> decode(STATE, SERVERBOUND, 0, wire), Reason.STRING_TOO_LONG);
  }

  @Test
  void theUuidOfTheClientIsKeptByTheCodec() throws ProtocolException {
    // spec: R57 (the server ignores it, the codec does not)
    UUID claimed = new UUID(0x1111111111111111L, 0x2222222222222222L);
    HelloServerboundPacket packet = new HelloServerboundPacket("a", claimed);
    assertWire(
        STATE, SERVERBOUND, 0, packet, "01 61 11 11 11 11 11 11 11 11 22 22 22 22 22 22 22 22");
  }

  @Test
  void theEncryptionRequestIsServerIdKeyTokenAndFlag() throws ProtocolException {
    // spec: R40, R50 (empty server id, DER key of 162 bytes, token of 4 bytes, should_authenticate)
    HelloClientboundPacket packet =
        new HelloClientboundPacket(
            "", ByteBlob.of(Hex.ramp(162)), ByteBlob.of(Hex.bytes("01 02 03 04")), true);
    assertThat(PacketCodecs.encode(packet).packetId()).isEqualTo(1);
    String expected = "00 " + Hex.format(Wire.prefixedBytes(Hex.ramp(162))) + " 04 01 02 03 04 01";
    assertWire(STATE, CLIENTBOUND, 1, packet, expected);
    assertThat(packet.type()).isEqualTo(LoginClientbound.HELLO);
  }

  @Test
  void theAuthenticationFlagIsABoolean() throws ProtocolException {
    // spec: R50 (should_authenticate), R5
    HelloClientboundPacket offline =
        new HelloClientboundPacket("", ByteBlob.of(new byte[0]), ByteBlob.of(new byte[0]), false);
    assertWire(STATE, CLIENTBOUND, 1, offline, "00 00 00 00");
  }

  @Test
  void provisionalAServerIdOf20CharactersIsAcceptedAnd21IsTooLong() throws ProtocolException {
    // spec: R50 (server_id String(20)), R14 (a case at n and a case at n + 1)
    HelloClientboundPacket atLimit =
        new HelloClientboundPacket(
            "s".repeat(20), ByteBlob.of(new byte[1]), ByteBlob.of(new byte[1]), true);
    assertThat(decode(STATE, CLIENTBOUND, 1, PacketCodecs.encode(atLimit).payload()))
        .isEqualTo(atLimit);
    HelloClientboundPacket over =
        new HelloClientboundPacket(
            "s".repeat(21), ByteBlob.of(new byte[1]), ByteBlob.of(new byte[1]), true);
    assertFails(() -> PacketCodecs.encode(over), Reason.STRING_TOO_LONG);
  }

  @Test
  void theKeyAnswerIsTwoPrefixedByteArrays() throws ProtocolException {
    // spec: R51 (secret and token, each 128 bytes under RSA-1024)
    KeyPacket packet = new KeyPacket(ByteBlob.of(Hex.ramp(128)), ByteBlob.of(Hex.filled(7, 128)));
    String expected =
        Hex.format(Wire.prefixedBytes(Hex.ramp(128)))
            + " "
            + Hex.format(Wire.prefixedBytes(Hex.filled(7, 128)));
    assertWire(STATE, SERVERBOUND, 1, packet, expected);
    assertThat(packet.type()).isEqualTo(LoginServerbound.KEY);
  }

  @Test
  void loginAcknowledgedHasNoField() throws ProtocolException {
    // spec: R39, vector F06 (id 3)
    assertWire(STATE, SERVERBOUND, 3, new LoginAcknowledgedPacket(), "");
    assertThat(new LoginAcknowledgedPacket().type()).isEqualTo(LoginServerbound.LOGIN_ACKNOWLEDGED);
  }

  @Test
  void theCompressionThresholdIsAVarInt() throws ProtocolException {
    // spec: R28, vector F07 (256 is 80 02), PN3 thresholds -1, 0, 1 and 256
    assertWire(STATE, CLIENTBOUND, 3, new LoginCompressionPacket(256), "80 02");
    assertWire(STATE, CLIENTBOUND, 3, new LoginCompressionPacket(0), "00");
    assertWire(STATE, CLIENTBOUND, 3, new LoginCompressionPacket(1), "01");
    assertWire(STATE, CLIENTBOUND, 3, new LoginCompressionPacket(-1), "ff ff ff ff 0f");
    assertThat(new LoginCompressionPacket(1).type()).isEqualTo(LoginClientbound.LOGIN_COMPRESSION);
  }

  @Test
  void aLoginDisconnectCarriesAJsonTextComponent() throws ProtocolException {
    // spec: R36, R49 (translate component with the key outdated_client)
    String json = "{\"translate\":\"multiplayer.disconnect.outdated_client\",\"with\":[\"26.3\"]}";
    LoginDisconnectPacket packet = new LoginDisconnectPacket(json);
    assertWire(STATE, CLIENTBOUND, 0, packet, Hex.format(Wire.string(json)));
    assertThat(packet.type()).isEqualTo(LoginClientbound.LOGIN_DISCONNECT);
  }

  @Test
  void provisionalBytesAfterTheLastFieldAreRefused() {
    // spec: R90
    assertFails(() -> decode(STATE, SERVERBOUND, 3, "00"), Reason.TRAILING_BYTES);
    assertFails(() -> decode(STATE, CLIENTBOUND, 3, "01 00"), Reason.TRAILING_BYTES);
    assertFails(
        () ->
            decode(STATE, SERVERBOUND, 0, Hex.concat(Wire.string("a"), new byte[16], new byte[1])),
        Reason.TRAILING_BYTES);
    assertFails(() -> decode(STATE, CLIENTBOUND, 1, "00 00 00 00 00"), Reason.TRAILING_BYTES);
  }

  @Test
  void provisionalTruncatedPayloadsAreAnUnexpectedEnd() {
    // spec: R90
    assertFails(() -> decode(STATE, SERVERBOUND, 0, Wire.string("Notch")), Reason.UNEXPECTED_END);
    assertFails(() -> decode(STATE, CLIENTBOUND, 3, "80"), Reason.UNEXPECTED_END);
    assertFails(() -> decode(STATE, CLIENTBOUND, 3, ""), Reason.UNEXPECTED_END);
    assertFails(() -> decode(STATE, CLIENTBOUND, 1, "00 00 00"), Reason.UNEXPECTED_END);
    assertFails(() -> decode(STATE, SERVERBOUND, 1, "05 01 02"), Reason.UNEXPECTED_END);
  }

  @Test
  void theSameIdMeansAnotherPacketInTheOtherDirection() throws ProtocolException {
    // spec: R2 (login clientbound 0 login_disconnect, serverbound 0 hello)
    assertThat(decode(STATE, CLIENTBOUND, 0, Wire.string("{}")))
        .isEqualTo(new LoginDisconnectPacket("{}"));
    assertThat(decode(STATE, SERVERBOUND, 0, Hex.concat(Wire.string("{}"), new byte[16])))
        .isEqualTo(new HelloServerboundPacket("{}", new UUID(0, 0)));
  }

  @Test
  void provisionalIdsOutsideTheTablesAreUnknown() {
    // spec: R2 (login: 6 clientbound, 5 serverbound), R49
    assertFails(() -> decode(STATE, CLIENTBOUND, 6, ""), Reason.UNKNOWN_PACKET_ID);
    assertFails(() -> decode(STATE, SERVERBOUND, 5, ""), Reason.UNKNOWN_PACKET_ID);
    assertFails(() -> decode(STATE, SERVERBOUND, 255, ""), Reason.UNKNOWN_PACKET_ID);
  }
}

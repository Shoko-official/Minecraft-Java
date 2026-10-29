package dev.shoko.minecraft.net.packet;

import static dev.shoko.minecraft.net.testing.Codecs.decode;
import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.frame.Frame;
import dev.shoko.minecraft.net.packet.login.LoginFinishedPacket;
import dev.shoko.minecraft.net.registry.LoginClientbound;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Samples;
import dev.shoko.minecraft.net.testing.Wire;
import dev.shoko.minecraft.net.type.GameProfile;
import dev.shoko.minecraft.net.type.ProfileProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R20 (game profile), R39, R54, R57 (login_finished).
// The bytes after the profile (the session id of the table, to be confirmed) are not asserted: the
// tests check the profile layout, the limits and the round trip only.
class LoginFinishedCodecTest {

  private static final ConnectionState STATE = ConnectionState.LOGIN;
  private static final PacketDirection CLIENTBOUND = PacketDirection.CLIENTBOUND;
  private static final UUID SESSION = new UUID(0x0102030405060708L, 0x090a0b0c0d0e0f10L);

  private static LoginFinishedPacket packet(String name, List<ProfileProperty> properties) {
    return new LoginFinishedPacket(new GameProfile(Samples.NOTCH, name, properties), SESSION);
  }

  private static List<ProfileProperty> unsignedProperties(int count) {
    List<ProfileProperty> properties = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      properties.add(new ProfileProperty.Unsigned("p" + i, "v"));
    }
    return properties;
  }

  private static void assertRoundTrip(LoginFinishedPacket packet) throws ProtocolException {
    Frame frame = PacketCodecs.encode(packet);
    assertThat(frame.packetId()).isEqualTo(2);
    assertThat(decode(STATE, CLIENTBOUND, 2, frame.payload())).isEqualTo(packet);
  }

  @Test
  void anOfflineProfileStartsWithUuidNameAndAnEmptyPropertyArray() throws ProtocolException {
    // spec: R20, R57 (offline: empty properties, "00"), vectors F05 and U01 for the UUID
    Frame frame = PacketCodecs.encode(packet("Notch", List.of()));
    byte[] expectedPrefix =
        Hex.bytes("b5 0a d3 85 82 9d 31 41 a2 16 7e 7d 75 39 ba 7f 05 4e 6f 74 63 68 00");
    assertThat(Hex.format(frame.payload())).startsWith(Hex.format(expectedPrefix));
    assertThat(packet("Notch", List.of()).type()).isEqualTo(LoginClientbound.LOGIN_FINISHED);
  }

  @Test
  void anUnsignedPropertyIsNameValueAndAFalseSignatureFlag() throws ProtocolException {
    // spec: R20 ({name String(64), value String(32767), signature optional String(1024)})
    Frame frame = PacketCodecs.encode(packet("a", List.of(new ProfileProperty.Unsigned("n", "v"))));
    String prefix = Hex.format(Hex.concat(Wire.uuid(Samples.NOTCH), Wire.string("a"))) + " 01 ";
    assertThat(Hex.format(frame.payload())).startsWith(prefix + "01 6e 01 76 00");
  }

  @Test
  void aSignedPropertyHasATrueFlagThenTheSignature() throws ProtocolException {
    // spec: R20
    Frame frame =
        PacketCodecs.encode(packet("a", List.of(new ProfileProperty.Signed("n", "v", "s"))));
    String prefix = Hex.format(Hex.concat(Wire.uuid(Samples.NOTCH), Wire.string("a"))) + " 01 ";
    assertThat(Hex.format(frame.payload())).startsWith(prefix + "01 6e 01 76 01 01 73");
  }

  @Test
  void profilesWithAndWithoutPropertiesRoundTrip() throws ProtocolException {
    // spec: R20, R54
    assertRoundTrip(packet("Notch", List.of()));
    assertRoundTrip(
        packet(
            "Notch",
            List.of(
                new ProfileProperty.Unsigned("a", "b"),
                new ProfileProperty.Signed("textures", "eyJ0ZXh0dXJlcyI6e319", "c2lnbmF0dXJl"))));
    assertRoundTrip(packet("", List.of()));
  }

  @Test
  void provisionalSixteenPropertiesAreAcceptedAndSeventeenAreTooMany() throws ProtocolException {
    // spec: R20 (at most 16 properties), R14 (a case at n and a case at n + 1)
    assertRoundTrip(packet("Notch", unsignedProperties(16)));
    assertFails(
        () -> PacketCodecs.encode(packet("Notch", unsignedProperties(17))), Reason.ARRAY_TOO_LONG);
    byte[] seventeen =
        Hex.concat(Wire.uuid(Samples.NOTCH), Wire.string("Notch"), Wire.varInt(17), new byte[40]);
    assertFails(() -> decode(STATE, CLIENTBOUND, 2, seventeen), Reason.ARRAY_TOO_LONG);
  }

  @Test
  void provisionalANameOf16CharactersIsAcceptedAnd17IsTooLong() throws ProtocolException {
    // spec: R20 (name String(16)), R14
    assertRoundTrip(packet("n".repeat(16), List.of()));
    assertFails(
        () -> PacketCodecs.encode(packet("n".repeat(17), List.of())), Reason.STRING_TOO_LONG);
    byte[] wire = Hex.concat(Wire.uuid(Samples.NOTCH), Wire.string("n".repeat(17)), new byte[1]);
    assertFails(() -> decode(STATE, CLIENTBOUND, 2, wire), Reason.STRING_TOO_LONG);
  }

  @Test
  void provisionalPropertyFieldsHonorTheirMaximums() throws ProtocolException {
    // spec: R20 (name String(64), value String(32767), signature String(1024))
    assertRoundTrip(packet("a", List.of(new ProfileProperty.Unsigned("n".repeat(64), "v"))));
    assertFails(
        () ->
            PacketCodecs.encode(
                packet("a", List.of(new ProfileProperty.Unsigned("n".repeat(65), "v")))),
        Reason.STRING_TOO_LONG);
    assertRoundTrip(packet("a", List.of(new ProfileProperty.Unsigned("n", "v".repeat(32767)))));
    assertFails(
        () ->
            PacketCodecs.encode(
                packet("a", List.of(new ProfileProperty.Unsigned("n", "v".repeat(32768))))),
        Reason.STRING_TOO_LONG);
    assertRoundTrip(packet("a", List.of(new ProfileProperty.Signed("n", "v", "s".repeat(1024)))));
    assertFails(
        () ->
            PacketCodecs.encode(
                packet("a", List.of(new ProfileProperty.Signed("n", "v", "s".repeat(1025))))),
        Reason.STRING_TOO_LONG);
  }

  @Test
  void provisionalBytesAfterTheLastFieldAreRefused() throws ProtocolException {
    // spec: R90
    byte[] payload = PacketCodecs.encode(packet("Notch", List.of())).payload();
    assertFails(
        () -> decode(STATE, CLIENTBOUND, 2, Hex.concat(payload, new byte[] {0})),
        Reason.TRAILING_BYTES);
  }

  @Test
  void provisionalATruncatedPayloadIsAnUnexpectedEnd() throws ProtocolException {
    // spec: R90
    byte[] payload = PacketCodecs.encode(packet("Notch", List.of())).payload();
    for (int length = 0; length < payload.length; length++) {
      byte[] cut = java.util.Arrays.copyOf(payload, length);
      assertFails(() -> decode(STATE, CLIENTBOUND, 2, cut), Reason.UNEXPECTED_END);
    }
  }
}

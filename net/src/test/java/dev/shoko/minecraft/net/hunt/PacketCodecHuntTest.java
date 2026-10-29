package dev.shoko.minecraft.net.hunt;

import static dev.shoko.minecraft.net.testing.Codecs.decode;
import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static dev.shoko.minecraft.net.testing.Failures.failure;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.frame.Frame;
import dev.shoko.minecraft.net.packet.PacketCodecs;
import dev.shoko.minecraft.net.packet.handshake.IntentionPacket;
import dev.shoko.minecraft.net.packet.login.LoginDisconnectPacket;
import dev.shoko.minecraft.net.registry.PacketRegistry;
import dev.shoko.minecraft.net.registry.PacketType;
import dev.shoko.minecraft.net.state.ConnectionIntent;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import dev.shoko.minecraft.net.testing.Golden;
import dev.shoko.minecraft.net.testing.Golden.PacketRow;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Wire;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// spec: net-protocol-777 R34 (port is an unsigned short), R49 and R90 (what comes from the network
// is refused with a typed error), R17 (JSON component of 262144), R91 (volume)
class PacketCodecHuntTest {

  /** Generous cap on the elements of one array of a clientbound registry or tag packet. */
  private static final int ELEMENT_CAP = 1_000_000;

  @ParameterizedTest
  @ValueSource(ints = {65536, -1, 70000, Integer.MIN_VALUE, Integer.MAX_VALUE})
  void provisionalAPortOutsideTheUnsignedShortIsNeverReadBackAsAnotherPort(int port)
      throws ProtocolException {
    // spec: R34 (server_port:UShort). Either the record refuses the value, or the encoder refuses
    // it with VALUE_OUT_OF_RANGE; what is written must never read back as a different port.
    IntentionPacket packet;
    try {
      packet = new IntentionPacket(777, "localhost", port, ConnectionIntent.STATUS);
    } catch (IllegalArgumentException refusedByTheRecord) {
      return;
    }
    Frame frame;
    try {
      frame = PacketCodecs.encode(packet);
    } catch (ProtocolException refusedByTheEncoder) {
      assertThat(refusedByTheEncoder.reason()).isEqualTo(Reason.VALUE_OUT_OF_RANGE);
      return;
    }
    assertThat(PacketCodecs.decode(ConnectionState.HANDSHAKE, PacketDirection.SERVERBOUND, frame))
        .as("port %d written and read back", port)
        .isEqualTo(packet);
  }

  @Test
  void theBordersOfThePortStayValid() throws ProtocolException {
    // spec: R34
    for (int port : new int[] {0, 1, 25565, 65535}) {
      IntentionPacket packet = new IntentionPacket(777, "h", port, ConnectionIntent.LOGIN);
      assertThat(
              PacketCodecs.decode(
                  ConnectionState.HANDSHAKE,
                  PacketDirection.SERVERBOUND,
                  PacketCodecs.encode(packet)))
          .isEqualTo(packet);
    }
  }

  @Test
  void provisionalAKnownButUnsupportedPacketFromTheNetworkIsAProtocolError() {
    // spec: R49, R90. The frame comes from the network: its id is valid but the packet is not
    // handled in this batch. The answer must be a typed ProtocolException (any reason), not the
    // programming error IllegalArgumentException. isSupported stays the exact test.
    int checked = 0;
    for (PacketRow row : Golden.packets()) {
      PacketType type =
          PacketRegistry.byName(row.state(), row.direction(), row.name()).orElseThrow();
      Frame frame = Frame.of(row.id(), new byte[0]);
      if (PacketCodecs.isSupported(type)) {
        continue;
      }
      checked++;
      ProtocolException refused =
          failure(() -> PacketCodecs.decode(row.state(), row.direction(), frame));
      assertThat(refused.reason()).as(row.toString()).isNotNull();
    }
    assertThat(checked).isEqualTo(260 - 21);
  }

  private static byte[] repeat(byte[] unit, int times) {
    ByteArrayOutputStream out = new ByteArrayOutputStream(unit.length * times);
    for (int i = 0; i < times; i++) {
      out.writeBytes(unit);
    }
    return out.toByteArray();
  }

  private static void assertRefused(int packetId, byte[] payload) {
    Reason reason =
        failure(
                () ->
                    decode(
                        ConnectionState.CONFIGURATION,
                        PacketDirection.CLIENTBOUND,
                        packetId,
                        payload))
            .reason();
    assertThat(reason).isNotNull();
  }

  @Test
  @Timeout(30)
  void provisionalAnUpdateTagsAnnouncingMoreThanOneMillionTagsIsRefused() {
    // spec: R90, R91. The cap (1 000 000 elements per array) is a project choice to confirm.
    byte[] tag = Hex.concat(Wire.string("a:b"), Wire.varInt(0));
    byte[] payload =
        Hex.concat(
            Wire.varInt(1),
            Wire.string("minecraft:block"),
            Wire.varInt(ELEMENT_CAP + 1),
            repeat(tag, ELEMENT_CAP + 1));
    assertRefused(14, payload);
  }

  @Test
  @Timeout(30)
  void provisionalATagWithMoreThanOneMillionMembersIsRefused() {
    // spec: R90, R91 (same cap, to confirm)
    byte[] payload =
        Hex.concat(
            Wire.varInt(1),
            Wire.string("minecraft:block"),
            Wire.varInt(1),
            Wire.string("minecraft:a"),
            Wire.varInt(ELEMENT_CAP + 1),
            new byte[ELEMENT_CAP + 1]);
    assertRefused(14, payload);
  }

  @Test
  @Timeout(30)
  void provisionalARegistryAnnouncingMoreThanOneMillionEntriesIsRefused() {
    // spec: R90, R91 (same cap, to confirm)
    byte[] entry = Hex.concat(Wire.string("a:b"), new byte[] {0});
    byte[] payload =
        Hex.concat(
            Wire.string("minecraft:x"),
            Wire.varInt(ELEMENT_CAP + 1),
            repeat(entry, ELEMENT_CAP + 1));
    assertRefused(7, payload);
  }

  @Test
  @Timeout(30)
  void aTagArrayOfAFewThousandElementsIsStillAccepted() throws ProtocolException {
    // spec: R61 (35 723 block states): the cap must not touch real data
    byte[] tag = Hex.concat(Wire.string("a:b"), Wire.varInt(0));
    byte[] payload =
        Hex.concat(
            Wire.varInt(1),
            Wire.string("minecraft:block"),
            Wire.varInt(40_000),
            repeat(tag, 40_000));
    assertThat(
            decode(ConnectionState.CONFIGURATION, PacketDirection.CLIENTBOUND, 14, payload)
                .type()
                .id())
        .isEqualTo(14);
  }

  @Test
  void provisionalALoginDisconnectJsonOf262144CharactersIsAcceptedAnd262145IsRefused()
      throws ProtocolException {
    // spec: R17 (the JSON component is a String of at most 262144 characters), a case at n and a
    // case at n + 1 (R14)
    String atLimit = "a".repeat(262_144);
    LoginDisconnectPacket packet = new LoginDisconnectPacket(atLimit);
    Frame frame = PacketCodecs.encode(packet);
    assertThat(decode(ConnectionState.LOGIN, PacketDirection.CLIENTBOUND, 0, frame.payload()))
        .isEqualTo(packet);
    assertFails(
        () -> PacketCodecs.encode(new LoginDisconnectPacket(atLimit + "a")),
        Reason.STRING_TOO_LONG);
    byte[] over = Wire.string(atLimit + "a");
    assertFails(
        () -> decode(ConnectionState.LOGIN, PacketDirection.CLIENTBOUND, 0, over),
        Reason.STRING_TOO_LONG);
  }
}

package dev.shoko.minecraft.net.packet;

import static dev.shoko.minecraft.net.testing.Codecs.assertWire;
import static dev.shoko.minecraft.net.testing.Codecs.decode;
import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static dev.shoko.minecraft.net.testing.Failures.failure;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.packet.configuration.DisconnectPacket;
import dev.shoko.minecraft.net.packet.configuration.FinishConfigurationClientboundPacket;
import dev.shoko.minecraft.net.packet.configuration.FinishConfigurationServerboundPacket;
import dev.shoko.minecraft.net.packet.configuration.KeepAliveClientboundPacket;
import dev.shoko.minecraft.net.packet.configuration.KeepAliveServerboundPacket;
import dev.shoko.minecraft.net.packet.configuration.PingPacket;
import dev.shoko.minecraft.net.packet.configuration.PongPacket;
import dev.shoko.minecraft.net.registry.ConfigurationClientbound;
import dev.shoko.minecraft.net.registry.ConfigurationServerbound;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R43, R47, R48, R49, R93 (configuration packets other than the registries),
// vectors F08, F15
class ConfigurationCodecTest {

  private static final ConnectionState STATE = ConnectionState.CONFIGURATION;
  private static final PacketDirection CLIENTBOUND = PacketDirection.CLIENTBOUND;
  private static final PacketDirection SERVERBOUND = PacketDirection.SERVERBOUND;

  @Test
  void finishConfigurationHasNoFieldAndIdThreeInBothDirections() throws ProtocolException {
    // spec: R93, vector F08
    assertWire(STATE, CLIENTBOUND, 3, new FinishConfigurationClientboundPacket(), "");
    assertWire(STATE, SERVERBOUND, 3, new FinishConfigurationServerboundPacket(), "");
    assertThat(new FinishConfigurationClientboundPacket().type())
        .isEqualTo(ConfigurationClientbound.FINISH_CONFIGURATION);
    assertThat(new FinishConfigurationServerboundPacket().type())
        .isEqualTo(ConfigurationServerbound.FINISH_CONFIGURATION);
  }

  @Test
  void keepAliveIsALongWithIdFourInBothDirections() throws ProtocolException {
    // spec: R47, vector F15 (0x1122334455667788)
    assertWire(
        STATE,
        CLIENTBOUND,
        4,
        new KeepAliveClientboundPacket(0x1122334455667788L),
        "11 22 33 44 55 66 77 88");
    assertWire(
        STATE,
        SERVERBOUND,
        4,
        new KeepAliveServerboundPacket(0x1122334455667788L),
        "11 22 33 44 55 66 77 88");
    assertThat(new KeepAliveClientboundPacket(1).type())
        .isEqualTo(ConfigurationClientbound.KEEP_ALIVE);
    assertThat(new KeepAliveServerboundPacket(1).type())
        .isEqualTo(ConfigurationServerbound.KEEP_ALIVE);
  }

  @Test
  void keepAliveKeepsNegativeValues() throws ProtocolException {
    // spec: R47, R5 (Long)
    assertWire(
        STATE, CLIENTBOUND, 4, new KeepAliveClientboundPacket(-1L), "ff ff ff ff ff ff ff ff");
  }

  @Test
  void pingAndPongAreAnIntWithIdFive() throws ProtocolException {
    // spec: R48 (Int), tables of the section "Paquets"
    assertWire(STATE, CLIENTBOUND, 5, new PingPacket(0x01020304), "01 02 03 04");
    assertWire(STATE, SERVERBOUND, 5, new PongPacket(0x01020304), "01 02 03 04");
    assertWire(STATE, CLIENTBOUND, 5, new PingPacket(-2), "ff ff ff fe");
    assertThat(new PingPacket(1).type()).isEqualTo(ConfigurationClientbound.PING);
    assertThat(new PongPacket(1).type()).isEqualTo(ConfigurationServerbound.PONG);
  }

  @Test
  void aPlainTextDisconnectIsAStringTagWithoutRootName() throws ProtocolException {
    // spec: R49, R17 (a String tag for plain text)
    assertWire(
        STATE, CLIENTBOUND, 2, new DisconnectPacket(new StringTag("bye")), "08 00 03 62 79 65");
    assertThat(new DisconnectPacket(new StringTag("x")).type())
        .isEqualTo(ConfigurationClientbound.DISCONNECT);
  }

  @Test
  void aStyledDisconnectIsACompound() throws ProtocolException {
    // spec: R49, R17 (a Compound for a styled component)
    SequencedMap<String, StringTag> entries = new LinkedHashMap<>();
    entries.put("translate", new StringTag("multiplayer.disconnect.kicked"));
    NbtTag reason = CompoundTag.of(entries);
    DisconnectPacket packet = new DisconnectPacket(reason);
    assertThat(decode(STATE, CLIENTBOUND, 2, PacketCodecs.encode(packet).payload()))
        .isEqualTo(packet);
    assertThat(PacketCodecs.encode(packet).payload()[0]).isEqualTo((byte) 0x0a);
  }

  @Test
  void provisionalADisconnectWithAnAbsentReasonIsRefused() {
    // spec: R17 (the End byte is the absence of a value, a disconnect needs a component)
    assertThat(failure(() -> decode(STATE, CLIENTBOUND, 2, "00")).reason())
        .isEqualTo(Reason.INVALID_NBT);
  }

  @Test
  void provisionalBytesAfterTheLastFieldAreRefused() {
    // spec: R90
    assertFails(() -> decode(STATE, CLIENTBOUND, 3, "00"), Reason.TRAILING_BYTES);
    assertFails(() -> decode(STATE, SERVERBOUND, 3, "00"), Reason.TRAILING_BYTES);
    assertFails(
        () -> decode(STATE, CLIENTBOUND, 4, "00 00 00 00 00 00 00 01 02"), Reason.TRAILING_BYTES);
    assertFails(() -> decode(STATE, SERVERBOUND, 5, "00 00 00 01 02"), Reason.TRAILING_BYTES);
    assertFails(() -> decode(STATE, CLIENTBOUND, 2, "08 00 03 62 79 65 00"), Reason.TRAILING_BYTES);
  }

  @Test
  void provisionalTruncatedPayloadsAreAnUnexpectedEnd() {
    // spec: R90
    assertFails(() -> decode(STATE, CLIENTBOUND, 4, "00 00 00 00 00 00 00"), Reason.UNEXPECTED_END);
    assertFails(() -> decode(STATE, SERVERBOUND, 4, ""), Reason.UNEXPECTED_END);
    assertFails(() -> decode(STATE, CLIENTBOUND, 5, "00 00 00"), Reason.UNEXPECTED_END);
    assertFails(() -> decode(STATE, SERVERBOUND, 5, ""), Reason.UNEXPECTED_END);
  }

  @Test
  void theSameIdInTheOtherDirectionIsAnotherPacket() throws ProtocolException {
    // spec: R2 (configuration 21 clientbound, 10 serverbound: id 5 is ping then pong)
    assertThat(decode(STATE, CLIENTBOUND, 5, "00 00 00 07")).isEqualTo(new PingPacket(7));
    assertThat(decode(STATE, SERVERBOUND, 5, "00 00 00 07")).isEqualTo(new PongPacket(7));
    assertThat(decode(STATE, CLIENTBOUND, 4, "00 00 00 00 00 00 00 07"))
        .isEqualTo(new KeepAliveClientboundPacket(7));
    assertThat(decode(STATE, SERVERBOUND, 4, "00 00 00 00 00 00 00 07"))
        .isEqualTo(new KeepAliveServerboundPacket(7));
  }

  @Test
  void provisionalIdsOutsideTheTablesAreUnknown() {
    // spec: R2 (configuration: 21 clientbound, 10 serverbound), R49
    assertFails(() -> decode(STATE, CLIENTBOUND, 21, ""), Reason.UNKNOWN_PACKET_ID);
    assertFails(() -> decode(STATE, SERVERBOUND, 10, ""), Reason.UNKNOWN_PACKET_ID);
    assertFails(() -> decode(STATE, SERVERBOUND, 200, ""), Reason.UNKNOWN_PACKET_ID);
  }
}

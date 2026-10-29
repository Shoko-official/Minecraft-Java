package dev.shoko.minecraft.net.state;

import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.buffer.ByteBlob;
import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.packet.configuration.DisconnectPacket;
import dev.shoko.minecraft.net.packet.configuration.FinishConfigurationClientboundPacket;
import dev.shoko.minecraft.net.packet.configuration.FinishConfigurationServerboundPacket;
import dev.shoko.minecraft.net.packet.configuration.KeepAliveClientboundPacket;
import dev.shoko.minecraft.net.packet.configuration.KeepAliveServerboundPacket;
import dev.shoko.minecraft.net.packet.configuration.PingPacket;
import dev.shoko.minecraft.net.packet.configuration.PongPacket;
import dev.shoko.minecraft.net.packet.handshake.IntentionPacket;
import dev.shoko.minecraft.net.packet.login.HelloClientboundPacket;
import dev.shoko.minecraft.net.packet.login.HelloServerboundPacket;
import dev.shoko.minecraft.net.packet.login.KeyPacket;
import dev.shoko.minecraft.net.packet.login.LoginAcknowledgedPacket;
import dev.shoko.minecraft.net.packet.login.LoginCompressionPacket;
import dev.shoko.minecraft.net.packet.login.LoginFinishedPacket;
import dev.shoko.minecraft.net.packet.status.PingRequestPacket;
import dev.shoko.minecraft.net.packet.status.PongResponsePacket;
import dev.shoko.minecraft.net.packet.status.StatusRequestPacket;
import dev.shoko.minecraft.net.packet.status.StatusResponsePacket;
import dev.shoko.minecraft.net.testing.Samples;
import dev.shoko.minecraft.net.testing.Samples.Sample;
import dev.shoko.minecraft.net.type.GameProfile;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R34, R37, R39, R45, R49, R93 (states and transitions)
class ConnectionStateMachineTest {

  private static ConnectionStateMachine inLogin() throws ProtocolException {
    ConnectionStateMachine machine = ConnectionStateMachine.start();
    machine.advance(new IntentionPacket(777, "localhost", 25565, ConnectionIntent.LOGIN));
    return machine;
  }

  private static ConnectionStateMachine inConfiguration() throws ProtocolException {
    ConnectionStateMachine machine = inLogin();
    machine.advance(new LoginAcknowledgedPacket());
    return machine;
  }

  private static ConnectionStateMachine inPlay() throws ProtocolException {
    ConnectionStateMachine machine = inConfiguration();
    machine.advance(new FinishConfigurationServerboundPacket());
    return machine;
  }

  private static ConnectionStateMachine inStatus() throws ProtocolException {
    ConnectionStateMachine machine = ConnectionStateMachine.start();
    machine.advance(new IntentionPacket(777, "localhost", 25565, ConnectionIntent.STATUS));
    return machine;
  }

  @Test
  void aNewConnectionStartsInHandshake() {
    // spec: R34
    assertThat(ConnectionStateMachine.start().state()).isEqualTo(ConnectionState.HANDSHAKE);
  }

  @Test
  void theIntentionSelectsTheNextState() throws ProtocolException {
    // spec: R34 (1 status; 2 and 3 login), vectors F01, F04
    for (ConnectionIntent intent : ConnectionIntent.values()) {
      ConnectionStateMachine machine = ConnectionStateMachine.start();
      ConnectionState after = machine.advance(new IntentionPacket(777, "h", 1, intent));
      assertThat(after).isEqualTo(intent.nextState());
      assertThat(machine.state()).isEqualTo(intent.nextState());
    }
  }

  @Test
  void theIntentionIsAcceptedWhateverTheProtocolVersion() throws ProtocolException {
    // spec: R36 (a status ping with protocol -1 or 776 is answered; the version is not a state
    // rule)
    for (int version : new int[] {-1, 776, 778}) {
      ConnectionStateMachine machine = ConnectionStateMachine.start();
      machine.advance(new IntentionPacket(version, "localhost", 25565, ConnectionIntent.STATUS));
      assertThat(machine.state()).isEqualTo(ConnectionState.STATUS);
    }
  }

  @Test
  void statusPacketsInBothDirectionsKeepTheStatusState() throws ProtocolException {
    // spec: R37
    ConnectionStateMachine machine = inStatus();
    List<Packet> packets =
        List.of(
            new StatusRequestPacket(),
            new StatusResponsePacket("{}"),
            new PingRequestPacket(1),
            new PongResponsePacket(1));
    for (Packet packet : packets) {
      assertThat(machine.advance(packet)).isEqualTo(ConnectionState.STATUS);
    }
    assertThat(machine.state()).isEqualTo(ConnectionState.STATUS);
  }

  @Test
  void loginPacketsKeepTheLoginStateUntilTheAcknowledgement() throws ProtocolException {
    // spec: R39, R40
    ConnectionStateMachine machine = inLogin();
    UUID uuid = new UUID(1, 2);
    List<Packet> packets =
        List.of(
            new HelloServerboundPacket("Notch", uuid),
            new HelloClientboundPacket(
                "", ByteBlob.of(new byte[162]), ByteBlob.of(new byte[4]), true),
            new KeyPacket(ByteBlob.of(new byte[128]), ByteBlob.of(new byte[128])),
            new LoginCompressionPacket(256),
            new LoginFinishedPacket(new GameProfile(uuid, "Notch", List.of()), uuid));
    for (Packet packet : packets) {
      assertThat(machine.advance(packet)).isEqualTo(ConnectionState.LOGIN);
    }
  }

  @Test
  void theLoginAcknowledgementEntersConfiguration() throws ProtocolException {
    // spec: R39, vector F06
    ConnectionStateMachine machine = inLogin();
    assertThat(machine.advance(new LoginAcknowledgedPacket()))
        .isEqualTo(ConnectionState.CONFIGURATION);
    assertThat(machine.state()).isEqualTo(ConnectionState.CONFIGURATION);
  }

  @Test
  void configurationPacketsKeepTheConfigurationState() throws ProtocolException {
    // spec: R43
    ConnectionStateMachine machine = inConfiguration();
    List<Packet> packets =
        List.of(
            new KeepAliveClientboundPacket(1),
            new KeepAliveServerboundPacket(1),
            new PingPacket(1),
            new PongPacket(1),
            Samples.registryData(),
            Samples.updateTags());
    for (Packet packet : packets) {
      assertThat(machine.advance(packet)).isEqualTo(ConnectionState.CONFIGURATION);
    }
  }

  @Test
  void theClientboundFinishConfigurationDoesNotLeaveConfiguration() throws ProtocolException {
    // spec: R93 (the server enters play at the acknowledgement, and not before)
    ConnectionStateMachine machine = inConfiguration();
    assertThat(machine.advance(new FinishConfigurationClientboundPacket()))
        .isEqualTo(ConnectionState.CONFIGURATION);
    assertThat(machine.state()).isEqualTo(ConnectionState.CONFIGURATION);
  }

  @Test
  void theServerboundFinishConfigurationEntersPlay() throws ProtocolException {
    // spec: R93
    ConnectionStateMachine machine = inConfiguration();
    machine.advance(new FinishConfigurationClientboundPacket());
    assertThat(machine.advance(new FinishConfigurationServerboundPacket()))
        .isEqualTo(ConnectionState.PLAY);
    assertThat(machine.state()).isEqualTo(ConnectionState.PLAY);
  }

  @Test
  void aWholeOfflineSessionGoesThroughTheFiveStates() throws ProtocolException {
    // spec: R34, R39, R57, R93 (handshake, hello, compression, finished, acknowledged,
    // configuration)
    ConnectionStateMachine machine = ConnectionStateMachine.start();
    UUID uuid = new UUID(3, 4);
    machine.advance(new IntentionPacket(777, "localhost", 25565, ConnectionIntent.LOGIN));
    machine.advance(new HelloServerboundPacket("Notch", uuid));
    machine.advance(new LoginCompressionPacket(256));
    machine.advance(new LoginFinishedPacket(new GameProfile(uuid, "Notch", List.of()), uuid));
    assertThat(machine.state()).isEqualTo(ConnectionState.LOGIN);
    machine.advance(new LoginAcknowledgedPacket());
    machine.advance(new FinishConfigurationClientboundPacket());
    machine.advance(new FinishConfigurationServerboundPacket());
    assertThat(machine.state()).isEqualTo(ConnectionState.PLAY);
  }

  @Test
  void provisionalASecondIntentionIsNotAllowed() throws ProtocolException {
    // spec: R34 (handshake accepts only the intention), R49
    for (ConnectionStateMachine machine :
        List.of(inStatus(), inLogin(), inConfiguration(), inPlay())) {
      ConnectionState before = machine.state();
      assertFails(
          () -> machine.advance(new IntentionPacket(777, "h", 1, ConnectionIntent.LOGIN)),
          Reason.PACKET_NOT_ALLOWED);
      assertThat(machine.state()).isEqualTo(before);
    }
  }

  @Test
  void provisionalOnlyTheIntentionIsAllowedInHandshake() {
    // spec: R34, R49
    for (Sample sample : Samples.all()) {
      if (sample.state() == ConnectionState.HANDSHAKE) {
        continue;
      }
      ConnectionStateMachine machine = ConnectionStateMachine.start();
      assertFails(() -> machine.advance(sample.packet()), Reason.PACKET_NOT_ALLOWED);
      assertThat(machine.state()).as(sample.toString()).isEqualTo(ConnectionState.HANDSHAKE);
    }
  }

  @Test
  void provisionalAPacketOfAnotherStateIsNotAllowedAndLeavesTheStateUnchanged()
      throws ProtocolException {
    // spec: R49 (a packet received in a wrong state), API contract of advance
    List<ConnectionStateMachine> machines =
        List.of(inStatus(), inLogin(), inConfiguration(), inPlay());
    for (ConnectionStateMachine machine : machines) {
      ConnectionState current = machine.state();
      for (Sample sample : Samples.all()) {
        if (sample.state() == current) {
          continue;
        }
        assertFails(() -> machine.advance(sample.packet()), Reason.PACKET_NOT_ALLOWED);
        assertThat(machine.state()).as("%s in %s", sample, current).isEqualTo(current);
      }
    }
  }

  @Test
  void provisionalAFailedPacketDoesNotPreventTheNextValidOne() throws ProtocolException {
    // spec: API contract of advance (the state is then unchanged)
    ConnectionStateMachine machine = inLogin();
    assertFails(
        () -> machine.advance(new KeepAliveServerboundPacket(1)), Reason.PACKET_NOT_ALLOWED);
    assertThat(machine.advance(new LoginAcknowledgedPacket()))
        .isEqualTo(ConnectionState.CONFIGURATION);
  }

  @Test
  void theDisconnectOfConfigurationIsAllowedAndKeepsTheState() throws ProtocolException {
    // spec: R49 (disconnect in configuration)
    ConnectionStateMachine machine = inConfiguration();
    assertThat(machine.advance(new DisconnectPacket(new StringTag("bye"))))
        .isEqualTo(ConnectionState.CONFIGURATION);
  }
}

package dev.shoko.minecraft.net.hunt;

import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.packet.configuration.FinishConfigurationClientboundPacket;
import dev.shoko.minecraft.net.packet.configuration.FinishConfigurationServerboundPacket;
import dev.shoko.minecraft.net.packet.handshake.IntentionPacket;
import dev.shoko.minecraft.net.packet.login.HelloServerboundPacket;
import dev.shoko.minecraft.net.packet.login.LoginAcknowledgedPacket;
import dev.shoko.minecraft.net.packet.login.LoginCompressionPacket;
import dev.shoko.minecraft.net.packet.login.LoginFinishedPacket;
import dev.shoko.minecraft.net.registry.PlayClientbound;
import dev.shoko.minecraft.net.registry.PlayServerbound;
import dev.shoko.minecraft.net.state.ConnectionIntent;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.ConnectionStateMachine;
import dev.shoko.minecraft.net.testing.Samples;
import dev.shoko.minecraft.net.type.GameProfile;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R39 (hello, login_compression, login_finished, then login_acknowledged),
// R45 (start_configuration, then configuration_acknowledged), R49 (a packet in the wrong state is
// refused), R93 (finish_configuration clientbound, then its acknowledgement). A packet that is
// valid in a state but not at this point of the sequence is refused, with the state unchanged.
//
// CONFLICT with ConnectionStateMachineTest: its helpers inConfiguration() and inPlay() reach
// CONFIGURATION and PLAY without login_finished and without the clientbound finish_configuration.
// The tests here are provisional: the sequence rules are not stated as a typed failure by the spec.
class StateSequenceHuntTest {

  private static final Packet START_CONFIGURATION = () -> PlayClientbound.START_CONFIGURATION;
  private static final Packet CONFIGURATION_ACKNOWLEDGED =
      () -> PlayServerbound.CONFIGURATION_ACKNOWLEDGED;

  private static ConnectionStateMachine inLogin() throws ProtocolException {
    ConnectionStateMachine machine = ConnectionStateMachine.start();
    machine.advance(new IntentionPacket(777, "localhost", 25565, ConnectionIntent.LOGIN));
    return machine;
  }

  private static ConnectionStateMachine inConfiguration() throws ProtocolException {
    ConnectionStateMachine machine = inLogin();
    machine.advance(new HelloServerboundPacket("Notch", Samples.NOTCH));
    machine.advance(new LoginCompressionPacket(256));
    machine.advance(
        new LoginFinishedPacket(new GameProfile(Samples.NOTCH, "Notch", List.of()), Samples.NOTCH));
    machine.advance(new LoginAcknowledgedPacket());
    return machine;
  }

  private static ConnectionStateMachine inPlay() throws ProtocolException {
    ConnectionStateMachine machine = inConfiguration();
    machine.advance(new FinishConfigurationClientboundPacket());
    machine.advance(new FinishConfigurationServerboundPacket());
    return machine;
  }

  @Test
  void provisionalLoginAcknowledgedRightAfterTheHandshakeIsRefused() throws ProtocolException {
    // spec: R39, R49
    ConnectionStateMachine machine = inLogin();
    assertFails(() -> machine.advance(new LoginAcknowledgedPacket()), Reason.PACKET_NOT_ALLOWED);
    assertThat(machine.state()).isEqualTo(ConnectionState.LOGIN);
  }

  @Test
  void provisionalLoginAcknowledgedBeforeLoginFinishedIsRefused() throws ProtocolException {
    // spec: R39 (the client answers login_finished), R49
    ConnectionStateMachine machine = inLogin();
    machine.advance(new HelloServerboundPacket("Notch", Samples.NOTCH));
    machine.advance(new LoginCompressionPacket(256));
    assertFails(() -> machine.advance(new LoginAcknowledgedPacket()), Reason.PACKET_NOT_ALLOWED);
    assertThat(machine.state()).isEqualTo(ConnectionState.LOGIN);
  }

  @Test
  void loginAcknowledgedAfterLoginFinishedEntersConfiguration() throws ProtocolException {
    // spec: R39
    assertThat(inConfiguration().state()).isEqualTo(ConnectionState.CONFIGURATION);
  }

  @Test
  void provisionalAnUnsolicitedFinishConfigurationAcknowledgementIsRefused()
      throws ProtocolException {
    // spec: R93 (the acknowledgement answers the clientbound finish_configuration), R49
    ConnectionStateMachine machine = inConfiguration();
    assertFails(
        () -> machine.advance(new FinishConfigurationServerboundPacket()),
        Reason.PACKET_NOT_ALLOWED);
    assertThat(machine.state()).isEqualTo(ConnectionState.CONFIGURATION);
  }

  @Test
  void theFinishConfigurationAcknowledgementAfterTheServerOneEntersPlay() throws ProtocolException {
    // spec: R93
    assertThat(inPlay().state()).isEqualTo(ConnectionState.PLAY);
  }

  @Test
  void provisionalConfigurationAcknowledgedWithoutStartConfigurationIsRefused()
      throws ProtocolException {
    // spec: R45 (start_configuration, then the client answers), R49
    ConnectionStateMachine machine = inPlay();
    assertFails(() -> machine.advance(CONFIGURATION_ACKNOWLEDGED), Reason.PACKET_NOT_ALLOWED);
    assertThat(machine.state()).isEqualTo(ConnectionState.PLAY);
  }

  @Test
  void startConfigurationThenItsAcknowledgementReturnsToConfiguration() throws ProtocolException {
    // spec: R45 (play to configuration with configuration_acknowledged)
    ConnectionStateMachine machine = inPlay();
    assertThat(machine.advance(START_CONFIGURATION)).isEqualTo(ConnectionState.PLAY);
    assertThat(machine.advance(CONFIGURATION_ACKNOWLEDGED))
        .isEqualTo(ConnectionState.CONFIGURATION);
    assertThat(machine.state()).isEqualTo(ConnectionState.CONFIGURATION);
  }

  @Test
  void provisionalAReconfigurationNeedsTheFullSequenceAgain() throws ProtocolException {
    // spec: R45, R93
    ConnectionStateMachine machine = inPlay();
    machine.advance(START_CONFIGURATION);
    machine.advance(CONFIGURATION_ACKNOWLEDGED);
    assertFails(
        () -> machine.advance(new FinishConfigurationServerboundPacket()),
        Reason.PACKET_NOT_ALLOWED);
    machine.advance(new FinishConfigurationClientboundPacket());
    assertThat(machine.advance(new FinishConfigurationServerboundPacket()))
        .isEqualTo(ConnectionState.PLAY);
    assertFails(() -> machine.advance(CONFIGURATION_ACKNOWLEDGED), Reason.PACKET_NOT_ALLOWED);
  }
}

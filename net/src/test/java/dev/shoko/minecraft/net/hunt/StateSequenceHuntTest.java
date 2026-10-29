package dev.shoko.minecraft.net.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
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
// R45 (start_configuration, then configuration_acknowledged), R93 (finish_configuration
// clientbound, then its acknowledgement). The full sequences are accepted and R45 returns to
// CONFIGURATION. Limit recorded: the machine does not check the order of the acknowledgements; the
// connection layer does.
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
  void loginAcknowledgedAfterLoginFinishedEntersConfiguration() throws ProtocolException {
    // spec: R39
    assertThat(inConfiguration().state()).isEqualTo(ConnectionState.CONFIGURATION);
  }

  @Test
  void theFinishConfigurationAcknowledgementAfterTheServerOneEntersPlay() throws ProtocolException {
    // spec: R93
    assertThat(inPlay().state()).isEqualTo(ConnectionState.PLAY);
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
}

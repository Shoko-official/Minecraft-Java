package dev.shoko.minecraft.net.state;

import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// spec: net-protocol-777 R34 (intention 1 status, 2 login, 3 transfer), R35, R46, vector I07
class ConnectionIntentTest {

  @Test
  void theWireValuesAreOneTwoAndThree() {
    // spec: R34
    assertThat(ConnectionIntent.STATUS.id()).isEqualTo(1);
    assertThat(ConnectionIntent.LOGIN.id()).isEqualTo(2);
    assertThat(ConnectionIntent.TRANSFER.id()).isEqualTo(3);
    assertThat(ConnectionIntent.values()).hasSize(3);
  }

  @Test
  void intentionsTwoAndThreeLeadToLoginAndOneToStatus() {
    // spec: R34, R46
    assertThat(ConnectionIntent.STATUS.nextState()).isEqualTo(ConnectionState.STATUS);
    assertThat(ConnectionIntent.LOGIN.nextState()).isEqualTo(ConnectionState.LOGIN);
    assertThat(ConnectionIntent.TRANSFER.nextState()).isEqualTo(ConnectionState.LOGIN);
  }

  @Test
  void fromIdFindsTheIntentionOfAWireValue() throws ProtocolException {
    // spec: R34
    assertThat(ConnectionIntent.fromId(1)).isEqualTo(ConnectionIntent.STATUS);
    assertThat(ConnectionIntent.fromId(2)).isEqualTo(ConnectionIntent.LOGIN);
    assertThat(ConnectionIntent.fromId(3)).isEqualTo(ConnectionIntent.TRANSFER);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 4, 5, -1, 255, 777, Integer.MAX_VALUE, Integer.MIN_VALUE})
  void provisionalAnyOtherValueIsAnUnknownIntention(int id) {
    // spec: R35, vector I07 (intention 4)
    assertFails(() -> ConnectionIntent.fromId(id), Reason.UNKNOWN_INTENTION);
  }

  @Test
  void theStatesAreTheFiveOfAConnectionInOrder() {
    // spec: R34 to R49
    assertThat(ConnectionState.values())
        .containsExactly(
            ConnectionState.HANDSHAKE,
            ConnectionState.STATUS,
            ConnectionState.LOGIN,
            ConnectionState.CONFIGURATION,
            ConnectionState.PLAY);
    assertThat(PacketDirection.values())
        .containsExactlyInAnyOrder(PacketDirection.CLIENTBOUND, PacketDirection.SERVERBOUND);
  }
}

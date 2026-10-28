package dev.shoko.minecraft.tools.oracle.rcon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class RconClientTest {

  private static final String PASSWORD = "s3cret-for-test";
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private static RconClient connect(FakeRconServer server, String password) throws IOException {
    InetSocketAddress address =
        new InetSocketAddress(InetAddress.getLoopbackAddress(), server.port());
    return RconClient.connect(address, password, TIMEOUT);
  }

  private static FakeRconServer server(
      UnaryOperator<String> handler, FakeRconServer.Behavior behavior) throws IOException {
    return new FakeRconServer(0, PASSWORD, handler, behavior);
  }

  @Test
  void encodesPacketsAsTheProtocolDescribes() {
    byte[] bytes = new RconPacket(7, RconPacket.TYPE_COMMAND, "list").encode();

    assertThat(HexFormat.of().formatHex(bytes))
        .isEqualTo("0e000000" + "07000000" + "02000000" + "6c697374" + "0000");
  }

  @Test
  void decodesAPacket() throws IOException {
    byte[] bytes = HexFormat.of().parseHex("0c000000" + "ffffffff" + "02000000" + "6f6b" + "0000");

    RconPacket packet = RconPacket.read(new ByteArrayInputStream(bytes));

    assertThat(packet).isEqualTo(new RconPacket(-1, RconPacket.TYPE_COMMAND, "ok"));
  }

  @Test
  void rejectsPacketsWithoutTerminatorsOrWithAnInvalidLength() {
    byte[] unterminated =
        HexFormat.of().parseHex("0c000000" + "01000000" + "00000000" + "6f6b6f6b");
    byte[] tooShort = HexFormat.of().parseHex("09000000" + "010000000000000000");
    byte[] truncated = HexFormat.of().parseHex("0e000000" + "01000000");

    assertThatThrownBy(() -> RconPacket.read(new ByteArrayInputStream(unterminated)))
        .hasMessageContaining("null terminators");
    assertThatThrownBy(() -> RconPacket.read(new ByteArrayInputStream(tooShort)))
        .hasMessageContaining("invalid length 9");
    assertThatThrownBy(() -> RconPacket.read(new ByteArrayInputStream(truncated)))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("middle of a packet");
  }

  @Test
  void hidesTheLoginPayloadFromToString() {
    assertThat(new RconPacket(1, RconPacket.TYPE_LOGIN, PASSWORD).toString())
        .doesNotContain(PASSWORD);
  }

  @Test
  void authenticatesAndRunsCommands() throws IOException {
    try (FakeRconServer server = server(cmd -> "ran " + cmd, FakeRconServer.Behavior.STANDARD);
        RconClient client = connect(server, PASSWORD)) {
      assertThat(client.command("seed")).isEqualTo("ran seed");
      assertThat(client.command("list")).isEqualTo("ran list");
    }
  }

  @Test
  void refusedAuthenticationFailsWithoutRevealingThePassword() throws IOException {
    try (FakeRconServer server = server(cmd -> "", FakeRconServer.Behavior.STANDARD)) {
      assertThatThrownBy(() -> connect(server, "wrong-password"))
          .isInstanceOf(RconAuthenticationException.class)
          .hasMessageContaining("refused")
          .hasMessageNotContaining("wrong-password");
    }
  }

  @Test
  void joinsAResponseFragmentedOverSeveralPackets() throws IOException {
    String longText = "0123456789".repeat(1000);
    try (FakeRconServer server = server(cmd -> longText, FakeRconServer.Behavior.STANDARD);
        RconClient client = connect(server, PASSWORD)) {
      assertThat(client.command("data get")).isEqualTo(longText);
      assertThat(client.command("again")).isEqualTo(longText);
    }
  }

  @Test
  void readsPacketsSplitAcrossTcpWrites() throws IOException {
    String text = "abcdefghij".repeat(30);
    FakeRconServer.Behavior split = new FakeRconServer.Behavior(64, true, true);
    try (FakeRconServer server = server(cmd -> text, split);
        RconClient client = connect(server, PASSWORD)) {
      assertThat(client.command("x")).isEqualTo(text);
    }
  }

  @Test
  void joinsFragmentsThatCutAMultiByteCharacter() throws IOException {
    String text = "\u00e9".repeat(100);
    FakeRconServer.Behavior oddCut = new FakeRconServer.Behavior(5, false, true);
    try (FakeRconServer server = server(cmd -> text, oddCut);
        RconClient client = connect(server, PASSWORD)) {
      assertThat(client.command("x")).isEqualTo(text);
    }
  }

  @Test
  void refusesAResponseThatIsNotValidUtf8InsteadOfReplacingIt() throws IOException {
    byte[] invalid = {(byte) 0xa7, 'c', 'R', 'e', 'd'};
    try (FakeRconServer server =
            FakeRconServer.withRawResponses(
                PASSWORD, cmd -> invalid, FakeRconServer.Behavior.STANDARD);
        RconClient client = connect(server, PASSWORD)) {
      assertThatThrownBy(() -> client.command("x"))
          .isInstanceOf(IOException.class)
          .hasMessageContaining("not valid UTF-8: byte 0xa7 at offset 0");
    }
  }

  @Test
  void decodesPacketsStrictly() {
    byte[] bytes = HexFormat.of().parseHex("0b000000" + "01000000" + "00000000" + "a7" + "0000");

    assertThatThrownBy(() -> RconPacket.read(new ByteArrayInputStream(bytes)))
        .hasMessageContaining("byte 0xa7 at offset 0");
  }

  @Test
  void limitsTheCommandSize() throws IOException {
    try (FakeRconServer server = server(cmd -> "ok", FakeRconServer.Behavior.STANDARD);
        RconClient client = connect(server, PASSWORD)) {
      assertThat(client.command("a".repeat(1446))).isEqualTo("ok");
      assertThatThrownBy(() -> client.command("a".repeat(1447)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("1447 bytes exceeds 1446 bytes");
      assertThatThrownBy(() -> client.command("\u00e9".repeat(724)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("1448 bytes");
    }
  }

  @Test
  void acceptsAnotherCommandLimitAndMarkerType() throws IOException {
    RconClient.Options options = new RconClient.Options(TIMEOUT, 0x7ead, 10);
    try (FakeRconServer server = server(cmd -> "ran " + cmd, FakeRconServer.Behavior.STANDARD);
        RconClient client =
            RconClient.connect(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), server.port()),
                PASSWORD,
                options)) {
      assertThat(client.command("list")).isEqualTo("ran list");
      assertThatThrownBy(() -> client.command("a".repeat(11)))
          .isInstanceOf(IllegalArgumentException.class);
      assertThat(server.markerTypes()).containsExactly(0x7ead);
    }
    assertThatThrownBy(() -> new RconClient.Options(TIMEOUT, RconPacket.TYPE_COMMAND, 10))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void returnsAnEmptyResponse() throws IOException {
    try (FakeRconServer server = server(cmd -> "", FakeRconServer.Behavior.STANDARD);
        RconClient client = connect(server, PASSWORD)) {
      assertThat(client.command("time set day")).isEmpty();
    }
  }

  @Test
  void failsOnTimeoutWhenTheEndOfResponseIsNeverConfirmed() throws IOException {
    FakeRconServer.Behavior silent = new FakeRconServer.Behavior(4096, false, false);
    InetSocketAddress address;
    try (FakeRconServer server = server(cmd -> "partial", silent)) {
      address = new InetSocketAddress(InetAddress.getLoopbackAddress(), server.port());
      try (RconClient client = RconClient.connect(address, PASSWORD, Duration.ofSeconds(1))) {
        assertThatThrownBy(() -> client.command("x"))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("No RCON reply");
      }
    }
  }
}

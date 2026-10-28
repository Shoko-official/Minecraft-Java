package dev.shoko.minecraft.tools.oracle.rcon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A server that breaks the protocol: wrong ids, unexpected types, cut packets, oversized frames.
 */
class RconProtocolErrorsTest {

  private static final String PASSWORD = "pw";
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  /** Bytes to send back for one request, then whether to close the connection. */
  private record Reply(List<byte[]> packets, boolean close) {
    static final Reply NOTHING = new Reply(List.of(), false);

    static Reply of(byte[]... packets) {
      return new Reply(List.of(packets), false);
    }

    static Reply thenClose(byte[]... packets) {
      return new Reply(List.of(packets), true);
    }
  }

  private interface Script {
    Reply reply(RconPacket request) throws IOException;
  }

  /** Serves one connection: every request packet is answered with the bytes of the script. */
  private static final class ScriptedServer implements AutoCloseable {
    private final ServerSocket server;

    ScriptedServer(Script script) throws IOException {
      server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
      Thread.ofPlatform().daemon().name("scripted-rcon").start(() -> serve(script));
    }

    private void serve(Script script) {
      try (Socket socket = server.accept()) {
        InputStream in = new BufferedInputStream(socket.getInputStream());
        OutputStream out = socket.getOutputStream();
        while (true) {
          Reply reply = script.reply(RconPacket.read(in));
          for (byte[] bytes : reply.packets()) {
            out.write(bytes);
          }
          out.flush();
          if (reply.close()) {
            return;
          }
        }
      } catch (IOException e) {
        // The client went away or the server was closed.
      }
    }

    RconClient connect() throws IOException {
      InetSocketAddress address =
          new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort());
      return RconClient.connect(address, PASSWORD, TIMEOUT);
    }

    @Override
    public void close() throws IOException {
      server.close();
    }
  }

  private static byte[] packet(int id, int type, String payload) {
    return new RconPacket(id, type, payload).encode();
  }

  /** Accepts the login with the request id, answers the other requests with {@code afterLogin}. */
  private static Script accepting(Script afterLogin) {
    return request ->
        request.type() == RconPacket.TYPE_LOGIN
            ? Reply.of(packet(request.requestId(), RconPacket.TYPE_COMMAND, ""))
            : afterLogin.reply(request);
  }

  private static byte[] response(int id, String payload) {
    return packet(id, RconPacket.TYPE_RESPONSE, payload);
  }

  @Test
  void refusesALoginReplyWithAnotherRequestId() throws IOException {
    try (ScriptedServer server =
        new ScriptedServer(
            request -> Reply.of(packet(request.requestId() + 5, RconPacket.TYPE_COMMAND, "")))) {
      assertThatThrownBy(server::connect)
          .isInstanceOf(IOException.class)
          .isNotInstanceOf(RconAuthenticationException.class)
          .hasMessage("Unexpected RCON packet during authentication: id 6 type 2");
    }
  }

  @Test
  void refusesALoginReplyOfAnUnexpectedType() throws IOException {
    try (ScriptedServer server =
        new ScriptedServer(request -> Reply.of(packet(request.requestId(), 9, "")))) {
      assertThatThrownBy(server::connect)
          .isInstanceOf(IOException.class)
          .hasMessage("Unexpected RCON packet during authentication: id 1 type 9");
    }
  }

  @Test
  void skipsAnEmptyResponsePacketBeforeTheLoginResult() throws IOException {
    Script script =
        request ->
            Reply.of(
                response(request.requestId(), ""),
                packet(request.requestId(), RconPacket.TYPE_COMMAND, ""));
    try (ScriptedServer server = new ScriptedServer(script)) {
      assertThatCode(() -> server.connect().close()).doesNotThrowAnyException();
    }
  }

  @Test
  void failsOnAReplyIdFromTheFutureAndDropsTheConnection() throws IOException {
    Script script = accepting(request -> Reply.of(response(request.requestId() + 5, "")));
    try (ScriptedServer server = new ScriptedServer(script);
        RconClient client = server.connect()) {
      assertThatThrownBy(() -> client.command("list"))
          .isInstanceOf(IOException.class)
          .hasMessage("Unexpected RCON request id 7 for 2");
      assertThatThrownBy(() -> client.command("list"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("closed");
    }
  }

  @Test
  void failsOnReplyIdsZeroAndMinusOne() throws IOException {
    for (int badId : new int[] {0, -1}) {
      Script script = accepting(request -> Reply.of(response(badId, "x")));
      try (ScriptedServer server = new ScriptedServer(script);
          RconClient client = server.connect()) {
        assertThatThrownBy(() -> client.command("list"))
            .hasMessage("Unexpected RCON request id " + badId + " for 2");
      }
    }
  }

  @Test
  void skipsLatePacketsOfEarlierRequests() throws IOException {
    Script script =
        accepting(
            request ->
                request.type() == RconPacket.TYPE_COMMAND
                    ? Reply.of(
                        response(1, "late login echo"),
                        response(request.requestId(), "answer"),
                        response(request.requestId() + 1, ""))
                    : Reply.NOTHING);
    try (ScriptedServer server = new ScriptedServer(script);
        RconClient client = server.connect()) {
      assertThat(client.command("list")).isEqualTo("answer");
      assertThat(client.command("list")).isEqualTo("answer");
    }
  }

  @Test
  void dropsTheConnectionWhenAPacketIsCutInTheMiddle() throws IOException {
    Script script =
        accepting(
            request ->
                request.type() == RconPacket.TYPE_COMMAND
                    ? Reply.NOTHING
                    : Reply.thenClose(
                        Arrays.copyOf(response(request.requestId() - 1, "abcdef"), 9)));
    try (ScriptedServer server = new ScriptedServer(script);
        RconClient client = server.connect()) {
      assertThatThrownBy(() -> client.command("x")).isInstanceOf(EOFException.class);
      assertThatThrownBy(() -> client.command("y")).isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  void dropsTheConnectionWhenTheServerClosesBeforeTheEndOfTheResponse() throws IOException {
    Script script =
        accepting(
            request ->
                request.type() == RconPacket.TYPE_COMMAND ? Reply.NOTHING : Reply.thenClose());
    try (ScriptedServer server = new ScriptedServer(script);
        RconClient client = server.connect()) {
      assertThatThrownBy(() -> client.command("x")).isInstanceOf(EOFException.class);
      assertThatThrownBy(() -> client.command("y")).isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  void reportsTheOffsetOfTheInvalidByteInTheJoinedResponse() throws IOException {
    byte[] badByte = {'a', 'b', (byte) 0xa7, 'c'};
    byte[] cutCharacter = {'a', 'b', (byte) 0xc3};
    for (byte[] bytes : List.of(badByte, cutCharacter)) {
      Script script =
          accepting(
              request ->
                  Reply.of(
                      RconPacket.encode(request.requestId(), RconPacket.TYPE_RESPONSE, bytes),
                      response(request.requestId() + 1, "")));
      try (ScriptedServer server = new ScriptedServer(script);
          RconClient client = server.connect()) {
        assertThatThrownBy(() -> client.command("x"))
            .hasMessageContaining("byte 0x%02x at offset 2".formatted(bytes[2] & 0xff));
      }
    }
  }

  @Test
  void acceptsAPacketOfExactlyTheMaximumLengthAndRefusesOneByteMore() throws IOException {
    int payload = RconPacket.MAX_LENGTH - 10;
    byte[] largest = RconPacket.encode(3, RconPacket.TYPE_RESPONSE, new byte[payload]);

    RconPacket packet = RconPacket.read(new ByteArrayInputStream(largest));

    assertThat(packet.payload()).hasSize(payload);
    byte[] tooLong =
        ByteBuffer.allocate(Integer.BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(RconPacket.MAX_LENGTH + 1)
            .array();
    assertThatThrownBy(() -> RconPacket.read(new ByteArrayInputStream(tooLong)))
        .isInstanceOf(IOException.class)
        .hasMessage("RCON packet with invalid length " + (RconPacket.MAX_LENGTH + 1));
  }
}

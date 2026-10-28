package dev.shoko.minecraft.tools.oracle.rcon;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Test double of an RCON server following the public protocol: login answered with the request id
 * or -1, command responses split in fragments of at most {@code fragmentBytes} bytes, and a reply
 * to an unknown request type so that clients can detect the end of a response.
 */
public final class FakeRconServer implements AutoCloseable {

  /**
   * @param fragmentBytes largest payload of one response packet
   * @param splitWrites write every packet in two TCP writes, cutting it in the middle
   * @param answerMarker reply to packets of the response type, as a real server does
   */
  public record Behavior(int fragmentBytes, boolean splitWrites, boolean answerMarker) {
    public static final Behavior STANDARD = new Behavior(4096, false, true);
  }

  private final ServerSocket server;
  private final String password;
  private final Function<String, byte[]> handler;
  private final Behavior behavior;
  private final List<Integer> markerTypes = Collections.synchronizedList(new ArrayList<>());

  public FakeRconServer(int port, String password, UnaryOperator<String> handler, Behavior behavior)
      throws IOException {
    this(port, password, behavior, command -> handler.apply(command).getBytes(UTF_8));
  }

  private FakeRconServer(
      int port, String password, Behavior behavior, Function<String, byte[]> handler)
      throws IOException {
    this.server = new ServerSocket(port, 4, InetAddress.getLoopbackAddress());
    this.password = password;
    this.handler = handler;
    this.behavior = behavior;
    Thread.ofPlatform().daemon().name("fake-rcon").start(this::acceptLoop);
  }

  /** A server whose responses are raw bytes, possibly not valid UTF-8. */
  public static FakeRconServer withRawResponses(
      String password, Function<String, byte[]> handler, Behavior behavior) throws IOException {
    return new FakeRconServer(0, password, behavior, handler);
  }

  /** Types of the packets received that were neither login nor command. */
  public List<Integer> markerTypes() {
    return List.copyOf(markerTypes);
  }

  public int port() {
    return server.getLocalPort();
  }

  private void acceptLoop() {
    while (!server.isClosed()) {
      try (Socket socket = server.accept()) {
        serve(new BufferedInputStream(socket.getInputStream()), socket.getOutputStream());
      } catch (IOException e) {
        // The client went away or the server was closed: wait for the next connection.
      }
    }
  }

  private void serve(InputStream in, OutputStream out) throws IOException {
    while (true) {
      RconPacket request = RconPacket.read(in);
      switch (request.type()) {
        case RconPacket.TYPE_LOGIN -> {
          int id = request.payload().equals(password) ? request.requestId() : -1;
          send(out, new RconPacket(id, RconPacket.TYPE_COMMAND, ""));
        }
        case RconPacket.TYPE_COMMAND -> respond(out, request.requestId(), request.payload());
        default -> {
          markerTypes.add(request.type());
          if (behavior.answerMarker()) {
            String text = "Unknown request " + Integer.toHexString(request.type());
            send(out, new RconPacket(request.requestId(), RconPacket.TYPE_RESPONSE, text));
          }
        }
      }
    }
  }

  private void respond(OutputStream out, int id, String command) throws IOException {
    byte[] response = handler.apply(command);
    int offset = 0;
    do {
      int length = Math.min(behavior.fragmentBytes(), response.length - offset);
      byte[] fragment = Arrays.copyOfRange(response, offset, offset + length);
      send(out, RconPacket.encode(id, RconPacket.TYPE_RESPONSE, fragment));
      offset += length;
    } while (offset < response.length);
  }

  private void send(OutputStream out, RconPacket packet) throws IOException {
    send(out, packet.encode());
  }

  /** Fragments are cut by bytes, possibly inside a multi-byte character. */
  private void send(OutputStream out, byte[] bytes) throws IOException {
    if (behavior.splitWrites()) {
      out.write(bytes, 0, bytes.length / 2);
      out.flush();
      out.write(bytes, bytes.length / 2, bytes.length - bytes.length / 2);
    } else {
      out.write(bytes);
    }
    out.flush();
  }

  @Override
  public void close() throws IOException {
    server.close();
  }
}

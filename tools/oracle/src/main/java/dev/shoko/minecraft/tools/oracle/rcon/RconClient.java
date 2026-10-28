package dev.shoko.minecraft.tools.oracle.rcon;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Minimal client of the public RCON protocol. Not thread-safe: one command at a time.
 *
 * <p>A long response arrives as several packets that carry the id of the command, with no marker on
 * the last one. After each command the client sends an empty {@link RconPacket#TYPE_RESPONSE}
 * packet with the next id; the server answers requests in order, so the reply to that marker ends
 * the response. A server that never answers the marker makes the command fail on the read timeout
 * instead of returning a truncated response.
 */
public final class RconClient implements AutoCloseable {

  private final Socket socket;
  private final InputStream in;
  private final OutputStream out;
  private final Duration timeout;
  private int nextId = 1;

  private RconClient(Socket socket, Duration timeout) throws IOException {
    this.socket = socket;
    this.in = new BufferedInputStream(socket.getInputStream());
    this.out = new BufferedOutputStream(socket.getOutputStream());
    this.timeout = timeout;
  }

  /**
   * Connects and authenticates.
   *
   * @throws RconAuthenticationException when the server refuses the password
   */
  public static RconClient connect(InetSocketAddress address, String password, Duration timeout)
      throws IOException {
    int millis = Math.toIntExact(timeout.toMillis());
    Socket socket = new Socket();
    try {
      socket.connect(address, millis);
      socket.setSoTimeout(millis);
      RconClient client = new RconClient(socket, timeout);
      client.authenticate(password);
      return client;
    } catch (IOException | RuntimeException e) {
      socket.close();
      throw e;
    }
  }

  private void authenticate(String password) throws IOException {
    int id = nextId++;
    new RconPacket(id, RconPacket.TYPE_LOGIN, password).write(out);
    while (true) {
      RconPacket.Frame reply = receive("authentication");
      if (reply.requestId() == RconPacket.AUTH_FAILED_ID) {
        throw new RconAuthenticationException("RCON authentication refused by the server");
      }
      if (reply.requestId() == id && reply.type() == RconPacket.TYPE_COMMAND) {
        return;
      }
      // Some servers send an empty response packet before the authentication result: skip it.
      if (reply.requestId() != id || reply.type() != RconPacket.TYPE_RESPONSE) {
        throw new IOException(
            "Unexpected RCON packet during authentication: id "
                + reply.requestId()
                + " type "
                + reply.type());
      }
    }
  }

  /** Runs a console command and returns its whole response, fragments joined in order. */
  public String command(String command) throws IOException {
    if (socket.isClosed()) {
      throw new IllegalStateException("RCON client is closed");
    }
    int id = nextId++;
    int marker = nextId++;
    new RconPacket(id, RconPacket.TYPE_COMMAND, command).write(out);
    new RconPacket(marker, RconPacket.TYPE_RESPONSE, "").write(out);
    ByteArrayOutputStream response = new ByteArrayOutputStream();
    while (true) {
      RconPacket.Frame reply = receive("command");
      if (reply.requestId() == marker) {
        return response.toString(StandardCharsets.UTF_8);
      }
      if (reply.requestId() == id) {
        response.writeBytes(reply.payload());
      } else if (reply.requestId() <= 0 || reply.requestId() > id) {
        // Lower positive ids are late packets of earlier, already completed requests: skipped.
        throw new IOException("Unexpected RCON request id " + reply.requestId() + " for " + id);
      }
    }
  }

  private RconPacket.Frame receive(String phase) throws IOException {
    try {
      return RconPacket.readFrame(in);
    } catch (SocketTimeoutException e) {
      throw new IOException("No RCON reply during " + phase + " within " + timeout, e);
    }
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }
}

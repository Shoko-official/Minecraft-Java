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
 * the last one. After each command the client sends an empty marker packet with the next id and a
 * type that is neither login nor command ({@link Options#markerType()}, {@link
 * RconPacket#TYPE_RESPONSE} by default); the reply to that marker ends the response. A server that
 * never answers the marker makes the command fail on the read timeout instead of returning a
 * truncated response. The documented alternative is a marker of a type the server does not know,
 * which the public protocol description says is answered with "Unknown request": set {@code
 * markerType} to such a type if the default turns out not to be answered.
 *
 * <p>Response payloads are decoded as strict UTF-8 once all fragments are joined: an invalid byte
 * fails the command with its offset instead of being replaced.
 *
 * <p>Not observed on the reference server yet, each is a HYPOTHESIS until a probe confirms it:
 *
 * <ul>
 *   <li>HYPOTHESIS encoding: payloads are UTF-8 (the public protocol description only speaks of
 *       ASCII payloads);
 *   <li>HYPOTHESIS order: the server answers the requests of one connection in the order sent, so
 *       the marker reply comes after every fragment of the command;
 *   <li>HYPOTHESIS end marker: the server replies to a packet of type 0 with the same request id.
 * </ul>
 *
 * <p>Probes to run at the first real start of the server: a command whose output holds non-ASCII
 * text, compared byte for byte with the console log (encoding); a command with an output longer
 * than one fragment, checking that fragments arrive before the marker reply (order); the raw reply
 * to a type 0 packet and to an unknown type (end marker); {@code netstat -ano} during a run,
 * checking that the RCON port listens on 127.0.0.1 only (binding).
 */
public final class RconClient implements AutoCloseable {

  /**
   * Client settings.
   *
   * @param timeout connection and read timeout
   * @param markerType type of the packet sent after each command to detect the end of its response
   * @param maxCommandBytes largest UTF-8 size of a command; the public protocol description gives
   *     1446 bytes for packets sent to the server, a value that may be outdated
   */
  public record Options(Duration timeout, int markerType, int maxCommandBytes) {

    public static final int DEFAULT_MAX_COMMAND_BYTES = 1446;

    public Options {
      if (markerType == RconPacket.TYPE_COMMAND || markerType == RconPacket.TYPE_LOGIN) {
        throw new IllegalArgumentException("the marker type must not be login or command");
      }
      if (maxCommandBytes < 1) {
        throw new IllegalArgumentException("maxCommandBytes must be positive");
      }
    }

    public static Options defaults(Duration timeout) {
      return new Options(timeout, RconPacket.TYPE_RESPONSE, DEFAULT_MAX_COMMAND_BYTES);
    }
  }

  private final Socket socket;
  private final InputStream in;
  private final OutputStream out;
  private final Options options;
  private int nextId = 1;

  private RconClient(Socket socket, Options options) throws IOException {
    this.socket = socket;
    this.in = new BufferedInputStream(socket.getInputStream());
    this.out = new BufferedOutputStream(socket.getOutputStream());
    this.options = options;
  }

  /**
   * Connects and authenticates.
   *
   * @throws RconAuthenticationException when the server refuses the password
   */
  public static RconClient connect(InetSocketAddress address, String password, Duration timeout)
      throws IOException {
    return connect(address, password, Options.defaults(timeout));
  }

  /**
   * Connects and authenticates with explicit settings.
   *
   * @throws RconAuthenticationException when the server refuses the password
   */
  public static RconClient connect(InetSocketAddress address, String password, Options options)
      throws IOException {
    int millis = Math.toIntExact(options.timeout().toMillis());
    Socket socket = new Socket();
    try {
      socket.connect(address, millis);
      socket.setSoTimeout(millis);
      RconClient client = new RconClient(socket, options);
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

  /**
   * Runs a console command and returns its whole response, fragments joined in order.
   *
   * @throws IllegalArgumentException when the command is longer than {@code maxCommandBytes}
   */
  public String command(String command) throws IOException {
    if (socket.isClosed()) {
      throw new IllegalStateException("RCON client is closed");
    }
    int size = command.getBytes(StandardCharsets.UTF_8).length;
    if (size > options.maxCommandBytes()) {
      throw new IllegalArgumentException(
          "RCON command of " + size + " bytes exceeds " + options.maxCommandBytes() + " bytes");
    }
    int id = nextId++;
    int marker = nextId++;
    new RconPacket(id, RconPacket.TYPE_COMMAND, command).write(out);
    new RconPacket(marker, options.markerType(), "").write(out);
    ByteArrayOutputStream response = new ByteArrayOutputStream();
    while (true) {
      RconPacket.Frame reply = receive("command");
      if (reply.requestId() == marker) {
        return RconPacket.decodeStrict(response.toByteArray());
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
      throw new IOException("No RCON reply during " + phase + " within " + options.timeout(), e);
    }
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }
}

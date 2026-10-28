package dev.shoko.minecraft.tools.oracle;

import dev.shoko.minecraft.tools.oracle.rcon.RconAuthenticationException;
import dev.shoko.minecraft.tools.oracle.rcon.RconClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * One running reference server, driven as a black box (ADR 0002, point 2a): properties in, console
 * log, RCON responses and region files out.
 *
 * <p>The RCON password is random for each run and lives only in {@code server.properties} and in
 * memory; it is never logged nor passed on the command line.
 */
public final class OracleServer implements AutoCloseable {

  /** How the server process ended. */
  public enum StopOutcome {
    /** The server exited by itself after the {@code stop} command. */
    STOPPED,
    /** The server ignored {@code stop} within the delay and was destroyed. */
    DESTROYED,
    /** The process had already exited before the stop request. */
    ALREADY_EXITED
  }

  static final String CONSOLE_LOG = "oracle-console.log";
  static final String EULA_FILE = "eula.txt";

  /** Console line printed by the server once it accepts commands. */
  private static final Pattern READY = Pattern.compile("Done \\([^)]*\\)! For help, type \"help\"");

  private static final Duration RCON_TIMEOUT = Duration.ofMinutes(2);
  private static final Duration EXIT_GRACE = Duration.ofSeconds(5);
  private static final long RCON_RETRY_MILLIS = 200;
  private static final int PASSWORD_BYTES = 24;

  private final Process process;
  private final Thread pump;
  private final RconClient rcon;
  private final Duration stopTimeout;
  private final Consumer<String> log;
  private StopOutcome outcome;

  private OracleServer(
      Process process, Thread pump, RconClient rcon, Duration stopTimeout, Consumer<String> log) {
    this.process = process;
    this.pump = pump;
    this.rcon = rcon;
    this.stopTimeout = stopTimeout;
    this.log = log;
  }

  /**
   * Prepares the run directory, starts the server and waits until it is ready and RCON answers.
   *
   * @throws EulaNotAcceptedException when {@code options.acceptEula()} is false; nothing is written
   */
  public static OracleServer start(OracleOptions options, Consumer<String> log)
      throws IOException, InterruptedException {
    if (!options.acceptEula()) {
      throw new EulaNotAcceptedException();
    }
    Path runDir = options.runDir().toAbsolutePath();
    checkReusableWorld(runDir, options.settings());
    int[] ports = freePorts();
    String password = randomPassword();
    Files.createDirectories(runDir);
    Files.writeString(
        runDir.resolve(ServerProperties.FILE_NAME),
        ServerProperties.render(options.settings(), ports[0], ports[1], password),
        StandardCharsets.UTF_8);
    Files.writeString(
        runDir.resolve(EULA_FILE),
        "# Accepted by the repository owner ("
            + EulaNotAcceptedException.EULA_URL
            + ")\neula=true\n",
        StandardCharsets.UTF_8);

    log.accept("oracle: starting server in " + runDir);
    Process process =
        new ProcessBuilder(options.command())
            .directory(runDir.toFile())
            .redirectErrorStream(true)
            .start();
    CompletableFuture<Void> ready = new CompletableFuture<>();
    Path consoleLog = runDir.resolve(CONSOLE_LOG);
    Thread pump =
        Thread.ofPlatform()
            .daemon()
            .name("oracle-console")
            .start(() -> pumpConsole(process.getInputStream(), consoleLog, ready, log));
    long deadline = System.nanoTime() + options.startupTimeout().toNanos();
    try {
      awaitReady(process, ready, options.startupTimeout(), consoleLog);
      RconClient rcon = connectRcon(process, ports[1], password, deadline);
      log.accept("oracle: ready, RCON on 127.0.0.1:" + ports[1]);
      return new OracleServer(process, pump, rcon, options.stopTimeout(), log);
    } catch (IOException | RuntimeException | InterruptedException e) {
      kill(process);
      pump.join(options.stopTimeout().toMillis());
      throw e;
    }
  }

  /** Runs a console command through RCON and returns the whole response. */
  public String command(String command) throws IOException {
    return rcon.command(command);
  }

  /**
   * Sends {@code stop} on the console, waits for the delay, then destroys the process (politely,
   * then forcibly). Calling it again returns the first outcome.
   */
  public synchronized StopOutcome stop() throws InterruptedException {
    if (outcome != null) {
      return outcome;
    }
    try {
      rcon.close();
    } catch (IOException e) {
      log.accept("oracle: closing RCON failed: " + e.getMessage());
    }
    if (!process.isAlive()) {
      outcome = StopOutcome.ALREADY_EXITED;
    } else {
      sendConsole("stop");
      if (process.waitFor(stopTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
        outcome = StopOutcome.STOPPED;
      } else {
        log.accept("oracle: server ignored stop for " + stopTimeout + ", destroying it");
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        if (!process.waitFor(stopTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
          kill(process);
          process.waitFor();
        }
        outcome = StopOutcome.DESTROYED;
      }
    }
    pump.join(stopTimeout.toMillis());
    log.accept("oracle: server " + outcome.name().toLowerCase(Locale.ROOT));
    return outcome;
  }

  /** Stops the server; an interruption kills the process at once and is passed on. */
  @Override
  public void close() {
    try {
      stop();
    } catch (InterruptedException e) {
      kill(process);
      Thread.currentThread().interrupt();
    }
  }

  private void sendConsole(String command) {
    try {
      OutputStream stdin = process.getOutputStream();
      stdin.write((command + "\n").getBytes(StandardCharsets.UTF_8));
      stdin.flush();
    } catch (IOException e) {
      log.accept("oracle: could not write to the server console: " + e.getMessage());
    }
  }

  /** Refuses to reuse a world whose recorded seed differs: the server would ignore the new one. */
  private static void checkReusableWorld(Path runDir, ServerSettings settings) throws IOException {
    if (!Files.exists(runDir.resolve(settings.levelName()))) {
      return;
    }
    Optional<String> recorded = ServerProperties.recordedSeed(runDir);
    String wanted = Long.toString(settings.seed());
    if (!recorded.equals(Optional.of(wanted))) {
      throw new IOException(
          "World "
              + runDir.resolve(settings.levelName())
              + " already exists with seed "
              + recorded.orElse("<unknown>")
              + ", not "
              + wanted
              + ": delete it or use another run name");
    }
  }

  private static void awaitReady(
      Process process, CompletableFuture<Void> ready, Duration timeout, Path consoleLog)
      throws IOException, InterruptedException {
    try {
      ready.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      throw new IOException("Server not ready within " + timeout + ", see " + consoleLog, e);
    } catch (ExecutionException e) {
      // The console closes slightly before the process is reaped.
      String exit =
          process.waitFor(EXIT_GRACE.toMillis(), TimeUnit.MILLISECONDS)
              ? "exit code " + process.exitValue()
              : "still running";
      throw new IOException(
          "Server console ended before it was ready (" + exit + "), see " + consoleLog,
          e.getCause());
    }
  }

  /** The RCON listener may open slightly after the ready line: retry until the deadline. */
  private static RconClient connectRcon(Process process, int port, String password, long deadline)
      throws IOException, InterruptedException {
    InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
    while (true) {
      try {
        return RconClient.connect(address, password, RCON_TIMEOUT);
      } catch (RconAuthenticationException e) {
        throw e;
      } catch (IOException e) {
        if (!process.isAlive() || System.nanoTime() - deadline > 0) {
          throw new IOException("RCON did not answer on port " + port + " in time", e);
        }
        Thread.sleep(RCON_RETRY_MILLIS);
      }
    }
  }

  /**
   * Copies the console to the log file byte for byte (ISO-8859-1 maps every byte to one char) and
   * completes {@code ready} on the ready line, or exceptionally when the console ends first.
   */
  private static void pumpConsole(
      InputStream console, Path logFile, CompletableFuture<Void> ready, Consumer<String> log) {
    try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(console, StandardCharsets.ISO_8859_1));
        Writer writer = Files.newBufferedWriter(logFile, StandardCharsets.ISO_8859_1)) {
      String line;
      while ((line = reader.readLine()) != null) {
        writer.write(line);
        writer.write('\n');
        writer.flush();
        if (!ready.isDone() && READY.matcher(line).find()) {
          ready.complete(null);
        }
      }
      ready.completeExceptionally(new IOException("server console closed"));
    } catch (IOException e) {
      ready.completeExceptionally(e);
      log.accept("oracle: console copy to " + logFile + " failed: " + e.getMessage());
    }
  }

  /** Two distinct free loopback ports: game, then RCON. Another process may still race for them. */
  private static int[] freePorts() throws IOException {
    InetAddress loopback = InetAddress.getLoopbackAddress();
    try (ServerSocket game = new ServerSocket(0, 1, loopback);
        ServerSocket rcon = new ServerSocket(0, 1, loopback)) {
      return new int[] {game.getLocalPort(), rcon.getLocalPort()};
    }
  }

  private static String randomPassword() {
    byte[] bytes = new byte[PASSWORD_BYTES];
    new SecureRandom().nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private static void kill(Process process) {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
  }
}

package dev.shoko.minecraft.tools.oracle;

import dev.shoko.minecraft.tools.oracle.rcon.RconAuthenticationException;
import dev.shoko.minecraft.tools.oracle.rcon.RconClient;
import java.io.IOException;
import java.io.OutputStream;
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

/**
 * One running reference server, driven as a black box (ADR 0002, point 2a): properties in, console
 * log, RCON responses and region files out.
 *
 * <p>The RCON password is random for each run, never logged nor passed on the command line. It is
 * written to {@code server.properties} (owner-only permissions where the file system has them) for
 * the server to read, and emptied there once the process has exited, whatever the outcome,
 * including through the shutdown hook. The run directory keeps {@code eula.txt}, {@code
 * server.properties} (seed included, so that a world can be reused with the same seed), the console
 * log and the world. If the JVM ends without {@link #stop()}, a shutdown hook destroys the server
 * and empties the password; a JVM killed outright runs no hook and leaves the server and its
 * password behind.
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

  private static final Duration RCON_TIMEOUT = Duration.ofMinutes(2);
  private static final Duration EXIT_GRACE = Duration.ofSeconds(5);
  private static final long RCON_RETRY_MILLIS = 200;
  private static final int PASSWORD_BYTES = 24;

  /** A started process and what must be undone when it ends. */
  private record Run(
      Path runDir, Process process, Thread pump, Thread hook, Consumer<String> log) {}

  private final Run run;
  private final RconClient rcon;
  private final Duration stopTimeout;
  private StopOutcome outcome;

  private OracleServer(Run run, RconClient rcon, Duration stopTimeout) {
    this.run = run;
    this.rcon = rcon;
    this.stopTimeout = stopTimeout;
  }

  /**
   * Checks the jar, prepares the run directory, starts the server and waits until it is ready and
   * RCON answers.
   *
   * @throws EulaNotAcceptedException when {@code options.acceptEula()} is false; nothing is written
   */
  public static OracleServer start(OracleOptions options, Consumer<String> log)
      throws IOException, InterruptedException {
    if (!options.acceptEula()) {
      throw new EulaNotAcceptedException();
    }
    options.command().reference().verify();
    Path runDir = options.runDir().toAbsolutePath();
    checkReusableWorld(runDir, options.settings());
    int[] ports = freePorts();
    String password = randomPassword();
    Files.createDirectories(runDir);
    ServerProperties.write(
        runDir, ServerProperties.render(options.settings(), ports[0], ports[1], password));
    Files.writeString(
        runDir.resolve(EULA_FILE),
        "# Accepted by the repository owner ("
            + EulaNotAcceptedException.EULA_URL
            + ")\neula=true\n",
        StandardCharsets.UTF_8);

    log.accept("oracle: starting " + options.command().reference().describe() + " in " + runDir);
    Process process =
        new ProcessBuilder(options.command().args())
            .directory(runDir.toFile())
            .redirectErrorStream(true)
            .start();
    Thread hook = new Thread(() -> killAndRedact(process, runDir), "oracle-shutdown");
    Runtime.getRuntime().addShutdownHook(hook);
    CompletableFuture<Void> ready = new CompletableFuture<>();
    Path consoleLog = runDir.resolve(CONSOLE_LOG);
    Thread pump = ConsolePump.start(process.getInputStream(), consoleLog, ready, log);
    Run run = new Run(runDir, process, pump, hook, log);
    long deadline = System.nanoTime() + options.startupTimeout().toNanos();
    try {
      awaitReady(run.process(), ready, options.startupTimeout(), consoleLog);
      RconClient rcon = connectRcon(run.process(), ports[1], password, deadline);
      log.accept("oracle: ready, RCON on 127.0.0.1:" + ports[1]);
      return new OracleServer(run, rcon, options.stopTimeout());
    } catch (IOException | RuntimeException | InterruptedException e) {
      kill(run.process());
      run.process().waitFor(options.stopTimeout().toMillis(), TimeUnit.MILLISECONDS);
      finish(run, options.stopTimeout());
      throw e;
    }
  }

  /** Process id of the server, for diagnostics. */
  public long pid() {
    return run.process().pid();
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
    Process process = run.process();
    try {
      rcon.close();
    } catch (IOException e) {
      run.log().accept("oracle: closing RCON failed: " + e.getMessage());
    }
    if (!process.isAlive()) {
      outcome = StopOutcome.ALREADY_EXITED;
    } else {
      sendConsole("stop");
      if (process.waitFor(stopTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
        outcome = StopOutcome.STOPPED;
      } else {
        run.log().accept("oracle: server ignored stop for " + stopTimeout + ", destroying it");
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        if (!process.waitFor(stopTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
          kill(process);
          process.waitFor();
        }
        outcome = StopOutcome.DESTROYED;
      }
    }
    finish(run, stopTimeout);
    run.log().accept("oracle: server " + outcome.name().toLowerCase(Locale.ROOT));
    return outcome;
  }

  /** Stops the server; an interruption kills the process at once and is passed on. */
  @Override
  public void close() {
    try {
      stop();
    } catch (InterruptedException e) {
      killAndRedact(run.process(), run.runDir());
      removeHook(run);
      Thread.currentThread().interrupt();
    }
  }

  /** Once the process has exited: drop the hook, wait for the console, empty the password. */
  private static void finish(Run run, Duration timeout) throws InterruptedException {
    removeHook(run);
    run.pump().join(timeout.toMillis());
    try {
      ServerProperties.redactPassword(run.runDir());
    } catch (IOException e) {
      run.log().accept("oracle: could not empty the RCON password: " + e.getMessage());
    }
  }

  private static void removeHook(Run run) {
    try {
      Runtime.getRuntime().removeShutdownHook(run.hook());
    } catch (IllegalStateException e) {
      // The JVM is already shutting down: the hook runs anyway.
    }
  }

  private void sendConsole(String command) {
    try {
      OutputStream stdin = run.process().getOutputStream();
      stdin.write((command + "\n").getBytes(StandardCharsets.UTF_8));
      stdin.flush();
    } catch (IOException e) {
      run.log().accept("oracle: could not write to the server console: " + e.getMessage());
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

  /**
   * Kills the server, waits briefly for it to exit, then empties the RCON password. Runs in the
   * shutdown hook and on an interrupted close, where no other code will do it.
   */
  private static void killAndRedact(Process process, Path runDir) {
    kill(process);
    try {
      process.waitFor(EXIT_GRACE.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    try {
      ServerProperties.redactPassword(runDir);
    } catch (IOException e) {
      // Nothing can be reported from a shutdown hook; the file keeps the password of a dead run.
    }
  }

  private static void kill(Process process) {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
  }
}

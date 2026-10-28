package dev.shoko.minecraft.tools.oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class OracleServerTest {

  private static final Duration STARTUP = Duration.ofSeconds(30);
  private static final Duration STOP = Duration.ofSeconds(10);

  private final List<String> log = Collections.synchronizedList(new ArrayList<>());

  static List<String> javaCommand(Class<?> main, String... args) {
    List<String> command = new ArrayList<>();
    command.add(OracleOptions.currentJava().toString());
    command.add("-cp");
    command.add(System.getProperty("java.class.path"));
    command.add(main.getName());
    command.addAll(List.of(args));
    return command;
  }

  static OracleOptions fake(
      Path runDir, String mode, boolean acceptEula, Duration startup, Duration stop) {
    ServerCommand command = ServerCommand.unchecked(javaCommand(FakeServerMain.class, mode));
    return new OracleOptions(runDir, command, ServerSettings.of(-42L), acceptEula, startup, stop);
  }

  private static Properties properties(Path runDir) throws IOException {
    Properties properties = new Properties();
    try (Reader reader =
        Files.newBufferedReader(runDir.resolve("server.properties"), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  @Test
  void refusesToStartWithoutTheEulaFlagAndWritesNothing(@TempDir Path dir) {
    Path runDir = dir.resolve("run-test");

    assertThatThrownBy(
            () -> OracleServer.start(fake(runDir, "ready", false, STARTUP, STOP), log::add))
        .isInstanceOf(EulaNotAcceptedException.class)
        .hasMessageContaining("https://aka.ms/MinecraftEULA")
        .hasMessageContaining(OracleMain.ACCEPT_EULA_FLAG);
    assertThat(runDir).doesNotExist();
    assertThat(log).isEmpty();
  }

  @Test
  void startsRunsCommandsAndStopsCleanly(@TempDir Path dir) throws Exception {
    Path runDir = dir.resolve("run-test");
    OracleServer.StopOutcome outcome;
    try (OracleServer server =
        OracleServer.start(fake(runDir, "ready", true, STARTUP, STOP), log::add)) {
      assertThat(server.command("seed")).isEqualTo("ran: seed");
      assertThat(server.command("time query daytime")).isEqualTo("ran: time query daytime");
      outcome = server.stop();
      assertThat(server.stop()).isEqualTo(outcome);
    }

    assertThat(outcome).isEqualTo(OracleServer.StopOutcome.STOPPED);
    assertThat(runDir.resolve("eula.txt")).content().contains("eula=true");
    assertThat(runDir.resolve(OracleServer.CONSOLE_LOG)).content().contains("console: stop");
    Properties properties = properties(runDir);
    assertThat(properties.getProperty("level-seed")).isEqualTo("-42");
    assertThat(properties.getProperty("enable-rcon")).isEqualTo("true");
    assertThat(properties.getProperty("online-mode")).isEqualTo("false");
    assertThat(properties.getProperty("rcon.password")).isEmpty();
    assertThat(properties.getProperty("rcon.port"))
        .isNotEqualTo(properties.getProperty("server-port"));
  }

  @Test
  void reusesAWorldWithTheSameSeedAfterTheRun(@TempDir Path dir) throws Exception {
    Path runDir = dir.resolve("run-test");
    OracleServer.start(fake(runDir, "ready", true, STARTUP, STOP), log::add).close();
    Files.createDirectories(runDir.resolve("world"));

    try (OracleServer server =
        OracleServer.start(fake(runDir, "ready", true, STARTUP, STOP), log::add)) {
      assertThat(server.command("seed")).isEqualTo("ran: seed");
    }
  }

  /** Password of the running server, read while it is still in the properties. */
  private static String runPassword(Path runDir) throws IOException {
    return properties(runDir).getProperty("rcon.password");
  }

  @Test
  void leavesTheRconPasswordNowhereOnceStopped(@TempDir Path dir) throws Exception {
    Path runDir = dir.resolve("run-test");
    String password;
    try (OracleServer server =
        OracleServer.start(fake(runDir, "ready", true, STARTUP, STOP), log::add)) {
      password = runPassword(runDir);
      server.command("list");
    }

    assertThat(password).matches("[0-9a-f]{48}");
    assertThat(log).isNotEmpty().noneMatch(line -> line.contains(password));
    try (Stream<Path> files = Files.walk(runDir)) {
      for (Path file : files.filter(Files::isRegularFile).toList()) {
        assertThat(Files.readString(file, StandardCharsets.ISO_8859_1))
            .as(file.toString())
            .doesNotContain(password);
      }
    }
  }

  @Test
  void emptiesThePasswordAlsoWhenTheServerIsDestroyedOrFailsToStart(@TempDir Path dir)
      throws Exception {
    Path destroyed = dir.resolve("run-destroyed");
    OracleOptions ignoring = fake(destroyed, "ignore-stop", true, STARTUP, Duration.ofSeconds(1));
    OracleServer.start(ignoring, log::add).close();
    Path failed = dir.resolve("run-failed");

    assertThatThrownBy(
            () -> OracleServer.start(fake(failed, "crash", true, STARTUP, STOP), log::add))
        .isInstanceOf(IOException.class);
    assertThat(runPassword(destroyed)).isEmpty();
    assertThat(runPassword(failed)).isEmpty();
  }

  @Test
  void drawsANewPasswordForEachRun(@TempDir Path dir) throws Exception {
    Path runDir = dir.resolve("run-test");
    String first;
    try (OracleServer server =
        OracleServer.start(fake(runDir, "ready", true, STARTUP, STOP), log::add)) {
      assertThat(server.command("seed")).isEqualTo("ran: seed");
      first = runPassword(runDir);
    }
    try (OracleServer server =
        OracleServer.start(fake(runDir, "ready", true, STARTUP, STOP), log::add)) {
      assertThat(server.command("seed")).isEqualTo("ran: seed");
      assertThat(runPassword(runDir)).hasSize(48).isNotEqualTo(first);
    }
  }

  @Test
  void checksTheReferenceJarBeforeWritingAnything(@TempDir Path cache) {
    OracleOptions options = OracleOptions.defaults(cache, "probe", ServerSettings.of(1), true);

    assertThatThrownBy(() -> OracleServer.start(options, log::add))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("state.json is missing: run tools/bootstrap");
    assertThat(cache).isEmptyDirectory();
  }

  @Test
  @Timeout(90)
  void destroysTheServerWhenTheJvmExitsWithoutStoppingIt(@TempDir Path dir) throws Exception {
    Process host =
        new ProcessBuilder(javaCommand(HostMain.class, dir.resolve("run-host").toString()))
            .redirectErrorStream(true)
            .start();
    long pid = -1;
    StringBuilder output = new StringBuilder();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(host.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append('\n');
        if (line.startsWith("PID ")) {
          pid = Long.parseLong(line.substring(4));
        }
      }
    }
    assertThat(host.waitFor(30, TimeUnit.SECONDS)).as(output.toString()).isTrue();
    assertThat(pid).as(output.toString()).isPositive();
    Optional<ProcessHandle> server = ProcessHandle.of(pid);
    try {
      if (server.isPresent()) {
        server.get().onExit().get(20, TimeUnit.SECONDS);
      }
      assertThat(server.map(ProcessHandle::isAlive)).isNotEqualTo(Optional.of(true));
    } catch (java.util.concurrent.TimeoutException e) {
      throw new AssertionError("server " + pid + " survived its JVM\n" + output, e);
    } finally {
      server.ifPresent(ProcessHandle::destroyForcibly);
    }
  }

  @Test
  void failsWhenTheServerIsNotReadyInTime(@TempDir Path dir) {
    Path runDir = dir.resolve("run-test");
    OracleOptions options = fake(runDir, "never-ready", true, Duration.ofSeconds(2), STOP);

    assertThatThrownBy(() -> OracleServer.start(options, log::add))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("not ready within PT2S")
        .hasMessageContaining(OracleServer.CONSOLE_LOG);
    assertThat(runDir.resolve(OracleServer.CONSOLE_LOG)).content().contains("Starting fake");
  }

  @Test
  void reportsAServerThatExitsBeforeBeingReady(@TempDir Path dir) {
    Path runDir = dir.resolve("run-test");

    assertThatThrownBy(
            () -> OracleServer.start(fake(runDir, "crash", true, STARTUP, STOP), log::add))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("exit code 3");
  }

  @Test
  void destroysAServerThatIgnoresStop(@TempDir Path dir) throws Exception {
    Path runDir = dir.resolve("run-test");
    OracleOptions options = fake(runDir, "ignore-stop", true, STARTUP, Duration.ofSeconds(1));
    try (OracleServer server = OracleServer.start(options, log::add)) {
      assertThat(server.stop()).isEqualTo(OracleServer.StopOutcome.DESTROYED);
    }
    assertThat(log).anyMatch(line -> line.contains("ignored stop"));
  }

  @Test
  void refusesToReuseAWorldCreatedWithAnotherSeed(@TempDir Path dir) throws IOException {
    Path runDir = dir.resolve("run-test");
    Files.createDirectories(runDir.resolve("world"));
    Files.writeString(runDir.resolve("server.properties"), "level-seed=5\n");

    assertThatThrownBy(
            () -> OracleServer.start(fake(runDir, "ready", true, STARTUP, STOP), log::add))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("already exists with seed 5, not -42");
    assertThat(runDir.resolve("eula.txt")).doesNotExist();
  }
}

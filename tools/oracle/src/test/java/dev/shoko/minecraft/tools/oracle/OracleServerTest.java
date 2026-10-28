package dev.shoko.minecraft.tools.oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OracleServerTest {

  private static final Duration STARTUP = Duration.ofSeconds(30);
  private static final Duration STOP = Duration.ofSeconds(10);

  private final List<String> log = Collections.synchronizedList(new ArrayList<>());

  private static OracleOptions fake(
      Path runDir, String mode, boolean acceptEula, Duration startup, Duration stop) {
    List<String> command =
        List.of(
            OracleOptions.currentJava().toString(),
            "-cp",
            System.getProperty("java.class.path"),
            FakeServerMain.class.getName(),
            mode);
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
    assertThat(properties.getProperty("rcon.port"))
        .isNotEqualTo(properties.getProperty("server-port"));
  }

  @Test
  void neverWritesTheRconPasswordAnywhereButTheProperties(@TempDir Path dir) throws Exception {
    Path runDir = dir.resolve("run-test");
    try (OracleServer server =
        OracleServer.start(fake(runDir, "ready", true, STARTUP, STOP), log::add)) {
      server.command("list");
    }
    String password = properties(runDir).getProperty("rcon.password");

    assertThat(password).matches("[0-9a-f]{48}");
    assertThat(log).isNotEmpty().noneMatch(line -> line.contains(password));
    try (Stream<Path> files = Files.walk(runDir)) {
      for (Path file : files.filter(Files::isRegularFile).toList()) {
        if (!file.getFileName().toString().equals("server.properties")) {
          assertThat(Files.readString(file, StandardCharsets.ISO_8859_1))
              .as(file.toString())
              .doesNotContain(password);
        }
      }
    }
  }

  @Test
  void drawsANewPasswordForEachRun(@TempDir Path dir) throws Exception {
    Path runDir = dir.resolve("run-test");
    OracleServer.start(fake(runDir, "ready", true, STARTUP, STOP), log::add).close();
    String first = properties(runDir).getProperty("rcon.password");
    OracleServer.start(fake(runDir, "ready", true, STARTUP, STOP), log::add).close();

    assertThat(properties(runDir).getProperty("rcon.password")).isNotEqualTo(first);
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

package dev.shoko.minecraft.tools.oracle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OracleMainTest {

  private final ByteArrayOutputStream out = new ByteArrayOutputStream();
  private final ByteArrayOutputStream err = new ByteArrayOutputStream();

  private int run(String... args) {
    return OracleMain.run(
        args,
        new PrintStream(out, true, StandardCharsets.UTF_8),
        new PrintStream(err, true, StandardCharsets.UTF_8));
  }

  private String errors() {
    return err.toString(StandardCharsets.UTF_8);
  }

  @Test
  void refusesToRunWithoutTheEulaFlagAndWritesNothing(@TempDir Path cache) {
    int exit = run("--cache-dir", cache.toString(), "--name", "probe", "--seed", "1");

    assertThat(exit).isEqualTo(OracleMain.EXIT_USAGE);
    assertThat(errors())
        .contains("https://aka.ms/MinecraftEULA")
        .contains(OracleMain.ACCEPT_EULA_FLAG);
    assertThat(cache).isEmptyDirectory();
  }

  @Test
  void needsTheBootstrappedServerJar(@TempDir Path cache) {
    int exit =
        run(
            "--cache-dir",
            cache.toString(),
            "--name",
            "probe",
            "--seed",
            "1",
            OracleMain.ACCEPT_EULA_FLAG);

    assertThat(exit).isEqualTo(OracleMain.EXIT_FAILURE);
    assertThat(errors()).contains("server-26.3.jar").contains("bootstrap");
    assertThat(cache).isEmptyDirectory();
  }

  @Test
  void rejectsInvalidArguments(@TempDir Path cache) {
    String dir = cache.toString();
    String flag = OracleMain.ACCEPT_EULA_FLAG;

    assertThat(run("--cache-dir", dir, "--name", "probe", "--seed", "abc", flag))
        .isEqualTo(OracleMain.EXIT_USAGE);
    assertThat(run("--cache-dir", dir, "--name", "../escape", "--seed", "1", flag))
        .isEqualTo(OracleMain.EXIT_USAGE);
    assertThat(run("--cache-dir", dir, "--seed", "1", flag)).isEqualTo(OracleMain.EXIT_USAGE);
    assertThat(run("--cache-dir", dir, "--name", "p", "--seed", "1", "--difficulty", "x", flag))
        .isEqualTo(OracleMain.EXIT_USAGE);
    assertThat(run("--cache-dir", dir, flag, "--name")).isEqualTo(OracleMain.EXIT_USAGE);
    assertThat(cache).isEmptyDirectory();
  }
}

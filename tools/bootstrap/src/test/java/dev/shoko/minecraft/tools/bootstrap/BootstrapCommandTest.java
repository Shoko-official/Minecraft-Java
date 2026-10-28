package dev.shoko.minecraft.tools.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Command line of the tool: exit codes and messages. */
class BootstrapCommandTest {

  @TempDir Path dir;

  private static int execute(ByteArrayOutputStream err, String... args) {
    return Bootstrap.execute(
        List.of(args), null, new PrintStream(err, true, StandardCharsets.UTF_8));
  }

  @Test
  void anUnknownArgumentPrintsTheUsage() {
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    assertThat(execute(err, "--bogus")).isEqualTo(2);
    assertThat(err.toString(StandardCharsets.UTF_8)).contains("--bogus").contains("usage:");
  }

  @Test
  void aMissingOrInvalidCacheDirectoryPrintsTheUsage() {
    ByteArrayOutputStream missing = new ByteArrayOutputStream();
    ByteArrayOutputStream invalid = new ByteArrayOutputStream();

    assertThat(execute(missing, "--cache-dir")).isEqualTo(2);
    assertThat(execute(invalid, "--cache-dir", "bad\0path")).isEqualTo(2);
    assertThat(missing.toString(StandardCharsets.UTF_8)).contains("usage:");
    assertThat(invalid.toString(StandardCharsets.UTF_8)).contains("usage:");
  }

  @Test
  void aFailureReportsTheExceptionTypeWithoutTheUsage() throws IOException {
    Path notADirectory = Files.writeString(dir.resolve("cache-file"), "x");
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    assertThat(execute(err, "--cache-dir", notADirectory.toString(), "--no-generate")).isEqualTo(1);
    assertThat(err.toString(StandardCharsets.UTF_8))
        .startsWith("bootstrap failed: java.nio.file.")
        .doesNotContain("usage:");
  }
}

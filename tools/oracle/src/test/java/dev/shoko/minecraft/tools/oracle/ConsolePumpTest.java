package dev.shoko.minecraft.tools.oracle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class ConsolePumpTest {

  private static final String READY_LINE =
      "[00:00:01] [Server thread/INFO]: Done (1.0s)! For help, type \"help\"";

  private final List<String> log = Collections.synchronizedList(new ArrayList<>());
  private final CompletableFuture<Void> ready = new CompletableFuture<>();

  /** Many console lines, with the ready line in the middle. */
  private static ByteArrayInputStream console() {
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < 20_000; i++) {
      text.append(i == 10_000 ? READY_LINE : "line " + i).append('\n');
    }
    return new ByteArrayInputStream(text.toString().getBytes(StandardCharsets.ISO_8859_1));
  }

  /** A log that breaks after a few writes, like a full disk. */
  private static final class FailingWriter extends Writer {
    private int writes;

    @Override
    public void write(char[] buffer, int offset, int length) throws IOException {
      if (++writes > 6) {
        throw new IOException("disk full");
      }
    }

    @Override
    public void flush() {}

    @Override
    public void close() {}
  }

  @Test
  @Timeout(10)
  void keepsDrainingTheConsoleWhenTheLogFails() {
    ByteArrayInputStream console = console();

    ConsolePump.run(console, new FailingWriter(), ready, log::add);

    assertThat(console.available()).isZero();
    assertThat(ready).isCompleted();
    assertThat(log).filteredOn(line -> line.contains("still draining")).hasSize(1);
  }

  @Test
  @Timeout(10)
  void drainsTheConsoleWhenTheLogCannotBeOpened(@TempDir Path dir) throws InterruptedException {
    ByteArrayInputStream console = console();

    ConsolePump.start(console, dir, ready, log::add).join();

    assertThat(console.available()).isZero();
    assertThat(ready).isCompleted();
    assertThat(log).anyMatch(line -> line.contains("cannot write"));
  }

  @Test
  void failsTheReadyFutureWhenTheConsoleEndsFirst() {
    ConsolePump.run(
        new ByteArrayInputStream("Starting\n".getBytes(StandardCharsets.ISO_8859_1)),
        Writer.nullWriter(),
        ready,
        log::add);

    assertThat(ready).isCompletedExceptionally();
  }
}

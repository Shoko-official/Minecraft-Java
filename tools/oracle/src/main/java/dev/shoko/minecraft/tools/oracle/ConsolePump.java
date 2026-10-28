package dev.shoko.minecraft.tools.oracle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Copies the server console to a log file and completes a future on the ready line. The console is
 * always read to its end, even when the log file cannot be written: a full pipe would block the
 * server.
 */
final class ConsolePump {

  /** Console line printed by the server once it accepts commands. */
  static final Pattern READY = Pattern.compile("Done \\([^)]*\\)! For help, type \"help\"");

  private ConsolePump() {}

  /** Starts a daemon thread that pumps {@code console} into {@code logFile}. */
  static Thread start(
      InputStream console, Path logFile, CompletableFuture<Void> ready, Consumer<String> log) {
    Writer writer;
    try {
      // ISO-8859-1 maps every byte to one char and back: the log keeps the console bytes as is.
      writer = Files.newBufferedWriter(logFile, StandardCharsets.ISO_8859_1);
    } catch (IOException e) {
      log.accept("oracle: cannot write " + logFile + " (" + e.getMessage() + "), console dropped");
      writer = Writer.nullWriter();
    }
    Writer target = writer;
    return Thread.ofPlatform()
        .daemon()
        .name("oracle-console")
        .start(() -> run(console, target, ready, log));
  }

  /**
   * Pumps until the console ends, then completes {@code ready} exceptionally if the ready line was
   * never seen. A failing writer is reported once and dropped; reading goes on.
   */
  static void run(
      InputStream console, Writer writer, CompletableFuture<Void> ready, Consumer<String> log) {
    Writer target = writer;
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(console, StandardCharsets.ISO_8859_1))) {
      String line;
      while ((line = reader.readLine()) != null) {
        target = write(target, line, log);
        if (!ready.isDone() && READY.matcher(line).find()) {
          ready.complete(null);
        }
      }
      ready.completeExceptionally(new IOException("server console closed"));
    } catch (IOException e) {
      ready.completeExceptionally(e);
      log.accept("oracle: reading the server console failed: " + e.getMessage());
    } finally {
      close(target, log);
    }
  }

  private static Writer write(Writer target, String line, Consumer<String> log) {
    try {
      target.write(line);
      target.write('\n');
      target.flush();
      return target;
    } catch (IOException e) {
      log.accept("oracle: console log failed (" + e.getMessage() + "), still draining the console");
      close(target, log);
      return Writer.nullWriter();
    }
  }

  private static void close(Writer target, Consumer<String> log) {
    try {
      target.close();
    } catch (IOException e) {
      log.accept("oracle: closing the console log failed: " + e.getMessage());
    }
  }
}

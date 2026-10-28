package dev.shoko.minecraft.tools.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs the data generator shipped with the official server, as a program: arguments in, report
 * files out. The entry point is the documented one for server bundles (ADR 0002, point 2a).
 */
final class GeneratorRunner {

  private static final long TIMEOUT_MINUTES = 5;

  private GeneratorRunner() {}

  /** Generates the data reports into {@code outputDir}, running from {@code workDir}. */
  static void reports(Path serverJar, Path workDir, Path outputDir)
      throws IOException, InterruptedException {
    Files.createDirectories(workDir);
    Files.createDirectories(outputDir);
    Path log = workDir.resolve("generator.log");
    Path java = Path.of(System.getProperty("java.home"), "bin", "java");
    List<String> command =
        List.of(
            java.toString(),
            "-DbundlerMainClass=net.minecraft.data.Main",
            "-jar",
            serverJar.toAbsolutePath().toString(),
            "--reports",
            "--output",
            outputDir.toAbsolutePath().toString());
    Process process =
        new ProcessBuilder(command)
            .directory(workDir.toFile())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();
    if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
      process.destroyForcibly();
      throw new IOException("Data generator timed out after " + TIMEOUT_MINUTES + " minutes");
    }
    if (process.exitValue() != 0) {
      throw new IOException(
          "Data generator exited with " + process.exitValue() + ", see " + log.toAbsolutePath());
    }
  }
}

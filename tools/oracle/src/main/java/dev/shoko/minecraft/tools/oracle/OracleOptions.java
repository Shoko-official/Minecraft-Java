package dev.shoko.minecraft.tools.oracle;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * How to run one oracle server.
 *
 * @param runDir working directory of the server: properties, world and console log
 * @param command process to start from {@code runDir}, with the check of its jar
 * @param acceptEula true only when the repository owner accepted the Minecraft EULA in person
 * @param startupTimeout time allowed until the server is ready and RCON answers
 * @param stopTimeout time allowed after {@code stop}, then again after a polite destroy
 */
public record OracleOptions(
    Path runDir,
    ServerCommand command,
    ServerSettings settings,
    boolean acceptEula,
    Duration startupTimeout,
    Duration stopTimeout) {

  /** Target version, as pinned in docs/specs/reference-binaries.md. */
  public static final String GAME_VERSION = "26.3";

  private static final Pattern RUN_NAME = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");
  private static final Duration DEFAULT_STARTUP = Duration.ofMinutes(5);
  private static final Duration DEFAULT_STOP = Duration.ofSeconds(60);

  public OracleOptions {
    if (startupTimeout.isNegative() || startupTimeout.isZero()) {
      throw new IllegalArgumentException("startup timeout must be positive");
    }
    if (stopTimeout.isNegative() || stopTimeout.isZero()) {
      throw new IllegalArgumentException("stop timeout must be positive");
    }
  }

  /**
   * Runs the official server bundle of the cache, {@code <cache>/downloads/server-26.3.jar}, with
   * the Java of the current JVM, in {@code <cache>/oracle/run-<name>/}, once {@link ReferenceJar}
   * has checked it against the bootstrap state.
   */
  public static OracleOptions defaults(
      Path cacheDir, String runName, ServerSettings settings, boolean acceptEula) {
    return new OracleOptions(
        runDir(cacheDir, runName),
        new ServerCommand(
            List.of(
                currentJava().toString(),
                "-jar",
                serverJar(cacheDir).toAbsolutePath().toString(),
                "nogui"),
            ReferenceJar.inCache(cacheDir)),
        settings,
        acceptEula,
        DEFAULT_STARTUP,
        DEFAULT_STOP);
  }

  public static Path serverJar(Path cacheDir) {
    return cacheDir.resolve("downloads").resolve("server-" + GAME_VERSION + ".jar");
  }

  /** Run directory for a name made of lower-case letters, digits, dots, dashes, underscores. */
  public static Path runDir(Path cacheDir, String runName) {
    if (!RUN_NAME.matcher(runName).matches()) {
      throw new IllegalArgumentException("run name must match " + RUN_NAME + ": " + runName);
    }
    return cacheDir.resolve("oracle").resolve("run-" + runName);
  }

  public static Path currentJava() {
    return Path.of(System.getProperty("java.home"), "bin", "java");
  }
}

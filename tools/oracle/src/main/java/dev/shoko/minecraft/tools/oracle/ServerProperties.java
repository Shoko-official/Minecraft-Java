package dev.shoko.minecraft.tools.oracle;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Writes the {@code server.properties} of an oracle run, in a fixed order with no timestamp, and
 * reads back the seed of an earlier run.
 */
final class ServerProperties {

  static final String FILE_NAME = "server.properties";
  static final String PASSWORD_KEY = "rcon.password";

  private static final Set<PosixFilePermission> OWNER_ONLY =
      PosixFilePermissions.fromString("rw-------");

  private ServerProperties() {}

  /**
   * Writes the file, readable by its owner only where the file system has POSIX permissions: it
   * holds the RCON password while the server runs. Permissions are best effort, never an error.
   */
  static void write(Path runDir, String content) throws IOException {
    Path file = runDir.resolve(FILE_NAME);
    restrictToOwner(file);
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }

  private static void restrictToOwner(Path file) {
    if (!file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
      return;
    }
    try {
      if (Files.exists(file)) {
        Files.setPosixFilePermissions(file, OWNER_ONLY);
      } else {
        Files.createFile(file, PosixFilePermissions.asFileAttribute(OWNER_ONLY));
      }
    } catch (IOException | UnsupportedOperationException e) {
      // Best effort: the password is emptied when the server exits anyway.
    }
  }

  /**
   * Renders the file. The server only listens on the loopback interface since online mode is off.
   * Region files are forced to zlib, the compression the independent reader decodes.
   */
  static String render(ServerSettings settings, int serverPort, int rconPort, String rconPassword) {
    StringBuilder text = new StringBuilder();
    line(text, "level-seed", Long.toString(settings.seed()));
    line(text, "level-name", settings.levelName());
    line(text, "difficulty", settings.difficulty());
    line(text, "gamemode", settings.gamemode());
    line(text, "view-distance", Integer.toString(settings.viewDistance()));
    line(text, "simulation-distance", Integer.toString(settings.simulationDistance()));
    line(text, "server-ip", "127.0.0.1");
    line(text, "server-port", Integer.toString(serverPort));
    line(text, "online-mode", "false");
    line(text, "spawn-protection", "0");
    line(text, "motd", "oracle");
    line(text, "enable-rcon", "true");
    line(text, "rcon.port", Integer.toString(rconPort));
    line(text, PASSWORD_KEY, rconPassword);
    line(text, "enable-query", "false");
    line(text, "region-file-compression", "deflate");
    return text.toString();
  }

  /** Seed recorded in an existing {@code server.properties}, empty when there is none. */
  static Optional<String> recordedSeed(Path runDir) throws IOException {
    Path file = runDir.resolve(FILE_NAME);
    if (!Files.isRegularFile(file)) {
      return Optional.empty();
    }
    Properties properties = new Properties();
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return Optional.ofNullable(properties.getProperty("level-seed"));
  }

  /**
   * Empties the RCON password once the server has exited, keeping every other line, the seed
   * included, so that the world can still be reused. Does nothing when the file is absent.
   */
  static void redactPassword(Path runDir) throws IOException {
    Path file = runDir.resolve(FILE_NAME);
    if (!Files.isRegularFile(file)) {
      return;
    }
    StringBuilder text = new StringBuilder();
    for (String line : Files.readAllLines(file, StandardCharsets.ISO_8859_1)) {
      text.append(line.startsWith(PASSWORD_KEY + "=") ? PASSWORD_KEY + "=" : line).append('\n');
    }
    Files.writeString(file, text, StandardCharsets.ISO_8859_1);
  }

  private static void line(StringBuilder text, String key, String value) {
    text.append(key).append('=').append(value).append('\n');
  }
}

package dev.shoko.minecraft.tools.oracle;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Writes the {@code server.properties} of an oracle run, in a fixed order with no timestamp, and
 * reads back the seed of an earlier run.
 */
final class ServerProperties {

  static final String FILE_NAME = "server.properties";

  private ServerProperties() {}

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
    line(text, "rcon.password", rconPassword);
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

  private static void line(StringBuilder text, String key, String value) {
    text.append(key).append('=').append(value).append('\n');
  }
}

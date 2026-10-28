package dev.shoko.minecraft.tools.oracle;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * World and gameplay settings written to {@code server.properties}. Values are validated so that
 * nothing can inject another property line.
 */
public record ServerSettings(
    long seed,
    String levelName,
    String difficulty,
    String gamemode,
    int viewDistance,
    int simulationDistance) {

  private static final Pattern LEVEL_NAME = Pattern.compile("[A-Za-z0-9_-]{1,64}");
  private static final Set<String> DIFFICULTIES = Set.of("peaceful", "easy", "normal", "hard");
  private static final Set<String> GAMEMODES =
      Set.of("survival", "creative", "adventure", "spectator");

  /** Small distances keep the server cheap; the zones to compare are generated on demand. */
  private static final int SMALL_DISTANCE = 4;

  // Range accepted by the server for both distances, from the public server.properties docs.
  private static final int MIN_DISTANCE = 3;
  private static final int MAX_DISTANCE = 32;

  public ServerSettings {
    if (!LEVEL_NAME.matcher(levelName).matches()) {
      throw new IllegalArgumentException("level name must match " + LEVEL_NAME + ": " + levelName);
    }
    if (!DIFFICULTIES.contains(difficulty)) {
      throw new IllegalArgumentException("unknown difficulty: " + difficulty);
    }
    if (!GAMEMODES.contains(gamemode)) {
      throw new IllegalArgumentException("unknown gamemode: " + gamemode);
    }
    checkDistance("view distance", viewDistance);
    checkDistance("simulation distance", simulationDistance);
  }

  /** Default settings for a seed: level "world", easy, survival, small distances. */
  public static ServerSettings of(long seed) {
    return new ServerSettings(seed, "world", "easy", "survival", SMALL_DISTANCE, SMALL_DISTANCE);
  }

  public ServerSettings withDifficulty(String value) {
    return new ServerSettings(seed, levelName, value, gamemode, viewDistance, simulationDistance);
  }

  public ServerSettings withGamemode(String value) {
    return new ServerSettings(seed, levelName, difficulty, value, viewDistance, simulationDistance);
  }

  public ServerSettings withLevelName(String value) {
    return new ServerSettings(seed, value, difficulty, gamemode, viewDistance, simulationDistance);
  }

  private static void checkDistance(String what, int value) {
    if (value < MIN_DISTANCE || value > MAX_DISTANCE) {
      throw new IllegalArgumentException(
          what + " must be in " + MIN_DISTANCE + ".." + MAX_DISTANCE + ": " + value);
    }
  }
}

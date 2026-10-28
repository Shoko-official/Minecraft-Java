package dev.shoko.minecraft.tools.oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The values end up in {@code server.properties}: none may add or alter a property line. */
class ServerSettingsTest {

  private static ServerSettings with(String level, String difficulty, String gamemode, int view) {
    return new ServerSettings(1L, level, difficulty, gamemode, view, 4);
  }

  @Test
  void refusesALevelNameThatWouldInjectAnotherProperty() {
    for (String name :
        List.of("w\nrcon.password=", "w\rx", "a=b", "a b", "a:b", "a#b", "a/b", "a\\b", "..", "")) {
      assertThatThrownBy(() -> ServerSettings.of(1L).withLevelName(name))
          .as(name)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("level name");
    }
  }

  @Test
  void refusesALevelNameOfMoreThan64Characters() {
    String longest = "a".repeat(64);

    assertThat(ServerSettings.of(1L).withLevelName(longest).levelName()).isEqualTo(longest);
    assertThatThrownBy(() -> ServerSettings.of(1L).withLevelName(longest + "a"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void refusesAGameModeOrDifficultyThatIsNotKnown() {
    for (String value : List.of("", "Survival", "hardcore", "survival\nx=y", "creative ")) {
      assertThatThrownBy(() -> ServerSettings.of(1L).withGamemode(value))
          .as(value)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("gamemode");
      assertThatThrownBy(() -> ServerSettings.of(1L).withDifficulty(value))
          .as(value)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("difficulty");
    }
  }

  @Test
  void acceptsEveryKnownGameModeAndDifficulty() {
    for (String mode : List.of("survival", "creative", "adventure", "spectator")) {
      assertThat(ServerSettings.of(1L).withGamemode(mode).gamemode()).isEqualTo(mode);
    }
    for (String difficulty : List.of("peaceful", "easy", "normal", "hard")) {
      assertThat(ServerSettings.of(1L).withDifficulty(difficulty).difficulty())
          .isEqualTo(difficulty);
    }
  }

  @Test
  void acceptsDistancesOnlyFrom3To32() {
    assertThatCode(() -> with("world", "easy", "survival", 3)).doesNotThrowAnyException();
    assertThatCode(() -> with("world", "easy", "survival", 32)).doesNotThrowAnyException();
    assertThatCode(() -> new ServerSettings(1L, "world", "easy", "survival", 3, 32))
        .doesNotThrowAnyException();
    for (int bad : new int[] {Integer.MIN_VALUE, -1, 0, 2, 33, Integer.MAX_VALUE}) {
      assertThatThrownBy(() -> with("world", "easy", "survival", bad))
          .as("view " + bad)
          .hasMessageContaining("view distance must be in 3..32");
      assertThatThrownBy(() -> new ServerSettings(1L, "world", "easy", "survival", 4, bad))
          .as("simulation " + bad)
          .hasMessageContaining("simulation distance must be in 3..32");
    }
  }

  @Test
  void acceptsTheEdgeLevelNames() {
    for (String name : List.of("a", "A_z-0", "0".repeat(64), "-", "_")) {
      assertThat(ServerSettings.of(1L).withLevelName(name).levelName()).isEqualTo(name);
    }
  }

  @Test
  void defaultsToSmallDistancesAndTheWorldLevel() {
    ServerSettings settings = ServerSettings.of(-7L);

    assertThat(settings).isEqualTo(new ServerSettings(-7L, "world", "easy", "survival", 4, 4));
  }
}

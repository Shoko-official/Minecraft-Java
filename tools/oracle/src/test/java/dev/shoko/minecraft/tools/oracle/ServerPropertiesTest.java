package dev.shoko.minecraft.tools.oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerPropertiesTest {

  @Test
  void rendersAFixedOrderWithTheRequestedSettings() {
    ServerSettings settings =
        ServerSettings.of(Long.MIN_VALUE).withDifficulty("hard").withGamemode("creative");

    String text = ServerProperties.render(settings, 25001, 25002, "pw");

    assertThat(text.lines())
        .containsExactly(
            "level-seed=-9223372036854775808",
            "level-name=world",
            "difficulty=hard",
            "gamemode=creative",
            "view-distance=4",
            "simulation-distance=4",
            "server-ip=127.0.0.1",
            "server-port=25001",
            "online-mode=false",
            "spawn-protection=0",
            "motd=oracle",
            "enable-rcon=true",
            "rcon.port=25002",
            "rcon.password=pw",
            "enable-query=false",
            "region-file-compression=deflate");
  }

  @Test
  void readsBackTheRecordedSeed(@TempDir Path dir) throws IOException {
    assertThat(ServerProperties.recordedSeed(dir)).isEmpty();

    Files.writeString(dir.resolve("server.properties"), "level-seed=12345\n");

    assertThat(ServerProperties.recordedSeed(dir)).contains("12345");
  }

  @Test
  void settingsRefuseValuesThatCouldInjectAPropertyLine() {
    ServerSettings settings = ServerSettings.of(1);

    assertThatThrownBy(() -> settings.withLevelName("world\nenable-rcon=false"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> settings.withDifficulty("easy\nx=y"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ServerSettings(1, "world", "easy", "survival", 2, 4))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void writesTheContentAndReplacesAnEarlierFile(@TempDir Path dir) throws IOException {
    ServerProperties.write(dir, "level-seed=1\nrcon.password=old\n");
    ServerProperties.write(dir, "level-seed=2\n");

    assertThat(dir.resolve("server.properties")).content().isEqualTo("level-seed=2\n");
  }

  @Test
  void writesTheFileReadableByItsOwnerOnlyWhereTheFileSystemHasPosixPermissions(@TempDir Path dir)
      throws IOException {
    assumeTrue(dir.getFileSystem().supportedFileAttributeViews().contains("posix"));
    Set<PosixFilePermission> ownerOnly = PosixFilePermissions.fromString("rw-------");

    ServerProperties.write(dir, "a=b\n");
    assertThat(Files.getPosixFilePermissions(dir.resolve("server.properties")))
        .isEqualTo(ownerOnly);

    Files.setPosixFilePermissions(
        dir.resolve("server.properties"), PosixFilePermissions.fromString("rw-r--r--"));
    ServerProperties.write(dir, "a=c\n");
    assertThat(Files.getPosixFilePermissions(dir.resolve("server.properties")))
        .isEqualTo(ownerOnly);
  }
}

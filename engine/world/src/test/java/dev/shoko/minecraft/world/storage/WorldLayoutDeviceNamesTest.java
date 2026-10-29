package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * AR-R1 and the contract of {@link WorldLayout#dimensionFolder}: the DOS device names (con, prn,
 * aux, nul, com1 to com9, lpt1 to lpt9), with or without an extension, are not usable folder names
 * on Windows and are refused in the namespace and in the path; the names next to them are not.
 */
class WorldLayoutDeviceNamesTest {

  @TempDir Path world;

  static Stream<String> deviceNames() {
    return Stream.concat(
        Stream.of("con", "prn", "aux", "nul"),
        Stream.concat(
            IntStream.rangeClosed(1, 9).mapToObj(n -> "com" + n),
            IntStream.rangeClosed(1, 9).mapToObj(n -> "lpt" + n)));
  }

  @ParameterizedTest
  @MethodSource("deviceNames")
  void aDeviceNameIsRefusedAsThePath(String device) {
    assertThatThrownBy(() -> WorldLayout.dimensionFolder(world, "minecraft:" + device))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @MethodSource("deviceNames")
  void aDeviceNameIsRefusedAsTheNamespace(String device) {
    assertThatThrownBy(() -> WorldLayout.dimensionFolder(world, device + ":overworld"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @MethodSource("deviceNames")
  void aDeviceNameWithAnExtensionIsRefusedAsThePath(String device) {
    assertThatThrownBy(() -> WorldLayout.dimensionFolder(world, "minecraft:" + device + ".txt"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> WorldLayout.dimensionFolder(world, "minecraft:" + device + ".a.b"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @MethodSource("deviceNames")
  void aDeviceNameWithAnExtensionIsRefusedAsTheNamespace(String device) {
    assertThatThrownBy(() -> WorldLayout.dimensionFolder(world, device + ".ext:overworld"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "minecraft:com0",
        "minecraft:lpt0",
        "minecraft:com10",
        "minecraft:lpt10",
        "minecraft:console",
        "minecraft:conx",
        "minecraft:xcon",
        "minecraft:nulls",
        "minecraft:a..b",
        "minecraft:.hidden",
        "minecraft:overworld",
        "minecraft:the_nether",
        "com0:overworld",
        "console:overworld",
        "a..b:overworld",
        ".hidden:overworld",
        "my_pack:com0.txt",
        "my-pack:the.end"
      })
  void theNamesNextToTheDeviceNamesAreAccepted(String dimensionId) {
    int colon = dimensionId.indexOf(':');

    Path folder = WorldLayout.dimensionFolder(world, dimensionId);

    assertThat(folder)
        .isEqualTo(
            world
                .resolve("dimensions")
                .resolve(dimensionId.substring(0, colon))
                .resolve(dimensionId.substring(colon + 1)));
  }
}

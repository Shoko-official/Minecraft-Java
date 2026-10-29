package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Bug-hunter probes on the folder names of a dimension. */
class WorldLayoutHuntTest {

  @TempDir Path world;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "minecraft:con",
        "minecraft:nul",
        "minecraft:aux",
        "minecraft:prn",
        "minecraft:com1",
        "minecraft:lpt1",
        "minecraft:con.txt",
        "con:overworld",
        "nul:x"
      })
  void windowsDeviceNamesAreRefusedLikeTheOtherUnsafeNames(String dimensionId) {
    // WorldLayout says a part is limited to characters that are safe in a file name on every
    // platform; a Windows device name is not a usable folder name
    assertThatThrownBy(() -> WorldLayout.dimensionFolder(world, dimensionId))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"minecraft:overworld.", "minecraft:the_end..", "end.:the_end"})
  void aTrailingDotIsRefusedBecauseWindowsDropsIt(String dimensionId) {
    assertThatThrownBy(() -> WorldLayout.dimensionFolder(world, dimensionId))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void twoDifferentDimensionsNeverShareAFolderOnThisPlatform() throws Exception {
    Path plain = WorldLayout.dimensionFolder(world, "minecraft:foo");
    Files.createDirectories(plain);
    Path dotted;
    try {
      dotted = WorldLayout.dimensionFolder(world, "minecraft:foo.");
    } catch (IllegalArgumentException refused) {
      return;
    }
    Files.createDirectories(dotted);
    assertThat(Files.isSameFile(plain, dotted)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"minecraft:con", "minecraft:nul", "minecraft:aux", "minecraft:com1"})
  void aDimensionWhoseNameIsADeviceNameCanStillHoldARegionFile(String dimensionId)
      throws Exception {
    Path region;
    try {
      region = WorldLayout.regionFolder(WorldLayout.dimensionFolder(world, dimensionId));
    } catch (IllegalArgumentException refused) {
      return;
    }
    dev.shoko.minecraft.core.coord.ChunkPos pos = new dev.shoko.minecraft.core.coord.ChunkPos(1, 1);
    byte[] nbt = {10, 0, 0, 0};
    try (RegionFile file = RegionFile.open(region, pos.region())) {
      file.write(pos, nbt, RegionCompression.ZLIB, 1L);
    }
    try (RegionFile file = RegionFile.open(region, pos.region())) {
      assertThat(file.read(pos)).isInstanceOf(dev.shoko.minecraft.world.ReadResult.Present.class);
    }
  }
}

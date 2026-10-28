package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.RegionPos;
import dev.shoko.minecraft.world.support.Failures;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** AR-R1 to AR-R4, AR-R6, AR-R17: folders and file names of a 26.3 world. */
class WorldLayoutTest {

  private static final Path ROOT = Path.of("saves", "world");

  @Test
  void dimensionsLiveUnderDimensionsNamespaceAndPath() {
    // spec: AR-R1 (dimensions/minecraft/overworld, the_nether, the_end)
    assertThat(WorldLayout.dimensionFolder(ROOT, "minecraft:overworld"))
        .isEqualTo(ROOT.resolve("dimensions").resolve("minecraft").resolve("overworld"));
    assertThat(WorldLayout.dimensionFolder(ROOT, "minecraft:the_nether"))
        .isEqualTo(ROOT.resolve("dimensions").resolve("minecraft").resolve("the_nether"));
    assertThat(WorldLayout.dimensionFolder(ROOT, "minecraft:the_end"))
        .isEqualTo(ROOT.resolve("dimensions").resolve("minecraft").resolve("the_end"));
    assertThat(WorldLayout.dimensionFolder(ROOT, "mymod:caves"))
        .isEqualTo(ROOT.resolve("dimensions").resolve("mymod").resolve("caves"));
  }

  @Test
  void eachDimensionHasRegionEntitiesAndPoiFolders() {
    // spec: AR-R1 (region/, entities/, poi/)
    Path dimension = WorldLayout.dimensionFolder(ROOT, "minecraft:overworld");

    assertThat(WorldLayout.regionFolder(dimension)).isEqualTo(dimension.resolve("region"));
    assertThat(WorldLayout.entitiesFolder(dimension)).isEqualTo(dimension.resolve("entities"));
    assertThat(WorldLayout.poiFolder(dimension)).isEqualTo(dimension.resolve("poi"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "overworld",
        "",
        ":",
        "minecraft:",
        ":overworld",
        "minecraft:..",
        "..:overworld",
        "minecraft:a/b",
        "minecraft:a\\b",
        "mine/craft:overworld",
        "minecraft:../../etc"
      })
  void identifiersWithoutNamespaceOrWithEmptyOrEscapingPartsAreRejected(String id) {
    // spec: WorldLayout.dimensionFolder contract (IllegalArgumentException), AR-R1
    Failures.expect(IllegalArgumentException.class, () -> WorldLayout.dimensionFolder(ROOT, id));
  }

  @ParameterizedTest
  @CsvSource({
    "0,0,r.0.0.mca",
    "-1,-1,r.-1.-1.mca",
    "46,-19,r.46.-19.mca",
    "-2,0,r.-2.0.mca",
    "0,-1,r.0.-1.mca",
    "58593,-58594,r.58593.-58594.mca",
    "2147483647,-2147483648,r.2147483647.-2147483648.mca"
  })
  void regionFileNameIsDecimalWithAMinusSign(int rx, int rz, String name) {
    // spec: AR-R4, AR-R6
    assertThat(WorldLayout.regionFileName(new RegionPos(rx, rz))).isEqualTo(name);
  }

  @ParameterizedTest
  @CsvSource({
    "0,0,r.0.0.mca",
    "31,31,r.0.0.mca",
    "32,0,r.1.0.mca",
    "-1,-1,r.-1.-1.mca",
    "-32,0,r.-1.0.mca",
    "-33,31,r.-2.0.mca",
    "30,-3,r.0.-1.mca",
    "1500,-600,r.46.-19.mca"
  })
  void regionOfAChunkRoundsDownLikeTheVectors(int cx, int cz, String name) {
    // spec: AR-R4, AR-R6, CO1 (file of the chunk)
    assertThat(WorldLayout.regionFileName(new ChunkPos(cx, cz).region())).isEqualTo(name);
  }

  @ParameterizedTest
  @CsvSource({
    "-1,-1,c.-1.-1.mcc",
    "0,0,c.0.0.mcc",
    "1500,-600,c.1500.-600.mcc",
    "-2147483648,2147483647,c.-2147483648.2147483647.mcc"
  })
  void externalChunkFileIsNamedByTheAbsoluteChunkCoordinates(int cx, int cz, String name) {
    // spec: AR-R17 (c.<cx>.<cz>.mcc, coordinates of the chunk not of the region)
    assertThat(WorldLayout.externalChunkFileName(new ChunkPos(cx, cz))).isEqualTo(name);
  }

  @Test
  void aFreshAndAModernWorldAreNotLegacy(@TempDir Path world) throws IOException {
    // spec: AR-R3 (provisional: only the layout before 26.1 Snapshot 6 is refused)
    assertThat(WorldLayout.isLegacyLayout(world)).isFalse();

    Files.createDirectories(
        WorldLayout.regionFolder(WorldLayout.dimensionFolder(world, "minecraft:overworld")));
    Files.createDirectories(world.resolve("data"));
    Files.createDirectories(world.resolve("players"));

    assertThat(WorldLayout.isLegacyLayout(world)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"region", "entities", "poi", "DIM-1", "DIM1"})
  void anyFolderOfTheOldLayoutMakesTheWorldLegacy(String folder, @TempDir Path world)
      throws IOException {
    // spec: AR-R3 (provisional: region/, entities/, poi/ at the root, DIM-1 or DIM1)
    Files.createDirectories(world.resolve(folder));

    assertThat(WorldLayout.isLegacyLayout(world)).isTrue();
  }

  @Test
  void aModernWorldWithAnOldFolderToo(@TempDir Path world) throws IOException {
    // spec: AR-R3 ("any of them exists")
    Files.createDirectories(world.resolve("dimensions/minecraft/overworld/region"));
    Files.createDirectories(world.resolve("DIM1"));

    assertThat(WorldLayout.isLegacyLayout(world)).isTrue();
  }
}

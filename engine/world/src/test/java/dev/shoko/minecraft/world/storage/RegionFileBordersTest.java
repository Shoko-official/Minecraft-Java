package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.RegionFixtures;
import dev.shoko.minecraft.world.support.Regions;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** AR-R4 to AR-R6, vector CO1: negative coordinates and the borders of regions. */
class RegionFileBordersTest {

  @TempDir Path folder;

  @ParameterizedTest(name = "chunk ({0}, {1}) is index {3} of {2}")
  @CsvSource({
    "0,0,r.0.0.mca,0",
    "31,31,r.0.0.mca,1023",
    "32,0,r.1.0.mca,0",
    "-1,-1,r.-1.-1.mca,1023",
    "-32,0,r.-1.0.mca,0",
    "-33,31,r.-2.0.mca,1023",
    "31,32,r.0.1.mca,31",
    "30,-3,r.0.-1.mca,958",
    "1500,-600,r.46.-19.mca,284",
    "-32,-32,r.-1.-1.mca,0",
    "-1,0,r.-1.0.mca,31",
    "0,-1,r.0.-1.mca,992",
    "-31,-1,r.-1.-1.mca,993",
    "2147483647,2147483647,r.67108863.67108863.mca,1023",
    "-2147483648,-2147483648,r.-67108864.-67108864.mca,0",
    "-2147483648,2147483647,r.-67108864.67108863.mca,992"
  })
  void chunkIsStoredInTheFileAndTheSlotOfTheSpec(int cx, int cz, String fileName, int index)
      throws IOException {
    // spec: AR-R4 (rx = cx >> 5, rounding down), AR-R5 (index = (cx & 31) + 32 * (cz & 31)), CO1
    ChunkPos chunk = new ChunkPos(cx, cz);
    byte[] nbt = RegionFixtures.nbtOfB();

    try (RegionFile file = RegionFile.open(folder, chunk.region())) {
      file.write(chunk, nbt, RegionCompression.ZLIB, 1_790_000_000L);
    }

    assertThat(RegionFixtures.names(folder)).containsExactly(fileName);
    byte[] bytes = RegionFixtures.read(folder.resolve(fileName));
    assertThat(Regions.usedIndices(bytes)).containsExactly(index);
    assertThat(Regions.timestamp(bytes, index)).isEqualTo(1_790_000_000L);
    assertThat(Regions.location(bytes, index)).isNotZero();
    try (RegionFile file = RegionFile.open(folder, chunk.region())) {
      assertThat(Chunks.present(file.read(chunk)).value()).isEqualTo(nbt);
    }
  }

  @ParameterizedTest
  @CsvSource({"-1,-1,1023", "-32,-32,0", "-1,-32,31", "-32,-1,992"})
  void fourCornersOfTheMinusOneMinusOneRegionDoNotCollideWithTheNeighbours(
      int cx, int cz, int index) throws IOException {
    // spec: "Cas limites" of anvil-region ((-1, -1) is index 1023 of r.-1.-1.mca, not index 0 of
    // r.0.0.mca)
    ChunkPos chunk = new ChunkPos(cx, cz);

    try (RegionFile file = RegionFile.open(folder, chunk.region())) {
      file.write(chunk, new byte[] {1, 2, 3}, RegionCompression.NONE, 1L);
    }

    assertThat(RegionFixtures.names(folder)).containsExactly("r.-1.-1.mca");
    assertThat(Regions.usedIndices(RegionFixtures.read(RegionFixtures.regionFile(folder))))
        .containsExactly(index);
  }
}

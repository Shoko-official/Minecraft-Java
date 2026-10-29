package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.abort;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.world.AbsentCause;
import dev.shoko.minecraft.world.support.Chunks;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The store keeps at most 256 region files open and closes the least recently used one when a new
 * one is needed. The ceiling and the order are observed without counting file handles: after the
 * reads, the region files are moved away. A region still open keeps answering from its channel
 * (EMPTY_SLOT), a region that was closed is opened again, finds no file and answers NO_REGION_FILE.
 */
class AnvilChunkStoreOpenRegionsTest {

  // The documented ceiling, written out so that a change of the constant cannot move the test.
  private static final int MAX_OPEN_REGIONS = 256;
  private static final int REGIONS = MAX_OPEN_REGIONS + 1;
  private static final int HEADER_BYTES = 8192;

  @TempDir Path base;

  private Path regions;
  private Path hidden;

  @BeforeEach
  void createRegionFilesWithAnEmptyHeader() throws IOException {
    regions = Files.createDirectories(base.resolve("region"));
    hidden = Files.createDirectories(base.resolve("hidden"));
    for (int region = 0; region < REGIONS; region++) {
      Files.write(regions.resolve("r." + region + ".0.mca"), new byte[HEADER_BYTES]);
    }
  }

  private AnvilChunkStore open() {
    return AnvilChunkStore.open(regions, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB);
  }

  private static ChunkPos inRegion(int region) {
    return new ChunkPos(32 * region, 0);
  }

  private static AbsentCause read(AnvilChunkStore store, int region) throws IOException {
    return Chunks.absent(store.load(inRegion(region))).cause();
  }

  // A rename keeps working on an open file on every platform, unlike a deletion on Windows; the
  // channel of an open region keeps the moved file.
  private void moveRegionFilesAway() throws IOException {
    for (int region = 0; region < REGIONS; region++) {
      String name = "r." + region + ".0.mca";
      try {
        Files.move(regions.resolve(name), hidden.resolve(name));
      } catch (FileSystemException e) {
        abort("this platform cannot move a region file that is open: " + e.getMessage());
      }
    }
  }

  private void assertStillOpen(AnvilChunkStore store, int region) throws IOException {
    assertThat(read(store, region))
        .as("region %d is expected to be still open", region)
        .isEqualTo(AbsentCause.EMPTY_SLOT);
  }

  private void assertWasClosed(AnvilChunkStore store, int region) throws IOException {
    assertThat(read(store, region))
        .as("region %d is expected to have been closed", region)
        .isEqualTo(AbsentCause.NO_REGION_FILE);
  }

  @Test
  void theFirstReadIsAnsweredFromTheFileOnDisk() throws IOException {
    // control of the method: a region that was never opened is opened from the folder
    try (AnvilChunkStore store = open()) {
      assertThat(read(store, 0)).isEqualTo(AbsentCause.EMPTY_SLOT);
    }
  }

  @Test
  void twoHundredAndFiftySixRegionsStayOpenTogether() throws IOException {
    // 256 is the ceiling: none of them is closed, the first one included
    try (AnvilChunkStore store = open()) {
      for (int region = 0; region < MAX_OPEN_REGIONS; region++) {
        read(store, region);
      }
      moveRegionFilesAway();

      for (int region = 0; region < MAX_OPEN_REGIONS; region++) {
        assertStillOpen(store, region);
      }
    }
  }

  @Test
  void theTwoHundredAndFiftySeventhRegionClosesTheLeastRecentlyUsedOne() throws IOException {
    // 257 regions read once each, in order: only the first (the oldest) is closed
    try (AnvilChunkStore store = open()) {
      for (int region = 0; region < REGIONS; region++) {
        read(store, region);
      }
      moveRegionFilesAway();

      for (int region = 1; region < REGIONS; region++) {
        assertStillOpen(store, region);
      }
      assertWasClosed(store, 0);
    }
  }

  @Test
  void aRegionReadAgainIsNoLongerTheOneClosedFirst() throws IOException {
    // least recently used, not first opened: reading region 0 again protects it, region 1 goes
    try (AnvilChunkStore store = open()) {
      for (int region = 0; region < MAX_OPEN_REGIONS; region++) {
        read(store, region);
      }
      read(store, 0);
      read(store, MAX_OPEN_REGIONS);
      moveRegionFilesAway();

      assertStillOpen(store, 0);
      for (int region = 2; region < REGIONS; region++) {
        assertStillOpen(store, region);
      }
      assertWasClosed(store, 1);
    }
  }
}

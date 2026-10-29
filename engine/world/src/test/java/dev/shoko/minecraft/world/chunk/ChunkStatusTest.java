package dev.shoko.minecraft.world.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** CF-R27 to CF-R29: the ten statuses of 26.3, in generation order. */
class ChunkStatusTest {

  @ParameterizedTest
  @CsvSource({
    "EMPTY,minecraft:empty,0",
    "STRUCTURE_STARTS,minecraft:structure_starts,1",
    "STRUCTURE_REFERENCES,minecraft:structure_references,2",
    "BIOMES,minecraft:biomes,3",
    "TERRAIN,minecraft:terrain,4",
    "FEATURES,minecraft:features,5",
    "INITIALIZE_LIGHT,minecraft:initialize_light,6",
    "LIGHT,minecraft:light,7",
    "SPAWN,minecraft:spawn,8",
    "FULL,minecraft:full,9"
  })
  void statusHasItsIdentifierAndItsPlaceInGenerationOrder(
      ChunkStatus status, String id, int protocolId) {
    // spec: CF-R27 (identifiers and protocol ids), CF-R28 (order of generation)
    assertThat(status.id()).isEqualTo(id);
    assertThat(Arrays.asList(ChunkStatus.values()).indexOf(status)).isEqualTo(protocolId);
    assertThat(ChunkStatus.byId(id)).contains(status);
  }

  @Test
  void thereAreExactlyTenStatuses() {
    // spec: CF-R27
    assertThat(ChunkStatus.values()).hasSize(10);
    assertThat(Arrays.stream(ChunkStatus.values()).map(ChunkStatus::id).distinct().count())
        .isEqualTo(10);
  }

  @Test
  void onlyFullIsAFullChunk() {
    // spec: CF-R28
    for (ChunkStatus status : ChunkStatus.values()) {
      assertThat(status.isFull()).as(status.name()).isEqualTo(status == ChunkStatus.FULL);
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "minecraft:noise",
        "minecraft:surface",
        "minecraft:carvers",
        "minecraft:liquid_carvers",
        "minecraft:heightmaps",
        "minecraft:unknown",
        ""
      })
  void removedAndUnknownStatusesAreEmpty(String id) {
    // spec: CF-R29 (noise, surface and carvers no longer exist), ChunkStatus.byId contract
    assertThat(ChunkStatus.byId(id)).isEmpty();
  }
}

package dev.shoko.minecraft.world.chunk;

import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.i;
import static dev.shoko.minecraft.world.support.Nbt.l;
import static dev.shoko.minecraft.world.support.Nbt.list;
import static dev.shoko.minecraft.world.support.Nbt.str;
import static dev.shoko.minecraft.world.support.Nbt.with;
import static dev.shoko.minecraft.world.support.Nbt.without;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.heightmap.HeightmapType;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Chunks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** CF-R20, CF-R21, CF-R35: reading the root of a chunk (vector RG1 and its neighbours). */
class ChunkCodecReadTest {

  private static final int BEDROCK = 4;

  private static CompoundTag chunkA() {
    return ChunkNbt.chunk(-1, -1, true, "bedrock");
  }

  @Test
  void supportedDataVersionIs5023() {
    // spec: CF-R20 (DataVersion 5023 for 26.3)
    assertThat(ChunkCodec.SUPPORTED_DATA_VERSION).isEqualTo(5023);
  }

  @Test
  void chunkAOfVectorRg1IsReadWithoutWarning() {
    // spec: RG1 chunk A (-1, -1): full, 24 sections, bedrock layer at y = -64
    Chunk chunk = Chunks.decodeClean(chunkA());

    assertThat(chunk.pos()).isEqualTo(new ChunkPos(-1, -1));
    assertThat(chunk.height()).isEqualTo(Chunks.OVERWORLD);
    assertThat(chunk.status()).isEqualTo(ChunkStatus.FULL);
    assertThat(chunk.lastUpdate()).isZero();
    assertThat(chunk.inhabitedTime()).isZero();
    assertThat(chunk.sections()).hasSize(24);
    for (int rank = 0; rank < 24; rank++) {
      assertThat(chunk.sections().get(rank).y()).isEqualTo(rank - 4);
    }
  }

  @Test
  void bottomLayerOfChunkAIsBedrockAndEverythingElseAir() {
    // spec: RG1 chunk A (PK4: indices 0 to 255 of section -4 are the second palette entry)
    Chunk chunk = Chunks.decodeClean(chunkA());

    for (int index = 0; index < 4096; index++) {
      int expected = index < 256 ? BEDROCK : 0;
      assertThat(chunk.section(-4).blockStates().get(index).id())
          .as("index " + index)
          .isEqualTo(expected);
    }
    for (int y = -3; y <= 19; y++) {
      assertThat(chunk.section(y).blockStates().isUniform()).as("section " + y).isTrue();
      assertThat(chunk.section(y).blockStates().get(0).id()).isZero();
    }
  }

  @Test
  void biomesAreAllPlainsAndNoLightIsStored() {
    // spec: RG1 (palette of biomes is plains alone), CF-R15 (no BlockLight and no SkyLight)
    Chunk chunk = Chunks.decodeClean(chunkA());

    for (ChunkSection section : chunk.sections()) {
      for (int cell = 0; cell < 64; cell++) {
        assertThat(section.biomes().get(cell)).isEqualTo(Chunks.PLAINS);
      }
      assertThat(section.light().blockLight()).isEmpty();
      assertThat(section.light().skyLight()).isEmpty();
    }
  }

  @Test
  void blocksAreAddressedByAbsolutePosition() {
    // spec: CF-R3, CF-R4 (chunk (-1, -1): block (-16, -64, -16) is index 0, (-1, -64, -1) index
    // 255)
    Chunk chunk = Chunks.decodeClean(chunkA());

    assertThat(chunk.blockState(new dev.shoko.minecraft.core.coord.BlockPos(-16, -64, -16)).id())
        .isEqualTo(BEDROCK);
    assertThat(chunk.blockState(new dev.shoko.minecraft.core.coord.BlockPos(-1, -64, -1)).id())
        .isEqualTo(BEDROCK);
    assertThat(chunk.blockState(new dev.shoko.minecraft.core.coord.BlockPos(-1, -63, -1)).id())
        .isZero();
  }

  @Test
  void heightmapsOfChunkAAreTheFourValidTypesAtOne() {
    // spec: RG1 (MOTION_BLOCKING, MOTION_BLOCKING_NO_LEAVES, OCEAN_FLOOR, WORLD_SURFACE = HM1),
    // CF-R34
    Chunk chunk = Chunks.decodeClean(chunkA());

    for (HeightmapType type :
        new HeightmapType[] {
          HeightmapType.MOTION_BLOCKING,
          HeightmapType.MOTION_BLOCKING_NO_LEAVES,
          HeightmapType.OCEAN_FLOOR,
          HeightmapType.WORLD_SURFACE
        }) {
      assertThat(chunk.heightmaps().get(type).orElseThrow().get(9, 4)).as(type.name()).isEqualTo(1);
    }
    assertThat(chunk.heightmaps().unknownTypes())
        .containsExactly(HeightmapType.WORLD_SURFACE_WG, HeightmapType.OCEAN_FLOOR_WG);
  }

  @Test
  void chunkBOfVectorRg1IsAllAirWithHeightmapsAtZero() {
    // spec: RG1 chunk B (-32, -32): all air, heightmaps 0
    Chunk chunk = Chunks.decodeClean(ChunkNbt.chunk(-32, -32, false, "bedrock"));

    assertThat(chunk.pos()).isEqualTo(new ChunkPos(-32, -32));
    for (ChunkSection section : chunk.sections()) {
      assertThat(section.blockStates().isUniform()).isTrue();
    }
    assertThat(chunk.heightmaps().get(HeightmapType.WORLD_SURFACE).orElseThrow().get(0, 0))
        .isZero();
  }

  @Test
  void scalarFieldsAreReadWithTheirTypes() {
    // spec: CF-R20 (LastUpdate and InhabitedTime are Long)
    CompoundTag root =
        with(with(chunkA(), "LastUpdate", l(5_000_000_000L)), "InhabitedTime", l(Long.MAX_VALUE));

    Chunk chunk = Chunks.decodeClean(root);

    assertThat(chunk.lastUpdate()).isEqualTo(5_000_000_000L);
    assertThat(chunk.inhabitedTime()).isEqualTo(Long.MAX_VALUE);
  }

  @ParameterizedTest
  @CsvSource({
    "minecraft:empty,EMPTY", "minecraft:structure_starts,STRUCTURE_STARTS",
    "minecraft:structure_references,STRUCTURE_REFERENCES", "minecraft:biomes,BIOMES",
    "minecraft:terrain,TERRAIN", "minecraft:features,FEATURES",
    "minecraft:initialize_light,INITIALIZE_LIGHT", "minecraft:light,LIGHT",
    "minecraft:spawn,SPAWN", "minecraft:full,FULL"
  })
  void everyStatusOfTheRegistryIsAccepted(String id, ChunkStatus status) {
    // spec: CF-R27 (ten statuses)
    Chunk chunk = Chunks.decodeClean(with(chunkA(), "Status", str(id)));

    assertThat(chunk.status()).isEqualTo(status);
  }

  @Test
  void unknownRootKeysAreIgnored() {
    // spec: CF-R21 (the reader ignores every unknown key)
    CompoundTag root = with(chunkA(), "isLightOn", dev.shoko.minecraft.world.support.Nbt.b(1));
    root = with(root, "Foo", compound("bar", i(1)));
    root = with(root, "below_zero_retrogen", compound("target_status", str("minecraft:full")));

    Chunk chunk = Chunks.decodeClean(root);

    assertThat(chunk.section(-4).blockStates().get(0).id()).isEqualTo(BEDROCK);
  }

  @Test
  void absentOptionalFieldsTakeTheirDefaultsWithoutWarning() {
    // spec: CF-R35 (every other field is optional: absent means default, heightmaps unknown)
    CompoundTag root = chunkA();
    for (String key :
        new String[] {
          "yPos",
          "LastUpdate",
          "InhabitedTime",
          "Heightmaps",
          "block_entities",
          "block_ticks",
          "fluid_ticks",
          "PostProcessing",
          "structures",
          "entities"
        }) {
      root = without(root, key);
    }

    Chunk chunk = Chunks.decodeClean(root);

    assertThat(chunk.lastUpdate()).isZero();
    assertThat(chunk.inhabitedTime()).isZero();
    assertThat(chunk.heightmaps().unknownTypes()).containsExactly(HeightmapType.values());
    assertThat(chunk.attachments().blockEntities()).isEmpty();
    assertThat(chunk.attachments().blockTicks()).isEmpty();
    assertThat(chunk.attachments().fluidTicks()).isEmpty();
    assertThat(chunk.attachments().entities()).isEmpty();
    assertThat(chunk.attachments().postProcessing()).hasSize(24).allMatch(java.util.List::isEmpty);
    assertThat(chunk.attachments().structures().references()).isEmpty();
    assertThat(chunk.attachments().structures().starts().entries()).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "LastUpdate",
    "InhabitedTime",
    "block_entities",
    "block_ticks",
    "fluid_ticks",
    "PostProcessing",
    "structures",
    "Heightmaps"
  })
  void optionalFieldOfAnotherTagTypeIsTreatedAsAbsentWithAWarning(String key) {
    // spec: CF-R35 (wrong type of an optional field: default plus a FIELD_WRONG_TYPE warning)
    NbtTag wrong =
        key.equals("LastUpdate") || key.equals("InhabitedTime")
            ? i(7)
            : str("not the expected type");

    ReadResult<Chunk> result = Chunks.decode(with(chunkA(), key, wrong));

    ReadResult.Present<Chunk> present = Chunks.present(result);
    assertThat(present.warnings()).extracting(w -> w.code()).contains(WarningCode.FIELD_WRONG_TYPE);
    assertThat(present.warnings().stream().filter(w -> w.message().contains(key))).isNotEmpty();
    assertThat(present.value().lastUpdate()).isZero();
    assertThat(present.value().section(-4).blockStates().get(0).id()).isEqualTo(BEDROCK);
  }

  @Test
  void wrongHeightmapsFieldMakesEveryTypeUnknown() {
    // spec: CF-R35, CF-R34 (a Heightmaps of another type is absent: six unknown types)
    ReadResult.Present<Chunk> present =
        Chunks.present(Chunks.decode(with(chunkA(), "Heightmaps", list(i(1)))));

    assertThat(present.value().heightmaps().unknownTypes()).containsExactly(HeightmapType.values());
  }

  @Test
  void netherChunkHasSixteenSectionsAndYPosZero() {
    // spec: CF-R1, CF-R2, CF-R33 (min_y 0, height 256)
    ReadResult.Present<Chunk> present =
        Chunks.present(Chunks.codec().decode(ChunkNbt.netherChunk(5, 6), Chunks.NETHER));

    assertThat(present.warnings()).isEmpty();
    assertThat(present.value().sections()).hasSize(16);
    assertThat(present.value().section(15).y()).isEqualTo(15);
    assertThat(present.value().pos()).isEqualTo(new ChunkPos(5, 6));
    assertThat(present.value().height().minY()).isZero();
  }

  @Test
  void blockStatesOfTheMadeUpRegistryAreUsed() {
    // spec: BS-R19 (palette entries resolve against the loaded registry)
    Chunk chunk = Chunks.decodeClean(chunkA());

    assertThat(chunk.section(-4).blockStates().get(0).block().id()).isEqualTo(Blocks.BEDROCK);
    assertThat(chunk.section(-4).blockStates().get(4000).block().id()).isEqualTo(Blocks.AIR);
  }
}

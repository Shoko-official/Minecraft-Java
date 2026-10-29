package dev.shoko.minecraft.world.support;

import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.i;
import static dev.shoko.minecraft.world.support.Nbt.str;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.chunk.BlockEntityData;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.chunk.ChunkAttachments;
import dev.shoko.minecraft.world.chunk.ChunkSection;
import dev.shoko.minecraft.world.chunk.LightOnlySection;
import dev.shoko.minecraft.world.chunk.NibbleArray;
import dev.shoko.minecraft.world.chunk.ScheduledTickEntry;
import dev.shoko.minecraft.world.chunk.SectionLight;
import dev.shoko.minecraft.world.chunk.StructureData;
import dev.shoko.minecraft.world.heightmap.Heightmap;
import dev.shoko.minecraft.world.heightmap.HeightmapType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

/** A chunk of the Overworld at (-3, 7) that uses every part of the model, built without codecs. */
public final class RichChunk {

  public static final ChunkPos POS = new ChunkPos(-3, 7);

  private RichChunk() {}

  public static Chunk build(BlockStateRegistry registry) {
    Chunk chunk = Chunks.empty(registry, POS, Chunks.OVERWORLD);
    chunk.setLastUpdate(123_456L);
    chunk.setInhabitedTime(789L);
    fillSections(registry, chunk);
    fillLight(chunk);
    fillHeightmaps(chunk);
    chunk.setAttachments(attachments());
    return chunk;
  }

  private static void fillSections(BlockStateRegistry registry, Chunk chunk) {
    ChunkSection low = chunk.section(-4);
    for (int index = 0; index < 20; index++) {
      low.blockStates().set(index, registry.state(index));
    }
    ChunkSection big = chunk.section(0);
    for (int index = 0; index < registry.stateCount(); index++) {
      big.blockStates().set(index, registry.state(index));
    }
    ChunkSection mixed = chunk.section(5);
    mixed.blockStates().set(300, registry.state(26));
    mixed.blockStates().set(301, registry.state(3));
    ChunkSection biomes = chunk.section(3);
    biomes.biomes().set(0, Chunks.DESERT);
    biomes.biomes().set(63, "minecraft:ocean");
  }

  private static void fillLight(Chunk chunk) {
    chunk.section(2).light().setBlockLight(Chunks.nibbles(index -> index % 16));
    chunk.section(2).light().setSkyLight(Chunks.nibbles(index -> (index / 16) % 16));
    chunk.section(6).light().setBlockLight(NibbleArray.filled(7));
    chunk.section(8).light().setSkyLight(NibbleArray.filled(0));
  }

  private static void fillHeightmaps(Chunk chunk) {
    Heightmap ramp = Heightmap.filled(Chunks.OVERWORLD, 0);
    for (int index = 0; index < 256; index++) {
      ramp.set(index % 16, index / 16, Math.min(384, index + 100));
    }
    chunk.heightmaps().put(HeightmapType.MOTION_BLOCKING, ramp);
    chunk.heightmaps().put(HeightmapType.WORLD_SURFACE, Heightmap.filled(Chunks.OVERWORLD, 64));
  }

  private static ChunkAttachments attachments() {
    CompoundTag chestData =
        compound(
            "id", str("minecraft:chest"), "x", i(-40), "y", i(10), "z", i(120), "Lock", str("k"));
    List<BlockEntityData> blockEntities =
        List.of(new BlockEntityData("minecraft:chest", new BlockPos(-40, 10, 120), chestData));
    List<ScheduledTickEntry> blockTicks =
        List.of(new ScheduledTickEntry("minecraft:stone", 1, -2, new BlockPos(-48, -64, 127)));
    List<ScheduledTickEntry> fluidTicks =
        List.of(new ScheduledTickEntry("minecraft:water", 0, 5, new BlockPos(-33, 319, 112)));
    List<List<Short>> postProcessing = new ArrayList<>();
    for (int k = 0; k < 24; k++) {
      postProcessing.add(k == 2 ? List.of((short) 5, (short) 4095, (short) 0) : List.of());
    }
    SequencedMap<String, List<ChunkPos>> references = new LinkedHashMap<>();
    references.put("minecraft:village", List.of(new ChunkPos(-1, 2), new ChunkPos(0, 0)));
    StructureData structures =
        new StructureData(
            compound("minecraft:village", compound("id", str("INVALID"))), references);
    SequencedMap<Integer, LightOnlySection> lightOnly = new LinkedHashMap<>();
    SectionLight bottom = SectionLight.none();
    bottom.setBlockLight(NibbleArray.filled(3));
    lightOnly.put(-5, new LightOnlySection(bottom, CompoundTag.empty()));
    SectionLight top = SectionLight.none();
    top.setSkyLight(NibbleArray.filled(15));
    lightOnly.put(20, new LightOnlySection(top, compound("note", str("kept"))));
    return new ChunkAttachments(
        blockEntities, blockTicks, fluidTicks, postProcessing, structures, List.of(), lightOnly);
  }
}

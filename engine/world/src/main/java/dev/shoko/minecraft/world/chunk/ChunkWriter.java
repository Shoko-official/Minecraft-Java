package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.world.biome.BiomeEntryCodec;
import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.block.BlockStateEntryCodec;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.palette.ContainerLayout;
import dev.shoko.minecraft.world.palette.PalettedContainerCodec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;

/** Internal: writes the root Compound of a chunk (CF-R20 to CF-R26, CF-R31 to CF-R34). */
final class ChunkWriter {

  private final PalettedContainerCodec<BlockState> blockCodec;
  private final PalettedContainerCodec<String> biomeCodec;

  ChunkWriter(BlockStateRegistry blocks) {
    // Writing never reports: the entry codecs only warn while reading.
    this.blockCodec =
        PalettedContainerCodec.of(
            ContainerLayout.BLOCKS, BlockStateEntryCodec.of(blocks, (code, message) -> {}));
    this.biomeCodec =
        PalettedContainerCodec.of(
            ContainerLayout.BIOMES, BiomeEntryCodec.of(biome -> true, (code, message) -> {}));
  }

  CompoundTag write(Chunk chunk) {
    WorldHeight height = chunk.height();
    SequencedMap<String, NbtTag> root = new LinkedHashMap<>();
    root.put("DataVersion", new IntTag(ChunkCodec.SUPPORTED_DATA_VERSION));
    root.put("xPos", new IntTag(chunk.pos().x()));
    root.put("zPos", new IntTag(chunk.pos().z()));
    root.put("yPos", new IntTag(height.minSectionY()));
    root.put("Status", new StringTag(chunk.status().id()));
    root.put("LastUpdate", new LongTag(chunk.lastUpdate()));
    root.put("InhabitedTime", new LongTag(chunk.inhabitedTime()));
    root.put("sections", sections(chunk));
    root.put("Heightmaps", chunk.heightmaps().toNbt());
    AttachmentsWriter.write(chunk.attachments(), height.sectionCount(), !chunk.status().isFull())
        .forEach(root::put);
    return CompoundTag.of(root);
  }

  private ListTag sections(Chunk chunk) {
    WorldHeight height = chunk.height();
    SequencedMap<Integer, LightOnlySection> lightOnly = chunk.attachments().lightOnlySections();
    List<CompoundTag> written = new ArrayList<>(height.sectionCount() + 2);
    lightSection(lightOnly, height.minSectionY() - 1).ifPresent(written::add);
    for (ChunkSection section : chunk.sections()) {
      written.add(section(section));
    }
    lightSection(lightOnly, height.maxSectionY() + 1).ifPresent(written::add);
    return ListTag.of(written);
  }

  private CompoundTag section(ChunkSection section) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    entries.put("Y", new ByteTag((byte) section.y()));
    entries.put("block_states", blockCodec.encode(section.blockStates()));
    entries.put("biomes", biomeCodec.encode(section.biomes()));
    putLight(entries, section.light());
    return CompoundTag.of(entries);
  }

  private static Optional<CompoundTag> lightSection(
      SequencedMap<Integer, LightOnlySection> lightOnly, int y) {
    LightOnlySection section = lightOnly.get(y);
    if (section == null) {
      return Optional.empty();
    }
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    entries.put("Y", new ByteTag((byte) y));
    putLight(entries, section.light());
    entries.putAll(section.otherFields().entries());
    return Optional.of(CompoundTag.of(entries));
  }

  private static void putLight(SequencedMap<String, NbtTag> entries, SectionLight light) {
    light.blockLight().ifPresent(array -> entries.put("BlockLight", bytes(array)));
    light.skyLight().ifPresent(array -> entries.put("SkyLight", bytes(array)));
  }

  private static ByteArrayTag bytes(NibbleArray array) {
    return ByteArrayTag.of(array.toBytes());
  }
}

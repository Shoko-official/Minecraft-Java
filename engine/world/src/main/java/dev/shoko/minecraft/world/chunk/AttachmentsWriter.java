package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

/** Internal: writes the opaque data of a chunk, in a fixed key order (CF-R21 to CF-R26). */
final class AttachmentsWriter {

  private AttachmentsWriter() {}

  static SequencedMap<String, NbtTag> write(
      ChunkAttachments attachments, int sectionCount, boolean protoChunk) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    List<CompoundTag> blockEntities = new ArrayList<>();
    attachments.blockEntities().forEach(entity -> blockEntities.add(entity.data()));
    entries.put("block_entities", ListTag.of(blockEntities));
    entries.put("block_ticks", ticks(attachments.blockTicks()));
    entries.put("fluid_ticks", ticks(attachments.fluidTicks()));
    entries.put("PostProcessing", postProcessing(attachments.postProcessing(), sectionCount));
    entries.put("structures", structures(attachments.structures()));
    if (protoChunk) {
      entries.put("entities", ListTag.of(attachments.entities()));
    }
    return entries;
  }

  private static ListTag ticks(List<ScheduledTickEntry> ticks) {
    List<CompoundTag> written = new ArrayList<>(ticks.size());
    for (ScheduledTickEntry tick : ticks) {
      SequencedMap<String, NbtTag> entry = new LinkedHashMap<>();
      BlockPos at = tick.pos();
      entry.put("i", new StringTag(tick.id()));
      entry.put("p", new IntTag(tick.priority()));
      entry.put("t", new IntTag(tick.delay()));
      entry.put("x", new IntTag(at.x()));
      entry.put("y", new IntTag(at.y()));
      entry.put("z", new IntTag(at.z()));
      written.add(CompoundTag.of(entry));
    }
    return ListTag.of(written);
  }

  private static ListTag postProcessing(List<List<Short>> lists, int sectionCount) {
    List<ListTag> written = new ArrayList<>(sectionCount);
    for (int index = 0; index < sectionCount; index++) {
      List<ShortTag> shorts = new ArrayList<>();
      if (index < lists.size()) {
        lists.get(index).forEach(value -> shorts.add(new ShortTag(value)));
      }
      written.add(ListTag.of(shorts));
    }
    return ListTag.of(written);
  }

  private static CompoundTag structures(StructureData structures) {
    SequencedMap<String, NbtTag> references = new LinkedHashMap<>();
    structures
        .references()
        .forEach(
            (structure, positions) -> {
              long[] packed = new long[positions.size()];
              for (int index = 0; index < packed.length; index++) {
                packed[index] = positions.get(index).pack();
              }
              references.put(structure, LongArrayTag.of(packed));
            });
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    entries.put("starts", structures.starts());
    entries.put("References", CompoundTag.of(references));
    return CompoundTag.of(entries);
  }
}

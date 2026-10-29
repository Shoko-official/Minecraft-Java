package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

/**
 * What a chunk carries without interpreting it in milestone M3 (CF-R20 to CF-R26). Replaced as a
 * whole on the chunk.
 *
 * @param blockEntities block entities in file order
 * @param blockTicks scheduled block ticks
 * @param fluidTicks scheduled fluid ticks
 * @param postProcessing one list per section of the world height, bottom to top (missing lists are
 *     empty), of local positions packed as {@code x | (y << 4) | (z << 8)} (CF-R24, provisional). A
 *     Short with a non-zero high nibble and any list beyond the sections are dropped on reading,
 *     each with a warning; the order of the Short values is kept
 * @param structures starts and references
 * @param entities entities of a proto-chunk, opaque; empty for a complete chunk, whose entities
 *     live in {@code entities/} files
 * @param lightOnlySections the light sections of section y {@code sMin - 1} and {@code sMax + 1},
 *     by section y, bottom to top, only those that were read (CF-R31)
 */
public record ChunkAttachments(
    List<BlockEntityData> blockEntities,
    List<ScheduledTickEntry> blockTicks,
    List<ScheduledTickEntry> fluidTicks,
    List<List<Short>> postProcessing,
    StructureData structures,
    List<CompoundTag> entities,
    SequencedMap<Integer, LightOnlySection> lightOnlySections) {

  /**
   * Returns attachments with every list empty and no structure start or reference.
   *
   * @param sectionCount number of sections of the world height, the length of {@code
   *     postProcessing}
   * @return the empty attachments
   */
  public static ChunkAttachments empty(int sectionCount) {
    List<List<Short>> postProcessing = new ArrayList<>(sectionCount);
    for (int k = 0; k < sectionCount; k++) {
      postProcessing.add(List.of());
    }
    return new ChunkAttachments(
        List.of(),
        List.of(),
        List.of(),
        List.copyOf(postProcessing),
        new StructureData(CompoundTag.empty(), new LinkedHashMap<>()),
        List.of(),
        new LinkedHashMap<>());
  }
}

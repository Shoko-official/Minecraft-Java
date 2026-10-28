package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import java.util.List;
import java.util.SequencedMap;

/**
 * What a chunk carries without interpreting it in milestone M3 (CF-R20 to CF-R26). Replaced as a
 * whole on the chunk.
 *
 * @param blockEntities block entities in file order
 * @param blockTicks scheduled block ticks
 * @param fluidTicks scheduled fluid ticks
 * @param postProcessing one list per section of the world height, bottom to top, of packed local
 *     positions as stored (Short values kept verbatim: their bit layout is provisional, CF-R24)
 * @param structures starts and references
 * @param entities entities of a proto-chunk, opaque; empty for a complete chunk, whose entities
 *     live in {@code entities/} files
 * @param lightOnlySections light of sections outside the world height, by section y (CF-R15)
 */
public record ChunkAttachments(
    List<BlockEntityData> blockEntities,
    List<ScheduledTickEntry> blockTicks,
    List<ScheduledTickEntry> fluidTicks,
    List<List<Short>> postProcessing,
    StructureData structures,
    List<CompoundTag> entities,
    SequencedMap<Integer, SectionLight> lightOnlySections) {

  /**
   * Returns attachments with every list empty and no structure start or reference.
   *
   * @param sectionCount number of sections of the world height, the length of {@code
   *     postProcessing}
   * @return the empty attachments
   */
  public static ChunkAttachments empty(int sectionCount) {
    return Skeleton.throwNotImplemented();
  }
}

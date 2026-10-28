package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import java.util.List;
import java.util.SequencedMap;

/**
 * The {@code structures} Compound of a chunk (CF-R25, provisional).
 *
 * @param starts the {@code starts} Compound, opaque; a finished structure has {@code id} {@code
 *     INVALID}
 * @param references per structure identifier, the chunks that hold its start; stored as a
 *     Long_Array of {@code ChunkPos.pack()} values (chunk (-1, 2) is 12884901887). Read from {@code
 *     References} or {@code references}, written as {@code References}
 */
public record StructureData(CompoundTag starts, SequencedMap<String, List<ChunkPos>> references) {}

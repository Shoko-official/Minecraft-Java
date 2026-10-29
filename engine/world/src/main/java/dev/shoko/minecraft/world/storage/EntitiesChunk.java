package dev.shoko.minecraft.world.storage;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import java.util.List;

/**
 * The root of a chunk of an {@code entities/} region file (AR-R22, provisional). Entities are
 * opaque NBT in milestone M3.
 *
 * @param dataVersion {@code DataVersion}
 * @param position {@code Position}, the Int_Array {@code [cx, cz]}
 * @param entities {@code Entities}, in file order
 */
public record EntitiesChunk(int dataVersion, ChunkPos position, List<CompoundTag> entities) {}

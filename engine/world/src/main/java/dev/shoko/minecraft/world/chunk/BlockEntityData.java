package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.nbt.CompoundTag;

/**
 * A block entity as stored: opaque NBT (CF-R26, ADR 0005).
 *
 * @param id block entity type identifier, key {@code id}
 * @param pos absolute position, keys {@code x}, {@code y}, {@code z}
 * @param data the whole Compound including {@code id}, {@code x}, {@code y}, {@code z}, kept as
 *     read
 */
public record BlockEntityData(String id, BlockPos pos, CompoundTag data) {}

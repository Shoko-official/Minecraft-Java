package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.WorldHeight;

/**
 * The scalar fields of a chunk (CF-R20).
 *
 * @param pos chunk position, {@code xPos} and {@code zPos}
 * @param height vertical extent of the dimension; {@code yPos} is its lowest section
 * @param status generation status
 * @param lastUpdate game tick of the last save, {@code LastUpdate}
 * @param inhabitedTime ticks of player presence, {@code InhabitedTime}
 */
public record ChunkHeader(
    ChunkPos pos, WorldHeight height, ChunkStatus status, long lastUpdate, long inhabitedTime) {}

package dev.shoko.minecraft.core.coord;

/**
 * Position of an Anvil region of 32 x 32 chunks, in region units (R28, R33). Chunk {@code (30, -3)}
 * is in region {@code (0, -1)}. File names, sectors and header offsets belong to the Anvil storage
 * spec.
 *
 * @param x region x, {@code chunkX >> 5}
 * @param z region z, {@code chunkZ >> 5}
 */
public record RegionPos(int x, int z) {}

package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.palette.PalettedContainer;

/**
 * A 16 x 16 x 16 part of a chunk (CF-R2, CF-R22): 4096 block states, 64 biome cells and its stored
 * light. Mutable, confined to the thread that ticks the world.
 */
public final class ChunkSection {

  private ChunkSection() {}

  /**
   * Creates a section from its parts.
   *
   * @param y section y, {@code blockY >> 4}
   * @param blockStates 4096 states in the order of CF-R4
   * @param biomes 64 biome identifiers in the order of CF-R5
   * @param light the stored light
   * @return the section
   * @throws IllegalArgumentException if a container has the wrong size
   */
  public static ChunkSection of(
      int y,
      PalettedContainer<BlockState> blockStates,
      PalettedContainer<String> biomes,
      SectionLight light) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Creates a section with one state and one biome everywhere and no light.
   *
   * @param y section y
   * @param state the state of every block, usually air
   * @param biome the biome of every cell
   * @return the section
   */
  public static ChunkSection uniform(int y, BlockState state, String biome) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the index of the biome cell holding a block, {@code (qy * 4 + qz) * 4 + qx} with {@code
   * q = (block & 15) >> 2} on each axis (CF-R5). Block (15, -64, 0) gives 3 and (-1, 319, -1) gives
   * 63. The index of a block state is {@code LocalIndex.sectionStateIndex} of the core module.
   *
   * @param x block x
   * @param y block y
   * @param z block z
   * @return the index, in {@code [0, 63]}
   */
  public static int biomeIndex(int x, int y, int z) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the section y.
   *
   * @return {@code blockY >> 4}
   */
  public int y() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the block states, 4096 positions.
   *
   * @return the live container
   */
  public PalettedContainer<BlockState> blockStates() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the biomes, 64 positions.
   *
   * @return the live container
   */
  public PalettedContainer<String> biomes() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the stored light.
   *
   * @return the live light
   */
  public SectionLight light() {
    return Skeleton.throwNotImplemented();
  }
}

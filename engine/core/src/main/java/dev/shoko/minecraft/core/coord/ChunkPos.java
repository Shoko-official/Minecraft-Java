package dev.shoko.minecraft.core.coord;

/**
 * Horizontal position of a chunk, in chunk units (R28, R30, R33).
 *
 * @param x chunk x, {@code blockX >> 4}
 * @param z chunk z, {@code blockZ >> 4}
 */
public record ChunkPos(int x, int z) {

  /**
   * Returns the lowest block x of this chunk, {@code x << 4} (R28).
   *
   * @return the first block x
   */
  public int minBlockX() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the lowest block z of this chunk, {@code z << 4} (R28).
   *
   * @return the first block z
   */
  public int minBlockZ() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the region containing this chunk, {@code (x >> 5, z >> 5)} (R28, R33).
   *
   * @return the region position
   */
  public RegionPos region() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the index of this chunk in its region's 32 x 32 grid, {@code (x & 31) + 32 * (z & 31)}
   * (R33, provisional until probe P30). Chunk {@code (30, -3)} has index 958.
   *
   * @return the index, in {@code [0, 1023]}
   */
  public int indexInRegion() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Packs this position into a {@code long}: x in the 32 low bits taken unsigned, z in the 32 high
   * bits (R30, provisional until probe P30). {@code (-1, -1)} gives -1, {@code (1, -1)} gives
   * {@code 0xFFFFFFFF00000001}.
   *
   * @return the packed position
   */
  public long pack() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Unpacks a position written by {@link #pack()}: {@code x = (int) v}, {@code z = (int) (v >>>
   * 32)} (R30, provisional).
   *
   * @param packed packed position, any {@code long}
   * @return the position
   */
  public static ChunkPos unpack(long packed) {
    return Skeleton.throwNotImplemented();
  }
}

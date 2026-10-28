package dev.shoko.minecraft.core.coord;

/**
 * Integer position of a block (R27 to R29). Any {@code int} triple is representable; the valid
 * world domain (R25, R26) is checked by callers, not here.
 *
 * @param x block x, east positive
 * @param y block y, up positive
 * @param z block z, south positive
 */
public record BlockPos(int x, int y, int z) {

  /**
   * Returns the block containing a point: {@code floorToInt} of each coordinate (R27, provisional).
   * A point at {@code -0.0001} is in block -1, a point at {@code -0.0} in block 0.
   *
   * @param x point x
   * @param y point y
   * @param z point z
   * @return the containing block
   */
  public static BlockPos containing(double x, double y, double z) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the chunk containing this block, {@code (x >> 4, z >> 4)} (R28).
   *
   * @return the chunk position
   */
  public ChunkPos chunk() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the section containing this block, {@code (x >> 4, y >> 4, z >> 4)} (R28).
   *
   * @return the section position
   */
  public SectionPos section() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Packs this position into a {@code long} (R29, provisional for the internal representation): x
   * on the 26 high bits (shift 38), z on 26 bits (shift 12), y on the 12 low bits, each field
   * masked in two's complement. Values outside x, z in {@code [-2^25, 2^25 - 1]} and y in {@code
   * [-2048, 2047]} wrap silently.
   *
   * @return the packed position
   */
  public long pack() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Unpacks a position written by {@link #pack()} with arithmetic shifts: {@code x = v >> 38},
   * {@code z = (v << 26) >> 38}, {@code y = (v << 52) >> 52} (R29, provisional).
   *
   * @param packed packed position, any {@code long}
   * @return the position, each field sign-extended
   */
  public static BlockPos unpack(long packed) {
    return Skeleton.throwNotImplemented();
  }
}

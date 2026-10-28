package dev.shoko.minecraft.core.coord;

/**
 * Position of a 16 x 16 x 16 section, in section units (R28, R31).
 *
 * @param x section x, {@code blockX >> 4}
 * @param y section y, {@code blockY >> 4}
 * @param z section z, {@code blockZ >> 4}
 */
public record SectionPos(int x, int y, int z) {

  /**
   * Packs this position into a {@code long}, network format (R31, provisional until a capture in
   * M8): x on 22 bits shifted by 42, z on 22 bits shifted by 20, y on the 20 low bits, each field
   * masked in two's complement. Values outside x, z in {@code [-2^21, 2^21 - 1]} and y in {@code
   * [-2^19, 2^19 - 1]} wrap silently.
   *
   * @return the packed position
   */
  public long pack() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Unpacks a position written by {@link #pack()}, each field sign-extended by arithmetic shifts
   * (R31, provisional).
   *
   * @param packed packed position, any {@code long}
   * @return the position
   */
  public static SectionPos unpack(long packed) {
    return Skeleton.throwNotImplemented();
  }
}

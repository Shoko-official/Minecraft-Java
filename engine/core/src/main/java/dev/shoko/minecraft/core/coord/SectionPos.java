package dev.shoko.minecraft.core.coord;

/**
 * Position of a 16 x 16 x 16 section, in section units (R28, R31).
 *
 * @param x section x, {@code blockX >> 4}
 * @param y section y, {@code blockY >> 4}
 * @param z section z, {@code blockZ >> 4}
 */
public record SectionPos(int x, int y, int z) {

  /** Width of the packed x and z fields (R31). */
  private static final int HORIZONTAL_BITS = 22;

  /** Width of the packed y field (R31). */
  private static final int Y_BITS = 20;

  private static final int Z_SHIFT = Y_BITS;
  private static final int X_SHIFT = Y_BITS + HORIZONTAL_BITS;
  private static final long HORIZONTAL_MASK = (1L << HORIZONTAL_BITS) - 1;
  private static final long Y_MASK = (1L << Y_BITS) - 1;

  /**
   * Packs this position into a {@code long}, network format (R31, provisional until a capture in
   * M8): x on 22 bits shifted by 42, z on 22 bits shifted by 20, y on the 20 low bits, each field
   * masked in two's complement. Values outside x, z in {@code [-2^21, 2^21 - 1]} and y in {@code
   * [-2^19, 2^19 - 1]} wrap silently.
   *
   * @return the packed position
   */
  public long pack() {
    return ((x & HORIZONTAL_MASK) << X_SHIFT) | ((z & HORIZONTAL_MASK) << Z_SHIFT) | (y & Y_MASK);
  }

  /**
   * Unpacks a position written by {@link #pack()}, each field sign-extended by arithmetic shifts
   * (R31, provisional).
   *
   * @param packed packed position, any {@code long}
   * @return the position
   */
  public static SectionPos unpack(long packed) {
    int x = (int) (packed >> X_SHIFT);
    int z = (int) ((packed << (Long.SIZE - X_SHIFT)) >> (Long.SIZE - HORIZONTAL_BITS));
    int y = (int) ((packed << (Long.SIZE - Y_BITS)) >> (Long.SIZE - Y_BITS));
    return new SectionPos(x, y, z);
  }
}

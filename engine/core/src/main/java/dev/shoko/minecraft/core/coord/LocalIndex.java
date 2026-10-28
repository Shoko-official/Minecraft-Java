package dev.shoko.minecraft.core.coord;

/**
 * The three distinct packings of a block position inside its section (R32, provisional until probe
 * P30 for the first two, network capture in M8 for the third). Never interchange them.
 *
 * <p>Each method masks every block coordinate with {@code & 15} first, so it accepts absolute block
 * coordinates, negatives included, and returns a value in {@code [0, 4095]}. The order of the 64
 * biome cells of a section is not documented yet (Q32) and has no method here.
 */
public final class LocalIndex {

  /** Bits of one local coordinate, 16 blocks per section side (R32). */
  private static final int LOCAL_BITS = 4;

  private LocalIndex() {}

  /**
   * Returns {@code (y & 15) << 8 | (z & 15) << 4 | (x & 15)}: the index in the block state array of
   * a section, order YZX (R32).
   *
   * @param x block x
   * @param y block y
   * @param z block z
   * @return the index, in {@code [0, 4095]}
   */
  public static int sectionStateIndex(int x, int y, int z) {
    return pack(y, z, x);
  }

  /**
   * Returns {@code (z & 15) << 8 | (y & 15) << 4 | (x & 15)}: the {@code short} of a chunk's {@code
   * PostProcessing} list, form {@code 0ZYX} (R32).
   *
   * @param x block x
   * @param y block y
   * @param z block z
   * @return the packed value, in {@code [0, 4095]}
   */
  public static int postProcessingIndex(int x, int y, int z) {
    return pack(z, y, x);
  }

  /**
   * Returns {@code (x & 15) << 8 | (z & 15) << 4 | (y & 15)}: the position part of a network
   * section update entry, before the state identifier shifted by 12 (R32).
   *
   * @param x block x
   * @param y block y
   * @param z block z
   * @return the packed value, in {@code [0, 4095]}
   */
  public static int sectionUpdateIndex(int x, int y, int z) {
    return pack(x, z, y);
  }

  private static int pack(int high, int middle, int low) {
    return (Coordinates.sectionLocal(high) << (2 * LOCAL_BITS))
        | (Coordinates.sectionLocal(middle) << LOCAL_BITS)
        | Coordinates.sectionLocal(low);
  }
}

package dev.shoko.minecraft.core.coord;

/**
 * Conversions of a single integer coordinate between block, section, quart and region units, and
 * the valid horizontal domain (R26, R28).
 *
 * <p>Every conversion is an arithmetic shift or a mask (R9), defined for every {@code int}
 * including negatives: block -1 is in section -1 at local 15, and in region -1. A chunk coordinate
 * on x and z is a section coordinate.
 */
public final class Coordinates {

  /**
   * Exclusive bound of the provisional horizontal block domain {@code [-30,000,000, 30,000,000)}
   * (R26, provisional, minecraft.wiki "World border"; exact bounds: probe P26).
   */
  public static final int HORIZONTAL_LIMIT = 30_000_000;

  /** A section is 16 blocks wide (R28). */
  private static final int SECTION_SHIFT = 4;

  private static final int SECTION_MASK = (1 << SECTION_SHIFT) - 1;

  /** A biome cell (quart) is 4 blocks wide (R28). */
  private static final int QUART_SHIFT = 2;

  /** A region is 32 chunks wide (R28, R33). */
  private static final int REGION_SHIFT = 5;

  private static final int REGION_MASK = (1 << REGION_SHIFT) - 1;

  private Coordinates() {}

  /**
   * Returns {@code block >> 4}: the chunk coordinate on x or z, the section coordinate on y (R28).
   *
   * @param block block coordinate
   * @return the section coordinate
   */
  public static int blockToSection(int block) {
    return block >> SECTION_SHIFT;
  }

  /**
   * Returns {@code block & 15}: the coordinate inside its section, in {@code [0, 15]} (R28).
   *
   * @param block block coordinate
   * @return the local coordinate
   */
  public static int sectionLocal(int block) {
    return block & SECTION_MASK;
  }

  /**
   * Returns {@code section << 4}: the first block coordinate of a section or chunk (R28). Overflows
   * silently for {@code |section| >= 2^27}, outside the domain of R26.
   *
   * @param section section or chunk coordinate
   * @return the lowest block coordinate in it
   */
  public static int sectionToBlock(int section) {
    return section << SECTION_SHIFT;
  }

  /**
   * Returns {@code block >> 2}: the biome cell (quart) coordinate (R28).
   *
   * @param block block coordinate
   * @return the quart coordinate
   */
  public static int blockToQuart(int block) {
    return block >> QUART_SHIFT;
  }

  /**
   * Returns {@code chunk >> 5}: the region coordinate (R28, R33).
   *
   * @param chunk chunk coordinate
   * @return the region coordinate
   */
  public static int chunkToRegion(int chunk) {
    return chunk >> REGION_SHIFT;
  }

  /**
   * Returns {@code chunk & 31}: the chunk coordinate inside its region, in {@code [0, 31]} (R28,
   * R33).
   *
   * @param chunk chunk coordinate
   * @return the local chunk coordinate
   */
  public static int regionLocal(int chunk) {
    return chunk & REGION_MASK;
  }

  /**
   * Returns {@code block >> 9}: the region coordinate of a block (R28).
   *
   * @param block block coordinate
   * @return the region coordinate
   */
  public static int blockToRegion(int block) {
    return block >> (SECTION_SHIFT + REGION_SHIFT);
  }

  /**
   * Tells whether a horizontal block coordinate is in {@code [-HORIZONTAL_LIMIT, HORIZONTAL_LIMIT)}
   * (R26, provisional).
   *
   * @param block block coordinate on x or z
   * @return {@code true} inside the domain
   */
  public static boolean isWithinHorizontalLimit(int block) {
    return block >= -HORIZONTAL_LIMIT && block < HORIZONTAL_LIMIT;
  }
}

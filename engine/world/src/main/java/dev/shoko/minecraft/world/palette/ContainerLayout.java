package dev.shoko.minecraft.world.palette;

/**
 * The fixed sizes of one kind of container on disk (CF-R7).
 *
 * @param entryCount number of positions: 4096 for blocks, 64 for biomes
 * @param minimumBits least bits per entry when the palette has more than one entry: 4 for blocks, 0
 *     for biomes
 * @param maxPaletteSize greatest palette size: 4096 for blocks (12 bits), 64 for biomes (6 bits)
 */
public record ContainerLayout(int entryCount, int minimumBits, int maxPaletteSize) {

  /** Blocks of a section, 16 x 16 x 16 (CF-R2, CF-R4, CF-R7). */
  public static final ContainerLayout BLOCKS = new ContainerLayout(4096, 4, 4096);

  /** Biome cells of a section, 4 x 4 x 4 (CF-R5, CF-R7). */
  public static final ContainerLayout BIOMES = new ContainerLayout(64, 0, 64);

  /**
   * Returns the bits per entry for a palette size (CF-R7): 0 for a single entry (no {@code data}),
   * else {@code max(minimumBits, ceil(log2(paletteSize)))}. Blocks: 2 -> 4, 16 -> 4, 17 -> 5, 33 ->
   * 6, 257 -> 9. Biomes: 2 -> 1, 3 -> 2, 5 -> 3, 9 -> 4.
   *
   * @param paletteSize number of palette entries, from 1 to {@link #maxPaletteSize()}
   * @return the bits per entry
   * @throws IllegalArgumentException if the size is out of range
   */
  public int bitsFor(int paletteSize) {
    if (paletteSize < 1 || paletteSize > maxPaletteSize) {
      throw new IllegalArgumentException("palette size out of range: " + paletteSize);
    }
    if (paletteSize == 1) {
      return 0;
    }
    int needed = Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1);
    return Math.max(minimumBits, needed);
  }

  /**
   * Returns the number of longs of {@code data} for a palette size (CF-R8, CF-R9): 0 for a single
   * entry. Blocks: 4 bits -> 256, 5 -> 342, 9 -> 586, 12 -> 820.
   *
   * @param paletteSize number of palette entries, from 1 to {@link #maxPaletteSize()}
   * @return the number of longs
   * @throws IllegalArgumentException if the size is out of range
   */
  public int longCount(int paletteSize) {
    int bits = bitsFor(paletteSize);
    return bits == 0 ? 0 : BitPacking.longCount(entryCount, bits);
  }
}

package dev.shoko.minecraft.world.palette;

/**
 * Packs small unsigned integers into longs, the way containers and heightmaps are stored (CF-R8,
 * CF-R17).
 *
 * <p>With {@code p = floor(64 / bits)} entries per long, entry {@code i} is in long {@code floor(i
 * / p)} at bits {@code bits * (i mod p)} up to {@code bits * (i mod p) + bits - 1}, counted from
 * the least significant bit. An entry never spans two longs: the unused high bits are 0 when
 * writing and ignored when reading. Only the bit pattern of a signed long counts: sixteen entries
 * of 15 at 4 bits make {@code -1}. A continuous packing (before 1.16) is wrong: 5 bits give 342
 * longs for 4096 entries, not 320.
 */
public final class BitPacking {

  private BitPacking() {}

  /**
   * Returns how many entries one long holds, {@code floor(64 / bits)} (CF-R8).
   *
   * @param bits bits per entry, from 1 to 32
   * @return the entries per long
   * @throws IllegalArgumentException if bits is out of range
   */
  public static int entriesPerLong(int bits) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of longs for a count of entries, {@code ceil(count / entriesPerLong(bits))}
   * (CF-R8).
   *
   * @param entryCount number of entries, not negative
   * @param bits bits per entry, from 1 to 32
   * @return the number of longs
   * @throws IllegalArgumentException if an argument is out of range
   */
  public static int longCount(int entryCount, int bits) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Packs values; the high bits left over in each long are 0.
   *
   * @param values the entries
   * @param bits bits per entry, from 1 to 32
   * @return {@code longCount(values.length, bits)} longs
   * @throws IllegalArgumentException if a value is negative or does not fit in {@code bits} bits
   */
  public static long[] pack(int[] values, int bits) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Unpacks values.
   *
   * @param data the longs
   * @param entryCount number of entries to read
   * @param bits bits per entry, from 1 to 32
   * @return the entries, each in {@code [0, 2^bits)}
   * @throws IllegalArgumentException if {@code data.length} is not {@code longCount(entryCount,
   *     bits)}
   */
  public static int[] unpack(long[] data, int entryCount, int bits) {
    return Skeleton.throwNotImplemented();
  }
}

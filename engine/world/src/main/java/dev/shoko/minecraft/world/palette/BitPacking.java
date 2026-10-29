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
    checkBits(bits);
    return Long.SIZE / bits;
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
    if (entryCount < 0) {
      throw new IllegalArgumentException("negative entry count " + entryCount);
    }
    int perLong = entriesPerLong(bits);
    return (int) (((long) entryCount + perLong - 1) / perLong);
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
    int perLong = entriesPerLong(bits);
    long limit = 1L << bits;
    long[] packed = new long[longCount(values.length, bits)];
    for (int i = 0; i < values.length; i++) {
      int value = values[i];
      if (value < 0 || value >= limit) {
        throw new IllegalArgumentException(
            "value " + value + " at " + i + " does not fit in " + bits + " bits");
      }
      packed[i / perLong] |= (long) value << (bits * (i % perLong));
    }
    return packed;
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
    int perLong = entriesPerLong(bits);
    if (data.length != longCount(entryCount, bits)) {
      throw new IllegalArgumentException(
          "expected " + longCount(entryCount, bits) + " longs, got " + data.length);
    }
    // An int entry is never negative (pack rejects negatives), so bit 31 of a 32 bit entry is
    // dropped.
    long mask = (1L << Math.min(bits, Integer.SIZE - 1)) - 1;
    int[] values = new int[entryCount];
    for (int i = 0; i < entryCount; i++) {
      values[i] = (int) ((data[i / perLong] >>> (bits * (i % perLong))) & mask);
    }
    return values;
  }

  private static void checkBits(int bits) {
    if (bits < 1 || bits > Integer.SIZE) {
      throw new IllegalArgumentException("bits per entry must be from 1 to 32: " + bits);
    }
  }
}

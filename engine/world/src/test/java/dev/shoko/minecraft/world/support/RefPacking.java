package dev.shoko.minecraft.world.support;

/** Reference packing of CF-R8, written from the spec and independent of the code under test. */
public final class RefPacking {

  private RefPacking() {}

  public static int perLong(int bits) {
    return 64 / bits;
  }

  public static int longCount(int count, int bits) {
    int per = perLong(bits);
    return (count + per - 1) / per;
  }

  public static long[] pack(int[] values, int bits) {
    int per = perLong(bits);
    long[] out = new long[longCount(values.length, bits)];
    for (int i = 0; i < values.length; i++) {
      out[i / per] |= (long) values[i] << (bits * (i % per));
    }
    return out;
  }

  public static int[] unpack(long[] data, int bits, int count) {
    int per = perLong(bits);
    long mask = (1L << bits) - 1;
    int[] out = new int[count];
    for (int i = 0; i < count; i++) {
      out[i] = (int) ((data[i / per] >>> (bits * (i % per))) & mask);
    }
    return out;
  }

  /** Bits per entry of a block palette of {@code n > 1} entries (CF-R7). */
  public static int blockBits(int n) {
    return Math.max(4, 32 - Integer.numberOfLeadingZeros(n - 1));
  }

  /** Bits per entry of a biome palette of {@code n > 1} entries (CF-R7). */
  public static int biomeBits(int n) {
    return 32 - Integer.numberOfLeadingZeros(n - 1);
  }

  /** Index values {@code i mod modulus} for {@code count} positions. */
  public static int[] cycle(int count, int modulus) {
    int[] values = new int[count];
    for (int i = 0; i < count; i++) {
      values[i] = i % modulus;
    }
    return values;
  }
}

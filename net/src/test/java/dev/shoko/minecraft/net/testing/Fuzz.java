package dev.shoko.minecraft.net.testing;

import java.util.Random;

/** Seeded random inputs for the attack tests: never an unseeded source, never a clock. */
public final class Fuzz {

  /** Bytes that matter to the wire formats: zero, continuation bits, maximum and small values. */
  private static final int[] INTERESTING = {
    0x00, 0x01, 0x02, 0x03, 0x04, 0x07, 0x0a, 0x0f, 0x20, 0x7f, 0x80, 0x81, 0xc0, 0xe2, 0xff
  };

  private Fuzz() {}

  /** Returns a random byte, often one of the interesting values. */
  public static byte nextByte(Random random) {
    if (random.nextInt(3) == 0) {
      return (byte) random.nextInt(256);
    }
    return (byte) INTERESTING[random.nextInt(INTERESTING.length)];
  }

  /** Returns {@code 0..maxLength} random bytes. */
  public static byte[] bytes(Random random, int maxLength) {
    byte[] out = new byte[random.nextInt(maxLength + 1)];
    for (int i = 0; i < out.length; i++) {
      out[i] = nextByte(random);
    }
    return out;
  }

  /** Returns a copy of the bytes with a few random bit flips, a truncation or an insertion. */
  public static byte[] mutate(Random random, byte[] source) {
    byte[] out = source.clone();
    int kind = random.nextInt(4);
    if (kind == 0 && out.length > 0) {
      out[random.nextInt(out.length)] ^= (byte) (1 << random.nextInt(8));
    } else if (kind == 1 && out.length > 0) {
      out[random.nextInt(out.length)] = nextByte(random);
    } else if (kind == 2) {
      return java.util.Arrays.copyOf(out, random.nextInt(out.length + 1));
    } else {
      byte[] longer = java.util.Arrays.copyOf(out, out.length + 1 + random.nextInt(3));
      for (int i = out.length; i < longer.length; i++) {
        longer[i] = nextByte(random);
      }
      return longer;
    }
    return out;
  }
}

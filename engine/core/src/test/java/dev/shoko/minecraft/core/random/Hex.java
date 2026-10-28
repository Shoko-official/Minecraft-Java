package dev.shoko.minecraft.core.random;

/** Parses the unsigned hexadecimal words of the spec vectors, written {@code 0x...}. */
final class Hex {

  private Hex() {}

  static long u64(String hex) {
    return Long.parseUnsignedLong(strip(hex), 16);
  }

  static int u32(String hex) {
    return Integer.parseUnsignedInt(strip(hex), 16);
  }

  static float f32(String hex) {
    return Float.intBitsToFloat(u32(hex));
  }

  static double f64(String hex) {
    return Double.longBitsToDouble(u64(hex));
  }

  static Seed128 seed(String lo, String hi) {
    return new Seed128(u64(lo), u64(hi));
  }

  static String of(long word) {
    return String.format("0x%016X", word);
  }

  private static String strip(String hex) {
    if (!hex.startsWith("0x")) {
      throw new IllegalArgumentException("expected 0x prefix: " + hex);
    }
    return hex.substring(2);
  }
}

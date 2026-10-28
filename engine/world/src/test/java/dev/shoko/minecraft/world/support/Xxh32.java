package dev.shoko.minecraft.world.support;

/** XXH32 from the public specification of the xxHash project, for building LZ4 test streams. */
public final class Xxh32 {

  private static final int P1 = (int) 2654435761L;
  private static final int P2 = (int) 2246822519L;
  private static final int P3 = (int) 3266489917L;
  private static final int P4 = 668265263;
  private static final int P5 = 374761393;

  private Xxh32() {}

  public static int hash(byte[] data, int seed) {
    int n = data.length;
    int i = 0;
    int h;
    if (n >= 16) {
      int v1 = seed + P1 + P2;
      int v2 = seed + P2;
      int v3 = seed;
      int v4 = seed - P1;
      while (i <= n - 16) {
        v1 = round(v1, lane(data, i));
        v2 = round(v2, lane(data, i + 4));
        v3 = round(v3, lane(data, i + 8));
        v4 = round(v4, lane(data, i + 12));
        i += 16;
      }
      h = Integer.rotateLeft(v1, 1) + Integer.rotateLeft(v2, 7);
      h += Integer.rotateLeft(v3, 12) + Integer.rotateLeft(v4, 18);
    } else {
      h = seed + P5;
    }
    h += n;
    while (i <= n - 4) {
      h = Integer.rotateLeft(h + lane(data, i) * P3, 17) * P4;
      i += 4;
    }
    while (i < n) {
      h = Integer.rotateLeft(h + (data[i] & 0xFF) * P5, 11) * P1;
      i++;
    }
    h ^= h >>> 15;
    h *= P2;
    h ^= h >>> 13;
    h *= P3;
    h ^= h >>> 16;
    return h;
  }

  private static int round(int acc, int lane) {
    return Integer.rotateLeft(acc + lane * P2, 13) * P1;
  }

  private static int lane(byte[] data, int at) {
    return (data[at] & 0xFF)
        | (data[at + 1] & 0xFF) << 8
        | (data[at + 2] & 0xFF) << 16
        | (data[at + 3] & 0xFF) << 24;
  }
}

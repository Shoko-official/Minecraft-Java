package dev.shoko.minecraft.world.storage;

/**
 * XXH32 of the public xxHash specification, the checksum of the LZ4Block frame (AR-R15). Only the
 * one-shot form is needed: a block is checksummed once, whole.
 */
final class Xxh32 {

  private static final int PRIME1 = 0x9E3779B1;
  private static final int PRIME2 = 0x85EBCA77;
  private static final int PRIME3 = 0xC2B2AE3D;
  private static final int PRIME4 = 0x27D4EB2F;
  private static final int PRIME5 = 0x165667B1;
  private static final int STRIPE = 16;

  private Xxh32() {}

  static int hash(byte[] data, int seed) {
    int length = data.length;
    int at = 0;
    int h;
    if (length >= STRIPE) {
      int v1 = seed + PRIME1 + PRIME2;
      int v2 = seed + PRIME2;
      int v3 = seed;
      int v4 = seed - PRIME1;
      for (; at <= length - STRIPE; at += STRIPE) {
        v1 = round(v1, readInt(data, at));
        v2 = round(v2, readInt(data, at + 4));
        v3 = round(v3, readInt(data, at + 8));
        v4 = round(v4, readInt(data, at + 12));
      }
      h = Integer.rotateLeft(v1, 1) + Integer.rotateLeft(v2, 7);
      h += Integer.rotateLeft(v3, 12) + Integer.rotateLeft(v4, 18);
    } else {
      h = seed + PRIME5;
    }
    h += length;
    for (; at <= length - 4; at += 4) {
      h = Integer.rotateLeft(h + readInt(data, at) * PRIME3, 17) * PRIME4;
    }
    for (; at < length; at++) {
      h = Integer.rotateLeft(h + (data[at] & 0xFF) * PRIME5, 11) * PRIME1;
    }
    h ^= h >>> 15;
    h *= PRIME2;
    h ^= h >>> 13;
    h *= PRIME3;
    h ^= h >>> 16;
    return h;
  }

  private static int round(int accumulator, int input) {
    return Integer.rotateLeft(accumulator + input * PRIME2, 13) * PRIME1;
  }

  private static int readInt(byte[] data, int at) {
    return (data[at] & 0xFF)
        | (data[at + 1] & 0xFF) << 8
        | (data[at + 2] & 0xFF) << 16
        | (data[at + 3] & 0xFF) << 24;
  }
}

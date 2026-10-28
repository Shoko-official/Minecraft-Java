package dev.shoko.minecraft.core.nbt.binary;

/**
 * Container around the binary form, chosen by the caller for each file: there is no automatic
 * detection.
 */
public enum NbtCompression {
  /** No container: the bytes are the binary form itself (R19, R20). */
  NONE,
  /**
   * Single-member gzip (RFC 1952). Written with the 10-byte header {@code 1f 8b 08 00 00 00 00 00
   * 00 ff} (R16, R17).
   */
  GZIP,
  /** Zlib (RFC 1950), the default for chunk data in region files (R18). */
  ZLIB
}

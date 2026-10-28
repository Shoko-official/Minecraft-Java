package dev.shoko.minecraft.world.storage;

import java.util.Optional;

/**
 * Compression types a region file reader accepts and the writer can produce (AR-R13, AR-R14). The
 * custom type 127 and any unknown type are not listed: reading them refuses the chunk.
 */
public enum RegionCompression {
  /** Type 1, gzip (RFC 1952). */
  GZIP(1),
  /** Type 2, zlib (RFC 1950); the default of the writer, server setting {@code deflate}. */
  ZLIB(2),
  /** Type 3, raw NBT; server setting {@code none}. */
  NONE(3),
  /** Type 4, LZ4 block frame of {@link Lz4BlockCodec}; server setting {@code lz4}. */
  LZ4(4);

  private final int id;

  RegionCompression(int id) {
    this.id = id;
  }

  /**
   * Returns the byte stored after the length of a chunk record.
   *
   * @return the type, 1 to 4
   */
  public int id() {
    return id;
  }

  /**
   * Finds a compression by the stored byte.
   *
   * @param id the byte, with the external flag (128) already removed
   * @return the compression, or empty for 127 and unknown values
   */
  public static Optional<RegionCompression> byId(int id) {
    return Skeleton.throwNotImplemented();
  }
}

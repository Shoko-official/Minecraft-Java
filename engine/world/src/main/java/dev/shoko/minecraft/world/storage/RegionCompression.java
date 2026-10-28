package dev.shoko.minecraft.world.storage;

import java.util.Optional;

/**
 * Compression types a region file reader accepts and the writer can produce (AR-R13, AR-R14). The
 * custom type 127 and any unknown type are not listed: reading them refuses the chunk.
 *
 * <p>Decompression is bounded for every type (AR-R29).
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

  /**
   * Default limit of decompressed bytes of one chunk record, 134 217 728 (2^27), the default quota
   * of the NBT reader (AR-R29, provisional: a safety ceiling, not the size of a real chunk).
   */
  public static final long DEFAULT_DECOMPRESSION_LIMIT = 134_217_728L;

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
   * Decompresses a record, counting the bytes produced (AR-R29). Gzip, zlib and LZ4 (all blocks
   * together) stop as soon as the excess is certain, without producing or allocating more than the
   * limit, and believe no announced size (neither the gzip ISIZE nor the length in an LZ4 block
   * header). For {@link #NONE} the length of {@code data} is compared with the limit. A total of
   * exactly {@code maxOutputBytes} is accepted, one byte more is not. The first event in stream
   * order decides: a corruption before the limit is corruption, the limit reached first is the
   * limit.
   *
   * @param data the compressed bytes, without the length and type bytes of the record
   * @param maxOutputBytes the most bytes that may be produced
   * @return the decompressed bytes
   * @throws OutputLimitExceededException if more than {@code maxOutputBytes} would be produced
   * @throws CorruptStreamException if the stream does not decode: error of the codec, wrong or
   *     missing trailer, truncated data
   */
  public byte[] decompress(byte[] data, long maxOutputBytes)
      throws CorruptStreamException, OutputLimitExceededException {
    return Skeleton.throwNotImplemented();
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

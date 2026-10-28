package dev.shoko.minecraft.world.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

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

  private static final int BUFFER_SIZE = 8192;

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
    return switch (this) {
      case NONE -> {
        if (data.length > maxOutputBytes) {
          throw new OutputLimitExceededException(maxOutputBytes);
        }
        yield data.clone();
      }
      case LZ4 -> Lz4BlockCodec.decode(data, maxOutputBytes);
      case ZLIB -> inflateZlib(data, maxOutputBytes);
      case GZIP -> inflate(openGzip(data), maxOutputBytes, () -> 0L);
    };
  }

  /**
   * Compresses NBT bytes for a record or a {@code .mcc} file; the output is deterministic.
   *
   * @param nbt the uncompressed bytes
   * @return the stream of this type
   */
  byte[] compress(byte[] nbt) {
    return switch (this) {
      case NONE -> nbt.clone();
      case LZ4 -> Lz4BlockCodec.encode(nbt);
      case ZLIB -> deflate(nbt, false);
      case GZIP -> deflate(nbt, true);
    };
  }

  private static byte[] deflate(byte[] nbt, boolean gzip) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (OutputStream stream = gzip ? new GZIPOutputStream(out) : new DeflaterOutputStream(out)) {
      stream.write(nbt);
    } catch (IOException e) {
      throw new UncheckedIOException("in-memory compression cannot fail", e);
    }
    return out.toByteArray();
  }

  private static InputStream openGzip(byte[] data) throws CorruptStreamException {
    try {
      return new GZIPInputStream(new ByteArrayInputStream(data));
    } catch (IOException e) {
      throw new CorruptStreamException("gzip header is invalid", e);
    }
  }

  private static byte[] inflateZlib(byte[] data, long limit)
      throws CorruptStreamException, OutputLimitExceededException {
    Inflater inflater = new Inflater();
    try {
      return inflate(
          new InflaterInputStream(new ByteArrayInputStream(data), inflater),
          limit,
          inflater::getBytesWritten);
    } finally {
      inflater.end();
    }
  }

  // Reads at most one byte past the limit, so the excess is certain as soon as it is seen. The
  // zlib trailer is checked by the call that produces the last bytes: when it fails, the bytes the
  // decoder had already produced tell which event came first.
  private static byte[] inflate(InputStream stream, long limit, LongSupplier producedSoFar)
      throws CorruptStreamException, OutputLimitExceededException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[BUFFER_SIZE];
    long total = 0;
    try (stream) {
      while (true) {
        int wanted = (int) Math.min(buffer.length, limit - total + 1);
        int read = stream.read(buffer, 0, wanted);
        if (read < 0) {
          return out.toByteArray();
        }
        total += read;
        if (total > limit) {
          throw new OutputLimitExceededException(limit);
        }
        out.write(buffer, 0, read);
      }
    } catch (IOException e) {
      if (producedSoFar.getAsLong() > limit) {
        throw new OutputLimitExceededException(limit);
      }
      throw new CorruptStreamException("compressed stream does not decode: " + e.getMessage(), e);
    }
  }

  /**
   * Finds a compression by the stored byte.
   *
   * @param id the byte, with the external flag (128) already removed
   * @return the compression, or empty for 127 and unknown values
   */
  public static Optional<RegionCompression> byId(int id) {
    for (RegionCompression compression : values()) {
      if (compression.id == id) {
        return Optional.of(compression);
      }
    }
    return Optional.empty();
  }
}

package dev.shoko.minecraft.world.storage;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.RegionPos;
import dev.shoko.minecraft.world.ReadResult;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * One region file {@code r.<rx>.<rz>.mca}: 32 x 32 chunks behind an 8192 byte header of 1024
 * locations and 1024 timestamps, then 4096 byte sectors (AR-R4 to AR-R12). Exchanges the NBT bytes
 * of chunks, uncompressed; it knows neither NBT nor chunks.
 *
 * <p>The index of a chunk is {@code (cx & 31) + 32 * (cz & 31)}; a location is {@code (offset << 8)
 * | count}, zero for an absent chunk (AR-R5, AR-R8). A record is a big-endian Int length {@code L},
 * the compression byte, then {@code L - 1} bytes (AR-R10). A chunk whose compressed data does not
 * fit in 255 sectors is stored in {@code c.<cx>.<cz>.mcc} next to the region file, and its record
 * is only {@code 00 00 00 01} and the compression byte plus 128 (AR-R11, AR-R17). Not thread safe;
 * one owner at a time.
 */
public final class RegionFile implements Closeable {

  private RegionFile() {}

  /**
   * Opens a region file. A missing file is valid: every chunk is absent (AR-R19) and the file is
   * created by the first write. The findings of a read go to the result of that read.
   *
   * @param regionFolder the {@code region/} folder
   * @param region the region position, which gives the file name (AR-R4)
   * @return the open file
   * @throws IOException if the file exists but cannot be read
   */
  public static RegionFile open(Path regionFolder, RegionPos region) throws IOException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads the NBT bytes of a chunk, decompressed, with {@link
   * RegionCompression#DEFAULT_DECOMPRESSION_LIMIT}.
   *
   * @param chunk absolute chunk position, which must be in this region
   * @return the issue of the read, see {@link #read(ChunkPos, long)}
   * @throws IOException on a read failure of the file system (AR-R27)
   * @throws IllegalArgumentException if the chunk is in another region
   */
  public ReadResult<byte[]> read(ChunkPos chunk) throws IOException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads the NBT bytes of a chunk, decompressed (AR-R19 to AR-R21, AR-R26 to AR-R29). Never
   * writes. Types 1 to 4 are accepted in any file (AR-R14). The issues, with their causes:
   *
   * <ul>
   *   <li>Present: the decompressed bytes of the record, or of its {@code .mcc} file for an
   *       external chunk (AR-R17). No warning.
   *   <li>Absent, no warning: NO_REGION_FILE when the file does not exist, EMPTY_SLOT when the
   *       location is zero (AR-R19).
   *   <li>Absent, with one warning: INVALID_LOCATION for an offset below 2, a zero count with a
   *       non-zero offset, sectors beyond the end of the file (a file shorter than its locations is
   *       invalid content, not a failure), {@code L <= 0} or {@code L + 4 > count * 4096} (AR-R20);
   *       UNREADABLE_RECORD when the stream does not decompress, trailer included (AR-R20).
   *   <li>Refused: UNSUPPORTED_COMPRESSION for type 127 or unknown (AR-R14); MISSING_EXTERNAL_FILE
   *       when the {@code .mcc} file is absent (AR-R18); TOO_LARGE when the decompressed output
   *       exceeds the limit (AR-R29). For type 3, {@code L - 1} (or the size of the {@code .mcc})
   *       is compared with the limit before any read.
   * </ul>
   *
   * The message of a refusal names the file and the index in the region.
   *
   * @param chunk absolute chunk position, which must be in this region
   * @param decompressionLimit the most bytes the decompression may produce; exactly that many is
   *     accepted
   * @return the issue of the read
   * @throws IOException on a read failure of the file system, which is none of the issues
   * @throws IllegalArgumentException if the chunk is in another region or the limit is negative
   */
  public ReadResult<byte[]> read(ChunkPos chunk, long decompressionLimit) throws IOException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes the NBT bytes of a chunk, replacing any previous record. The data are written first,
   * then the location and the timestamp, so a cut leaves the old chunk readable (AR "Ecriture",
   * provisional). The file stays a multiple of 4096 bytes. Where the sectors go is free (AR-R12). A
   * chunk too large for 255 sectors goes to its {@code .mcc} file; a chunk that fits removes a
   * leftover {@code .mcc} (AR-R18, provisional).
   *
   * @param chunk absolute chunk position, which must be in this region
   * @param nbt the uncompressed NBT bytes, root included
   * @param compression the type to use, {@link RegionCompression#ZLIB} by default in the game
   * @param timestampSeconds Unix seconds of the write, an unsigned 32 bit value, from the caller
   *     (AR-R9)
   * @throws IOException on a write failure
   * @throws IllegalArgumentException if the chunk is in another region or the timestamp is out of
   *     range
   */
  public void write(
      ChunkPos chunk, byte[] nbt, RegionCompression compression, long timestampSeconds)
      throws IOException {
    Skeleton.throwNotImplemented();
  }

  @Override
  public void close() throws IOException {
    Skeleton.throwNotImplemented();
  }
}

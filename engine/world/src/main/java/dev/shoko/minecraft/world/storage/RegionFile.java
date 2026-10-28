package dev.shoko.minecraft.world.storage;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.RegionPos;
import dev.shoko.minecraft.world.WarningSink;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

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
   * created by the first write.
   *
   * @param regionFolder the {@code region/} folder
   * @param region the region position, which gives the file name (AR-R4)
   * @param warnings where findings about invalid locations go (AR-R20)
   * @return the open file
   * @throws IOException if the file exists but cannot be read
   */
  public static RegionFile open(Path regionFolder, RegionPos region, WarningSink warnings)
      throws IOException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads the NBT bytes of a chunk, decompressed.
   *
   * <p>Absent (empty result, no error) when the location is zero or the file does not exist
   * (AR-R19). Absent with a warning, provisional, when the location is invalid: offset below 2,
   * zero count with a non-zero offset, sectors beyond the end of the file, {@code L <= 0}, {@code L
   * + 4 > count * 4096}, or a stream that does not decompress (AR-R20). Types 1 to 4 are accepted
   * in any file (AR-R14).
   *
   * @param chunk absolute chunk position, which must be in this region
   * @return the bytes, or empty when absent
   * @throws ChunkRefusedException for compression type 127 or unknown (AR-R14) and for a missing
   *     external file (AR-R18)
   * @throws IOException on a read failure
   * @throws IllegalArgumentException if the chunk is in another region
   */
  public Optional<byte[]> read(ChunkPos chunk) throws IOException, ChunkRefusedException {
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

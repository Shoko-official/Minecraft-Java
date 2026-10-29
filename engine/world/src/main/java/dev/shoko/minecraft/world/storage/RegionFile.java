package dev.shoko.minecraft.world.storage;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.RegionPos;
import dev.shoko.minecraft.world.AbsentCause;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.ReadWarning;
import dev.shoko.minecraft.world.RefusedCause;
import dev.shoko.minecraft.world.WarningCode;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
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
 *
 * <p>A file shorter than its 8192 byte header reads as if the missing part were zeros (AR, open
 * question): nothing there is a failure of the file system.
 */
public final class RegionFile implements Closeable {

  static final int SECTOR = 4096;
  static final int HEADER_BYTES = 2 * SECTOR;
  static final int EXTERNAL_FLAG = 0x80;
  private static final int RECORD_HEAD = 5;
  private static final int LOCATIONS = 1024;
  // A stream that decodes to the limit is at most a few percent larger than it (stored blocks and
  // block headers); a bigger external file cannot be a stream within the limit.
  private static final long MCC_SLACK = 4096;

  private final Path folder;
  private final RegionPos region;
  private final Path file;
  private FileChannel channel;
  private boolean writable;

  private RegionFile(Path folder, RegionPos region) {
    this.folder = folder;
    this.region = region;
    this.file = folder.resolve(WorldLayout.regionFileName(region));
  }

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
    RegionFile opened = new RegionFile(regionFolder, region);
    if (existsAsRegularFile(opened.file)) {
      opened.channel =
          FileChannel.open(opened.file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
    }
    return opened;
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
    return read(chunk, RegionCompression.DEFAULT_DECOMPRESSION_LIMIT);
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
    checkRegion(chunk);
    if (decompressionLimit < 0) {
      throw new IllegalArgumentException("negative decompression limit " + decompressionLimit);
    }
    if (channel == null) {
      if (!existsAsRegularFile(file)) {
        return new ReadResult.Absent<>(AbsentCause.NO_REGION_FILE, List.of());
      }
      channel = FileChannel.open(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
    }
    int index = chunk.indexInRegion();
    int location = ByteBuffer.wrap(readAt(4L * index, Integer.BYTES)).getInt();
    if (location == 0) {
      return new ReadResult.Absent<>(AbsentCause.EMPTY_SLOT, List.of());
    }
    long size = channel.size();
    int offset = location >>> 8;
    int count = location & 0xFF;
    if (offset < 2 || count == 0 || (long) (offset + count) * SECTOR > size) {
      return invalidLocation(index, "location " + offset + "/" + count + " is outside the file");
    }
    ByteBuffer head = ByteBuffer.wrap(readAt((long) offset * SECTOR, RECORD_HEAD));
    int length = head.getInt();
    int type = head.get() & 0xFF;
    if (length <= 0 || (long) length + Integer.BYTES > (long) count * SECTOR) {
      return invalidLocation(index, "record length " + length + " does not fit its sectors");
    }
    return decodeRecord(chunk, index, offset, length, type, decompressionLimit);
  }

  private ReadResult<byte[]> decodeRecord(
      ChunkPos chunk, int index, int offset, int length, int type, long limit) throws IOException {
    Optional<RegionCompression> known = RegionCompression.byId(type & ~EXTERNAL_FLAG);
    if (known.isEmpty()) {
      return refused(
          RefusedCause.UNSUPPORTED_COMPRESSION, index, "compression type " + type + " is not read");
    }
    RegionCompression compression = known.get();
    byte[] data;
    if ((type & EXTERNAL_FLAG) != 0) {
      Path external = folder.resolve(WorldLayout.externalChunkFileName(chunk));
      if (!existsAsRegularFile(external)) {
        return refused(
            RefusedCause.MISSING_EXTERNAL_FILE, index, external.getFileName() + " does not exist");
      }
      long externalSize = Files.size(external);
      if (externalSize > Math.min(externalBound(compression, limit), OutputPieces.MAX_BYTES)) {
        return refused(RefusedCause.TOO_LARGE, index, external.getFileName() + " is too large");
      }
      data = readExternal(external, (int) externalSize);
    } else {
      if (compression == RegionCompression.NONE && length - 1 > limit) {
        return refused(RefusedCause.TOO_LARGE, index, "raw record exceeds the limit " + limit);
      }
      data = readAt((long) offset * SECTOR + RECORD_HEAD, length - 1);
    }
    try {
      return new ReadResult.Present<>(compression.decompress(data, limit), List.of());
    } catch (OutputLimitExceededException e) {
      return refused(RefusedCause.TOO_LARGE, index, "decompressed output exceeds " + limit);
    } catch (CorruptStreamException e) {
      return new ReadResult.Absent<>(
          AbsentCause.UNREADABLE_RECORD,
          List.of(
              new ReadWarning(
                  WarningCode.UNREADABLE_RECORD, where(index) + ": " + e.getMessage())));
    }
  }

  private static long externalBound(RegionCompression compression, long limit) {
    if (compression == RegionCompression.NONE) {
      return limit;
    }
    long extra = limit / 16 + MCC_SLACK;
    return limit > Long.MAX_VALUE - extra ? Long.MAX_VALUE : limit + extra;
  }

  // Anything that is not a plain file, a symbolic link included, is refused rather than followed.
  private static boolean existsAsRegularFile(Path path) throws IOException {
    BasicFileAttributes attributes;
    try {
      attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    } catch (NoSuchFileException e) {
      return false;
    }
    if (!attributes.isRegularFile()) {
      throw new FileSystemException(path.toString(), null, "not a regular file");
    }
    return true;
  }

  private static byte[] readExternal(Path path, int size) throws IOException {
    try (FileChannel external =
        FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
      ByteBuffer buffer = ByteBuffer.allocate(size);
      while (buffer.hasRemaining() && external.read(buffer) >= 0) {
        // read until the buffer is full or the file ends
      }
      return buffer.position() == size
          ? buffer.array()
          : Arrays.copyOf(buffer.array(), buffer.position());
    }
  }

  // The stream is complete under its final name before the location points to it.
  private static void writeExternal(Path path, byte[] payload) throws IOException {
    Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
    Files.deleteIfExists(temporary);
    try {
      Files.write(temporary, payload, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
      Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private ReadResult<byte[]> invalidLocation(int index, String detail) {
    return new ReadResult.Absent<>(
        AbsentCause.INVALID_LOCATION,
        List.of(new ReadWarning(WarningCode.INVALID_LOCATION, where(index) + ": " + detail)));
  }

  private ReadResult<byte[]> refused(RefusedCause cause, int index, String detail) {
    return new ReadResult.Refused<>(cause, where(index) + ": " + detail);
  }

  private String where(int index) {
    return file.getFileName() + " index " + index;
  }

  /** Reads {@code length} bytes at a position; whatever lies beyond the end of the file is zero. */
  private byte[] readAt(long position, int length) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(length);
    long at = position;
    while (buffer.hasRemaining()) {
      int read = channel.read(buffer, at);
      if (read < 0) {
        break;
      }
      at += read;
    }
    return buffer.array();
  }

  private void checkRegion(ChunkPos chunk) {
    if (!chunk.region().equals(region)) {
      throw new IllegalArgumentException("chunk " + chunk + " is not in region " + region);
    }
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
    checkRegion(chunk);
    if (timestampSeconds < 0 || timestampSeconds > 0xFFFF_FFFFL) {
      throw new IllegalArgumentException("timestamp out of the unsigned 32 bit range");
    }
    byte[] payload = compression.compress(nbt);
    boolean external = payload.length + RECORD_HEAD > 255 * SECTOR;
    byte[] record =
        external
            ? RegionRecords.externalRecord(compression)
            : RegionRecords.record(compression.id(), payload);
    Path mcc = folder.resolve(WorldLayout.externalChunkFileName(chunk));
    makeWritable();
    if (external) {
      writeExternal(mcc, payload);
    }
    int sector =
        RegionRecords.firstFit(readAt(0, LOCATIONS * Integer.BYTES), record.length / SECTOR);
    writeAt((long) sector * SECTOR, ByteBuffer.wrap(record));
    padToWholeSectors();
    int index = chunk.indexInRegion();
    writeAt(4L * index, intBuffer((sector << 8) | (record.length / SECTOR)));
    writeAt(SECTOR + 4L * index, intBuffer((int) timestampSeconds));
    if (!external) {
      Files.deleteIfExists(mcc);
    }
  }

  private void makeWritable() throws IOException {
    if (writable) {
      return;
    }
    if (channel != null) {
      channel.close();
    }
    Files.createDirectories(folder);
    existsAsRegularFile(file);
    channel =
        FileChannel.open(
            file,
            StandardOpenOption.CREATE,
            StandardOpenOption.READ,
            StandardOpenOption.WRITE,
            LinkOption.NOFOLLOW_LINKS);
    writable = true;
    if (channel.size() < HEADER_BYTES) {
      writeAt(HEADER_BYTES - 1L, ByteBuffer.wrap(new byte[1]));
    }
  }

  private void padToWholeSectors() throws IOException {
    long size = channel.size();
    if (size % SECTOR != 0) {
      writeAt((size / SECTOR + 1) * SECTOR - 1, ByteBuffer.wrap(new byte[1]));
    }
  }

  private void writeAt(long position, ByteBuffer buffer) throws IOException {
    long at = position;
    while (buffer.hasRemaining()) {
      at += channel.write(buffer, at);
    }
  }

  private static ByteBuffer intBuffer(int value) {
    return ByteBuffer.allocate(Integer.BYTES).putInt(0, value);
  }

  @Override
  public void close() throws IOException {
    if (channel != null) {
      FileChannel closing = channel;
      channel = null;
      writable = false;
      closing.close();
    }
  }
}

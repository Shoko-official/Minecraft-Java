package dev.shoko.minecraft.tools.oracle.anvil;

import dev.shoko.minecraft.tools.oracle.nbt.NamedCompound;
import dev.shoko.minecraft.tools.oracle.nbt.NbtReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only view of one Anvil region file ({@code r.<x>.<z>.mca}), independent of the engine.
 *
 * <p>Layout: a header of 1024 big-endian location entries (sector offset on 3 bytes, sector count
 * on 1 byte) followed by 1024 timestamps, then 4096-byte sectors. A chunk starts with its length on
 * 4 bytes (compression byte included) and a compression byte: 1 gzip, 2 zlib, 3 none. LZ4 (4) and
 * external chunk files (flag 0x80) are refused explicitly rather than misread.
 */
public final class RegionFile {

  public static final int SECTOR_BYTES = 4096;
  public static final int CHUNKS_PER_SIDE = 32;

  private static final int CHUNK_COUNT = CHUNKS_PER_SIDE * CHUNKS_PER_SIDE;
  private static final int HEADER_SECTORS = 2;
  private static final int HEADER_BYTES = HEADER_SECTORS * SECTOR_BYTES;
  private static final int TIMESTAMPS_OFFSET = CHUNK_COUNT * Integer.BYTES;
  private static final int CHUNK_HEADER_BYTES = 5;
  private static final int COMPRESSION_GZIP = 1;
  private static final int COMPRESSION_ZLIB = 2;
  private static final int COMPRESSION_NONE = 3;
  private static final int COMPRESSION_LZ4 = 4;
  private static final int EXTERNAL_FLAG = 0x80;
  private static final long MAX_FILE_BYTES = 1L << 30;
  private static final Pattern FILE_NAME = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");

  private final String source;
  private final int regionX;
  private final int regionZ;
  private final ByteBuffer data;
  private final NbtReader reader;

  private RegionFile(String source, int regionX, int regionZ, byte[] bytes, NbtReader reader)
      throws IOException {
    if (bytes.length < HEADER_BYTES) {
      throw new IOException(
          source
              + ": "
              + bytes.length
              + " bytes, shorter than the "
              + HEADER_BYTES
              + "-byte header");
    }
    this.source = source;
    this.regionX = regionX;
    this.regionZ = regionZ;
    this.data = ByteBuffer.wrap(bytes).asReadOnlyBuffer();
    this.reader = reader;
  }

  /** Reads a region file whose name gives its coordinates, as {@code r.-1.0.mca}. */
  public static RegionFile read(Path file) throws IOException {
    Matcher name = FILE_NAME.matcher(file.getFileName().toString());
    if (!name.matches()) {
      throw new IOException(file + ": not a region file name (r.<x>.<z>.mca)");
    }
    int regionX;
    int regionZ;
    try {
      regionX = Integer.parseInt(name.group(1));
      regionZ = Integer.parseInt(name.group(2));
    } catch (NumberFormatException e) {
      throw new IOException(file + ": region coordinates in the file name do not fit an int", e);
    }
    long size = Files.size(file);
    if (size > MAX_FILE_BYTES) {
      throw new IOException(file + ": " + size + " bytes, larger than " + MAX_FILE_BYTES);
    }
    return new RegionFile(
        file.toString(), regionX, regionZ, Files.readAllBytes(file), new NbtReader());
  }

  /** Wraps region bytes that are already in memory. */
  public static RegionFile of(int regionX, int regionZ, byte[] bytes, NbtReader reader)
      throws IOException {
    return new RegionFile(fileName(regionX, regionZ), regionX, regionZ, bytes.clone(), reader);
  }

  /** Region coordinate that holds a chunk coordinate, correct for negative values. */
  public static int regionOf(int chunkCoordinate) {
    return chunkCoordinate >> 5;
  }

  public static String fileName(int regionX, int regionZ) {
    return "r." + regionX + "." + regionZ + ".mca";
  }

  /** Header slot of a chunk: its coordinates masked to 0..31, x varying fastest. */
  public static int index(int chunkX, int chunkZ) {
    return (chunkX & (CHUNKS_PER_SIDE - 1)) + (chunkZ & (CHUNKS_PER_SIDE - 1)) * CHUNKS_PER_SIDE;
  }

  public int regionX() {
    return regionX;
  }

  public int regionZ() {
    return regionZ;
  }

  /** True when the header has a location entry for the chunk. */
  public boolean contains(int chunkX, int chunkZ) {
    return location(chunkX, chunkZ) != 0;
  }

  /** Last save time recorded in the header, unsigned epoch seconds, 0 when absent. */
  public long timestamp(int chunkX, int chunkZ) {
    location(chunkX, chunkZ);
    return Integer.toUnsignedLong(
        data.getInt(TIMESTAMPS_OFFSET + index(chunkX, chunkZ) * Integer.BYTES));
  }

  /**
   * Decodes a chunk, or returns empty when the header has no entry for it. An entry that points
   * outside the file, into the header, onto sectors of another chunk, or to an inconsistent length
   * fails with the chunk named.
   */
  public Optional<NamedCompound> chunk(int chunkX, int chunkZ) throws IOException {
    int location = location(chunkX, chunkZ);
    if (location == 0) {
      return Optional.empty();
    }
    int offset = location >>> 8;
    int sectors = location & 0xff;
    String where = source + " chunk (" + chunkX + "," + chunkZ + ")";
    if (offset < HEADER_SECTORS || sectors == 0) {
      throw new IOException(where + ": invalid location, sector " + offset + " count " + sectors);
    }
    checkNoOverlap(index(chunkX, chunkZ), offset, sectors, where);
    long start = (long) offset * SECTOR_BYTES;
    if (start + CHUNK_HEADER_BYTES > data.capacity()) {
      throw new IOException(where + ": sector " + offset + " is outside the file");
    }
    int length = data.getInt((int) start);
    if (length < 1) {
      throw new IOException(where + ": invalid chunk length " + length);
    }
    if ((long) length + Integer.BYTES > (long) sectors * SECTOR_BYTES) {
      throw new IOException(
          where + ": length " + length + " does not fit in its " + sectors + " sector(s)");
    }
    if (start + Integer.BYTES + length > data.capacity()) {
      throw new IOException(where + ": length " + length + " runs past the end of the file");
    }
    int compression = data.get((int) start + Integer.BYTES) & 0xff;
    byte[] payload = new byte[length - 1];
    data.get((int) start + CHUNK_HEADER_BYTES, payload);
    try {
      return Optional.of(decode(compression, new ByteArrayInputStream(payload)));
    } catch (IOException e) {
      throw new IOException(where + ": " + e.getMessage(), e);
    }
  }

  /**
   * Like {@link #chunk}, and also checks that the {@code xPos} and {@code zPos} ints at the root of
   * the chunk name the requested position, which catches a chunk stored in the wrong slot.
   */
  public Optional<NamedCompound> chunkCheckingPosition(int chunkX, int chunkZ) throws IOException {
    Optional<NamedCompound> chunk = chunk(chunkX, chunkZ);
    if (chunk.isPresent()) {
      Object x = chunk.get().value().get("xPos");
      Object z = chunk.get().value().get("zPos");
      if (!Integer.valueOf(chunkX).equals(x) || !Integer.valueOf(chunkZ).equals(z)) {
        throw new IOException(
            source
                + " chunk ("
                + chunkX
                + ","
                + chunkZ
                + "): its NBT says xPos="
                + x
                + " zPos="
                + z);
      }
    }
    return chunk;
  }

  /** Fails when the sectors of the chunk at {@code slot} are also claimed by another chunk. */
  private void checkNoOverlap(int slot, int offset, int sectors, String where) throws IOException {
    for (int other = 0; other < CHUNK_COUNT; other++) {
      int location = data.getInt(other * Integer.BYTES);
      if (other == slot || location == 0) {
        continue;
      }
      int otherOffset = location >>> 8;
      int otherEnd = otherOffset + (location & 0xff);
      if (otherOffset < offset + sectors && offset < otherEnd) {
        int otherX = regionX * CHUNKS_PER_SIDE + other % CHUNKS_PER_SIDE;
        int otherZ = regionZ * CHUNKS_PER_SIDE + other / CHUNKS_PER_SIDE;
        throw new IOException(
            where
                + ": sectors "
                + offset
                + ".."
                + (offset + sectors - 1)
                + " overlap those of chunk ("
                + otherX
                + ","
                + otherZ
                + ")");
      }
    }
  }

  private NamedCompound decode(int compression, InputStream payload) throws IOException {
    if ((compression & EXTERNAL_FLAG) != 0) {
      throw new IOException("chunk stored in an external .mcc file, which is not supported");
    }
    return switch (compression) {
      case COMPRESSION_GZIP -> reader.readGzip(payload);
      case COMPRESSION_ZLIB -> reader.readZlib(payload);
      case COMPRESSION_NONE -> reader.read(payload);
      case COMPRESSION_LZ4 ->
          throw new IOException(
              "LZ4 compression is not supported; run the oracle with"
                  + " region-file-compression=deflate");
      default -> throw new IOException("unknown compression type " + compression);
    };
  }

  private int location(int chunkX, int chunkZ) {
    if (regionOf(chunkX) != regionX || regionOf(chunkZ) != regionZ) {
      throw new IllegalArgumentException(
          "chunk ("
              + chunkX
              + ","
              + chunkZ
              + ") is not in region ("
              + regionX
              + ","
              + regionZ
              + ")");
    }
    return data.getInt(index(chunkX, chunkZ) * Integer.BYTES);
  }
}

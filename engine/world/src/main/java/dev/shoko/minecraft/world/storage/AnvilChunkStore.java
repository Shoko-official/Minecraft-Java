package dev.shoko.minecraft.world.storage;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.chunk.ChunkCodec;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Loads and saves the chunks of one dimension in its {@code region/} folder: region file, then NBT,
 * then {@link ChunkCodec} (AR-R19 to AR-R21, AR-R24, AR-R26 to AR-R30). Not thread safe; the thread
 * that ticks the world owns it.
 *
 * <p>The NBT written is one named root Compound with an empty name and nothing after it (AR-R24).
 * The NBT read is more tolerant (AR-R30): see {@link #load}.
 *
 * <p>The store remembers, until it is closed, the positions whose load was refused (the mark,
 * AR-R28). A marked position is never read again from the file: it keeps answering the same
 * refusal, and a save there fails. A position that was absent is never marked. A save at a position
 * that was never loaded is not checked: the caller loads before it saves.
 */
public final class AnvilChunkStore implements Closeable {

  private AnvilChunkStore() {}

  /**
   * Opens the store of a dimension with {@link RegionCompression#DEFAULT_DECOMPRESSION_LIMIT}.
   *
   * @param regionFolder the {@code region/} folder, which may not exist yet
   * @param height vertical extent of the dimension
   * @param codec the chunk NBT codec
   * @param compression type used for writing
   * @return the store
   */
  public static AnvilChunkStore open(
      Path regionFolder, WorldHeight height, ChunkCodec codec, RegionCompression compression) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Opens the store of a dimension.
   *
   * @param regionFolder the {@code region/} folder, which may not exist yet
   * @param height vertical extent of the dimension
   * @param codec the chunk NBT codec
   * @param compression type used for writing
   * @param decompressionLimit the most bytes the decompression of one record may produce (AR-R29)
   * @return the store
   * @throws IllegalArgumentException if the limit is negative
   */
  public static AnvilChunkStore open(
      Path regionFolder,
      WorldHeight height,
      ChunkCodec codec,
      RegionCompression compression,
      long decompressionLimit) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Loads a chunk. Exactly one of the three issues of AR-R26; the causes and warnings are those of
   * {@link RegionFile#read(ChunkPos, long)}, of {@link ChunkCodec#decode}, and of these rules:
   *
   * <ul>
   *   <li>Absent, UNREADABLE_RECORD, with a warning: the NBT is malformed (unknown type, premature
   *       end, unterminated Compound, negative array length) or the root is not a Compound (AR-R30
   *       a, b).
   *   <li>Absent, POSITION_MISMATCH, with a warning: {@code xPos} or {@code zPos} differ from
   *       {@code pos} or are missing (AR-R21, CF-R35).
   *   <li>Refused, TOO_LARGE or TOO_DEEP: the NBT quota or depth is exceeded (AR-R30 e).
   *   <li>Present with a warning: a non-empty root name, ignored, ROOT_NAME_IGNORED; bytes after
   *       the root, ignored, TRAILING_BYTES (AR-R30 c, d).
   * </ul>
   *
   * A refusal marks the position (AR-R28); later loads of it answer the same refusal without
   * reading the file. Nothing is generated and the bytes are never touched. The message of a
   * refusal names the file, the index in the region, and the faulty field or section.
   *
   * @param pos absolute chunk position
   * @return the issue of the read
   * @throws IOException on a failure of the file system, which is none of the issues: nothing is
   *     generated, no mark is set (AR-R27)
   */
  public ReadResult<Chunk> load(ChunkPos pos) throws IOException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Saves a chunk, replacing what is stored at its position. Writes nothing for a marked position:
   * no record, no timestamp, no {@code .mcc} written or deleted, and the region file keeps its size
   * and content (AR-R28).
   *
   * @param chunk the chunk
   * @param timestampSeconds Unix seconds written in the region header, from the caller
   * @throws WriteOnRefusedPositionException if the position is marked: a load of it was refused
   * @throws IOException on a write failure
   */
  public void save(Chunk chunk, long timestampSeconds) throws IOException {
    Skeleton.throwNotImplemented();
  }

  /** Closes the region files and clears the marks: an external repair is seen on reopening. */
  @Override
  public void close() throws IOException {
    Skeleton.throwNotImplemented();
  }
}

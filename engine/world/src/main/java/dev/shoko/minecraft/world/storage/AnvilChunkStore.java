package dev.shoko.minecraft.world.storage;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.world.WarningSink;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.chunk.ChunkCodec;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Loads and saves the chunks of one dimension in its {@code region/} folder: region file, then NBT,
 * then {@link ChunkCodec} (AR-R19 to AR-R21, AR-R24). Not thread safe; the thread that ticks the
 * world owns it.
 *
 * <p>The NBT after decompression is one named root Compound with an empty name and nothing after it
 * (AR-R24, provisional). A chunk is absent, with a warning, when its {@code xPos} or {@code zPos}
 * differ from its position (AR-R21). A chunk is refused when its tree breaks the chunk schema
 * ({@code ChunkFormatException}) or its record is refused by the region file; a refused position
 * keeps raising the same {@link ChunkRefusedException} on every load and is never overwritten by a
 * save, so the bytes stay for a repair. The world keeps loading and running.
 */
public final class AnvilChunkStore implements Closeable {

  private AnvilChunkStore() {}

  /**
   * Opens the store of a dimension.
   *
   * @param regionFolder the {@code region/} folder, which may not exist yet
   * @param height vertical extent of the dimension
   * @param codec the chunk NBT codec
   * @param compression type used for writing
   * @param warnings where findings about absent chunks go
   * @return the store
   */
  public static AnvilChunkStore open(
      Path regionFolder,
      WorldHeight height,
      ChunkCodec codec,
      RegionCompression compression,
      WarningSink warnings) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Loads a chunk.
   *
   * @param pos absolute chunk position
   * @return the chunk, or empty when absent: not saved yet, an invalid location, or a position
   *     mismatch; the caller then generates it
   * @throws ChunkRefusedException when the chunk is refused
   * @throws IOException on a read failure
   */
  public Optional<Chunk> load(ChunkPos pos) throws IOException, ChunkRefusedException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Saves a chunk, replacing what is stored at its position. Does nothing for a position that was
   * refused by an earlier load.
   *
   * @param chunk the chunk
   * @param timestampSeconds Unix seconds written in the region header, from the caller
   * @throws IOException on a write failure
   */
  public void save(Chunk chunk, long timestampSeconds) throws IOException {
    Skeleton.throwNotImplemented();
  }

  @Override
  public void close() throws IOException {
    Skeleton.throwNotImplemented();
  }
}

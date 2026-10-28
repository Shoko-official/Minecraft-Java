package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.world.WarningSink;
import dev.shoko.minecraft.world.biome.BiomeCatalog;
import dev.shoko.minecraft.world.block.BlockStateRegistry;

/**
 * Converts a {@link Chunk} to and from the root Compound of a region file chunk (CF-R20 to CF-R26).
 *
 * <p>Reading: only {@code DataVersion} {@link #SUPPORTED_DATA_VERSION} is accepted. Unknown root
 * keys are ignored (R21). The fields of R20 are read, with these provisional behaviors:
 *
 * <ul>
 *   <li>Sections: {@code block_states} and {@code biomes} are paletted containers (R6 to R11); a
 *       section without {@code block_states} is all air and without {@code biomes} all {@code
 *       minecraft:plains} (R22). A section outside the world height carries light only (R15).
 *   <li>{@code Heightmaps}: see {@link dev.shoko.minecraft.world.heightmap.Heightmaps#fromNbt}.
 *   <li>{@code block_entities}: an entry outside the chunk is ignored with a warning (R26).
 *   <li>{@code entities} is read only for a proto-chunk (R21).
 *   <li>A status absent from {@link ChunkStatus}, noise, surface and carvers included, is refused
 *       (R29).
 * </ul>
 *
 * <p>Writing produces R20 to R26: the field order is not significant, palettes are compacted (R7),
 * {@code entities} only for a proto-chunk, heightmaps only for held types, {@code References} for
 * structure references. Only the keys the engine can produce are written (R21). The decoded tree,
 * not the bytes, is what is compared with the reference.
 */
public final class ChunkCodec {

  /** Data version of the reference game 26.3, read from its {@code version.json} (CF-R20). */
  public static final int SUPPORTED_DATA_VERSION = 5023;

  private ChunkCodec() {}

  /**
   * Creates a codec.
   *
   * @param blocks the blocks and states that palette entries resolve against
   * @param biomes the known biomes
   * @param warnings where the findings of the lenient reads go
   * @return the codec
   */
  public static ChunkCodec of(
      BlockStateRegistry blocks, BiomeCatalog biomes, WarningSink warnings) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a chunk.
   *
   * @param root the root Compound, after decompression
   * @param height vertical extent of the dimension the chunk belongs to
   * @return the chunk; whether its position is the one expected is for the caller to check
   * @throws ChunkFormatException if a required field is missing or mistyped, the data version or
   *     status is unsupported, or a container is inconsistent (R10); the fault names the field
   */
  public Chunk decode(CompoundTag root, WorldHeight height) throws ChunkFormatException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes a chunk.
   *
   * @param chunk the chunk
   * @return the root Compound, with {@code DataVersion} {@link #SUPPORTED_DATA_VERSION}
   */
  public CompoundTag encode(Chunk chunk) {
    return Skeleton.throwNotImplemented();
  }
}

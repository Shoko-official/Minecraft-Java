package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.biome.BiomeCatalog;
import dev.shoko.minecraft.world.block.BlockStateRegistry;

/**
 * Converts a {@link Chunk} to and from the root Compound of a region file chunk (CF-R20 to CF-R26,
 * CF-R30 to CF-R35).
 *
 * <p>Reading returns one of the three issues of {@link ReadResult} and never throws for a fault of
 * the data. Unknown root keys are ignored (R21). The fields of R20 are read, with these provisional
 * behaviors:
 *
 * <ul>
 *   <li>Refused, by cause: {@code DataVersion} missing, mistyped or not {@link
 *       #SUPPORTED_DATA_VERSION} (UNSUPPORTED_VERSION, R35); {@code Status} or {@code sections}
 *       missing or mistyped (INVALID_FIELD, R35, R32 a); a status absent from {@link ChunkStatus},
 *       noise, surface and carvers included (UNSUPPORTED_STATUS, R29); an inconsistent container
 *       (INVALID_CONTAINER, R10); a section without a Byte {@code Y} or two sections with the same
 *       {@code Y} (INVALID_SECTION, R32 b, c). No invalid palette entry refuses a chunk (BS-R22).
 *   <li>Absent, cause POSITION_MISMATCH: {@code xPos} or {@code zPos} missing or not an Int (R35).
 *       Whether a present position is the one expected is for the caller to check.
 *   <li>Sections: reconciled by R32. Order is irrelevant, a missing section of the world height is
 *       synthesized empty with a warning, a section at {@code sMin - 1} or {@code sMax + 1} is a
 *       {@link LightOnlySection} (R31), any other {@code Y} is ignored with a warning. {@code
 *       block_states} and {@code biomes} are paletted containers (R6 to R11); a section without
 *       {@code block_states} is all air and without {@code biomes} all {@code minecraft:plains}
 *       (R22). A missing {@code SkyLight} stays missing (R30).
 *   <li>{@code yPos} is informative only (R33). {@code Heightmaps}: see {@link
 *       dev.shoko.minecraft.world.heightmap.Heightmaps#fromNbt}. {@code PostProcessing}: R24.
 *   <li>{@code block_entities}: an entry outside the chunk is ignored with a warning (R26).
 *   <li>{@code entities} is read only for a proto-chunk (R21).
 *   <li>Optional fields of another tag type than R20 say are treated as absent, with a warning
 *       (R35).
 * </ul>
 *
 * <p>Every finding that does not change the issue is a warning of the result, with its code.
 *
 * <p>Writing produces R20 to R26, R31 to R34: the field order is not significant, the section list
 * is complete and sorted bottom to top with the light sections read, {@code yPos} is {@code minY >>
 * 4}, palettes are compacted (R7), {@code entities} only for a proto-chunk, heightmaps only for the
 * valid types (the {@code Heightmaps} key is always written), {@code References} for structure
 * references, and absent sky light stays absent. Only the keys the engine can produce are written
 * (R21). The decoded tree, not the bytes, is what is compared with the reference.
 */
public final class ChunkCodec {

  /** Data version of the reference game 26.3, read from its {@code version.json} (CF-R20). */
  public static final int SUPPORTED_DATA_VERSION = 5023;

  private ChunkCodec() {}

  /**
   * Creates a codec. Findings of a read go to the warning list of its result, not to the codec.
   *
   * @param blocks the blocks and states that palette entries resolve against
   * @param biomes the known biomes
   * @return the codec
   */
  public static ChunkCodec of(BlockStateRegistry blocks, BiomeCatalog biomes) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a chunk.
   *
   * @param root the root Compound, after decompression
   * @param height vertical extent of the dimension the chunk belongs to
   * @return present with the chunk and its warnings; absent (POSITION_MISMATCH) when {@code xPos}
   *     or {@code zPos} is missing or mistyped; refused with a cause and a message that names the
   *     faulty field or section (a path such as {@code sections[3].block_states.data})
   */
  public ReadResult<Chunk> decode(CompoundTag root, WorldHeight height) {
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

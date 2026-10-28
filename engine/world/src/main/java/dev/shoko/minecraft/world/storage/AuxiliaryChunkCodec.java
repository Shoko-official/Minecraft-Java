package dev.shoko.minecraft.world.storage;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.world.chunk.ChunkFormatException;

/**
 * Converts the roots of {@code entities/} and {@code poi/} chunks to and from NBT (AR-R22, AR-R23,
 * provisional). Unknown keys are ignored when reading; only the listed keys are written.
 */
public final class AuxiliaryChunkCodec {

  private AuxiliaryChunkCodec() {}

  /**
   * Reads the root of an {@code entities/} chunk.
   *
   * @param root the root Compound, after decompression
   * @return the entities chunk
   * @throws ChunkFormatException if {@code DataVersion}, {@code Position} (two ints) or {@code
   *     Entities} is missing or mistyped
   */
  public static EntitiesChunk decodeEntities(CompoundTag root) throws ChunkFormatException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes the root of an {@code entities/} chunk.
   *
   * @param chunk the entities chunk
   * @return the root Compound
   */
  public static CompoundTag encodeEntities(EntitiesChunk chunk) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads the root of a {@code poi/} chunk.
   *
   * @param root the root Compound, after decompression
   * @return the points of interest
   * @throws ChunkFormatException if {@code DataVersion} or {@code Sections} is missing or mistyped,
   *     a section key is not a decimal integer, or a record is malformed
   */
  public static PoiChunk decodePoi(CompoundTag root) throws ChunkFormatException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes the root of a {@code poi/} chunk.
   *
   * @param chunk the points of interest
   * @return the root Compound
   */
  public static CompoundTag encodePoi(PoiChunk chunk) {
    return Skeleton.throwNotImplemented();
  }
}

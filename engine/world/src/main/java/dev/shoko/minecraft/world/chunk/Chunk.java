package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.heightmap.Heightmaps;
import java.util.List;

/**
 * A column of sections at a chunk position (CF-R2, CF-R3, CF-R20, CF-R22). The sections cover
 * exactly the world height, bottom to top, even when empty: 24 in the Overworld (section y -4 to
 * 19), 16 in the Nether and the End. Mutable, confined to the thread that ticks the world.
 *
 * <p>Methods taking a {@link BlockPos} expect an absolute block position inside this chunk and
 * inside the world height, and throw {@link IllegalArgumentException} otherwise.
 */
public final class Chunk {

  private Chunk() {}

  /**
   * Creates a chunk.
   *
   * @param header position, height, status, times
   * @param sections one section per section y of the height, bottom to top
   * @param heightmaps the heightmaps held
   * @param attachments the opaque data
   * @return the chunk
   * @throws IllegalArgumentException if the sections do not cover the height exactly in order
   */
  public static Chunk of(
      ChunkHeader header,
      List<ChunkSection> sections,
      Heightmaps heightmaps,
      ChunkAttachments attachments) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the chunk position.
   *
   * @return the position
   */
  public ChunkPos pos() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the vertical extent.
   *
   * @return the height
   */
  public WorldHeight height() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the generation status.
   *
   * @return the status
   */
  public ChunkStatus status() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Sets the generation status.
   *
   * @param status the new status
   */
  public void setStatus(ChunkStatus status) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Returns the game tick of the last save.
   *
   * @return {@code LastUpdate}
   */
  public long lastUpdate() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Sets the game tick of the last save; the caller supplies it, nothing reads a clock.
   *
   * @param lastUpdate game tick
   */
  public void setLastUpdate(long lastUpdate) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Returns the ticks of player presence.
   *
   * @return {@code InhabitedTime}
   */
  public long inhabitedTime() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Sets the ticks of player presence.
   *
   * @param inhabitedTime ticks
   */
  public void setInhabitedTime(long inhabitedTime) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Returns the sections, bottom to top.
   *
   * @return an unmodifiable list of the live sections
   */
  public List<ChunkSection> sections() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the section with a section y, at list index {@code sectionY - (minY >> 4)} (CF-R3).
   *
   * @param sectionY section y
   * @return the section
   * @throws IllegalArgumentException if outside the world height
   */
  public ChunkSection section(int sectionY) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the heightmaps.
   *
   * @return the live holder
   */
  public Heightmaps heightmaps() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the opaque data.
   *
   * @return the attachments
   */
  public ChunkAttachments attachments() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Replaces the opaque data.
   *
   * @param attachments the new attachments
   */
  public void setAttachments(ChunkAttachments attachments) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Reads the state of a block (CF-R3, CF-R4).
   *
   * @param pos absolute position
   * @return the state
   */
  public BlockState blockState(BlockPos pos) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes the state of a block.
   *
   * @param pos absolute position
   * @param state the new state
   * @return the previous state
   */
  public BlockState setBlockState(BlockPos pos, BlockState state) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads the biome of the cell holding a block (CF-R5).
   *
   * @param pos absolute position
   * @return the biome identifier
   */
  public String biome(BlockPos pos) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes the biome of the cell holding a block.
   *
   * @param pos absolute position
   * @param biome the biome identifier
   * @return the previous biome identifier
   */
  public String setBiome(BlockPos pos, String biome) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the sky light of a section as the reader must interpret it (CF-R15, provisional): the
   * stored array if present, else the bottom layer (16 x 16 values, {@code y = 0}) of the sky light
   * of the section above repeated 16 times, else 15 everywhere when there is no section above or it
   * has no sky light either. The result is computed, never stored.
   *
   * @param sectionY section y inside the world height
   * @return a new array
   * @throws IllegalArgumentException if outside the world height
   */
  public NibbleArray effectiveSkyLight(int sectionY) {
    return Skeleton.throwNotImplemented();
  }
}

package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.LocalIndex;
import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.heightmap.Heightmaps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A column of sections at a chunk position (CF-R2, CF-R3, CF-R20, CF-R22). The sections cover
 * exactly the world height, bottom to top, even when empty: 24 in the Overworld (section y -4 to
 * 19), 16 in the Nether and the End. Mutable, confined to the thread that ticks the world.
 *
 * <p>Methods taking a {@link BlockPos} expect an absolute block position inside this chunk and
 * inside the world height, and throw {@link IllegalArgumentException} otherwise.
 */
public final class Chunk {

  private static final int FULL_SKY_LIGHT = 15;
  // One layer of 16 x 16 values at two per byte.
  private static final int LAYER_BYTES = 128;

  private final ChunkPos pos;
  private final WorldHeight height;
  private final List<ChunkSection> sections;
  private final Heightmaps heightmaps;
  private ChunkStatus status;
  private long lastUpdate;
  private long inhabitedTime;
  private ChunkAttachments attachments;

  private Chunk(
      ChunkHeader header,
      List<ChunkSection> sections,
      Heightmaps heightmaps,
      ChunkAttachments attachments) {
    this.pos = header.pos();
    this.height = header.height();
    this.status = header.status();
    this.lastUpdate = header.lastUpdate();
    this.inhabitedTime = header.inhabitedTime();
    this.sections = sections;
    this.heightmaps = heightmaps;
    this.attachments = attachments;
  }

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
    WorldHeight height = header.height();
    if (sections.size() != height.sectionCount()) {
      throw new IllegalArgumentException(
          sections.size() + " sections given, the height needs " + height.sectionCount());
    }
    for (int rank = 0; rank < sections.size(); rank++) {
      int expected = height.minSectionY() + rank;
      if (sections.get(rank).y() != expected) {
        throw new IllegalArgumentException(
            "section at rank "
                + rank
                + " has y "
                + sections.get(rank).y()
                + ", expected "
                + expected);
      }
    }
    return new Chunk(
        header,
        Collections.unmodifiableList(new ArrayList<>(sections)),
        Objects.requireNonNull(heightmaps, "heightmaps"),
        Objects.requireNonNull(attachments, "attachments"));
  }

  /**
   * Returns the chunk position.
   *
   * @return the position
   */
  public ChunkPos pos() {
    return pos;
  }

  /**
   * Returns the vertical extent.
   *
   * @return the height
   */
  public WorldHeight height() {
    return height;
  }

  /**
   * Returns the generation status.
   *
   * @return the status
   */
  public ChunkStatus status() {
    return status;
  }

  /**
   * Sets the generation status.
   *
   * @param status the new status
   */
  public void setStatus(ChunkStatus status) {
    this.status = Objects.requireNonNull(status, "status");
  }

  /**
   * Returns the game tick of the last save.
   *
   * @return {@code LastUpdate}
   */
  public long lastUpdate() {
    return lastUpdate;
  }

  /**
   * Sets the game tick of the last save; the caller supplies it, nothing reads a clock.
   *
   * @param lastUpdate game tick
   */
  public void setLastUpdate(long lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  /**
   * Returns the ticks of player presence.
   *
   * @return {@code InhabitedTime}
   */
  public long inhabitedTime() {
    return inhabitedTime;
  }

  /**
   * Sets the ticks of player presence.
   *
   * @param inhabitedTime ticks
   */
  public void setInhabitedTime(long inhabitedTime) {
    this.inhabitedTime = inhabitedTime;
  }

  /**
   * Returns the sections, bottom to top.
   *
   * @return an unmodifiable list of the live sections
   */
  public List<ChunkSection> sections() {
    return sections;
  }

  /**
   * Returns the section with a section y, at list index {@code sectionY - (minY >> 4)} (CF-R3).
   *
   * @param sectionY section y
   * @return the section
   * @throws IllegalArgumentException if outside the world height
   */
  public ChunkSection section(int sectionY) {
    if (sectionY < height.minSectionY() || sectionY > height.maxSectionY()) {
      throw new IllegalArgumentException("section y " + sectionY + " is outside the world height");
    }
    return sections.get(height.sectionIndex(sectionY));
  }

  /**
   * Returns the heightmaps.
   *
   * @return the live holder
   */
  public Heightmaps heightmaps() {
    return heightmaps;
  }

  /**
   * Returns the opaque data.
   *
   * @return the attachments
   */
  public ChunkAttachments attachments() {
    return attachments;
  }

  /**
   * Replaces the opaque data.
   *
   * @param attachments the new attachments
   */
  public void setAttachments(ChunkAttachments attachments) {
    this.attachments = Objects.requireNonNull(attachments, "attachments");
  }

  /**
   * Reads the state of a block (CF-R3, CF-R4).
   *
   * @param pos absolute position
   * @return the state
   */
  public BlockState blockState(BlockPos pos) {
    return sectionOf(pos)
        .blockStates()
        .get(LocalIndex.sectionStateIndex(pos.x(), pos.y(), pos.z()));
  }

  /**
   * Writes the state of a block.
   *
   * @param pos absolute position
   * @param state the new state
   * @return the previous state
   */
  public BlockState setBlockState(BlockPos pos, BlockState state) {
    return sectionOf(pos)
        .blockStates()
        .set(LocalIndex.sectionStateIndex(pos.x(), pos.y(), pos.z()), state);
  }

  /**
   * Reads the biome of the cell holding a block (CF-R5).
   *
   * @param pos absolute position
   * @return the biome identifier
   */
  public String biome(BlockPos pos) {
    return sectionOf(pos).biomes().get(ChunkSection.biomeIndex(pos.x(), pos.y(), pos.z()));
  }

  /**
   * Writes the biome of the cell holding a block.
   *
   * @param pos absolute position
   * @param biome the biome identifier
   * @return the previous biome identifier
   */
  public String setBiome(BlockPos pos, String biome) {
    return sectionOf(pos).biomes().set(ChunkSection.biomeIndex(pos.x(), pos.y(), pos.z()), biome);
  }

  /**
   * Returns the sky light of a section as the reader must interpret it (CF-R30, provisional): a
   * view computed on demand from the current state of the chunk, with no side effect. If the
   * section has a stored {@code SkyLight}, that array. Otherwise the effective sky light of the
   * section at {@code sectionY + 1} when it exists in the chunk (the light section above the world
   * height included, CF-R31), of which the bottom layer (indices 0 to 255, {@code ly = 0}) is
   * repeated 16 times, once per {@code ly}. Otherwise 15 everywhere (2048 bytes {@code ff}). The
   * derivation is recursive upwards and reflects later changes of the sections above. Nothing is
   * stored: neither reading nor writing a chunk materializes a derived array.
   *
   * @param sectionY section y inside the world height
   * @return a new array
   * @throws IllegalArgumentException if outside the world height
   */
  public NibbleArray effectiveSkyLight(int sectionY) {
    section(sectionY);
    for (int y = sectionY; y <= height.maxSectionY() + 1; y++) {
      Optional<NibbleArray> stored = storedSkyLight(y);
      if (stored.isPresent()) {
        return y == sectionY ? stored.get().copy() : bottomLayerRepeated(stored.get());
      }
    }
    return NibbleArray.filled(FULL_SKY_LIGHT);
  }

  private Optional<NibbleArray> storedSkyLight(int sectionY) {
    if (sectionY <= height.maxSectionY()) {
      return section(sectionY).light().skyLight();
    }
    LightOnlySection above = attachments.lightOnlySections().get(sectionY);
    return above == null ? Optional.empty() : above.light().skyLight();
  }

  private static NibbleArray bottomLayerRepeated(NibbleArray source) {
    byte[] bytes = source.toBytes();
    byte[] repeated = new byte[NibbleArray.BYTE_LENGTH];
    for (int offset = 0; offset < repeated.length; offset += LAYER_BYTES) {
      System.arraycopy(bytes, 0, repeated, offset, LAYER_BYTES);
    }
    return NibbleArray.ofBytes(repeated);
  }

  private ChunkSection sectionOf(BlockPos block) {
    if (!block.chunk().equals(pos)) {
      throw new IllegalArgumentException(block + " is outside chunk " + pos);
    }
    if (!height.contains(block.y())) {
      throw new IllegalArgumentException(block + " is outside the world height");
    }
    return section(block.y() >> 4);
  }
}

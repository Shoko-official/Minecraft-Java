package dev.shoko.minecraft.world.heightmap;

import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import java.util.Optional;

/**
 * The heightmaps a chunk carries, by type (CF-R16, CF-R19). Mutable, confined to the thread that
 * ticks the world.
 *
 * <p>A type that is not held needs to be recomputed (milestone M5): it was absent from the chunk
 * file or had a wrong length. Nothing is invented for it and it is not written back.
 */
public final class Heightmaps {

  private Heightmaps() {}

  /**
   * Creates a holder with no heightmap.
   *
   * @return the holder
   */
  public static Heightmaps empty() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads the {@code Heightmaps} Compound of a chunk (CF-R16, CF-R19). Unknown keys and values of
   * another tag type or length are skipped, not errors.
   *
   * @param compound the Compound, possibly empty
   * @param height the vertical extent of the dimension
   * @return the heightmaps that could be read
   */
  public static Heightmaps fromNbt(CompoundTag compound, WorldHeight height) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the heightmap of a type.
   *
   * @param type the type
   * @return the heightmap, or empty when it needs recomputing
   */
  public Optional<Heightmap> get(HeightmapType type) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Holds the heightmap of a type, replacing any previous one.
   *
   * @param type the type
   * @param heightmap the heightmap
   */
  public void put(HeightmapType type, Heightmap heightmap) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Forgets the heightmap of a type, which then needs recomputing.
   *
   * @param type the type
   */
  public void remove(HeightmapType type) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Returns the {@code Heightmaps} Compound to write: the held types only, in the declaration order
   * of {@link HeightmapType}.
   *
   * @return the Compound
   */
  public CompoundTag toNbt() {
    return Skeleton.throwNotImplemented();
  }
}

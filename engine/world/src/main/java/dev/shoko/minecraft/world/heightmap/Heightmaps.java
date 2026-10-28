package dev.shoko.minecraft.world.heightmap;

import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.world.WarningSink;
import java.util.Optional;
import java.util.Set;

/**
 * The heightmaps a chunk carries, by type (CF-R16, CF-R19, CF-R34). Mutable, confined to the thread
 * that ticks the world.
 *
 * <p>Each of the six types is either VALID (256 values in {@code [0, height]}) or UNKNOWN, meaning
 * "to recompute" (milestone M5): the type was absent from the chunk file, was not a Long_Array, had
 * a length other than {@link Heightmap#longCount}, or held a value above {@code height}. Nothing is
 * invented for an unknown type, never an array of zeros, and it is not written back. A caller
 * learns of it by three channels: {@link #get} returns empty, {@link #unknownTypes()} lists it, and
 * {@link #fromNbt} warned when the type was present but invalid. Putting a heightmap makes the type
 * valid.
 */
public final class Heightmaps {

  private Heightmaps() {}

  /**
   * Creates a holder with no heightmap: the six types are unknown.
   *
   * @return the holder
   */
  public static Heightmaps empty() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads the {@code Heightmaps} Compound of a chunk (CF-R16, CF-R19, CF-R34). A key that is not
   * one of the six types is ignored and not written back. A type present but invalid (not a
   * Long_Array, wrong length, a value above {@code height}) is unknown and sends one {@link
   * dev.shoko.minecraft.world.WarningCode#HEIGHTMAP_INVALID} warning; a type simply absent is
   * unknown without a warning. Neither is an error.
   *
   * @param compound the Compound, possibly empty
   * @param height the vertical extent of the dimension
   * @param warnings where the findings go
   * @return the heightmaps that could be read
   */
  public static Heightmaps fromNbt(CompoundTag compound, WorldHeight height, WarningSink warnings) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the heightmap of a type.
   *
   * @param type the type
   * @return the heightmap, or empty when the type is unknown (to recompute); never a heightmap of
   *     zeros and never an exception for that case
   */
  public Optional<Heightmap> get(HeightmapType type) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the types whose heightmap is unknown (CF-R34).
   *
   * @return an unmodifiable set, in the declaration order of {@link HeightmapType}
   */
  public Set<HeightmapType> unknownTypes() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Holds the heightmap of a type, replacing any previous one; the type becomes valid.
   *
   * @param type the type
   * @param heightmap the heightmap
   */
  public void put(HeightmapType type, Heightmap heightmap) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Forgets the heightmap of a type, which becomes unknown.
   *
   * @param type the type
   */
  public void remove(HeightmapType type) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Returns the {@code Heightmaps} Compound to write: the valid types only, in the declaration
   * order of {@link HeightmapType}. An unknown type is lost, which is accepted: it can be derived
   * again (CF-R34).
   *
   * @return the Compound, empty when no type is valid
   */
  public CompoundTag toNbt() {
    return Skeleton.throwNotImplemented();
  }
}

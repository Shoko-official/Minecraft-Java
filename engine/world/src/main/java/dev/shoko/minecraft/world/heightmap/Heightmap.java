package dev.shoko.minecraft.world.heightmap;

import dev.shoko.minecraft.core.coord.WorldHeight;
import java.util.Optional;

/**
 * One value per column of a chunk, 256 in all (CF-R17, CF-R18).
 *
 * <p>The stored value of a column is (provisional) {@code (y of the highest retained block + 1) -
 * minY}, 0 when no block of the column is retained, so in {@code [0, height]}: 0 to 384 in the
 * Overworld. Columns are indexed by {@code lz * 16 + lx}. On disk the values are packed with {@code
 * ceil(log2(height + 1))} bits (9 for every 26.3 dimension, 37 longs) using the packing of {@link
 * dev.shoko.minecraft.world.palette.BitPacking}. Mutable, confined to the thread that ticks the
 * world.
 */
public final class Heightmap {

  private Heightmap() {}

  /**
   * Creates a heightmap with the same value in every column.
   *
   * @param height the vertical extent of the dimension
   * @param value stored value in {@code [0, height.height()]}
   * @return the heightmap
   * @throws IllegalArgumentException if the value is out of range
   */
  public static Heightmap filled(WorldHeight height, int value) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a heightmap from its packed form (CF-R17, CF-R34).
   *
   * @param data the longs of the Long_Array
   * @param height the vertical extent of the dimension
   * @return the heightmap, or empty when {@code data.length} is not {@link #longCount(WorldHeight)}
   *     (36 is the old continuous packing and is invalid in the Overworld) or a decoded value
   *     exceeds {@code height.height()} (385 to 511 fit in 9 bits); the type is then unknown,
   *     recomputed later and never invented
   */
  public static Optional<Heightmap> fromLongs(long[] data, WorldHeight height) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the bits per column, {@code ceil(log2(height + 1))} (CF-R17).
   *
   * @param height the vertical extent of the dimension
   * @return the bits, 9 for heights 256 and 384
   */
  public static int bitsPerColumn(WorldHeight height) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of longs of the packed form (CF-R17), {@code ceil(256 / floor(64 / b))} with
   * {@code b} from {@link #bitsPerColumn(WorldHeight)}; not a constant, a dimension of another
   * height has another length.
   *
   * @param height the vertical extent of the dimension
   * @return the length, 37 for heights 256 and 384
   */
  public static int longCount(WorldHeight height) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the stored value of a column.
   *
   * @param localX x in the chunk, 0 to 15
   * @param localZ z in the chunk, 0 to 15
   * @return the value in {@code [0, height]}
   * @throws IndexOutOfBoundsException if a coordinate is out of range
   */
  public int get(int localX, int localZ) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Sets the stored value of a column.
   *
   * @param localX x in the chunk, 0 to 15
   * @param localZ z in the chunk, 0 to 15
   * @param value value in {@code [0, height]}
   * @throws IndexOutOfBoundsException if a coordinate is out of range
   * @throws IllegalArgumentException if the value is out of range
   */
  public void set(int localX, int localZ, int value) {
    Skeleton.throwNotImplemented();
  }

  /**
   * Returns the packed form (CF-R17).
   *
   * @return a new array of {@link #longCount(WorldHeight)} longs
   */
  public long[] toLongs() {
    return Skeleton.throwNotImplemented();
  }
}

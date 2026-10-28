package dev.shoko.minecraft.core.coord;

/**
 * Vertical extent of a dimension: its lowest block and its height (R25).
 *
 * <p>Values come from the {@code min_y} and {@code height} fields of the dimension type data, not
 * from code: the Overworld has {@code minY = -64}, {@code height = 384} (blocks -64 to 319,
 * sections -4 to 19); the Nether and the End have 0 and 256.
 *
 * @param minY lowest block y
 * @param height number of block layers
 */
public record WorldHeight(int minY, int height) {

  /**
   * Returns the highest block y, {@code minY + height - 1} (R25).
   *
   * @return the highest block y
   */
  public int maxY() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether {@code minY <= y <= maxY()} (R25).
   *
   * @param y block y
   * @return {@code true} inside the dimension
   */
  public boolean contains(int y) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the lowest section y, {@code minY >> 4} (R25).
   *
   * @return the lowest section y
   */
  public int minSectionY() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the highest section y, {@code maxY() >> 4} (R25).
   *
   * @return the highest section y
   */
  public int maxSectionY() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of sections, {@code maxSectionY() - minSectionY() + 1} (R25).
   *
   * @return the section count, 24 for the Overworld
   */
  public int sectionCount() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the index of a section in the chunk's section list, {@code sectionY - (minY >> 4)}
   * (R25). Not checked against the bounds.
   *
   * @param sectionY section y
   * @return the list index, 0 for the lowest section
   */
  public int sectionIndex(int sectionY) {
    return Skeleton.throwNotImplemented();
  }
}

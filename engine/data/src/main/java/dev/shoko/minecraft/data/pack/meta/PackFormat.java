package dev.shoko.minecraft.data.pack.meta;

/**
 * A pack format version: a major and a minor number (R5), compared major first, then minor.
 *
 * @param major the major number
 * @param minor the minor number
 */
public record PackFormat(int major, int minor) implements Comparable<PackFormat> {

  /**
   * Orders by major then minor number, as signed integers (R5).
   *
   * @param other the version to compare with
   * @return a negative, zero or positive number
   */
  @Override
  public int compareTo(PackFormat other) {
    return Skeleton.throwNotImplemented();
  }
}

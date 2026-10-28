package dev.shoko.minecraft.data.pack.meta;

/** The two kinds of pack, each with the format version the target game expects (R1). */
public enum PackType {
  /** A datapack, the {@code data/} tree: format 121.0 (R1). */
  DATA,
  /** A resource pack, the {@code assets/} tree: format 97.1 (R1, R18). */
  RESOURCE;

  // spec: data-datapack-loading R1
  private static final PackFormat DATA_FORMAT = new PackFormat(121, 0);
  private static final PackFormat RESOURCE_FORMAT = new PackFormat(97, 1);

  /**
   * Returns the format version the target game expects for this kind of pack.
   *
   * @return 121.0 for {@link #DATA}, 97.1 for {@link #RESOURCE}
   */
  public PackFormat targetFormat() {
    return this == DATA ? DATA_FORMAT : RESOURCE_FORMAT;
  }
}

package dev.shoko.minecraft.data.pack;

import dev.shoko.minecraft.data.diag.FileOrigin;
import java.io.IOException;

/** A file seen through a {@link PackStack}: where it lives, and the path it has once stacked. */
public final class PackFileRef {

  private PackFileRef() {}

  /**
   * Returns the pack that holds the file.
   *
   * @return the pack identifier
   */
  public String pack() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the path of the file in the stack: for a file of an overlay directory, the path without
   * the directory (R48).
   *
   * @return the path, such as {@code data/minecraft/loot_table/blocks/dirt.json}
   */
  public String path() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the place of the file in its pack, for diagnostics.
   *
   * @return the pack and the physical path, overlay directory included
   */
  public FileOrigin origin() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads the file.
   *
   * @return the bytes
   * @throws IOException when the file cannot be read
   */
  public byte[] read() throws IOException {
    return Skeleton.throwNotImplemented();
  }
}

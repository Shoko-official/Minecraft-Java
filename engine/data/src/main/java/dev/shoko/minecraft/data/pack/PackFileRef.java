package dev.shoko.minecraft.data.pack;

import dev.shoko.minecraft.data.diag.FileOrigin;
import java.io.IOException;

/** A file seen through a {@link PackStack}: where it lives, and the path it has once stacked. */
public final class PackFileRef {

  private final String pack;
  private final String path;
  private final String physicalPath;
  private final PackFiles files;

  PackFileRef(String pack, String path, String physicalPath, PackFiles files) {
    this.pack = pack;
    this.path = path;
    this.physicalPath = physicalPath;
    this.files = files;
  }

  /**
   * Returns the pack that holds the file.
   *
   * @return the pack identifier
   */
  public String pack() {
    return pack;
  }

  /**
   * Returns the path of the file in the stack: for a file of an overlay directory, the path without
   * the directory (R48).
   *
   * @return the path, such as {@code data/minecraft/loot_table/blocks/dirt.json}
   */
  public String path() {
    return path;
  }

  /**
   * Returns the place of the file in its pack, for diagnostics.
   *
   * @return the pack and the physical path, overlay directory included
   */
  public FileOrigin origin() {
    return new FileOrigin(pack, physicalPath);
  }

  /**
   * Reads the file.
   *
   * @return the bytes
   * @throws IOException when the file cannot be read
   */
  public byte[] read() throws IOException {
    return files.read(physicalPath);
  }
}

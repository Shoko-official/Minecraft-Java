package dev.shoko.minecraft.data.pack;

import java.io.IOException;
import java.util.List;

/** The files below one directory of another set, seen as a root of their own. */
final class SubtreeFiles extends BaseFiles {

  private final PackFiles parent;
  private final String prefix;

  SubtreeFiles(PackFiles parent, String directory) {
    this.parent = parent;
    this.prefix = directory + "/";
  }

  @Override
  public List<String> paths() throws IOException {
    // The parent order is by code point, and a common prefix does not change it.
    return parent.paths().stream()
        .filter(path -> path.startsWith(prefix))
        .map(path -> path.substring(prefix.length()))
        .toList();
  }

  @Override
  public byte[] read(String path) throws IOException {
    return parent.read(prefix + path);
  }
}

package dev.shoko.minecraft.data.pack;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;

/** Every file of another set except the ones below one directory. */
final class WithoutFiles extends BaseFiles {

  private final PackFiles parent;
  private final String prefix;

  WithoutFiles(PackFiles parent, String directory) {
    this.parent = parent;
    this.prefix = directory + "/";
  }

  @Override
  public List<String> paths() throws IOException {
    return parent.paths().stream().filter(path -> !path.startsWith(prefix)).toList();
  }

  @Override
  public byte[] read(String path) throws IOException {
    if (path.startsWith(prefix)) {
      throw new NoSuchFileException(path);
    }
    return parent.read(path);
  }
}

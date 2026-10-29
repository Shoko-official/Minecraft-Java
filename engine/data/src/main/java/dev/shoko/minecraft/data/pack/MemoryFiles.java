package dev.shoko.minecraft.data.pack;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Files held in memory. */
final class MemoryFiles extends BaseFiles {

  private final TreeMap<String, byte[]> files = new TreeMap<>(PathOrder.BY_CODE_POINT);

  MemoryFiles(Map<String, byte[]> source) {
    source.forEach((path, content) -> files.put(path, content.clone()));
  }

  @Override
  public List<String> paths() {
    return new ArrayList<>(files.keySet());
  }

  @Override
  public byte[] read(String path) throws IOException {
    byte[] content = files.get(path);
    if (content == null) {
      throw new NoSuchFileException(path);
    }
    if (content.length > PackFiles.MAX_FILE_BYTES) {
      throw BoundedRead.tooLarge(path);
    }
    return content.clone();
  }
}

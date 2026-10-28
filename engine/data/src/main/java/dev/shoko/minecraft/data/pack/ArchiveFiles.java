package dev.shoko.minecraft.data.pack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** The files of a {@code .zip} archive, whose tree starts at the root of the archive (R19). */
final class ArchiveFiles extends BaseFiles {

  private final Path archive;
  private List<String> listing;

  ArchiveFiles(Path archive) {
    this.archive = archive;
  }

  @Override
  public synchronized List<String> paths() throws IOException {
    if (listing == null) {
      TreeSet<String> found = new TreeSet<>(PathOrder.BY_CODE_POINT);
      try (ZipFile zip = new ZipFile(archive.toFile())) {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          if (!entry.isDirectory() && PathOrder.isPlainRelative(entry.getName())) {
            found.add(entry.getName());
          }
        }
      }
      listing = new ArrayList<>(found);
    }
    return new ArrayList<>(listing);
  }

  @Override
  public byte[] read(String path) throws IOException {
    try (ZipFile zip = new ZipFile(archive.toFile())) {
      ZipEntry entry = zip.getEntry(path);
      if (entry == null || entry.isDirectory() || !PathOrder.isPlainRelative(path)) {
        throw new NoSuchFileException(path);
      }
      try (InputStream in = zip.getInputStream(entry)) {
        return in.readAllBytes();
      }
    }
  }
}

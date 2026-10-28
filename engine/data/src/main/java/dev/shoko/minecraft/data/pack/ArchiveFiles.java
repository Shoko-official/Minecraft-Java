package dev.shoko.minecraft.data.pack;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * The files of a {@code .zip} archive, whose tree starts at the root of the archive (R19). An
 * archive entry is read with a bounded buffer, whatever size it declares; names that are not plain
 * relative paths are not listed and cannot be read. Of several entries with the same name the last
 * one is read, and the name is listed once.
 */
final class ArchiveFiles extends BaseFiles {

  private final Path archive;
  private List<String> listing;
  // The archive kept open by keepOpen, null otherwise.
  private ZipFile shared;
  private int sessions;

  ArchiveFiles(Path archive) {
    this.archive = archive;
  }

  @Override
  public synchronized List<String> paths() throws IOException {
    if (listing == null) {
      if (shared != null) {
        listing = list(shared);
      } else {
        try (ZipFile opened = new ZipFile(archive.toFile())) {
          listing = list(opened);
        }
      }
    }
    return new ArrayList<>(listing);
  }

  @Override
  public byte[] read(String path) throws IOException {
    if (!PathOrder.isPlainRelative(path)) {
      throw new NoSuchFileException(path);
    }
    ZipFile zip;
    synchronized (this) {
      zip = shared;
    }
    if (zip != null) {
      return readEntry(zip, path);
    }
    try (ZipFile opened = new ZipFile(archive.toFile())) {
      return readEntry(opened, path);
    }
  }

  @Override
  public synchronized Closeable keepOpen() throws IOException {
    if (sessions == 0) {
      shared = new ZipFile(archive.toFile());
    }
    sessions++;
    return this::endSession;
  }

  private synchronized void endSession() throws IOException {
    sessions--;
    if (sessions == 0) {
      ZipFile closing = shared;
      shared = null;
      closing.close();
    }
  }

  private static List<String> list(ZipFile zip) {
    TreeSet<String> found = new TreeSet<>(PathOrder.BY_CODE_POINT);
    Enumeration<? extends ZipEntry> entries = zip.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      if (!entry.isDirectory() && PathOrder.isPlainRelative(entry.getName())) {
        found.add(entry.getName());
      }
    }
    return new ArrayList<>(found);
  }

  private static byte[] readEntry(ZipFile zip, String path) throws IOException {
    ZipEntry entry = zip.getEntry(path);
    if (entry == null || entry.isDirectory()) {
      throw new NoSuchFileException(path);
    }
    if (entry.getSize() > PackFiles.MAX_FILE_BYTES) {
      throw BoundedRead.tooLarge(path);
    }
    // The declared size is not trusted: the stream is read up to the ceiling only.
    try (InputStream in = zip.getInputStream(entry)) {
      return BoundedRead.readAll(in, path);
    } catch (ZipException | EOFException e) {
      throw new PackContentException("damaged archive entry " + path + ": " + e.getMessage(), e);
    }
  }
}

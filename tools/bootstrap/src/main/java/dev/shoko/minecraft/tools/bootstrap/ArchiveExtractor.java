package dev.shoko.minecraft.tools.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Extracts selected entries of a zip stream, refusing entries that would leave the target. */
final class ArchiveExtractor {

  /**
   * Bounds on the bytes written, against decompression bombs. They are enforced while copying, so
   * an oversized entry is never written in full.
   */
  record Limits(long maxEntryBytes, long maxTotalBytes) {

    /** Far above the largest resource of the reference server (a few megabytes). */
    static final Limits DEFAULT = new Limits(256L << 20, 4L << 30);
  }

  private ArchiveExtractor() {}

  /**
   * Writes the entries whose name is accepted under {@code targetDir}.
   *
   * @return the number of files written
   * @throws IOException when any entry, accepted or not, resolves outside {@code targetDir}, or
   *     when the limits are exceeded
   */
  static int extract(InputStream zipStream, Path targetDir, Predicate<String> accept, Limits limits)
      throws IOException {
    Path root = targetDir.toAbsolutePath().normalize();
    int written = 0;
    long total = 0;
    try (ZipInputStream zip = new ZipInputStream(zipStream)) {
      for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
        Path out = resolve(root, entry.getName());
        if (entry.isDirectory() || !accept.test(entry.getName())) {
          continue;
        }
        Files.createDirectories(out.getParent());
        try (OutputStream sink = Files.newOutputStream(out)) {
          total = copy(zip, sink, entry.getName(), total, limits);
        }
        written++;
      }
    }
    return written;
  }

  private static Path resolve(Path root, String name) throws IOException {
    try {
      Path out = root.resolve(name).normalize();
      if (out.startsWith(root)) {
        return out;
      }
    } catch (InvalidPathException e) {
      throw new IOException("Unsafe archive entry: " + name, e);
    }
    throw new IOException("Unsafe archive entry: " + name);
  }

  /** Copies one entry and returns the new running total. */
  private static long copy(InputStream in, OutputStream out, String name, long total, Limits limits)
      throws IOException {
    byte[] buffer = new byte[64 * 1024];
    long entryBytes = 0;
    long runningTotal = total;
    for (int read = in.read(buffer); read >= 0; read = in.read(buffer)) {
      entryBytes += read;
      runningTotal += read;
      if (entryBytes > limits.maxEntryBytes()) {
        throw new IOException(
            "Archive entry " + name + " expands beyond " + limits.maxEntryBytes() + " bytes");
      }
      if (runningTotal > limits.maxTotalBytes()) {
        throw new IOException("Archive expands beyond " + limits.maxTotalBytes() + " bytes");
      }
      out.write(buffer, 0, read);
    }
    return runningTotal;
  }
}

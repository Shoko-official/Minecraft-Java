package dev.shoko.minecraft.tools.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Extracts selected entries of a zip stream, refusing entries that would leave the target. */
final class ArchiveExtractor {

  /** Upper bound on the extracted size, against decompression bombs. */
  private static final long MAX_TOTAL_BYTES = 4L << 30;

  private ArchiveExtractor() {}

  /**
   * Writes the entries whose name is accepted under {@code targetDir}.
   *
   * @return the number of files written
   * @throws IOException when any entry, accepted or not, resolves outside {@code targetDir}
   */
  static int extract(InputStream zipStream, Path targetDir, Predicate<String> accept)
      throws IOException {
    Path root = targetDir.toAbsolutePath().normalize();
    int written = 0;
    long total = 0;
    try (ZipInputStream zip = new ZipInputStream(zipStream)) {
      for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
        Path out = root.resolve(entry.getName()).normalize();
        if (!out.startsWith(root)) {
          throw new IOException("Unsafe archive entry: " + entry.getName());
        }
        if (entry.isDirectory() || !accept.test(entry.getName())) {
          continue;
        }
        Files.createDirectories(out.getParent());
        try (OutputStream sink = Files.newOutputStream(out)) {
          total += zip.transferTo(sink);
        }
        if (total > MAX_TOTAL_BYTES) {
          throw new IOException("Archive expands beyond " + MAX_TOTAL_BYTES + " bytes");
        }
        written++;
      }
    }
    return written;
  }
}

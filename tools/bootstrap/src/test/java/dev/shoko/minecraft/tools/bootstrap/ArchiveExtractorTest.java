package dev.shoko.minecraft.tools.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArchiveExtractorTest {

  private static byte[] zip(Map<String, String> entries) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream out = new ZipOutputStream(bytes)) {
      for (Map.Entry<String, String> entry : entries.entrySet()) {
        out.putNextEntry(new ZipEntry(entry.getKey()));
        out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
      }
    }
    return bytes.toByteArray();
  }

  @Test
  void writesOnlyAcceptedEntries(@TempDir Path dir) throws IOException {
    Map<String, String> entries = new LinkedHashMap<>();
    entries.put("data/minecraft/a.json", "{}");
    entries.put("assets/minecraft/b.json", "{}");
    entries.put("net/minecraft/Thing.class", "code");
    entries.put("version.json", "{}");

    int written =
        ArchiveExtractor.extract(
            new ByteArrayInputStream(zip(entries)),
            dir,
            name -> !name.endsWith(".class") && name.startsWith("data/"),
            ArchiveExtractor.Limits.DEFAULT);

    assertThat(written).isEqualTo(1);
    assertThat(dir.resolve("data/minecraft/a.json")).exists();
    assertThat(dir.resolve("assets")).doesNotExist();
    assertThat(dir.resolve("net")).doesNotExist();
  }

  @Test
  void refusesAnEntryThatEscapesTheTarget(@TempDir Path dir) throws IOException {
    Path target = dir.resolve("target");
    byte[] archive = zip(Map.of("../escape.txt", "x"));

    assertThatThrownBy(
            () ->
                ArchiveExtractor.extract(
                    new ByteArrayInputStream(archive),
                    target,
                    name -> true,
                    ArchiveExtractor.Limits.DEFAULT))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Unsafe");
    assertThat(dir.resolve("escape.txt")).doesNotExist();
  }

  @Test
  void refusesAnEscapingEntryEvenWhenItWouldBeFilteredOut(@TempDir Path dir) throws IOException {
    byte[] archive = zip(Map.of("../escape.txt", "x"));

    assertThatThrownBy(
            () ->
                ArchiveExtractor.extract(
                    new ByteArrayInputStream(archive),
                    dir.resolve("t"),
                    name -> false,
                    ArchiveExtractor.Limits.DEFAULT))
        .isInstanceOf(IOException.class);
  }

  @Test
  void refusesAnEntryNameThatIsNotAValidPath(@TempDir Path dir) throws IOException {
    byte[] archive = zip(Map.of("data/bad\0name.json", "x"));

    assertThatThrownBy(
            () ->
                ArchiveExtractor.extract(
                    new ByteArrayInputStream(archive),
                    dir,
                    name -> true,
                    ArchiveExtractor.Limits.DEFAULT))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Unsafe");
  }

  /** Zeros compress to almost nothing: the archive is small, the expanded entry is not. */
  private static byte[] zipOfZeros(int entries, int bytesPerEntry) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    byte[] chunk = new byte[4096];
    try (ZipOutputStream out = new ZipOutputStream(bytes)) {
      for (int i = 0; i < entries; i++) {
        out.putNextEntry(new ZipEntry("data/zeros" + i + ".bin"));
        for (int written = 0; written < bytesPerEntry; written += chunk.length) {
          out.write(chunk, 0, Math.min(chunk.length, bytesPerEntry - written));
        }
        out.closeEntry();
      }
    }
    return bytes.toByteArray();
  }

  @Test
  void stopsAnOversizedEntryWhileCopyingIt(@TempDir Path dir) throws IOException {
    byte[] archive = zipOfZeros(1, 1 << 20);
    ArchiveExtractor.Limits limits = new ArchiveExtractor.Limits(64 * 1024, 1L << 30);

    assertThatThrownBy(
            () ->
                ArchiveExtractor.extract(new ByteArrayInputStream(archive), dir, n -> true, limits))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("zeros0.bin expands beyond 65536");
    assertThat(archive.length).isLessThan(64 * 1024);
    assertThat(Files.size(dir.resolve("data/zeros0.bin"))).isLessThanOrEqualTo(64 * 1024);
  }

  @Test
  void stopsWhenTheTotalExceedsTheLimit(@TempDir Path dir) throws IOException {
    byte[] archive = zipOfZeros(3, 40 * 1024);
    ArchiveExtractor.Limits limits = new ArchiveExtractor.Limits(64 * 1024, 100 * 1024);

    assertThatThrownBy(
            () ->
                ArchiveExtractor.extract(new ByteArrayInputStream(archive), dir, n -> true, limits))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Archive expands beyond 102400");
    assertThat(Files.size(dir.resolve("data/zeros2.bin"))).isLessThanOrEqualTo(20 * 1024);
  }

  @Test
  void acceptsEntriesUpToTheLimits(@TempDir Path dir) throws IOException {
    byte[] archive = zipOfZeros(2, 64 * 1024);
    ArchiveExtractor.Limits limits = new ArchiveExtractor.Limits(64 * 1024, 128 * 1024);

    int written =
        ArchiveExtractor.extract(new ByteArrayInputStream(archive), dir, n -> true, limits);

    assertThat(written).isEqualTo(2);
    assertThat(Files.size(dir.resolve("data/zeros1.bin"))).isEqualTo(64 * 1024);
  }
}

package dev.shoko.minecraft.tools.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
            name -> !name.endsWith(".class") && name.startsWith("data/"));

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
            () -> ArchiveExtractor.extract(new ByteArrayInputStream(archive), target, name -> true))
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
                    new ByteArrayInputStream(archive), dir.resolve("t"), name -> false))
        .isInstanceOf(IOException.class);
  }
}

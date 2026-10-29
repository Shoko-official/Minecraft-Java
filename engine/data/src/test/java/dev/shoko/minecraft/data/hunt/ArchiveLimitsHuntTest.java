package dev.shoko.minecraft.data.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.PackDiscovery;
import dev.shoko.minecraft.data.pack.PackFiles;
import dev.shoko.minecraft.data.testing.Jx;
import dev.shoko.minecraft.data.testing.MiniData;
import dev.shoko.minecraft.data.testing.Packs;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;
import org.junit.jupiter.api.io.TempDir;

// spec: data-datapack-loading R19 (archives), R7, R54: errors in the data are diagnostics, never
// exceptions. Reproduction tests of the review of engine/data: oversized files, damaged archives,
// archives with many or duplicated entries.
class ArchiveLimitsHuntTest {

  private static final int OVERSIZED_MIB = 64;
  private static final String MCMETA = "pack.mcmeta";
  private static final String BOMB = "data/sonde/loot_table/bomb.json";
  private static final String OK = "data/sonde/loot_table/ok.json";

  @TempDir Path tmp;

  private Path datapacks() throws IOException {
    return Files.createDirectory(tmp.resolve("datapacks"));
  }

  private static void writeContent(OutputStream out, String path) throws IOException {
    byte[] chunk = new byte[1 << 20];
    if (path.equals(MCMETA)) {
      Arrays.fill(chunk, (byte) 'a');
      out.write(Jx.utf8("{\"pack\":{\"description\":\""));
      for (int i = 0; i < OVERSIZED_MIB; i++) {
        out.write(chunk);
      }
      out.write(Jx.utf8("\",\"min_format\":121,\"max_format\":121}}"));
    } else {
      Arrays.fill(chunk, (byte) ' ');
      for (int i = 0; i < OVERSIZED_MIB; i++) {
        out.write(chunk);
      }
      out.write(Jx.utf8("{}"));
    }
  }

  /** A pack archive whose only oversized file is {@code oversized}; a few kibibytes on disk. */
  private static void oversizedArchive(Path archive, String oversized) throws IOException {
    try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive))) {
      out.setLevel(Deflater.BEST_COMPRESSION);
      if (!oversized.equals(MCMETA)) {
        put(out, MCMETA, Packs.MCMETA);
        put(out, OK, "{}");
      }
      out.putNextEntry(new ZipEntry(oversized));
      writeContent(out, oversized);
      out.closeEntry();
    }
  }

  private static void put(ZipOutputStream out, String name, String content) throws IOException {
    out.putNextEntry(new ZipEntry(name));
    out.write(Jx.utf8(content));
    out.closeEntry();
  }

  private static void oversizedFolder(Path folder, String oversized) throws IOException {
    if (!oversized.equals(MCMETA)) {
      Packs.writeTree(folder, Packs.tree(MCMETA, Packs.MCMETA, OK, "{}"));
    }
    Path target = folder.resolve(oversized);
    Files.createDirectories(target.getParent());
    try (OutputStream out = Files.newOutputStream(target)) {
      writeContent(out, oversized);
    }
  }

  private static List<FileOrigin> origins(Outcome<?> outcome) {
    return outcome.diagnostics().stream().map(Diagnostic::origin).toList();
  }

  private static Outcome<LoadedData> loadAll(List<Pack> packs) {
    return MiniData.load(packs.toArray(Pack[]::new));
  }

  // The size ceiling belongs to the implementation; every size here is far above any sane one.
  @Test
  void anOversizedArchiveEntryIsADiagnosticNotAnOutOfMemoryError() throws IOException {
    Path folder = datapacks();
    Path archive = folder.resolve("bomb.zip");
    oversizedArchive(archive, BOMB);
    assertThat(Files.size(archive)).as("archive size on disk").isLessThan(1 << 20);
    List<Pack> packs = PackDiscovery.worldPacks(folder).value().orElseThrow();
    assertThat(packs).hasSize(1);
    Outcome<LoadedData> loaded = loadAll(packs);
    assertThat(MiniData.elementIds(loaded.value().orElseThrow(), "minecraft:loot_table"))
        .containsExactly("sonde:ok");
    assertThat(origins(loaded)).contains(new FileOrigin("file/bomb.zip", BOMB));
  }

  @Test
  void anOversizedFolderFileIsADiagnosticNotAnOutOfMemoryError() throws IOException {
    Path folder = datapacks();
    oversizedFolder(Files.createDirectory(folder.resolve("bomb")), BOMB);
    List<Pack> packs = PackDiscovery.worldPacks(folder).value().orElseThrow();
    assertThat(packs).hasSize(1);
    Outcome<LoadedData> loaded = loadAll(packs);
    assertThat(MiniData.elementIds(loaded.value().orElseThrow(), "minecraft:loot_table"))
        .containsExactly("sonde:ok");
    assertThat(origins(loaded)).contains(new FileOrigin("file/bomb", BOMB));
  }

  @Test
  void anOversizedPackMcmetaInAnArchiveMakesTheCandidateInvalid() throws IOException {
    Path folder = datapacks();
    oversizedArchive(folder.resolve("bomb.zip"), MCMETA);
    Outcome<List<Pack>> found = PackDiscovery.worldPacks(folder);
    assertThat(found.value().orElseThrow()).isEmpty();
    assertThat(found.diagnostics())
        .extracting(Diagnostic::code, Diagnostic::origin)
        .contains(
            org.assertj.core.groups.Tuple.tuple(
                DiagnosticCode.PACK_INVALID, new FileOrigin("file/bomb.zip", MCMETA)));
  }

  @Test
  void anOversizedPackMcmetaInAFolderMakesTheCandidateInvalid() throws IOException {
    Path folder = datapacks();
    oversizedFolder(Files.createDirectory(folder.resolve("bomb")), MCMETA);
    Outcome<List<Pack>> found = PackDiscovery.worldPacks(folder);
    assertThat(found.value().orElseThrow()).isEmpty();
    assertThat(found.diagnostics())
        .extracting(Diagnostic::code, Diagnostic::origin)
        .contains(
            org.assertj.core.groups.Tuple.tuple(
                DiagnosticCode.PACK_INVALID, new FileOrigin("file/bomb", MCMETA)));
  }

  /**
   * Writes a pack whose entry {@code entryName} is stored in deflate blocks without compression,
   * then breaks the length check of the first block of that entry.
   */
  private static void damageEntry(Path archive, String entryName) throws IOException {
    byte[] bytes = Files.readAllBytes(archive);
    byte[] name = entryName.getBytes(StandardCharsets.UTF_8);
    int at = indexOf(bytes, name) + name.length;
    // Local header without extra field, then: block header, LEN (2 bytes), NLEN (2 bytes).
    bytes[at + 3] ^= 0x55;
    bytes[at + 4] ^= 0x55;
    Files.write(archive, bytes);
  }

  private static int indexOf(byte[] haystack, byte[] needle) {
    for (int i = 0; i + needle.length <= haystack.length; i++) {
      if (Arrays.equals(haystack, i, i + needle.length, needle, 0, needle.length)) {
        return i;
      }
    }
    throw new AssertionError("entry name not found in the archive");
  }

  private static void storedArchive(Path archive, Map<String, String> files) throws IOException {
    try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive))) {
      out.setLevel(Deflater.NO_COMPRESSION);
      for (Map.Entry<String, String> file : files.entrySet()) {
        put(out, file.getKey(), file.getValue());
      }
    }
  }

  // spec: R54 and the loader contract. The damaged entry sorts before the sound one on purpose.
  @Test
  void aDamagedEntryOfAnArchiveIsADiagnosticAndTheOtherFilesStillLoad() throws IOException {
    Path folder = datapacks();
    Path archive = folder.resolve("damaged.zip");
    storedArchive(
        archive,
        Packs.tree(
            MCMETA,
            Packs.MCMETA,
            "data/sonde/loot_table/bad.json",
            "{\"a\":1}",
            "data/sonde/loot_table/good.json",
            "{\"b\":2}"));
    damageEntry(archive, "data/sonde/loot_table/bad.json");
    List<Pack> packs = PackDiscovery.worldPacks(folder).value().orElseThrow();
    assertThat(packs).hasSize(1);
    Outcome<LoadedData> loaded = loadAll(packs);
    assertThat(MiniData.elementIds(loaded.value().orElseThrow(), "minecraft:loot_table"))
        .containsExactly("sonde:good");
    assertThat(origins(loaded))
        .contains(new FileOrigin("file/damaged.zip", "data/sonde/loot_table/bad.json"));
  }

  @Test
  void aDamagedPackMcmetaInAnArchiveMakesTheCandidateInvalid() throws IOException {
    Path folder = datapacks();
    Path archive = folder.resolve("damaged.zip");
    storedArchive(archive, Packs.tree(MCMETA, Packs.MCMETA));
    damageEntry(archive, MCMETA);
    Outcome<List<Pack>> found = PackDiscovery.worldPacks(folder);
    assertThat(found.value().orElseThrow()).isEmpty();
    assertThat(found.diagnostics())
        .extracting(Diagnostic::code)
        .containsExactly(DiagnosticCode.PACK_INVALID);
  }

  @Test
  void truncatedEmptyAndFakeArchivesAreReportedNotFatal() throws IOException {
    Path folder = datapacks();
    Path good = Packs.zip(tmp.resolve("good.zip"), Packs.tree(MCMETA, Packs.MCMETA));
    byte[] bytes = Files.readAllBytes(good);
    Files.write(folder.resolve("truncated.zip"), Arrays.copyOf(bytes, bytes.length - 10));
    Files.write(folder.resolve("empty.zip"), new byte[0]);
    Files.write(folder.resolve("fake.zip"), Jx.utf8("not an archive"));
    Files.write(folder.resolve("ok.zip"), bytes);
    Outcome<List<Pack>> found = PackDiscovery.worldPacks(folder);
    assertThat(found.value().orElseThrow()).extracting(Pack::id).containsExactly("file/ok.zip");
    assertThat(found.diagnostics())
        .extracting(Diagnostic::code)
        .containsExactly(
            DiagnosticCode.PACK_INVALID, DiagnosticCode.PACK_INVALID, DiagnosticCode.PACK_INVALID);
  }

  // The listing and the content of one name: the last of two entries with the same name wins, and
  // the name is listed once.
  @Test
  void duplicatedEntryNamesAreListedOnceAndTheLastEntryWins() throws IOException {
    Path archive = tmp.resolve("dup.zip");
    Packs.zip(
        archive,
        Packs.tree(
            MCMETA,
            Packs.MCMETA,
            "data/sonde/loot_table/AAAA.json",
            "{\"which\":\"first\"}",
            "data/sonde/loot_table/BBBB.json",
            "{\"which\":\"second\"}"));
    // Same length, so the local headers and the central directory stay consistent.
    String raw = new String(Files.readAllBytes(archive), StandardCharsets.ISO_8859_1);
    Files.write(
        archive, raw.replace("BBBB.json", "AAAA.json").getBytes(StandardCharsets.ISO_8859_1));
    PackFiles files = PackFiles.archive(archive);
    assertThat(files.paths()).containsExactly("data/sonde/loot_table/AAAA.json", MCMETA);
    assertThat(new String(files.read("data/sonde/loot_table/AAAA.json"), StandardCharsets.UTF_8))
        .isEqualTo("{\"which\":\"second\"}");
  }

  @Test
  @Timeout(value = 12, unit = TimeUnit.SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
  void anArchiveOfTwentyThousandEntriesLoadsInLinearTime() throws IOException {
    Map<String, String> files = new LinkedHashMap<>();
    files.put(MCMETA, Packs.MCMETA);
    int count = 20_000;
    for (int i = 0; i < count; i++) {
      files.put("data/sonde/loot_table/f" + i + ".json", "{}");
    }
    Path folder = datapacks();
    Packs.zip(folder.resolve("many.zip"), files);
    List<Pack> packs = PackDiscovery.worldPacks(folder).value().orElseThrow();
    Outcome<LoadedData> loaded = loadAll(packs);
    assertThat(MiniData.elementIds(loaded.value().orElseThrow(), "minecraft:loot_table"))
        .hasSize(count);
    assertThat(loaded.diagnostics()).isEmpty();
  }
}

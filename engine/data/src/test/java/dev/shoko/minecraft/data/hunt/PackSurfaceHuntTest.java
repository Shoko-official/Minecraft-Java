package dev.shoko.minecraft.data.hunt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.PackDiscovery;
import dev.shoko.minecraft.data.pack.PackFiles;
import dev.shoko.minecraft.data.tag.TagResolution;
import dev.shoko.minecraft.data.testing.Jx;
import dev.shoko.minecraft.data.testing.MiniData;
import dev.shoko.minecraft.data.testing.Packs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Tests aimed at the mutants of the review that survived the first suite: names of archive entries
// (R19, R24), overlay folders in the stacked paths (R48), cycles of tags closed by an optional
// entry (R36, R37).
class PackSurfaceHuntTest {

  private static final String OK = "data/sonde/loot_table/ok.json";
  private static final List<String> UNSAFE_NAMES =
      List.of(
          "../evil.json",
          "/abs.json",
          "data/../../evil2.json",
          "data\\sonde\\loot_table\\backslash.json",
          "./data/sonde/loot_table/dot.json",
          "data/sonde/loot_table//double.json",
          "C:/data/sonde/loot_table/drive.json",
          "data/sonde/loot_table/../loot_table/trav.json");

  @TempDir Path tmp;

  private Path archiveWithUnsafeNames() throws IOException {
    Path archive = tmp.resolve("evil.zip");
    try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive))) {
      List<String> names = new java.util.ArrayList<>(List.of("pack.mcmeta", OK));
      names.addAll(UNSAFE_NAMES);
      names.add("data/sonde/loot_table/folder/");
      for (String name : names) {
        out.putNextEntry(new ZipEntry(name));
        out.write(Jx.utf8(name.equals("pack.mcmeta") ? Packs.MCMETA : "{}"));
        out.closeEntry();
      }
    }
    return archive;
  }

  // The unsafe names are left out of the listing, with or without a diagnostic: the spec does not
  // say which.
  @Test
  void archiveEntriesWithUnsafeNamesAreNotListed() throws IOException {
    PackFiles files = PackFiles.archive(archiveWithUnsafeNames());
    assertThat(files.paths()).containsExactly(OK, "pack.mcmeta");
  }

  @Test
  void archiveEntriesWithUnsafeNamesCannotBeRead() throws IOException {
    PackFiles files = PackFiles.archive(archiveWithUnsafeNames());
    for (String name : UNSAFE_NAMES) {
      assertThatThrownBy(() -> files.read(name)).as(name).isInstanceOf(IOException.class);
    }
  }

  @Test
  void archiveEntriesWithUnsafeNamesLoadNothing() throws IOException {
    Path folder = Files.createDirectory(tmp.resolve("datapacks"));
    Files.move(archiveWithUnsafeNames(), folder.resolve("evil.zip"));
    List<Pack> packs = PackDiscovery.worldPacks(folder).value().orElseThrow();
    LoadedData data = MiniData.load(packs.toArray(Pack[]::new)).value().orElseThrow();
    assertThat(MiniData.elementIds(data, "minecraft:loot_table")).containsExactly("sonde:ok");
  }

  @Test
  void overlayFoldersAreNotPartOfTheStackedPaths() throws IOException {
    Pack pack =
        Packs.withMeta(
            "file/overlaid",
            "{\"pack\":{\"min_format\":121,\"max_format\":121},\"overlays\":{\"entries\":["
                + "{\"directory\":\"on\",\"min_format\":121,\"max_format\":121},"
                + "{\"directory\":\"off\",\"min_format\":122,\"max_format\":122}]}}",
            "data/sonde/loot_table/base.json",
            "{}",
            "on/data/sonde/loot_table/added.json",
            "{}",
            "on/pack.mcmeta",
            "{}",
            "off/data/sonde/loot_table/never.json",
            "{}");
    assertThat(Packs.stack(pack).paths())
        .containsExactly(
            "data/sonde/loot_table/added.json", "data/sonde/loot_table/base.json", "pack.mcmeta");
  }

  private static List<String> messagesOf(Outcome<LoadedData> loaded, String file) {
    return loaded.diagnostics().stream()
        .filter(d -> d.origin().path().equals(file))
        .map(d -> d.code() + "@" + d.path())
        .toList();
  }

  // spec: R36 (the entry that cannot be satisfied is reported), R37 (an optional entry that closes
  // a cycle is not skipped: the cycle fails, and the entry is reported like a required one).
  @Test
  void anOptionalEntryThatClosesACycleOfTagsIsReported() {
    Pack pack =
        Packs.world(
            "file/cycle",
            "data/sonde/tags/loot_table/a.json",
            "{\"values\":[\"#sonde:b\"]}",
            "data/sonde/tags/loot_table/b.json",
            "{\"values\":[{\"id\":\"#sonde:a\",\"required\":false}]}");
    Outcome<LoadedData> loaded = MiniData.load(pack);
    TagResolution tags =
        loaded.value().orElseThrow().tags(MiniData.registry("minecraft:loot_table")).orElseThrow();
    assertThat(tags.failed())
        .containsExactly(Identifier.parse("sonde:a"), Identifier.parse("sonde:b"));
    assertThat(messagesOf(loaded, "data/sonde/tags/loot_table/a.json"))
        .containsExactly("CODEC_UNRESOLVED_REFERENCE@$.values[0]");
    assertThat(messagesOf(loaded, "data/sonde/tags/loot_table/b.json"))
        .containsExactly("CODEC_UNRESOLVED_REFERENCE@$.values[0]");
  }

  @Test
  void anOptionalEntryOutsideAnyCycleIsSkippedWithoutDiagnostic() {
    Pack pack =
        Packs.world(
            "file/optional",
            "data/sonde/tags/loot_table/c.json",
            "{\"values\":[{\"id\":\"#sonde:missing\",\"required\":false}]}");
    Outcome<LoadedData> loaded = MiniData.load(pack);
    assertThat(loaded.diagnostics())
        .extracting(Diagnostic::code)
        .doesNotContain(DiagnosticCode.CODEC_UNRESOLVED_REFERENCE);
    assertThat(MiniData.tagMembers(loaded.value().orElseThrow(), "minecraft:loot_table", "sonde:c"))
        .isEmpty();
  }
}

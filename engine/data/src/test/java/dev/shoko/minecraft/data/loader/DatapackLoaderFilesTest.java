package dev.shoko.minecraft.data.loader;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.PackFileRef;
import dev.shoko.minecraft.data.pack.PackFiles;
import dev.shoko.minecraft.data.pack.PackOrigin;
import dev.shoko.minecraft.data.pack.meta.FeatureFlags;
import dev.shoko.minecraft.data.pack.meta.PackType;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Expect;
import dev.shoko.minecraft.data.testing.Jx;
import dev.shoko.minecraft.data.testing.MiniData;
import dev.shoko.minecraft.data.testing.Packs;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R10, R12 to R16, R24, R41: file attribution, opaque content files
// and unreadable files on miniature datapacks
class DatapackLoaderFilesTest {
  private static final String DIRT = "data/minecraft/loot_table/blocks/dirt.json";

  private static LoadedData ok(Outcome<LoadedData> outcome) {
    assertThat(outcome.hasErrors()).as("%s", Diags.summary(outcome)).isFalse();
    return outcome.value().orElseThrow();
  }

  @Test
  void provisionalFilesOfStaticOrUnknownFoldersAreIgnoredWithAnInformation() {
    // spec: R15, R16 (provisional), loader step 3
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit(
                "vanilla",
                "data/sonde/block/x.json",
                "{}",
                "data/sonde/inconnu/x.json",
                "{}",
                "data/sonde/tags/structure/x.json",
                "{\"values\":[]}",
                "data/sonde/loot_table/valide.json",
                "{}"));
    assertThat(outcome.hasErrors()).isFalse();
    assertThat(Diags.codes(outcome))
        .containsExactly(
            DiagnosticCode.FILE_IGNORED, DiagnosticCode.FILE_IGNORED, DiagnosticCode.FILE_IGNORED);
    assertThat(outcome.diagnostics())
        .extracting(d -> d.origin().path())
        .containsExactly(
            "data/sonde/block/x.json",
            "data/sonde/inconnu/x.json",
            "data/sonde/tags/structure/x.json");
    assertThat(MiniData.elementIds(outcome.value().orElseThrow(), "minecraft:loot_table"))
        .containsExactly("sonde:valide");
  }

  @Test
  void provisionalAFileOfAnotherExtensionIsIgnoredWithoutAnyDiagnostic() {
    // spec: R12 (provisional)
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit(
                "vanilla",
                "data/sonde/loot_table/x.txt",
                "x",
                "data/sonde/loot_table/valide.json",
                "{}"));
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(MiniData.elementIds(outcome.value().orElseThrow(), "minecraft:loot_table"))
        .containsExactly("sonde:valide");
  }

  @Test
  void provisionalAnInvalidNamespaceOrPathIsIgnoredAndTheRestOfThePackLoads() {
    // spec: R24 (provisional), P9
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit(
                "vanilla",
                "data/sonde/loot_table/Maj.json",
                "{}",
                "data/sonde/loot_table/es pace.json",
                "{}",
                "data/Sonde/loot_table/x.json",
                "{}",
                "data/sonde/loot_table/valide.json",
                "{}"));
    assertThat(Diags.codes(outcome)).hasSize(3).containsOnly(DiagnosticCode.FILE_IGNORED);
    assertThat(MiniData.elementIds(outcome.value().orElseThrow(), "minecraft:loot_table"))
        .containsExactly("sonde:valide");
  }

  @Test
  void provisionalFunctionsAndStructuresAreKeptAsOpaqueFilesOfTheHighestPriorityPack()
      throws IOException {
    // spec: R10, R41, K2 (not interpreted)
    byte[] binary = Jx.bytes(0x1f, 0x8b, 0x08, 0, 0xff, 0x80);
    Map<String, byte[]> vanillaFiles =
        Map.of(
            "data/minecraft/function/a/b.mcfunction",
            Jx.utf8("say 1"),
            "data/minecraft/structure/village/x.nbt",
            binary);
    Pack vanilla =
        Pack.implicit(
            "vanilla", PackOrigin.BUILT_IN, PackType.DATA, PackFiles.inMemory(vanillaFiles));
    Outcome<LoadedData> outcome =
        MiniData.load(vanilla, Packs.world("A", "data/minecraft/function/a/b.mcfunction", "say 2"));
    LoadedData data = ok(outcome);
    assertThat(data.functions().keySet().stream().map(Identifier::toString))
        .containsExactly("minecraft:a/b");
    PackFileRef function = data.functions().get(Identifier.parse("minecraft:a/b"));
    assertThat(function.pack()).isEqualTo("A");
    assertThat(function.read()).isEqualTo(Jx.utf8("say 2"));
    PackFileRef structure = data.structures().get(Identifier.parse("minecraft:village/x"));
    assertThat(structure.pack()).isEqualTo("vanilla");
    assertThat(structure.read()).isEqualTo(binary);
  }

  @Test
  void anUnreadableFileIsAnIoFailureNotADiagnostic() {
    // spec: DatapackLoader.load Javadoc (@throws IOException when a file cannot be read)
    PackFiles failing = new FailingFiles();
    Pack pack = Pack.implicit("vanilla", PackOrigin.BUILT_IN, PackType.DATA, failing);
    Throwable thrown =
        Expect.thrown(() -> DatapackLoader.load(MiniData.request(FeatureFlags.vanilla(), pack)));
    assertThat(thrown).isInstanceOf(IOException.class);
  }

  /** A pack whose only file cannot be read. */
  private static final class FailingFiles implements PackFiles {
    @Override
    public List<String> paths() {
      return List.of(DIRT);
    }

    @Override
    public byte[] read(String path) throws IOException {
      throw new IOException("disk failure");
    }

    @Override
    public PackFiles subtree(String directory) {
      return this;
    }

    @Override
    public PackFiles without(String directory) {
      return this;
    }
  }
}

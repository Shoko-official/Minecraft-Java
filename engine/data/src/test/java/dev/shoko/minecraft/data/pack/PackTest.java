package dev.shoko.minecraft.data.pack;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.pack.meta.PackCompatibility;
import dev.shoko.minecraft.data.pack.meta.PackType;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Expect;
import dev.shoko.minecraft.data.testing.Jx;
import dev.shoko.minecraft.data.testing.Packs;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R3, R4, R5, R6, R7, R40 (opening a pack from its pack.mcmeta)
class PackTest {

  private static Outcome<Pack> open(String mcmeta) throws IOException {
    return Pack.open(
        "file/p", PackOrigin.WORLD, PackType.DATA, Packs.inMemory("pack.mcmeta", mcmeta));
  }

  @Test
  void provisionalAValidPackIsOpenedWithItsIdentityAndMetadata() throws IOException {
    // spec: R4, R40
    Outcome<Pack> outcome =
        Pack.open(
            "file/p",
            PackOrigin.WORLD,
            PackType.DATA,
            Packs.inMemory("pack.mcmeta", Packs.MCMETA, "data/a/b.json", "{}"));
    assertThat(outcome.diagnostics()).isEmpty();
    Pack pack = outcome.value().orElseThrow();
    assertThat(pack.id()).isEqualTo("file/p");
    assertThat(pack.origin()).isEqualTo(PackOrigin.WORLD);
    assertThat(pack.type()).isEqualTo(PackType.DATA);
    assertThat(pack.meta().compatibility(PackType.DATA)).isEqualTo(PackCompatibility.COMPATIBLE);
    assertThat(pack.files().paths()).containsExactly("data/a/b.json", "pack.mcmeta");
  }

  @Test
  void provisionalAPackWithoutPackMcmetaIsIgnoredWithAWarningOnTheMetadataFile()
      throws IOException {
    // spec: R7, Pack.open Javadoc
    Outcome<Pack> outcome =
        Pack.open("file/p", PackOrigin.WORLD, PackType.DATA, Packs.inMemory("data/a/b.json", "{}"));
    assertThat(outcome.value()).isEmpty();
    assertThat(outcome.diagnostics()).hasSize(1);
    Diagnostic diagnostic = outcome.diagnostics().get(0);
    assertThat(diagnostic.code()).isEqualTo(DiagnosticCode.PACK_INVALID);
    assertThat(diagnostic.code().severity()).isEqualTo(Severity.WARNING);
    assertThat(diagnostic.origin()).isEqualTo(new FileOrigin("file/p", "pack.mcmeta"));
    assertThat(outcome.hasErrors()).isFalse();
  }

  @Test
  void provisionalAnInvalidJsonPackMcmetaGivesThePackWarningAndTheJsonError() throws IOException {
    // spec: R7, R54, Pack.open Javadoc ("plus the JSON_* error")
    Outcome<Pack> outcome = open("{");
    assertThat(outcome.value()).isEmpty();
    assertThat(Diags.codes(outcome))
        .containsExactlyInAnyOrder(DiagnosticCode.PACK_INVALID, DiagnosticCode.JSON_SYNTAX);
    Diagnostic json =
        outcome.diagnostics().stream()
            .filter(d -> d.code() == DiagnosticCode.JSON_SYNTAX)
            .findFirst()
            .orElseThrow();
    assertThat(json.origin()).isEqualTo(new FileOrigin("file/p", "pack.mcmeta"));
  }

  @Test
  void provisionalAPackMcmetaWithoutPackObjectIsNotAPack() throws IOException {
    // spec: R7
    for (String mcmeta : List.of("[]", "{}", "{\"pack\":3}", "\"x\"")) {
      Outcome<Pack> outcome = open(mcmeta);
      assertThat(outcome.value()).as(mcmeta).isEmpty();
      assertThat(Diags.codes(outcome)).as(mcmeta).containsExactly(DiagnosticCode.PACK_INVALID);
    }
  }

  @Test
  void provisionalAPackWithUnusableFormatsIsPresentAndBroken() throws IOException {
    // spec: R4, R5, Pack.open Javadoc
    Outcome<Pack> outcome = open("{\"pack\":{\"description\":\"d\",\"pack_format\":121}}");
    Pack pack = outcome.value().orElseThrow();
    assertThat(pack.meta().compatibility(PackType.DATA)).isEqualTo(PackCompatibility.BROKEN);
    assertThat(outcome.hasErrors()).isTrue();
  }

  @Test
  void provisionalAnIncompatiblePackIsStillOpened() throws IOException {
    // spec: R6 (an incompatible pack stays listed)
    Outcome<Pack> old = open("{\"pack\":{\"min_format\":100,\"max_format\":120}}");
    Outcome<Pack> newer = open("{\"pack\":{\"min_format\":[121,1],\"max_format\":121}}");
    assertThat(old.value().orElseThrow().meta().compatibility(PackType.DATA))
        .isEqualTo(PackCompatibility.TOO_OLD);
    assertThat(newer.value().orElseThrow().meta().compatibility(PackType.DATA))
        .isEqualTo(PackCompatibility.TOO_NEW);
    assertThat(old.hasErrors()).isFalse();
    assertThat(newer.hasErrors()).isFalse();
  }

  @Test
  void provisionalUnknownFieldsOfPackMcmetaAreIgnored() throws IOException {
    // spec: R4, R63 c
    Outcome<Pack> outcome =
        open("{\"pack\":{\"min_format\":121,\"max_format\":121,\"zzz\":1},\"zzz\":{}}");
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(outcome.value()).isPresent();
  }

  @Test
  void provisionalTheImplicitPackNeedsNoMetadataFile() {
    // spec: R3
    Pack pack =
        Pack.implicit(
            "vanilla", PackOrigin.BUILT_IN, PackType.DATA, Packs.inMemory("data/a/b.json", "{}"));
    assertThat(pack.id()).isEqualTo("vanilla");
    assertThat(pack.origin()).isEqualTo(PackOrigin.BUILT_IN);
    assertThat(pack.type()).isEqualTo(PackType.DATA);
    assertThat(pack.meta().compatibility(PackType.DATA)).isEqualTo(PackCompatibility.COMPATIBLE);
    assertThat(pack.meta().features().flags()).hasSize(1);
  }

  @Test
  void anUnreadablePackMcmetaIsAnIoFailureNotADiagnostic() {
    // spec: Pack.open Javadoc ("@throws IOException when pack.mcmeta cannot be read")
    PackFiles failing =
        new PackFiles() {
          @Override
          public List<String> paths() {
            return List.of("pack.mcmeta");
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
        };
    Throwable thrown =
        Expect.thrown(() -> Pack.open("file/p", PackOrigin.WORLD, PackType.DATA, failing));
    assertThat(thrown).isInstanceOf(IOException.class);
  }

  @Test
  void provisionalTheJsonOfPackMcmetaIsReadAsStrictJson() throws IOException {
    // spec: R52, R54 applied to pack.mcmeta
    Outcome<Pack> outcome =
        Pack.open(
            "file/p",
            PackOrigin.WORLD,
            PackType.DATA,
            PackFiles.inMemory(
                Map.of(
                    "pack.mcmeta", Jx.concat(Jx.bytes(0xEF, 0xBB, 0xBF), Jx.utf8(Packs.MCMETA)))));
    assertThat(outcome.value()).isEmpty();
    assertThat(Diags.codes(outcome))
        .containsExactlyInAnyOrder(DiagnosticCode.PACK_INVALID, DiagnosticCode.JSON_BOM);
  }
}

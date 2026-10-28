package dev.shoko.minecraft.data.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.testing.Jx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// spec: data-datapack-loading R8 to R16, R20, R24, table "Identifiants" (path to registry element,
// tag, content or ignored file)
class PathClassifierTest {

  private static final String CATALOG =
      """
      {"others":{
         "function":{"elements":true,"format":"mcfunction","stable":true,"tags":true},
         "structure":{"elements":true,"format":"structure","stable":true,"tags":false}},
       "registries":{
         "minecraft:block":{"elements":false,"stable":true,"tags":true},
         "minecraft:loot_table":{"elements":true,"stable":true,"tags":true},
         "minecraft:worldgen/biome":{"elements":true,"stable":false,"tags":true},
         "minecraft:foo":{"elements":true,"stable":true,"tags":true},
         "minecraft:foo/bar":{"elements":true,"stable":true,"tags":true}}}
      """;

  private static PathClassifier classifier() {
    RegistryCatalog catalog = RegistryCatalog.read(Jx.parseLenient(CATALOG)).value().orElseThrow();
    return PathClassifier.of(catalog);
  }

  private static Identifier id(String text) {
    return Identifier.parse(text);
  }

  private static FileAttribution classify(String path) {
    return classifier().classify(path);
  }

  @Test
  void provisionalAnElementFileGivesTheElementOfItsFolder() {
    // spec: R8, table "Identifiants" (data/sonde/loot_table/a/b.json)
    assertThat(classify("data/sonde/loot_table/a/b.json"))
        .isEqualTo(new FileAttribution.Element(id("minecraft:loot_table"), id("sonde:a/b")));
    assertThat(classify("data/minecraft/loot_table/blocks/dirt.json"))
        .isEqualTo(
            new FileAttribution.Element(id("minecraft:loot_table"), id("minecraft:blocks/dirt")));
  }

  @Test
  void provisionalAFolderWithSeveralSegmentsIsOneRegistry() {
    // spec: R8, R11
    assertThat(classify("data/minecraft/worldgen/biome/plains.json"))
        .isEqualTo(
            new FileAttribution.Element(id("minecraft:worldgen/biome"), id("minecraft:plains")));
  }

  @Test
  void theLongestKnownFolderPrefixWins() {
    // spec: R11
    assertThat(classify("data/x/foo/bar/baz.json"))
        .isEqualTo(new FileAttribution.Element(id("minecraft:foo/bar"), id("x:baz")));
    assertThat(classify("data/x/foo/qux.json"))
        .isEqualTo(new FileAttribution.Element(id("minecraft:foo"), id("x:qux")));
    assertThat(classify("data/x/foo/bar.json"))
        .isEqualTo(new FileAttribution.Element(id("minecraft:foo"), id("x:bar")));
    assertThat(classify("data/x/foo/bar/deep/er/f.json"))
        .isEqualTo(new FileAttribution.Element(id("minecraft:foo/bar"), id("x:deep/er/f")));
  }

  @Test
  void provisionalATagFileGivesTheTagOfItsRegistry() {
    // spec: R9, table "Identifiants" (data/sonde/tags/worldgen/biome/x.json)
    assertThat(classify("data/sonde/tags/worldgen/biome/x.json"))
        .isEqualTo(new FileAttribution.Tag(id("minecraft:worldgen/biome"), id("sonde:x")));
    assertThat(classify("data/minecraft/tags/block/mineable/pickaxe.json"))
        .isEqualTo(
            new FileAttribution.Tag(id("minecraft:block"), id("minecraft:mineable/pickaxe")));
    assertThat(classify("data/minecraft/tags/loot_table/t.json"))
        .isEqualTo(new FileAttribution.Tag(id("minecraft:loot_table"), id("minecraft:t")));
  }

  @Test
  void functionsAndStructuresAreOpaqueContent() {
    // spec: R10
    assertThat(classify("data/minecraft/function/a/b.mcfunction"))
        .isEqualTo(new FileAttribution.Content(id("minecraft:function"), id("minecraft:a/b")));
    assertThat(classify("data/sonde/structure/village/x.nbt"))
        .isEqualTo(new FileAttribution.Content(id("minecraft:structure"), id("sonde:village/x")));
  }

  @Test
  void provisionalFunctionTagsAreTagsAndStructureTagsAreNot() {
    // spec: R10 (tags allowed in tags/function), R16 (tags/structure ignored)
    assertThat(classify("data/x/tags/function/t.json"))
        .isEqualTo(new FileAttribution.Tag(id("minecraft:function"), id("x:t")));
    assertThat(classify("data/sonde/tags/structure/x.json"))
        .isInstanceOf(FileAttribution.Ignored.class);
  }

  @Test
  void provisionalAFileOfTheWrongExtensionInAKnownFolderIsSkippedSilently() {
    // spec: R12 (provisional), FileAttribution.Skipped
    assertThat(classify("data/sonde/loot_table/x.txt")).isInstanceOf(FileAttribution.Skipped.class);
    assertThat(classify("data/sonde/loot_table/x.mcfunction"))
        .isInstanceOf(FileAttribution.Skipped.class);
    assertThat(classify("data/sonde/loot_table/x.nbt")).isInstanceOf(FileAttribution.Skipped.class);
    assertThat(classify("data/sonde/function/x.json")).isInstanceOf(FileAttribution.Skipped.class);
    assertThat(classify("data/sonde/structure/x.json")).isInstanceOf(FileAttribution.Skipped.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "pack.mcmeta",
        "pack.png",
        ".mcassetsroot",
        "data/.mcassetsroot",
        "assets/.mcassetsroot",
        "assets/minecraft/lang/en_us.json",
        "assets/minecraft/deprecated.json",
        "README.md",
        "ov1/data/x/loot_table/a.json"
      })
  void provisionalPackMetadataAndFilesOutsideDataTakeNoPartInLoading(String path) {
    // spec: R12, R13, R18 (pack.mcmeta, pack.png, the extraction marker, assets)
    assertThat(classify(path)).isEqualTo(new FileAttribution.Metadata());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "data/sonde/block/x.json",
        "data/sonde/inconnu/x.json",
        "data/sonde/tags/inconnu/x.json",
        "data/sonde/tags/structure/x.json",
      })
  void provisionalUnknownOrStaticFoldersAndTagsOfNoRegistryAreIgnoredWithADiagnostic(String path) {
    // spec: R15, R16 (provisional), table "Identifiants"
    assertThat(classify(path)).isInstanceOf(FileAttribution.Ignored.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "data/sonde/loot_table/Maj.json",
        "data/sonde/loot_table/es pace.json",
        "data/Sonde/loot_table/x.json",
        "data/a b/loot_table/x.json",
        "data/../loot_table/x.json",
        "data/sonde/tags/block/Upper.json",
        "data/Upper/tags/block/x.json",
        "data/sonde/loot_table/caf\351.json",
        "data/sonde/function/Up.mcfunction"
      })
  void provisionalAnInvalidNamespaceOrPathIsIgnoredWithADiagnostic(String path) {
    // spec: R24 (provisional), R20, table "Identifiants" (data/sonde/loot_table/Maj.json ignored)
    assertThat(classify(path)).isInstanceOf(FileAttribution.Ignored.class);
  }

  @Test
  void ignoredFilesDescribeTheirCause() {
    // spec: FileAttribution.Ignored (a message for humans)
    FileAttribution.Ignored ignored =
        (FileAttribution.Ignored) classify("data/sonde/loot_table/Maj.json");
    assertThat(ignored.message()).isNotBlank();
  }

  @Test
  void provisionalNamespacesMayUseDashAndDot() {
    // spec: R20
    assertThat(classify("data/my-pack.v2/loot_table/a.json"))
        .isEqualTo(new FileAttribution.Element(id("minecraft:loot_table"), id("my-pack.v2:a")));
  }

  @Test
  void provisionalTheExtensionIsRemovedOnce() {
    // spec: R8 (the path without the extension), R20 ('.' is allowed in a path)
    assertThat(classify("data/x/loot_table/a.json.json"))
        .isEqualTo(new FileAttribution.Element(id("minecraft:loot_table"), id("x:a.json")));
    assertThat(classify("data/x/loot_table/a.b.json"))
        .isEqualTo(new FileAttribution.Element(id("minecraft:loot_table"), id("x:a.b")));
  }

  @Test
  void provisionalThePathCharactersOfTheIdentifierAreKept() {
    // spec: R20
    assertThat(classify("data/x/loot_table/a_b-c.d/e0.json"))
        .isEqualTo(new FileAttribution.Element(id("minecraft:loot_table"), id("x:a_b-c.d/e0")));
  }
}

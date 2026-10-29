package dev.shoko.minecraft.data.pack;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.pack.meta.PackCompatibility;
import dev.shoko.minecraft.data.pack.meta.PackType;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Jx;
import dev.shoko.minecraft.data.testing.Packs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// spec: data-datapack-loading R3, R6, R7, R13, R14, R19, R40, R67, vector VD1, probe P24
class PackDiscoveryTest {

  private static final String LOOT = "data/sonde/loot_table/a.json";

  private static void folderPack(Path datapacks, String name) throws IOException {
    Packs.writeTree(datapacks.resolve(name), Packs.tree("pack.mcmeta", Packs.MCMETA, LOOT, "{}"));
  }

  private static List<String> ids(Outcome<List<Pack>> outcome) {
    return outcome.value().orElseThrow().stream().map(Pack::id).toList();
  }

  private static Path vd1(Path root) throws IOException {
    Path datapacks = Files.createDirectories(root.resolve("datapacks"));
    for (String name : new String[] {"p2", "p10", "Z", "a", "\351"}) {
      folderPack(datapacks, name);
    }
    Packs.zip(datapacks.resolve("x.zip"), Packs.tree("pack.mcmeta", Packs.MCMETA, LOOT, "{}"));
    Packs.zip(datapacks.resolve("y.ZIP"), Packs.tree("pack.mcmeta", Packs.MCMETA));
    Packs.writeTree(datapacks.resolve(".hidden"), Packs.tree("pack.mcmeta", Packs.MCMETA));
    Files.createDirectories(datapacks.resolve("notapack/data"));
    Files.writeString(datapacks.resolve("readme.txt"), "hello");
    return datapacks;
  }

  @Test
  void provisionalWorldPacksFollowTheCodePointOrderOfTheirNames(@TempDir Path tmp)
      throws IOException {
    // spec: VD1, R67 (a, b, c)
    Outcome<List<Pack>> outcome = PackDiscovery.worldPacks(vd1(tmp));
    assertThat(ids(outcome))
        .containsExactly("file/Z", "file/a", "file/p10", "file/p2", "file/x.zip", "file/\351");
  }

  @Test
  void provisionalOnlyTheFolderWithoutMetadataIsReported(@TempDir Path tmp) throws IOException {
    // spec: VD1, R7, R67 d ("notapack : PACK_INVALID"; y.ZIP, .hidden, readme.txt ignored)
    Outcome<List<Pack>> outcome = PackDiscovery.worldPacks(vd1(tmp));
    assertThat(outcome.diagnostics()).hasSize(1);
    Diagnostic diagnostic = outcome.diagnostics().get(0);
    assertThat(diagnostic.code()).isEqualTo(DiagnosticCode.PACK_INVALID);
    assertThat(diagnostic.origin().pack()).isEqualTo("file/notapack");
    assertThat(outcome.hasErrors()).isFalse();
  }

  @Test
  void provisionalWorldPacksHaveTheWorldOriginAndTheDataKind(@TempDir Path tmp) throws IOException {
    // spec: R40 (source "world")
    List<Pack> packs = PackDiscovery.worldPacks(vd1(tmp)).value().orElseThrow();
    assertThat(packs).allSatisfy(pack -> assertThat(pack.origin()).isEqualTo(PackOrigin.WORLD));
    assertThat(packs).allSatisfy(pack -> assertThat(pack.type()).isEqualTo(PackType.DATA));
  }

  @Test
  void provisionalFolderAndArchivePacksExposeTheirFiles(@TempDir Path tmp) throws IOException {
    // spec: R19 (archives accepted like folders)
    List<Pack> packs = PackDiscovery.worldPacks(vd1(tmp)).value().orElseThrow();
    for (Pack pack : packs) {
      assertThat(pack.files().paths()).contains(LOOT, "pack.mcmeta");
      assertThat(pack.files().read(LOOT)).isEqualTo(Jx.utf8("{}"));
    }
  }

  @Test
  void provisionalTheOrderDoesNotDependOnTheCreationOrder(@TempDir Path tmp) throws IOException {
    // spec: R67 c, P24 ("identique dans les deux mondes")
    Path forward = Files.createDirectories(tmp.resolve("forward"));
    Path backward = Files.createDirectories(tmp.resolve("backward"));
    List<String> names = List.of("\351", "p2", "a", "Z", "p10");
    for (String name : names) {
      folderPack(forward, name);
    }
    for (int i = names.size() - 1; i >= 0; i--) {
      folderPack(backward, names.get(i));
    }
    List<String> first = ids(PackDiscovery.worldPacks(forward));
    List<String> second = ids(PackDiscovery.worldPacks(backward));
    assertThat(first)
        .isEqualTo(second)
        .containsExactly("file/Z", "file/a", "file/p10", "file/p2", "file/\351");
  }

  @Test
  void provisionalAnEmptyFolderGivesNoPackAndNoDiagnostic(@TempDir Path tmp) throws IOException {
    Outcome<List<Pack>> outcome =
        PackDiscovery.worldPacks(Files.createDirectories(tmp.resolve("d")));
    assertThat(outcome.value().orElseThrow()).isEmpty();
    assertThat(outcome.diagnostics()).isEmpty();
  }

  @Test
  void provisionalAnIncompatiblePackIsDiscoveredLikeTheOthers(@TempDir Path tmp)
      throws IOException {
    // spec: R6, PackDiscovery.worldPacks Javadoc (an incompatible pack stays available)
    Path datapacks = Files.createDirectories(tmp.resolve("datapacks"));
    Packs.writeTree(
        datapacks.resolve("old"),
        Packs.tree("pack.mcmeta", "{\"pack\":{\"min_format\":100,\"max_format\":120}}"));
    Outcome<List<Pack>> outcome = PackDiscovery.worldPacks(datapacks);
    assertThat(ids(outcome)).containsExactly("file/old");
    Pack old = outcome.value().orElseThrow().get(0);
    assertThat(old.meta().compatibility(PackType.DATA)).isEqualTo(PackCompatibility.TOO_OLD);
  }

  @Test
  void provisionalAFolderWithUnreadableJsonMetadataIsLeftOutWithTwoDiagnostics(@TempDir Path tmp)
      throws IOException {
    // spec: R7, R54
    Path datapacks = Files.createDirectories(tmp.resolve("datapacks"));
    Packs.writeTree(datapacks.resolve("bad"), Packs.tree("pack.mcmeta", "{"));
    folderPack(datapacks, "good");
    Outcome<List<Pack>> outcome = PackDiscovery.worldPacks(datapacks);
    assertThat(ids(outcome)).containsExactly("file/good");
    assertThat(Diags.codes(outcome))
        .containsExactlyInAnyOrder(DiagnosticCode.PACK_INVALID, DiagnosticCode.JSON_SYNTAX);
  }

  @Test
  void provisionalNamesStartingWithADotAndOtherFilesAreSkippedSilently(@TempDir Path tmp)
      throws IOException {
    // spec: R67 a
    Path datapacks = Files.createDirectories(tmp.resolve("datapacks"));
    Packs.writeTree(datapacks.resolve(".git"), Packs.tree("pack.mcmeta", Packs.MCMETA));
    Packs.zip(datapacks.resolve(".hidden.zip"), Packs.tree("pack.mcmeta", Packs.MCMETA));
    Files.writeString(datapacks.resolve("notes.txt"), "x");
    Files.writeString(datapacks.resolve("pack.mcmeta"), Packs.MCMETA);
    Files.writeString(datapacks.resolve("archive.zip.bak"), "x");
    Outcome<List<Pack>> outcome = PackDiscovery.worldPacks(datapacks);
    assertThat(outcome.value().orElseThrow()).isEmpty();
    assertThat(outcome.diagnostics()).isEmpty();
  }

  private static Pack pack(String id, String min, String max) throws IOException {
    return Pack.open(
            id,
            PackOrigin.WORLD,
            PackType.DATA,
            Packs.inMemory(
                "pack.mcmeta",
                "{\"pack\":{\"min_format\":" + min + ",\"max_format\":" + max + "}}"))
        .value()
        .orElseThrow();
  }

  @Test
  void provisionalSavedPacksComeFirstInTheSavedOrderThenNewCompatibleOnes() throws IOException {
    // spec: R40, R67 e, PackDiscovery.activationOrder Javadoc
    Pack a = pack("file/a", "121", "121");
    Pack b = pack("file/b", "121", "121");
    Pack c = pack("file/c", "121", "121");
    Pack d = pack("file/d", "121", "121");
    List<Pack> order =
        PackDiscovery.activationOrder(List.of(a, b, c, d), List.of("file/c", "file/a"));
    assertThat(order).extracting(Pack::id).containsExactly("file/c", "file/a", "file/b", "file/d");
  }

  @Test
  void provisionalAnIncompatiblePackIsActivatedOnlyWhenSaved() throws IOException {
    // spec: R6, R67 e
    Pack ok = pack("file/ok", "121", "121");
    Pack old = pack("file/old", "100", "120");
    Pack newer = pack("file/new", "[121, 1]", "121");
    Pack broken = pack("file/broken", "122", "121");
    assertThat(PackDiscovery.activationOrder(List.of(ok, old, newer, broken), List.of()))
        .extracting(Pack::id)
        .containsExactly("file/ok");
    assertThat(
            PackDiscovery.activationOrder(
                List.of(ok, old, newer, broken), List.of("file/old", "file/broken")))
        .extracting(Pack::id)
        .containsExactly("file/old", "file/broken", "file/ok");
  }

  @Test
  void provisionalAnUnknownSavedIdentifierIsSkipped() throws IOException {
    // spec: PackDiscovery.activationOrder Javadoc
    Pack a = pack("file/a", "121", "121");
    assertThat(
            PackDiscovery.activationOrder(List.of(a), List.of("file/gone", "file/a", "file/other")))
        .extracting(Pack::id)
        .containsExactly("file/a");
  }

  @Test
  void provisionalNothingDiscoveredNothingActivated() {
    assertThat(PackDiscovery.activationOrder(List.of(), List.of("file/a"))).isEmpty();
    assertThat(PackDiscovery.activationOrder(List.of(), List.of())).isEmpty();
  }

  /** A miniature of the extracted reference data, with the same layout as the real one. */
  private static Path reference(Path root) throws IOException {
    Path resources = Files.createDirectories(root.resolve("resources"));
    Packs.writeTree(
        resources,
        Map.of(
            "data/.mcassetsroot", "",
            "assets/.mcassetsroot", "",
            "data/minecraft/loot_table/dirt.json", "{}",
            "data/minecraft/tags/block/logs.json", "{\"values\":[]}",
            "data/minecraft/datapacks/trade_rebalance/pack.mcmeta", featurePack("trade_rebalance"),
            "data/minecraft/datapacks/trade_rebalance/data/minecraft/loot_table/extra.json", "{}",
            "data/minecraft/datapacks/minecart_improvements/pack.mcmeta",
                featurePack("minecart_improvements"),
            "data/minecraft/datapacks/redstone_experiments/pack.mcmeta",
                featurePack("redstone_experiments")));
    return resources;
  }

  private static String featurePack(String name) {
    return "{\"features\":{\"enabled\":[\"minecraft:"
        + name
        + "\"]},\"pack\":{\"description\":"
        + "{\"translate\":\"dataPack."
        + name
        + ".description\"},\"max_format\":121,\"min_format\":121}}";
  }

  @Test
  void provisionalReferencePacksAreVanillaThenTheBuiltInPacksByName(@TempDir Path tmp)
      throws IOException {
    // spec: R3, R14, R40, R42
    Outcome<List<Pack>> outcome = PackDiscovery.referencePacks(reference(tmp));
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(ids(outcome))
        .containsExactly(
            "vanilla", "minecart_improvements", "redstone_experiments", "trade_rebalance");
  }

  @Test
  void provisionalReferencePacksHaveTheOriginsOfTheGame(@TempDir Path tmp) throws IOException {
    // spec: R40 (sources "built-in" and "feature")
    List<Pack> packs = PackDiscovery.referencePacks(reference(tmp)).value().orElseThrow();
    assertThat(packs.get(0).origin()).isEqualTo(PackOrigin.BUILT_IN);
    assertThat(packs.subList(1, 4))
        .allSatisfy(p -> assertThat(p.origin()).isEqualTo(PackOrigin.FEATURE));
    assertThat(packs).allSatisfy(p -> assertThat(p.type()).isEqualTo(PackType.DATA));
  }

  @Test
  void referenceVanillaNeverWalksTheBuiltInPackFolder(@TempDir Path tmp) throws IOException {
    // spec: R14
    Pack vanilla = PackDiscovery.referencePacks(reference(tmp)).value().orElseThrow().get(0);
    assertThat(vanilla.files().paths())
        .contains("data/minecraft/loot_table/dirt.json", "data/minecraft/tags/block/logs.json")
        .noneMatch(path -> path.startsWith("data/minecraft/datapacks"));
  }

  @Test
  void referenceBuiltInPacksAreRootedAtTheirOwnFolderWithTheirMetadata(@TempDir Path tmp)
      throws IOException {
    // spec: R14, R2, R44
    List<Pack> packs = PackDiscovery.referencePacks(reference(tmp)).value().orElseThrow();
    Pack trade = packs.get(3);
    assertThat(trade.files().paths())
        .containsExactly("data/minecraft/loot_table/extra.json", "pack.mcmeta");
    assertThat(trade.meta().features().flags()).hasSize(1);
    assertThat(trade.meta().features().flags().getFirst().toString())
        .isEqualTo("minecraft:trade_rebalance");
    assertThat(trade.meta().compatibility(PackType.DATA)).isEqualTo(PackCompatibility.COMPATIBLE);
  }

  @Test
  void provisionalReferenceVanillaIsImplicit(@TempDir Path tmp) throws IOException {
    // spec: R3, R66 b (the vanilla pack requires minecraft:vanilla only)
    Pack vanilla = PackDiscovery.referencePacks(reference(tmp)).value().orElseThrow().get(0);
    assertThat(vanilla.meta().compatibility(PackType.DATA)).isEqualTo(PackCompatibility.COMPATIBLE);
    assertThat(vanilla.meta().features().flags()).hasSize(1);
    assertThat(vanilla.meta().features().flags().getFirst().toString())
        .isEqualTo("minecraft:vanilla");
  }

  @Test
  void provisionalReferenceABuiltInFolderWithoutMetadataIsLeftOutWithAWarning(@TempDir Path tmp)
      throws IOException {
    // spec: R7, PackDiscovery.referencePacks Javadoc ("diagnostics of the packs that could not be
    // opened")
    Path resources = reference(tmp);
    Packs.writeTree(
        resources,
        Packs.tree("data/minecraft/datapacks/nometa/data/minecraft/loot_table/x.json", "{}"));
    Outcome<List<Pack>> outcome = PackDiscovery.referencePacks(resources);
    assertThat(ids(outcome)).doesNotContain("nometa").contains("vanilla", "trade_rebalance");
    assertThat(Diags.codes(outcome)).containsExactly(DiagnosticCode.PACK_INVALID);
  }
}

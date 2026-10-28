package dev.shoko.minecraft.data.loader;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.meta.FeatureFlags;
import dev.shoko.minecraft.data.pack.meta.PackCompatibility;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.MiniData;
import dev.shoko.minecraft.data.testing.Packs;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R5, R6, R17, R44, R66, vectors VF1 to VF5 (feature flags and the
// report of each pack)
class DatapackLoaderFlagsTest {

  private static final String LOOT = "data/minecraft/loot_table/trade.json";
  private static final String BIOME = "data/minecraft/worldgen/biome/b.json";

  private static FeatureFlags flags(String... ids) {
    return FeatureFlags.of(Arrays.stream(ids).map(Identifier::parse).toList());
  }

  private static String meta(String features, String min, String max) {
    return "{"
        + features
        + "\"pack\":{\"description\":\"d\",\"min_format\":"
        + min
        + ",\"max_format\":"
        + max
        + "}}";
  }

  private static Pack flagged(String id, String... flagIds) {
    String list = String.join(",", Arrays.stream(flagIds).map(f -> "\"" + f + "\"").toList());
    return Packs.withMeta(
        id,
        meta("\"features\":{\"enabled\":[" + list + "]},", "121", "121"),
        LOOT,
        "\"from " + id + "\"");
  }

  private static PackReport report(LoadedData data, String pack) {
    return data.packReports().stream().filter(r -> r.pack().equals(pack)).findFirst().orElseThrow();
  }

  private static Pack vanilla() {
    return Packs.implicit("vanilla", LOOT, "\"from vanilla\"");
  }

  private static String loot(LoadedData data) {
    return MiniData.describe(
        data.elementRegistry(Identifier.parse("minecraft:loot_table"))
            .orElseThrow()
            .get(Identifier.parse("minecraft:trade"))
            .orElseThrow());
  }

  @Test
  void provisionalAPackRequiringAnInactiveFlagIsDiscardedWithOneError() {
    // spec: VF1, R66 c
    Pack a = flagged("A", "minecraft:trade_rebalance");
    Outcome<LoadedData> outcome = MiniData.load(FeatureFlags.vanilla(), vanilla(), a);
    assertThat(outcome.diagnostics()).hasSize(1);
    Diagnostic diagnostic = outcome.diagnostics().get(0);
    assertThat(diagnostic.code()).isEqualTo(DiagnosticCode.PACK_FLAGS_INACTIVE);
    assertThat(diagnostic.origin()).isEqualTo(new FileOrigin("A", "pack.mcmeta"));
    assertThat(diagnostic.path().toString()).isEqualTo("$");
    assertThat(outcome.hasErrors()).isTrue();
  }

  @Test
  void provisionalTheDataOfADiscardedPackIsAbsentFromTheOutput() {
    // spec: VF1 ("les donnees de A absentes de la sortie")
    Pack a = flagged("A", "minecraft:trade_rebalance");
    LoadedData data = MiniData.load(FeatureFlags.vanilla(), vanilla(), a).value().orElseThrow();
    assertThat(loot(data)).contains("from vanilla").doesNotContain("from A");
    assertThat(
            data.elementRegistry(Identifier.parse("minecraft:loot_table"))
                .orElseThrow()
                .get(Identifier.parse("minecraft:trade"))
                .orElseThrow()
                .origin()
                .pack())
        .isEqualTo("vanilla");
  }

  @Test
  void provisionalARequiredFlagThatIsActiveStacksThePackWithoutDiagnostic() {
    // spec: VF2, R66
    Pack a = flagged("A", "minecraft:trade_rebalance");
    FeatureFlags active = flags("minecraft:vanilla", "minecraft:trade_rebalance");
    Outcome<LoadedData> outcome = MiniData.load(active, vanilla(), a);
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(loot(outcome.value().orElseThrow())).contains("from A");
  }

  @Test
  void provisionalAllRequiredFlagsMustBeActiveAndTheMissingOnesAreReported() {
    // spec: VF3, R66 c (x:y is defined by nobody, it is simply not active)
    Pack b = flagged("B", "x:y", "minecraft:trade_rebalance");
    FeatureFlags active = flags("minecraft:vanilla", "minecraft:trade_rebalance");
    Outcome<LoadedData> outcome = MiniData.load(active, vanilla(), b);
    assertThat(Diags.codes(outcome)).containsExactly(DiagnosticCode.PACK_FLAGS_INACTIVE);
    PackReport report = report(outcome.value().orElseThrow(), "B");
    assertThat(report.missingFlags()).isEqualTo(flags("x:y"));
    assertThat(report.features()).isEqualTo(flags("x:y", "minecraft:trade_rebalance"));
    assertThat(loot(outcome.value().orElseThrow())).contains("from vanilla");
  }

  @Test
  void provisionalAPackWithoutFeaturesLoadsWithOnlyTheVanillaFlag() {
    // spec: VF4, R66 b
    Pack plain = Packs.world("P", LOOT, "\"from P\"");
    Outcome<LoadedData> outcome = MiniData.load(FeatureFlags.vanilla(), vanilla(), plain);
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(loot(outcome.value().orElseThrow())).contains("from P");
    PackReport report = report(outcome.value().orElseThrow(), "P");
    assertThat(report.features().flags()).isEmpty();
    assertThat(report.missingFlags().flags()).isEmpty();
  }

  @Test
  void provisionalTheOutputRepeatsTheActiveFlagsInCodePointOrder() {
    // spec: VF5, R66 e
    FeatureFlags given =
        FeatureFlags.of(
            List.of(
                Identifier.parse("minecraft:vanilla"),
                Identifier.parse("minecraft:trade_rebalance")));
    LoadedData data = MiniData.load(given, vanilla()).value().orElseThrow();
    assertThat(data.featureFlags().flags().stream().map(Identifier::toString))
        .containsExactly("minecraft:trade_rebalance", "minecraft:vanilla");
    assertThat(data.featureFlags()).isEqualTo(given);
  }

  @Test
  void provisionalTheLoaderNeverWidensTheActiveFlags() {
    // spec: R66 a ("ne l'elargit pas"): a loaded pack with flags does not add them to the output
    Pack a = flagged("A", "minecraft:trade_rebalance");
    FeatureFlags active = flags("minecraft:vanilla", "minecraft:trade_rebalance");
    LoadedData data = MiniData.load(active, vanilla(), a).value().orElseThrow();
    assertThat(data.featureFlags()).isEqualTo(active);
    LoadedData discarded =
        MiniData.load(FeatureFlags.vanilla(), vanilla(), a).value().orElseThrow();
    assertThat(discarded.featureFlags()).isEqualTo(FeatureFlags.vanilla());
  }

  @Test
  void provisionalADiscardedPackDoesNotApplyItsFiltersOrAnyOfItsFiles() {
    // spec: loader step 2 ("none of its files, filters or overlays take part in the load")
    String json =
        "{\"features\":{\"enabled\":[\"minecraft:trade_rebalance\"]},\"pack\":{\"min_format\":121,"
            + "\"max_format\":121},\"filter\":{\"block\":[{\"path\":\"loot_table/.*\"}]}}";
    Pack hiding = Packs.withMeta("H", json);
    LoadedData data =
        MiniData.load(FeatureFlags.vanilla(), vanilla(), hiding).value().orElseThrow();
    assertThat(loot(data)).contains("from vanilla");
    Outcome<LoadedData> withFlag =
        MiniData.load(flags("minecraft:vanilla", "minecraft:trade_rebalance"), vanilla(), hiding);
    assertThat(
            withFlag
                .value()
                .orElseThrow()
                .elementRegistry(Identifier.parse("minecraft:loot_table"))
                .orElseThrow()
                .elements())
        .isEmpty();
  }

  @Test
  void provisionalOtherPacksStillLoadNormallyWhenOneIsDiscarded() {
    // spec: R66 c ("les autres sources se chargent normalement")
    Pack inactive = flagged("A", "minecraft:trade_rebalance");
    Pack ordinary = Packs.world("W", "data/minecraft/loot_table/w.json", "{}");
    Outcome<LoadedData> outcome =
        MiniData.load(FeatureFlags.vanilla(), vanilla(), inactive, ordinary);
    assertThat(outcome.diagnostics()).hasSize(1);
    assertThat(MiniData.elementIds(outcome.value().orElseThrow(), "minecraft:loot_table"))
        .containsExactly("minecraft:trade", "minecraft:w");
  }

  @Test
  void provisionalEveryPackHasAReportFromTheLowestToTheHighestPriority() {
    // spec: Entrees / sorties ("par pack, l'etat de compatibilite (R5) et le drapeau
    // experimental"),
    // LoadedData.packReports Javadoc (discarded packs included)
    Pack inactive = flagged("A", "minecraft:trade_rebalance");
    Pack ordinary = Packs.world("W");
    LoadedData data =
        MiniData.load(FeatureFlags.vanilla(), vanilla(), inactive, ordinary).value().orElseThrow();
    assertThat(data.packReports())
        .extracting(PackReport::pack)
        .containsExactly("vanilla", "A", "W");
    assertThat(report(data, "A").missingFlags()).isEqualTo(flags("minecraft:trade_rebalance"));
    assertThat(report(data, "A").experimental()).isFalse();
  }

  @Test
  void provisionalTheVanillaPackIsCompatibleAndRequiresOnlyTheVanillaFlag() {
    // spec: R3, R66 b
    LoadedData data = MiniData.load(vanilla()).value().orElseThrow();
    PackReport report = report(data, "vanilla");
    assertThat(report.compatibility()).isEqualTo(PackCompatibility.COMPATIBLE);
    assertThat(report.features()).isEqualTo(FeatureFlags.vanilla());
    assertThat(report.missingFlags().flags()).isEmpty();
  }

  @Test
  void provisionalAnIncompatiblePackIsStillLoadedAndReported() {
    // spec: R5, R6 (activating it is the caller's choice), loader step 1
    Pack old = Packs.withMeta("old", meta("", "100", "120"), LOOT, "\"from old\"");
    Pack newer =
        Packs.withMeta(
            "new", meta("", "[121, 1]", "121"), "data/minecraft/loot_table/n.json", "{}");
    Outcome<LoadedData> outcome = MiniData.load(FeatureFlags.vanilla(), vanilla(), old, newer);
    LoadedData data = outcome.value().orElseThrow();
    assertThat(report(data, "old").compatibility()).isEqualTo(PackCompatibility.TOO_OLD);
    assertThat(report(data, "new").compatibility()).isEqualTo(PackCompatibility.TOO_NEW);
    assertThat(report(data, "vanilla").compatibility()).isEqualTo(PackCompatibility.COMPATIBLE);
    assertThat(loot(data)).contains("from old");
    assertThat(MiniData.elementIds(data, "minecraft:loot_table")).contains("minecraft:n");
  }

  @Test
  void provisionalABrokenPackIsReportedBroken() {
    // spec: R4, R5 (broken or incompatible)
    Pack broken = Packs.withMeta("broken", "{\"pack\":{\"pack_format\":121}}");
    LoadedData data =
        MiniData.load(FeatureFlags.vanilla(), vanilla(), broken).value().orElseThrow();
    assertThat(report(data, "broken").compatibility()).isEqualTo(PackCompatibility.BROKEN);
  }

  @Test
  void provisionalAPackIsExperimentalWhenItHoldsAValidFileInAnUnstableRegistry() {
    // spec: R17 (the flag is computed per pack and has no effect on loading)
    Pack stable = Packs.world("S", "data/minecraft/loot_table/a.json", "{}");
    Pack unstable = Packs.world("U", BIOME, "{}");
    Pack both =
        Packs.world(
            "B",
            "data/minecraft/loot_table/b.json",
            "{}",
            "data/minecraft/villager_trade/t.json",
            "{}");
    Pack invalidOnly = Packs.world("I", BIOME, "{");
    Outcome<LoadedData> outcome =
        MiniData.load(FeatureFlags.vanilla(), vanilla(), stable, unstable, both, invalidOnly);
    LoadedData data = outcome.value().orElseThrow();
    assertThat(report(data, "S").experimental()).isFalse();
    assertThat(report(data, "U").experimental()).isTrue();
    assertThat(report(data, "B").experimental()).isTrue();
    assertThat(report(data, "I").experimental()).isFalse();
    assertThat(MiniData.elementIds(data, "minecraft:worldgen/biome"))
        .containsExactly("minecraft:b");
  }
}

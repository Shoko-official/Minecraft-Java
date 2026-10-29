package dev.shoko.minecraft.data.pack.meta;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Jx;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// spec: data-datapack-loading R2, R4, R5, R7, R47, R48, R63 c, vector VC16 and the table
// "pack.mcmeta"
class PackMetaReaderTest {

  private static final int ALL = Integer.MAX_VALUE;

  private static Outcome<PackMeta> read(String json) {
    return PackMetaReader.read(Jx.parse(json));
  }

  private static String meta(String min, String max) {
    return "{\"pack\":{\"description\":\"d\",\"min_format\":"
        + min
        + ",\"max_format\":"
        + max
        + "}}";
  }

  private static PackMeta metaOf(String json) {
    return read(json).value().orElseThrow();
  }

  @Test
  void aBuiltInPackMetaReadsWithoutDiagnostic() {
    // spec: R2 (shape of the three built-in pack.mcmeta files)
    String json =
        "{\"features\":{\"enabled\":[\"minecraft:trade_rebalance\"]},\"pack\":{\"description\":"
            + "{\"translate\":\"dataPack.trade_rebalance.description\"},\"max_format\":121,"
            + "\"min_format\":121}}";
    Outcome<PackMeta> outcome = read(json);
    assertThat(outcome.diagnostics()).isEmpty();
    PackMeta pack = outcome.value().orElseThrow();
    assertThat(pack.compatibility(PackType.DATA)).isEqualTo(PackCompatibility.COMPATIBLE);
    assertThat(pack.features())
        .isEqualTo(FeatureFlags.of(List.of(Identifier.parse("minecraft:trade_rebalance"))));
    assertThat(pack.filters()).isEmpty();
    assertThat(pack.overlays()).isEmpty();
    JsonObject description = (JsonObject) pack.description().orElseThrow();
    assertThat(((JsonString) description.get("translate").orElseThrow()).value())
        .isEqualTo("dataPack.trade_rebalance.description");
  }

  @Test
  void provisionalAStringDescriptionIsKeptAsWritten() {
    // spec: R4, vector VC16
    PackMeta pack =
        metaOf("{\"pack\":{\"description\":\"x\",\"min_format\":121,\"max_format\":121}}");
    assertThat(((JsonString) pack.description().orElseThrow()).value()).isEqualTo("x");
  }

  @ParameterizedTest
  @CsvSource({
    "121, 121, COMPATIBLE",
    "'[121, 0]', '[121, 0]', COMPATIBLE",
    "120, 121, COMPATIBLE",
    "'[121, 1]', 121, TOO_NEW",
    "100, 120, TOO_OLD",
    "'[120, 5]', '[121, 0]', COMPATIBLE",
    "122, 121, BROKEN",
    "'[121]', '[121]', COMPATIBLE",
    "'[121, 0]', 121, COMPATIBLE",
    "121, '[121, 5]', COMPATIBLE",
    "'[121, 0]', '[120, 9]', BROKEN",
    "'[122]', '[130]', TOO_NEW",
    "1, '[120, 2147483647]', TOO_OLD"
  })
  void provisionalTheTableOfFormatRangesIsClassified(String min, String max, String expected) {
    // spec: table "pack.mcmeta (S, provisoires ; cible data 121.0)", R5
    Outcome<PackMeta> outcome = read(meta(min, max));
    assertThat(outcome.value().orElseThrow().compatibility(PackType.DATA))
        .isEqualTo(PackCompatibility.valueOf(expected));
  }

  @Test
  void provisionalAnIntegerMinimumMeansMinorZero() {
    // spec: R5 (N or [N] is (N, 0) for min_format)
    PackFormatRange range = metaOf(meta("121", "121")).formats().orElseThrow();
    assertThat(range.min()).isEqualTo(new PackFormat(121, 0));
    PackFormatRange list = metaOf(meta("[121]", "[121]")).formats().orElseThrow();
    assertThat(list.min()).isEqualTo(new PackFormat(121, 0));
  }

  @Test
  void provisionalAnIntegerMaximumCoversEveryMinor() {
    // spec: R5 (N or [N] is (N, 2147483647) for max_format)
    assertThat(metaOf(meta("121", "121")).formats().orElseThrow().max())
        .isEqualTo(new PackFormat(121, ALL));
    assertThat(metaOf(meta("121", "[121]")).formats().orElseThrow().max())
        .isEqualTo(new PackFormat(121, ALL));
  }

  @Test
  void provisionalAMajorMinorPairIsExact() {
    // spec: R5 ([major, minor] is exact)
    PackFormatRange range = metaOf(meta("[120, 5]", "[121, 0]")).formats().orElseThrow();
    assertThat(range.min()).isEqualTo(new PackFormat(120, 5));
    assertThat(range.max()).isEqualTo(new PackFormat(121, 0));
  }

  @Test
  void provisionalAnInvertedRangeIsBrokenWithoutDiagnostic() {
    // spec: R5, PackMetaReader.read Javadoc ("broken without diagnostic")
    Outcome<PackMeta> outcome = read(meta("122", "121"));
    assertThat(outcome.diagnostics()).isEmpty();
    PackMeta pack = outcome.value().orElseThrow();
    assertThat(pack.formats()).isPresent();
    assertThat(pack.compatibility(PackType.DATA)).isEqualTo(PackCompatibility.BROKEN);
  }

  @Test
  void provisionalOnlyPackFormatIsBrokenWithAMissingFieldPerBound() {
    // spec: R5 (pack_format alone), table row "absent", PackMetaReader.read Javadoc
    Outcome<PackMeta> outcome = read("{\"pack\":{\"description\":\"d\",\"pack_format\":121}}");
    PackMeta pack = outcome.value().orElseThrow();
    assertThat(pack.formats()).isEmpty();
    assertThat(pack.compatibility(PackType.DATA)).isEqualTo(PackCompatibility.BROKEN);
    assertThat(Diags.summary(outcome))
        .containsExactlyInAnyOrder(
            "CODEC_MISSING_FIELD@$.pack.min_format", "CODEC_MISSING_FIELD@$.pack.max_format");
    assertThat(outcome.hasErrors()).isTrue();
  }

  @Test
  void provisionalMistypedBoundsBreakThePackWithATypeMismatchAtTheField() {
    // spec: R4 ("champ connu de mauvais type"), R5
    for (String bad : List.of("\"121\"", "true", "null", "{}")) {
      Outcome<PackMeta> outcome = read(meta(bad, "121"));
      PackMeta pack = outcome.value().orElseThrow();
      assertThat(pack.formats()).as(bad).isEmpty();
      assertThat(pack.compatibility(PackType.DATA)).as(bad).isEqualTo(PackCompatibility.BROKEN);
      Diags.assertSummary(outcome, "CODEC_TYPE_MISMATCH@$.pack.min_format");
      Outcome<PackMeta> maxBad = read(meta("121", bad));
      assertThat(maxBad.value().orElseThrow().formats()).as(bad).isEmpty();
      Diags.assertSummary(maxBad, "CODEC_TYPE_MISMATCH@$.pack.max_format");
    }
  }

  @Test
  void provisionalAMistypedPackFormatBreaksThePack() {
    // spec: R4: a known field of a wrong type gives CODEC_TYPE_MISMATCH at its path
    Outcome<PackMeta> outcome =
        read(
            "{\"pack\":{\"description\":\"d\",\"pack_format\":\"x\",\"min_format\":121,\"max_format\":121}}");
    Diags.assertSummary(outcome, "CODEC_TYPE_MISMATCH@$.pack.pack_format");
    assertThat(outcome.value().orElseThrow().compatibility(PackType.DATA))
        .isEqualTo(PackCompatibility.BROKEN);
  }

  @Test
  void provisionalUnknownFieldsAtEveryLevelAreIgnoredWithoutDiagnostic() {
    // spec: VC16, R4, R63 c
    String json =
        "{\"pack\":{\"description\":\"x\",\"min_format\":121,\"max_format\":121,\"zzz\":1},"
            + "\"zzz\":{},\"features\":{\"enabled\":[],\"zzz\":[1]},"
            + "\"filter\":{\"block\":[{\"namespace\":\"a\",\"zzz\":0}],\"zzz\":null},"
            + "\"overlays\":{\"entries\":[{\"directory\":\"ov\",\"min_format\":121,\"max_format\":121,"
            + "\"zzz\":true}],\"zzz\":1}}";
    Outcome<PackMeta> outcome = read(json);
    assertThat(outcome.diagnostics()).isEmpty();
    PackMeta pack = outcome.value().orElseThrow();
    assertThat(pack.compatibility(PackType.DATA)).isEqualTo(PackCompatibility.COMPATIBLE);
    assertThat(pack.filters()).hasSize(1);
    assertThat(pack.overlays()).hasSize(1);
  }

  @Test
  void provisionalAFileWithoutAPackObjectIsNotAPack() {
    // spec: R7, PackMetaReader.read Javadoc
    for (String json :
        List.of(
            "[]", "{}", "{\"pack\":1}", "{\"pack\":[]}", "{\"pack\":null}", "1", "\"x\"", "null")) {
      Outcome<PackMeta> outcome = read(json);
      assertThat(outcome.value()).as(json).isEmpty();
      assertThat(Diags.codes(outcome)).as(json).containsExactly(DiagnosticCode.PACK_INVALID);
      assertThat(outcome.hasErrors()).as(json).isFalse();
    }
  }

  @Test
  void provisionalFeaturesEnabledListsTheRequiredFlags() {
    // spec: R4, R44, R66 b
    PackMeta pack =
        metaOf(
            "{\"pack\":{\"min_format\":121,\"max_format\":121},"
                + "\"features\":{\"enabled\":[\"minecraft:trade_rebalance\",\"x:y\"]}}");
    assertThat(pack.features())
        .isEqualTo(
            FeatureFlags.of(
                List.of(Identifier.parse("x:y"), Identifier.parse("minecraft:trade_rebalance"))));
  }

  @Test
  void provisionalWithoutFeaturesThePackRequiresNoFlag() {
    // spec: R66 b ("liste vide sans ce champ"), VF4
    PackMeta pack = metaOf(meta("121", "121"));
    assertThat(pack.features().flags()).isEmpty();
  }

  @Test
  void provisionalABadFeaturesFieldBreaksThePack() {
    // spec: R4 (wrong type), PackMetaReader.read Javadoc
    Outcome<PackMeta> notObject =
        read("{\"pack\":{\"min_format\":121,\"max_format\":121},\"features\":[]}");
    Diags.assertSummary(notObject, "CODEC_TYPE_MISMATCH@$.features");
    assertThat(notObject.value().orElseThrow().compatibility(PackType.DATA))
        .isEqualTo(PackCompatibility.BROKEN);
    Outcome<PackMeta> notList =
        read(
            "{\"pack\":{\"min_format\":121,\"max_format\":121},\"features\":{\"enabled\":\"x:y\"}}");
    Diags.assertSummary(notList, "CODEC_TYPE_MISMATCH@$.features.enabled");
    Outcome<PackMeta> notText =
        read("{\"pack\":{\"min_format\":121,\"max_format\":121},\"features\":{\"enabled\":[5]}}");
    Diags.assertSummary(notText, "CODEC_TYPE_MISMATCH@$.features.enabled[0]");
    assertThat(notText.value().orElseThrow().compatibility(PackType.DATA))
        .isEqualTo(PackCompatibility.BROKEN);
  }

  @Test
  void provisionalAnInvalidFlagIdentifierBreaksThePackWithOneErrorAtTheEntry() {
    // spec: PackMetaReader.read Javadoc (a features.enabled entry); the code is not pinned down
    Outcome<PackMeta> outcome =
        read(
            "{\"pack\":{\"min_format\":121,\"max_format\":121},"
                + "\"features\":{\"enabled\":[\"minecraft:ok\",\"Bad Id\"]}}");
    assertThat(outcome.diagnostics()).hasSize(1);
    assertThat(outcome.diagnostics().get(0).path().toString()).isEqualTo("$.features.enabled[1]");
    assertThat(outcome.diagnostics().get(0).code())
        .isIn(DiagnosticCode.CODEC_TYPE_MISMATCH, DiagnosticCode.CODEC_INVALID_IDENTIFIER);
    assertThat(outcome.value().orElseThrow().compatibility(PackType.DATA))
        .isEqualTo(PackCompatibility.BROKEN);
  }

  @Test
  void provisionalAllErrorsOfTheFileAreCollectedInTextOrder() {
    // spec: PackMetaReader.read Javadoc, R62 g
    Outcome<PackMeta> outcome =
        read(
            "{\"pack\":{\"min_format\":\"x\",\"max_format\":true},"
                + "\"features\":{\"enabled\":\"y\"}}");
    Diags.assertSummary(
        outcome,
        "CODEC_TYPE_MISMATCH@$.pack.min_format",
        "CODEC_TYPE_MISMATCH@$.pack.max_format",
        "CODEC_TYPE_MISMATCH@$.features.enabled");
  }

  @Test
  void provisionalFilterRulesKeepFileOrderAndFillAbsentExpressions() {
    // spec: R4 (filter.block: list of {namespace?, path?}), R47
    PackMeta pack =
        metaOf(
            "{\"pack\":{\"min_format\":121,\"max_format\":121},\"filter\":{\"block\":["
                + "{\"namespace\":\"minecraft\",\"path\":\"loot_table/.*\"},{\"path\":\"x\"},"
                + "{\"namespace\":\"y\"},{}]}}");
    List<FilterRule> rules = pack.filters();
    assertThat(rules).hasSize(4);
    assertThat(rules.get(0).namespacePattern()).isEqualTo("minecraft");
    assertThat(rules.get(0).pathPattern()).isEqualTo("loot_table/.*");
    assertThat(rules.get(1).namespacePattern()).isEqualTo(FilterRule.MATCH_ALL);
    assertThat(rules.get(1).pathPattern()).isEqualTo("x");
    assertThat(rules.get(2).namespacePattern()).isEqualTo("y");
    assertThat(rules.get(2).pathPattern()).isEqualTo(FilterRule.MATCH_ALL);
    assertThat(rules.get(3).namespacePattern()).isEqualTo(FilterRule.MATCH_ALL);
    assertThat(rules.get(3).pathPattern()).isEqualTo(FilterRule.MATCH_ALL);
  }

  @Test
  void provisionalAnInvalidFilterExpressionBreaksThePackAtTheField() {
    // spec: PackMetaReader.read Javadoc (not a valid regular expression); the code is not pinned
    // down
    Outcome<PackMeta> outcome =
        read(
            "{\"pack\":{\"min_format\":121,\"max_format\":121},\"filter\":{\"block\":[{\"path\":\"(\"}]}}");
    assertThat(outcome.diagnostics()).hasSize(1);
    assertThat(outcome.diagnostics().get(0).path().toString()).isEqualTo("$.filter.block[0].path");
    assertThat(outcome.hasErrors()).isTrue();
    assertThat(outcome.value().orElseThrow().compatibility(PackType.DATA))
        .isEqualTo(PackCompatibility.BROKEN);
  }

  @Test
  void provisionalOverlayEntriesKeepFileOrderAndTheirRanges() {
    // spec: R4, R48
    PackMeta pack =
        metaOf(
            "{\"pack\":{\"min_format\":121,\"max_format\":121},\"overlays\":{\"entries\":["
                + "{\"directory\":\"ov1\",\"min_format\":121,\"max_format\":121},"
                + "{\"directory\":\"my-ov.2_x\",\"min_format\":[122,0],\"max_format\":122}]}}");
    List<OverlayEntry> overlays = pack.overlays();
    assertThat(overlays).hasSize(2);
    assertThat(overlays.get(0).directory()).isEqualTo("ov1");
    assertThat(overlays.get(0).formats())
        .isEqualTo(new PackFormatRange(new PackFormat(121, 0), new PackFormat(121, ALL)));
    assertThat(overlays.get(1).directory()).isEqualTo("my-ov.2_x");
    assertThat(overlays.get(1).formats())
        .isEqualTo(new PackFormatRange(new PackFormat(122, 0), new PackFormat(122, ALL)));
  }

  @Test
  void provisionalAnOverlayDirectoryOutsideTheAlphabetBreaksThePack() {
    // spec: R48 (a-z 0-9 _ - .), PackMetaReader.read Javadoc (CODEC_TYPE_MISMATCH, provisional)
    for (String directory : List.of("Bad", "has space", "a/b", "\351")) {
      Outcome<PackMeta> outcome =
          read(
              "{\"pack\":{\"min_format\":121,\"max_format\":121},\"overlays\":{\"entries\":"
                  + "[{\"directory\":\""
                  + directory
                  + "\",\"min_format\":121,\"max_format\":121}]}}");
      Diags.assertSummary(outcome, "CODEC_TYPE_MISMATCH@$.overlays.entries[0].directory");
      assertThat(outcome.value().orElseThrow().compatibility(PackType.DATA))
          .as(directory)
          .isEqualTo(PackCompatibility.BROKEN);
    }
  }
}

package dev.shoko.minecraft.data.tag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Expect;
import dev.shoko.minecraft.data.testing.TagLayers;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R33 to R38, table "Tags" and probes P6, P7, P8 (merge and resolution)
class TagResolverTest {

  private static final String[] ABCD = {"a", "b", "c", "d"};

  private static Optional<List<String>> members(Outcome<TagResolution> outcome, String tag) {
    return outcome
        .value()
        .orElseThrow()
        .members(Identifier.parse(tag))
        .map(list -> list.stream().map(Identifier::toString).toList());
  }

  private static List<String> texts(Collection<Identifier> ids) {
    return ids.stream().map(Identifier::toString).toList();
  }

  private static List<String> names(String... expected) {
    return Arrays.stream(expected).map(e -> "minecraft:" + e).toList();
  }

  @Test
  void provisionalAReferenceIsReplacedByTheResolvedTagAndDuplicatesKeepTheirFirstPlace() {
    // spec: table "Tags" row 1, R35
    Outcome<TagResolution> outcome =
        TagLayers.create().add("t", "P1", "a", "#u").add("u", "P1", "c", "a").resolve(ABCD);
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(members(outcome, "t")).hasValue(names("a", "c"));
    assertThat(members(outcome, "u")).hasValue(names("c", "a"));
  }

  @Test
  void provisionalEntriesOfTwoPacksAreAddedInPriorityOrder() {
    // spec: table "Tags" row 2, R34
    Outcome<TagResolution> outcome =
        TagLayers.create().add("t", "P1", "b").add("t", "P2", "a").resolve(ABCD);
    assertThat(members(outcome, "t")).hasValue(names("b", "a"));
    assertThat(outcome.diagnostics()).isEmpty();
  }

  @Test
  void provisionalAReplacingFileDropsWhatLowerPacksAccumulated() {
    // spec: table "Tags" row 3, R34, P5
    Outcome<TagResolution> outcome =
        TagLayers.create().add("t", "P1", "b").replace("t", "P2", "a").resolve(ABCD);
    assertThat(members(outcome, "t")).hasValue(names("a"));
  }

  @Test
  void provisionalAReplacingFileKeepsWhatHigherPacksAdd() {
    // spec: R34 (a layer with replace drops everything accumulated before it)
    Outcome<TagResolution> outcome =
        TagLayers.create()
            .add("t", "P1", "a")
            .replace("t", "P2", "b")
            .add("t", "P3", "c")
            .resolve(ABCD);
    assertThat(members(outcome, "t")).hasValue(names("b", "c"));
  }

  @Test
  void provisionalASecondReplacementDropsTheFirstOne() {
    // spec: R34
    Outcome<TagResolution> outcome =
        TagLayers.create().replace("t", "P1", "a").replace("t", "P2", "b").resolve(ABCD);
    assertThat(members(outcome, "t")).hasValue(names("b"));
  }

  @Test
  void provisionalEntriesDroppedByAReplacementAreNotCheckedAgainstTheUniverse() {
    // spec: R34, R36: only the merged entries are resolved
    Outcome<TagResolution> outcome =
        TagLayers.create().add("t", "P1", "missing").replace("t", "P2", "a").resolve(ABCD);
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(members(outcome, "t")).hasValue(names("a"));
  }

  @Test
  void provisionalAnEmptyTagExistsAndDiffersFromAnAbsentOne() {
    // spec: table "Tags" row 8, R38
    Outcome<TagResolution> outcome = TagLayers.create().add("t", "P1").resolve(ABCD);
    TagResolution result = outcome.value().orElseThrow();
    assertThat(result.members(Identifier.parse("t"))).hasValue(List.of());
    assertThat(texts(result.tags())).containsExactly("minecraft:t");
    assertThat(result.failed()).isEmpty();
    assertThat(result.members(Identifier.parse("absent"))).isEmpty();
  }

  @Test
  void provisionalAnEmptyTagCanBeReferencedAndContributesNothing() {
    // spec: R38
    Outcome<TagResolution> outcome =
        TagLayers.create().add("t", "P1", "a", "#e", "b").add("e", "P1").resolve(ABCD);
    assertThat(members(outcome, "t")).hasValue(names("a", "b"));
  }

  @Test
  void provisionalFunctionTagsKeepTheOrderOfTheMergedFileAcrossPacks() {
    // spec: P6, R35: t = [f2, f1, #u, f2], u = [f3, f1]; a second pack adds [f1, f3] to t
    Outcome<TagResolution> outcome =
        TagLayers.create()
            .add("t", "P1", "f2", "f1", "#u", "f2")
            .add("u", "P1", "f3", "f1")
            .add("t", "P2", "f1", "f3")
            .resolve("f1", "f2", "f3");
    assertThat(members(outcome, "t")).hasValue(names("f2", "f1", "f3"));
    assertThat(outcome.diagnostics()).isEmpty();
  }

  @Test
  void provisionalReferencesAreResolvedAfterTheMergeAcrossPacks() {
    // spec: R34 ("la fusion porte sur les entrees brutes, avant resolution")
    Outcome<TagResolution> outcome =
        TagLayers.create()
            .add("t", "P1", "#u")
            .add("u", "P1", "a")
            .add("u", "P2", "b")
            .replace("u", "P3", "c", "d")
            .resolve(ABCD);
    assertThat(members(outcome, "t")).hasValue(names("c", "d"));
  }

  @Test
  void provisionalATagMayBeDefinedOnlyInAHigherPackAndReferencedFromALowerOne() {
    // spec: R33, R34
    Outcome<TagResolution> outcome =
        TagLayers.create().add("t", "P1", "#u").add("u", "P2", "a").resolve(ABCD);
    assertThat(members(outcome, "t")).hasValue(names("a"));
  }

  @Test
  void provisionalAMissingEntryAddedByAHigherPackFailsTheTagAndNamesThatPackFile() {
    // spec: R36 (the diagnostic points at the file of the layer that holds the entry)
    Outcome<TagResolution> outcome =
        TagLayers.create().add("t", "P1", "a", "b").add("t", "P2", "x").resolve(ABCD);
    Diagnostic only = outcome.diagnostics().get(0);
    assertThat(outcome.diagnostics()).hasSize(1);
    assertThat(only.origin()).isEqualTo(TagLayers.origin("P2", "t"));
    assertThat(only.path().toString()).isEqualTo("$.values[0]");
  }

  @Test
  void provisionalTheDiagnosticIndexIsThePositionInTheFileOfTheLayer() {
    // spec: TagResolver.resolve Javadoc ($.values[i], i in that file)
    Outcome<TagResolution> outcome =
        TagLayers.create().add("t", "P1", "a", "b", "c").add("t", "P2", "d", "x").resolve(ABCD);
    Diags.assertSummary(outcome, "CODEC_UNRESOLVED_REFERENCE@$.values[1]");
    assertThat(outcome.diagnostics().get(0).origin()).isEqualTo(TagLayers.origin("P2", "t"));
  }

  @Test
  void provisionalTheUniverseDecidesWhetherAnElementExists() {
    // spec: R33
    Outcome<TagResolution> outcome = TagLayers.create().add("t", "P1", "a", "b").resolve("a");
    assertThat(members(outcome, "t")).isEmpty();
    Outcome<TagResolution> full = TagLayers.create().add("t", "P1", "a", "b").resolve("a", "b");
    assertThat(members(full, "t")).hasValue(names("a", "b"));
  }

  @Test
  void provisionalAnEmptyUniverseFailsEveryTagWithRequiredEntries() {
    Outcome<TagResolution> outcome =
        TagLayers.create().add("t", "P1", "a").add("e", "P1").resolve();
    TagResolution result = outcome.value().orElseThrow();
    assertThat(texts(result.failed())).containsExactly("minecraft:t");
    assertThat(texts(result.tags())).containsExactly("minecraft:e");
  }

  @Test
  void provisionalTagsAndFailuresAreListedInCodePointOrder() {
    // spec: R30, TagResolution.tags and failed Javadoc
    Outcome<TagResolution> outcome =
        TagLayers.create()
            .add("zeta", "P", "a")
            .add("alpha", "P", "b")
            .add("mid/x", "P", "c")
            .add("mid_y", "P", "d")
            .add("z_fail", "P", "gone")
            .add("a_fail", "P", "gone")
            .resolve(ABCD);
    TagResolution result = outcome.value().orElseThrow();
    assertThat(texts(result.tags()))
        .containsExactly("minecraft:alpha", "minecraft:mid/x", "minecraft:mid_y", "minecraft:zeta");
    assertThat(texts(result.failed())).containsExactly("minecraft:a_fail", "minecraft:z_fail");
  }

  @Test
  void provisionalNoTagIsLostEitherResolvedOrFailed() {
    // spec: R36 ("existe ou echoue"): every tag of the input is in exactly one of the two lists
    Outcome<TagResolution> outcome =
        TagLayers.create()
            .add("a1", "P", "a")
            .add("a2", "P", "x")
            .add("a3", "P")
            .add("a4", "P", "#a2")
            .add("a5", "P", "#a5")
            .resolve(ABCD);
    TagResolution result = outcome.value().orElseThrow();
    assertThat(texts(result.tags())).containsExactly("minecraft:a1", "minecraft:a3");
    assertThat(texts(result.failed()))
        .containsExactly("minecraft:a2", "minecraft:a4", "minecraft:a5");
  }

  @Test
  void provisionalNamespacedTagsAndElementsAreSupported() {
    // spec: R9, R22
    Outcome<TagResolution> outcome =
        TagLayers.create()
            .add("sonde:t", "P", "sonde:a", "#sonde:u", "b")
            .add("sonde:u", "P", "sonde:c")
            .resolve("sonde:a", "sonde:c", "b");
    assertThat(members(outcome, "sonde:t")).hasValue(List.of("sonde:a", "sonde:c", "minecraft:b"));
  }

  @Test
  void provisionalTheResultIsPresentEvenWhenEveryTagFails() {
    // spec: TagResolver.resolve Javadoc ("The result is always present")
    Outcome<TagResolution> outcome = TagLayers.create().add("t", "P", "gone").resolve(ABCD);
    assertThat(outcome.value()).isPresent();
    assertThat(TagLayers.create().resolve(ABCD).value().orElseThrow().tags()).isEmpty();
  }

  @Test
  void theResolvedListIsUnmodifiable() {
    Outcome<TagResolution> outcome = TagLayers.create().add("t", "P", "a").resolve(ABCD);
    List<Identifier> exposed =
        outcome.value().orElseThrow().members(Identifier.parse("t")).orElseThrow();
    Expect.unmodifiable(() -> exposed.add(Identifier.parse("b")));
  }
}

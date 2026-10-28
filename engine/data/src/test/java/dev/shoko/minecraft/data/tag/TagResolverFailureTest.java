package dev.shoko.minecraft.data.tag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.TagLayers;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R36, R37, probes P7 and P8 (failures, optional entries and cycles)
class TagResolverFailureTest {
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
  void provisionalAMissingRequiredElementFailsTheWholeTag() {
    // spec: table "Tags" row 4, R36
    Outcome<TagResolution> outcome = TagLayers.create().add("t", "P1", "a", "x").resolve(ABCD);
    TagResolution result = outcome.value().orElseThrow();
    assertThat(result.members(Identifier.parse("t"))).isEmpty();
    assertThat(texts(result.failed())).containsExactly("minecraft:t");
    assertThat(result.tags()).isEmpty();
    Diags.assertSummary(outcome, "CODEC_UNRESOLVED_REFERENCE@$.values[1]");
    assertThat(outcome.diagnostics().get(0).origin()).isEqualTo(TagLayers.origin("P1", "t"));
    assertThat(outcome.hasErrors()).isTrue();
  }

  @Test
  void provisionalEveryMissingReferenceGetsItsOwnDiagnostic() {
    // spec: R36 ("le diagnostic liste chaque reference manquante")
    Outcome<TagResolution> outcome =
        TagLayers.create().add("t", "P1", "x", "a", "y", "#nope", "z").resolve(ABCD);
    Diags.assertSummary(
        outcome,
        "CODEC_UNRESOLVED_REFERENCE@$.values[0]",
        "CODEC_UNRESOLVED_REFERENCE@$.values[2]",
        "CODEC_UNRESOLVED_REFERENCE@$.values[3]",
        "CODEC_UNRESOLVED_REFERENCE@$.values[4]");
  }

  @Test
  void provisionalAnOptionalMissingEntryIsSimplySkipped() {
    // spec: table "Tags" row 5, R36, P7
    Outcome<TagResolution> outcome = TagLayers.create().add("t", "P1", "a", "x?").resolve(ABCD);
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(members(outcome, "t")).hasValue(names("a"));
    assertThat(outcome.value().orElseThrow().failed()).isEmpty();
  }

  @Test
  void provisionalAnOptionalMissingTagIsSkippedToo() {
    // spec: R36 (required false), P7
    Outcome<TagResolution> outcome = TagLayers.create().add("t", "P1", "a", "#nope?").resolve(ABCD);
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(members(outcome, "t")).hasValue(names("a"));
  }

  @Test
  void provisionalAnOptionalReferenceToAnExistingTagIsExpanded() {
    // spec: R32, R36
    Outcome<TagResolution> outcome =
        TagLayers.create().add("t", "P1", "#u?", "d").add("u", "P1", "a", "b").resolve(ABCD);
    assertThat(members(outcome, "t")).hasValue(names("a", "b", "d"));
  }

  @Test
  void provisionalAnOptionalReferenceToAFailedTagIsSkipped() {
    // spec: R36 (a failed tag does not exist)
    Outcome<TagResolution> outcome =
        TagLayers.create().add("t", "P1", "a", "#v?").add("v", "P1", "x").resolve(ABCD);
    assertThat(members(outcome, "t")).hasValue(names("a"));
    assertThat(members(outcome, "v")).isEmpty();
    Diags.assertSummary(outcome, "CODEC_UNRESOLVED_REFERENCE@$.values[0]");
  }

  @Test
  void provisionalATagRequiringAFailedTagFailsInTurn() {
    // spec: table "Tags" row 6, R36
    Outcome<TagResolution> outcome =
        TagLayers.create().add("t", "P1", "#v").add("v", "P1", "x").resolve(ABCD);
    TagResolution result = outcome.value().orElseThrow();
    assertThat(texts(result.failed())).containsExactly("minecraft:t", "minecraft:v");
    assertThat(result.tags()).isEmpty();
    assertThat(Diags.summary(outcome))
        .containsExactlyInAnyOrder(
            "CODEC_UNRESOLVED_REFERENCE@$.values[0]", "CODEC_UNRESOLVED_REFERENCE@$.values[0]");
    List<String> files =
        outcome.diagnostics().stream().map(d -> d.origin().path()).sorted().toList();
    assertThat(files)
        .containsExactly("data/minecraft/tags/block/t.json", "data/minecraft/tags/block/v.json");
  }

  @Test
  void provisionalAFailureTravelsThroughAChainOfTags() {
    // spec: R36
    Outcome<TagResolution> outcome =
        TagLayers.create()
            .add("a1", "P", "#a2")
            .add("a2", "P", "#a3")
            .add("a3", "P", "#a4")
            .add("a4", "P", "gone")
            .add("ok", "P", "a")
            .resolve(ABCD);
    TagResolution result = outcome.value().orElseThrow();
    assertThat(texts(result.failed()))
        .containsExactly("minecraft:a1", "minecraft:a2", "minecraft:a3", "minecraft:a4");
    assertThat(texts(result.tags())).containsExactly("minecraft:ok");
    assertThat(outcome.diagnostics()).hasSize(4);
  }

  @Test
  void provisionalACycleMakesEveryTagOfTheCycleFail() {
    // spec: table "Tags" row 7, R37, P8
    Outcome<TagResolution> outcome =
        TagLayers.create().add("t", "P1", "#u").add("u", "P1", "#t", "a").resolve(ABCD);
    TagResolution result = outcome.value().orElseThrow();
    assertThat(texts(result.failed())).containsExactly("minecraft:t", "minecraft:u");
    assertThat(result.tags()).isEmpty();
    assertThat(result.members(Identifier.parse("t"))).isEmpty();
    assertThat(result.members(Identifier.parse("u"))).isEmpty();
    assertThat(outcome.diagnostics())
        .hasSize(2)
        .allSatisfy(d -> assertThat(d.code()).isEqualTo(DiagnosticCode.CODEC_UNRESOLVED_REFERENCE));
    assertThat(outcome.diagnostics())
        .extracting(d -> d.origin().path() + "@" + d.path())
        .containsExactlyInAnyOrder(
            "data/minecraft/tags/block/t.json@$.values[0]",
            "data/minecraft/tags/block/u.json@$.values[0]");
  }

  @Test
  void provisionalATagThatReferencesItselfFails() {
    // spec: R37
    Outcome<TagResolution> outcome = TagLayers.create().add("t", "P1", "a", "#t").resolve(ABCD);
    assertThat(texts(outcome.value().orElseThrow().failed())).containsExactly("minecraft:t");
    Diags.assertSummary(outcome, "CODEC_UNRESOLVED_REFERENCE@$.values[1]");
  }

  @Test
  void provisionalALongerCycleAndItsDependentsFailWhileOthersLoad() {
    // spec: R37 ("ainsi que les tags qui en dependent")
    Outcome<TagResolution> outcome =
        TagLayers.create()
            .add("c1", "P", "#c2")
            .add("c2", "P", "#c3")
            .add("c3", "P", "#c1", "a")
            .add("user", "P", "b", "#c2")
            .add("fine", "P", "c")
            .resolve(ABCD);
    TagResolution result = outcome.value().orElseThrow();
    assertThat(texts(result.failed()))
        .containsExactly("minecraft:c1", "minecraft:c2", "minecraft:c3", "minecraft:user");
    assertThat(members(outcome, "fine")).hasValue(names("c"));
  }

  @Test
  void provisionalADiamondIsNotACycle() {
    // spec: R37 (only a loop of references fails)
    Outcome<TagResolution> outcome =
        TagLayers.create()
            .add("top", "P", "#left", "#right")
            .add("left", "P", "#base")
            .add("right", "P", "#base", "d")
            .add("base", "P", "a")
            .resolve(ABCD);
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(members(outcome, "top")).hasValue(names("a", "d"));
  }
}

package dev.shoko.minecraft.data.loader;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.tag.TagResolution;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.MiniData;
import dev.shoko.minecraft.data.testing.Packs;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R9, R16, R31 to R38, R54, K2 (tags on miniature datapacks: the
// universe of each registry, merging across packs, failures)
class DatapackLoaderTagsTest {

  private static String tagFile(String registry, String name) {
    return "data/minecraft/tags/" + registry + "/" + name + ".json";
  }

  private static String values(String... entries) {
    return "{\"values\":[" + String.join(",", entries) + "]}";
  }

  private static String quote(String text) {
    return "\"" + text + "\"";
  }

  private static TagResolution tags(Outcome<LoadedData> outcome, String registry) {
    return outcome.value().orElseThrow().tags(Identifier.parse(registry)).orElseThrow();
  }

  @Test
  void provisionalATagOfAStaticRegistryIsResolvedAgainstTheEntriesOfTheReport() {
    // spec: R33 (universe of a static registry: its entries), R35
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit(
                "vanilla",
                tagFile("block", "logs"),
                values(quote("minecraft:oak_log"), quote("birch_log")),
                tagFile("block", "all"),
                values(quote("#logs"), quote("minecraft:stone"))));
    assertThat(outcome.hasErrors()).isFalse();
    assertThat(
            MiniData.tagMembers(outcome.value().orElseThrow(), "minecraft:block", "minecraft:logs"))
        .containsExactly("minecraft:oak_log", "minecraft:birch_log");
    assertThat(
            MiniData.tagMembers(outcome.value().orElseThrow(), "minecraft:block", "minecraft:all"))
        .containsExactly("minecraft:oak_log", "minecraft:birch_log", "minecraft:stone");
  }

  @Test
  void provisionalAStaticEntryThatIsNotInTheReportFailsTheTag() {
    // spec: R36
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit(
                "vanilla",
                tagFile("block", "bad"),
                values(quote("minecraft:stone"), quote("minecraft:nope"))));
    TagResolution block = tags(outcome, "minecraft:block");
    assertThat(block.failed().stream().map(Identifier::toString)).containsExactly("minecraft:bad");
    assertThat(block.tags()).isEmpty();
    Diags.assertSummary(outcome, "CODEC_UNRESOLVED_REFERENCE@$.values[1]");
    assertThat(outcome.diagnostics().get(0).origin())
        .isEqualTo(new FileOrigin("vanilla", tagFile("block", "bad")));
  }

  @Test
  void provisionalTheUniverseOfAFileRegistryIsTheElementsAfterStackingAllPacks() {
    // spec: R33 (identifiers of the elements after stacking), K2
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit(
                "vanilla",
                "data/minecraft/loot_table/a.json",
                "{}",
                tagFile("loot_table", "t"),
                values(quote("minecraft:a"), quote("sonde:b"))),
            Packs.world("W", "data/sonde/loot_table/b.json", "{}"));
    assertThat(outcome.hasErrors()).isFalse();
    assertThat(
            MiniData.tagMembers(
                outcome.value().orElseThrow(), "minecraft:loot_table", "minecraft:t"))
        .containsExactly("minecraft:a", "sonde:b");
  }

  @Test
  void provisionalAnInvalidElementFileIsNotInTheUniverse() {
    // spec: R33, R54 (the element is absent as if not provided)
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit(
                "vanilla",
                "data/minecraft/loot_table/broken.json",
                "{",
                "data/minecraft/loot_table/fine.json",
                "{}",
                tagFile("loot_table", "t"),
                values(quote("minecraft:fine"), quote("minecraft:broken"))));
    TagResolution loot = tags(outcome, "minecraft:loot_table");
    assertThat(loot.failed().stream().map(Identifier::toString)).containsExactly("minecraft:t");
    assertThat(Diags.codes(outcome))
        .containsExactlyInAnyOrder(
            DiagnosticCode.JSON_SYNTAX, DiagnosticCode.CODEC_UNRESOLVED_REFERENCE);
  }

  @Test
  void provisionalTheUniverseOfFunctionTagsIsTheFunctionsLoaded() {
    // spec: R33, R10 (tags in tags/function)
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit(
                "vanilla",
                "data/sonde/function/f1.mcfunction",
                "say 1",
                "data/sonde/function/f2.mcfunction",
                "say 2",
                "data/sonde/tags/function/t.json",
                values(quote("sonde:f2"), quote("sonde:f1"), quote("sonde:f2"))));
    assertThat(outcome.hasErrors()).isFalse();
    assertThat(MiniData.tagMembers(outcome.value().orElseThrow(), "minecraft:function", "sonde:t"))
        .containsExactly("sonde:f2", "sonde:f1");
  }

  @Test
  void provisionalFunctionTagsKeepTheOrderOfTheMergedFilesAcrossPacks() {
    // spec: P6, R34, R35
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit(
                "vanilla",
                "data/sonde/function/f1.mcfunction",
                "say 1",
                "data/sonde/function/f2.mcfunction",
                "say 2",
                "data/sonde/function/f3.mcfunction",
                "say 3",
                "data/sonde/tags/function/t.json",
                values(quote("sonde:f2"), quote("sonde:f1"), quote("#sonde:u"), quote("sonde:f2")),
                "data/sonde/tags/function/u.json",
                values(quote("sonde:f3"), quote("sonde:f1"))),
            Packs.world(
                "W",
                "data/sonde/tags/function/t.json",
                values(quote("sonde:f1"), quote("sonde:f3"))));
    assertThat(outcome.hasErrors()).isFalse();
    assertThat(MiniData.tagMembers(outcome.value().orElseThrow(), "minecraft:function", "sonde:t"))
        .containsExactly("sonde:f2", "sonde:f1", "sonde:f3");
  }

  @Test
  void provisionalAHigherPackAddsEntriesToATagOfALowerPack() {
    // spec: R34, P5
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit("vanilla", tagFile("block", "logs"), values(quote("minecraft:oak_log"))),
            Packs.world("W", tagFile("block", "logs"), values(quote("minecraft:stone"))));
    assertThat(
            MiniData.tagMembers(outcome.value().orElseThrow(), "minecraft:block", "minecraft:logs"))
        .containsExactly("minecraft:oak_log", "minecraft:stone");
  }

  @Test
  void provisionalAReplacingTagFileDropsTheEntriesOfLowerPacks() {
    // spec: R34, P5, R43 (trade_rebalance replaces tags)
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit("vanilla", tagFile("block", "logs"), values(quote("minecraft:oak_log"))),
            Packs.world(
                "W",
                tagFile("block", "logs"),
                "{\"replace\":true,\"values\":[\"minecraft:stone\"]}"));
    assertThat(
            MiniData.tagMembers(outcome.value().orElseThrow(), "minecraft:block", "minecraft:logs"))
        .containsExactly("minecraft:stone");
  }

  @Test
  void provisionalTagsDefinedOnlyInAHigherPackAreVisibleToReferencesOfLowerPacks() {
    // spec: R34, R33
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit("vanilla", tagFile("block", "t"), values(quote("#u"))),
            Packs.world("W", tagFile("block", "u"), values(quote("minecraft:glass"))));
    assertThat(MiniData.tagMembers(outcome.value().orElseThrow(), "minecraft:block", "minecraft:t"))
        .containsExactly("minecraft:glass");
  }

  @Test
  void provisionalAnInvalidTagFileCountsAsAbsentAndDoesNotHideALowerPackVersion() {
    // spec: R32, R54
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit("vanilla", tagFile("block", "logs"), values(quote("minecraft:oak_log"))),
            Packs.world("W", tagFile("block", "logs"), "{\"values\":\"oops\"}"));
    assertThat(
            MiniData.tagMembers(outcome.value().orElseThrow(), "minecraft:block", "minecraft:logs"))
        .containsExactly("minecraft:oak_log");
    Diags.assertSummary(outcome, "CODEC_TYPE_MISMATCH@$.values");
    assertThat(outcome.diagnostics().get(0).origin())
        .isEqualTo(new FileOrigin("W", tagFile("block", "logs")));
  }

  @Test
  void provisionalATagFileWithInvalidJsonIsReportedOnceAndIsAbsent() {
    // spec: R54
    Outcome<LoadedData> outcome =
        MiniData.load(Packs.implicit("vanilla", tagFile("block", "bad"), "{\"values\":[,]}"));
    assertThat(Diags.codes(outcome)).containsExactly(DiagnosticCode.JSON_SYNTAX);
    assertThat(tags(outcome, "minecraft:block").members(Identifier.parse("bad"))).isEmpty();
    assertThat(tags(outcome, "minecraft:block").failed()).isEmpty();
  }

  @Test
  void provisionalAnEmptyTagExistsAndAnAbsentOneDoesNot() {
    // spec: R31, R38
    Outcome<LoadedData> outcome =
        MiniData.load(Packs.implicit("vanilla", tagFile("block", "empty"), values()));
    TagResolution block = tags(outcome, "minecraft:block");
    assertThat(block.members(Identifier.parse("empty"))).hasValue(List.of());
    assertThat(block.members(Identifier.parse("absent"))).isEmpty();
  }

  @Test
  void provisionalACycleBetweenTagFilesFailsBothAndLoadsTheOthers() {
    // spec: R37, P8 (the valid loot table in the same pack still loads)
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit(
                "vanilla",
                tagFile("block", "a"),
                values(quote("#b")),
                tagFile("block", "b"),
                values(quote("#a"), quote("minecraft:stone")),
                tagFile("block", "fine"),
                values(quote("minecraft:glass")),
                "data/sonde/loot_table/valide.json",
                "{}"));
    TagResolution block = tags(outcome, "minecraft:block");
    assertThat(block.failed().stream().map(Identifier::toString))
        .containsExactly("minecraft:a", "minecraft:b");
    assertThat(block.tags().stream().map(Identifier::toString)).containsExactly("minecraft:fine");
    assertThat(MiniData.elementIds(outcome.value().orElseThrow(), "minecraft:loot_table"))
        .containsExactly("sonde:valide");
    assertThat(outcome.diagnostics()).hasSize(2);
  }

  @Test
  void provisionalAStructureHasNoTags() {
    // spec: R16, LoadedData.tags Javadoc ("empty when the registry takes no tags")
    Outcome<LoadedData> outcome = MiniData.load(Packs.implicit("vanilla"));
    LoadedData data = outcome.value().orElseThrow();
    assertThat(data.tags(Identifier.parse("minecraft:structure"))).isEmpty();
    assertThat(data.tags(Identifier.parse("minecraft:unknown"))).isEmpty();
  }

  @Test
  void provisionalEveryRegistryThatTakesTagsHasAResolutionEvenWithoutTagFiles() {
    // spec: R16 (every registry of datapack.json accepts tags), LoadedData.tags
    LoadedData data = MiniData.load(Packs.implicit("vanilla")).value().orElseThrow();
    for (String registry :
        List.of(
            "minecraft:block",
            "minecraft:item",
            "minecraft:loot_table",
            "minecraft:villager_trade",
            "minecraft:worldgen/biome",
            "minecraft:dimension",
            "minecraft:function")) {
      TagResolution resolution = data.tags(Identifier.parse(registry)).orElseThrow();
      assertThat(resolution.tags()).as(registry).isEmpty();
      assertThat(resolution.failed()).as(registry).isEmpty();
    }
  }

  @Test
  void provisionalTagsOfDifferentRegistriesDoNotSeeEachOther() {
    // spec: R33 (a # reference targets a tag of the same registry)
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit(
                "vanilla",
                tagFile("item", "sticks"),
                values(quote("minecraft:stick")),
                tagFile("block", "t"),
                values(quote("#sticks"))));
    TagResolution block = tags(outcome, "minecraft:block");
    assertThat(block.failed().stream().map(Identifier::toString)).containsExactly("minecraft:t");
    assertThat(
            MiniData.tagMembers(
                outcome.value().orElseThrow(), "minecraft:item", "minecraft:sticks"))
        .containsExactly("minecraft:stick");
  }

  @Test
  void provisionalATagOfAWorldPackNamespaceIsResolved() {
    // spec: R9, R22
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit("vanilla"),
            Packs.world(
                "W",
                "data/sonde/tags/block/mine.json",
                values(quote("minecraft:stone"), quote("#minecraft:rocks")),
                "data/minecraft/tags/block/rocks.json",
                values(quote("minecraft:dirt"))));
    assertThat(MiniData.tagMembers(outcome.value().orElseThrow(), "minecraft:block", "sonde:mine"))
        .containsExactly("minecraft:stone", "minecraft:dirt");
  }
}

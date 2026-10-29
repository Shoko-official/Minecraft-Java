package dev.shoko.minecraft.data.loader;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.MiniData;
import dev.shoko.minecraft.data.testing.Packs;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading K2, R8, R9, R28 to R30, R41, R54: loading elements on miniature
// datapacks (files, tags and flags have their own test classes)
class DatapackLoaderTest {

  private static final String DIRT = "data/minecraft/loot_table/blocks/dirt.json";

  private static JsonNode element(LoadedData data, String registry, String id) {
    return data.elementRegistry(Identifier.parse(registry))
        .orElseThrow()
        .get(Identifier.parse(id))
        .orElseThrow();
  }

  private static LoadedData ok(Outcome<LoadedData> outcome) {
    assertThat(outcome.hasErrors()).as("%s", Diags.summary(outcome)).isFalse();
    return outcome.value().orElseThrow();
  }

  @Test
  void aCleanPackLoadsWithoutAnyErrorDiagnostic() {
    // spec: K2 (exit criterion: zero error diagnostics on valid data)
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit(
                "vanilla",
                DIRT,
                "{\"pools\":[]}",
                "data/minecraft/tags/block/logs.json",
                "{\"values\":[\"minecraft:oak_log\",\"minecraft:birch_log\"]}"));
    assertThat(outcome.hasErrors()).isFalse();
    assertThat(outcome.value()).isPresent();
  }

  @Test
  void provisionalAnElementIsKeptAsAJsonTreeWithItsOriginAndPath() {
    // spec: R8, R65 a (the tree keeps its origin), "Arbre JSON"
    LoadedData data = ok(MiniData.load(Packs.implicit("vanilla", DIRT, "{\"a\":{\"b\":[1,2.5]}}")));
    JsonNode tree = element(data, "minecraft:loot_table", "minecraft:blocks/dirt");
    assertThat(tree).isInstanceOf(JsonObject.class);
    assertThat(tree.origin()).isEqualTo(new FileOrigin("vanilla", DIRT));
    assertThat(tree.path().toString()).isEqualTo("$");
    JsonObject a = (JsonObject) ((JsonObject) tree).get("a").orElseThrow();
    assertThat(a.get("b").orElseThrow().path().toString()).isEqualTo("$.a.b");
    assertThat(a.get("b").orElseThrow().origin().pack()).isEqualTo("vanilla");
  }

  @Test
  void anyJsonTypeIsAValidRootOfAnElement() {
    // spec: R29, K2
    LoadedData data =
        ok(
            MiniData.load(
                Packs.implicit(
                    "vanilla",
                    "data/minecraft/loot_table/arr.json",
                    "[1,2]",
                    "data/minecraft/loot_table/int.json",
                    "20",
                    "data/minecraft/loot_table/dec.json",
                    "1.0",
                    "data/minecraft/loot_table/str.json",
                    "\"s\"",
                    "data/minecraft/loot_table/yes.json",
                    "true",
                    "data/minecraft/loot_table/obj.json",
                    "{}")));
    assertThat(element(data, "minecraft:loot_table", "minecraft:arr"))
        .isInstanceOf(JsonArray.class);
    JsonNumber integer = (JsonNumber) element(data, "minecraft:loot_table", "minecraft:int");
    JsonNumber decimal = (JsonNumber) element(data, "minecraft:loot_table", "minecraft:dec");
    assertThat(integer.isInteger()).isTrue();
    assertThat(decimal.isInteger()).isFalse();
    assertThat(MiniData.elementIds(data, "minecraft:loot_table")).hasSize(6);
  }

  @Test
  void provisionalElementsAreListedInCodePointOrderOfTheirIdentifiers() {
    // spec: R30 (provisional)
    LoadedData data =
        ok(
            MiniData.load(
                Packs.implicit(
                    "vanilla",
                    "data/minecraft/loot_table/b.json",
                    "1",
                    "data/minecraft/loot_table/a/z.json",
                    "1",
                    "data/minecraft/loot_table/a.json",
                    "1",
                    "data/minecraft/loot_table/a_b.json",
                    "1",
                    "data/minecraft/loot_table/a-b.json",
                    "1",
                    "data/minecraft/loot_table/a0.json",
                    "1",
                    "data/other/loot_table/a.json",
                    "1",
                    "data/aaa/loot_table/z.json",
                    "1")));
    assertThat(MiniData.elementIds(data, "minecraft:loot_table"))
        .containsExactly(
            "aaa:z",
            "minecraft:a",
            "minecraft:a-b",
            "minecraft:a/z",
            "minecraft:a0",
            "minecraft:a_b",
            "minecraft:b",
            "other:a");
  }

  @Test
  void provisionalTheHighestPriorityFileReplacesTheWholeElement() {
    // spec: R41, P4 (no merge of content between packs)
    LoadedData data =
        ok(
            MiniData.load(
                Packs.implicit("vanilla", DIRT, "{\"a\":1,\"b\":2}"),
                Packs.world("A", DIRT, "{\"diamond\":1}"),
                Packs.world("B", DIRT, "{\"b\":3}")));
    JsonObject tree = (JsonObject) element(data, "minecraft:loot_table", "minecraft:blocks/dirt");
    assertThat(tree.members().keySet()).containsExactly("b");
    assertThat(tree.origin().pack()).isEqualTo("B");
    assertThat(MiniData.elementIds(data, "minecraft:loot_table")).hasSize(1);
  }

  @Test
  void provisionalALowerPriorityFileSurvivesWhereTheHigherPackHasNoFile() {
    // spec: R41
    LoadedData data =
        ok(
            MiniData.load(
                Packs.implicit("vanilla", DIRT, "1", "data/minecraft/loot_table/stone.json", "2"),
                Packs.world("A", DIRT, "3")));
    assertThat(element(data, "minecraft:loot_table", "minecraft:blocks/dirt").origin().pack())
        .isEqualTo("A");
    assertThat(element(data, "minecraft:loot_table", "minecraft:stone").origin().pack())
        .isEqualTo("vanilla");
  }

  @Test
  void provisionalSameNameInTwoRegistriesAreTwoElements() {
    // spec: R8
    LoadedData data =
        ok(
            MiniData.load(
                Packs.implicit(
                    "vanilla",
                    "data/minecraft/loot_table/x.json",
                    "1",
                    "data/minecraft/villager_trade/x.json",
                    "1.0")));
    assertThat(((JsonNumber) element(data, "minecraft:loot_table", "minecraft:x")).isInteger())
        .isTrue();
    assertThat(((JsonNumber) element(data, "minecraft:villager_trade", "minecraft:x")).isInteger())
        .isFalse();
  }

  @Test
  void provisionalAnInvalidFileIsReportedOnceAndTheElementIsAbsent() {
    // spec: R54, R61 c
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit("vanilla", DIRT, "{,}", "data/minecraft/loot_table/ok.json", "{}"));
    LoadedData data = outcome.value().orElseThrow();
    assertThat(outcome.diagnostics()).hasSize(1);
    Diagnostic diagnostic = outcome.diagnostics().get(0);
    assertThat(diagnostic.code()).isEqualTo(DiagnosticCode.JSON_SYNTAX);
    assertThat(diagnostic.origin()).isEqualTo(new FileOrigin("vanilla", DIRT));
    assertThat(MiniData.elementIds(data, "minecraft:loot_table")).containsExactly("minecraft:ok");
  }

  @Test
  void provisionalAnInvalidFileDoesNotHideTheVersionOfALowerPriorityPack() {
    // spec: R54 ("ne remplace pas la version d'un pack de priorite plus basse")
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit("vanilla", DIRT, "{\"from\":\"vanilla\"}"), Packs.world("A", DIRT, "{"));
    LoadedData data = outcome.value().orElseThrow();
    JsonNode tree = element(data, "minecraft:loot_table", "minecraft:blocks/dirt");
    assertThat(tree.origin().pack()).isEqualTo("vanilla");
    assertThat(Diags.codes(outcome)).containsExactly(DiagnosticCode.JSON_SYNTAX);
    assertThat(outcome.diagnostics().get(0).origin()).isEqualTo(new FileOrigin("A", DIRT));
  }

  @Test
  void provisionalTheWarningsOfAValidFileAreKeptAndTheFileIsLoaded() {
    // spec: R51, R58, loader step "the warnings of a valid file are kept"
    Outcome<LoadedData> outcome =
        MiniData.load(Packs.implicit("vanilla", DIRT, "{\"a\":1,\"a\":2}"));
    assertThat(outcome.hasErrors()).isFalse();
    Diags.assertSummary(outcome, "JSON_DUPLICATE_KEY@$.a");
    assertThat(outcome.diagnostics().get(0).origin()).isEqualTo(new FileOrigin("vanilla", DIRT));
    JsonObject tree =
        (JsonObject)
            element(outcome.value().orElseThrow(), "minecraft:loot_table", "minecraft:blocks/dirt");
    assertThat(((JsonNumber) tree.get("a").orElseThrow()).longValue()).isEqualTo(2);
  }

  @Test
  void everyElementRegistryOfTheCatalogExistsEvenWhenEmpty() {
    // spec: R28 (dimension, item_modifier, slot_source have no vanilla file),
    // LoadedData.elementRegistries
    LoadedData data = ok(MiniData.load(Packs.implicit("vanilla", DIRT, "{}")));
    assertThat(data.elementRegistries().stream().map(r -> r.registry().toString()))
        .containsExactly(
            "minecraft:dimension",
            "minecraft:loot_table",
            "minecraft:villager_trade",
            "minecraft:worldgen/biome");
    assertThat(
            data.elementRegistry(Identifier.parse("minecraft:dimension")).orElseThrow().elements())
        .isEmpty();
    assertThat(
            data.elementRegistry(Identifier.parse("minecraft:villager_trade"))
                .orElseThrow()
                .elements())
        .isEmpty();
  }

  @Test
  void staticAndUnknownRegistriesHaveNoElementRegistry() {
    // spec: LoadedData.elementRegistry Javadoc
    LoadedData data = ok(MiniData.load(Packs.implicit("vanilla")));
    assertThat(data.elementRegistry(Identifier.parse("minecraft:block"))).isEmpty();
    assertThat(data.elementRegistry(Identifier.parse("minecraft:nothing"))).isEmpty();
    assertThat(data.elementRegistry(Identifier.parse("minecraft:function"))).isEmpty();
  }

  @Test
  void provisionalTheExtractionMarkerAndPackMetadataTakePartInNothing() {
    // spec: R13, R12
    Outcome<LoadedData> outcome =
        MiniData.load(
            Packs.implicit(
                "vanilla",
                "data/.mcassetsroot",
                "",
                "assets/.mcassetsroot",
                "",
                "assets/minecraft/lang/en_us.json",
                "{}",
                "pack.png",
                "x",
                DIRT,
                "{}"));
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(MiniData.elementIds(outcome.value().orElseThrow(), "minecraft:loot_table"))
        .containsExactly("minecraft:blocks/dirt");
  }

  @Test
  void aPackWithoutFilesLoadsToEmptyRegistries() {
    // spec: R28
    LoadedData data = ok(MiniData.load(Packs.implicit("vanilla")));
    assertThat(data.functions()).isEmpty();
    assertThat(data.structures()).isEmpty();
    assertThat(data.staticRegistries()).hasSize(2);
  }

  @Test
  void staticRegistriesAreRepeatedFromTheRequestInProtocolOrder() {
    // spec: R26, LoadedData.staticRegistries
    LoadedData data = ok(MiniData.load(Packs.implicit("vanilla")));
    assertThat(data.staticRegistries().stream().map(r -> r.registry().toString()))
        .containsExactly("minecraft:block", "minecraft:item");
  }
}

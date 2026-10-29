package dev.shoko.minecraft.data.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.shoko.minecraft.data.catalog.RegistryCatalog;
import dev.shoko.minecraft.data.catalog.StaticRegistryData;
import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonReader;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.PackDiscovery;
import dev.shoko.minecraft.data.pack.PackStack;
import dev.shoko.minecraft.data.pack.meta.FeatureFlags;
import dev.shoko.minecraft.data.pack.meta.PackType;
import dev.shoko.minecraft.data.tag.TagResolution;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Exit criterion of M2 (K2 of the spec) on the extracted reference data: zero error diagnostics and
 * the counts of {@code data-registries-inventory.md} (R26 to R28, R31, R39, R43). The files are
 * read from the local cache ({@code MINECRAFT_CACHE_DIR}, else {@code .cache} at the repository
 * root) and never copied; every test is skipped when the cache is absent.
 */
@Tag("oracle")
class DatapackLoaderOracleTest {

  static Path reference() {
    String configured = System.getenv("MINECRAFT_CACHE_DIR");
    Path cache;
    if (configured != null && !configured.isBlank()) {
      cache = Path.of(configured);
    } else {
      Path dir = Path.of("").toAbsolutePath();
      while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
        dir = dir.getParent();
      }
      cache = (dir == null ? Path.of("") : dir).resolve(".cache");
    }
    return cache.resolve("reference/26.3");
  }

  static Path requireReference() {
    Path dir = reference();
    assumeTrue(
        Files.isDirectory(dir.resolve("resources/data")), () -> "reference cache absent: " + dir);
    return dir;
  }

  private static JsonNode json(Path file) throws IOException {
    Outcome<JsonNode> outcome =
        JsonReader.read(
            Files.readAllBytes(file), new FileOrigin("reference", file.getFileName().toString()));
    assertThat(outcome.diagnostics()).isEmpty();
    return outcome.value().orElseThrow();
  }

  private static Outcome<LoadedData> load(List<String> packNames, FeatureFlags flags)
      throws IOException {
    Path reference = requireReference();
    RegistryCatalog catalog =
        RegistryCatalog.read(json(reference.resolve("generated/reports/datapack.json")))
            .value()
            .orElseThrow();
    List<StaticRegistryData> statics =
        StaticRegistryData.read(json(reference.resolve("generated/reports/registries.json")))
            .value()
            .orElseThrow();
    List<Pack> available =
        PackDiscovery.referencePacks(reference.resolve("resources")).value().orElseThrow();
    List<Pack> chosen = available.stream().filter(p -> packNames.contains(p.id())).toList();
    return DatapackLoader.load(
        new LoadRequest(PackStack.of(PackType.DATA, chosen), catalog, statics, flags));
  }

  private static Outcome<LoadedData> vanilla() throws IOException {
    return load(List.of("vanilla"), FeatureFlags.vanilla());
  }

  private static Outcome<LoadedData> withTradeRebalance() throws IOException {
    FeatureFlags flags =
        FeatureFlags.vanilla()
            .union(FeatureFlags.of(List.of(Identifier.parse("minecraft:trade_rebalance"))));
    return load(List.of("vanilla", "trade_rebalance"), flags);
  }

  private static int tagMembers(LoadedData data, String registry, String tag) {
    return data.tags(Identifier.parse(registry))
        .orElseThrow()
        .members(Identifier.parse(tag))
        .orElseThrow()
        .size();
  }

  @Test
  void theVanillaPackLoadsWithoutAnyErrorDiagnostic() throws IOException {
    // spec: K2 (zero error diagnostics on the data of the target), R49
    Outcome<LoadedData> outcome = vanilla();
    assertThat(outcome.diagnostics().stream().filter(d -> d.code().severity() == Severity.ERROR))
        .isEmpty();
    assertThat(outcome.value()).isPresent();
  }

  @Test
  void theCountsMatchTheInventory() throws IOException {
    // spec: R26, R27, R28, R10 and the TOTAL line of data-registries-inventory.md
    LoadedData data = vanilla().value().orElseThrow();
    assertThat(data.staticRegistries()).hasSize(95);
    assertThat(data.staticRegistries().stream().mapToInt(r -> r.ids().size()).sum())
        .isEqualTo(7053);
    assertThat(data.elementRegistries()).hasSize(60);
    assertThat(data.elementRegistries().stream().mapToInt(r -> r.elements().size()).sum())
        .isEqualTo(7383);
    assertThat(data.structures()).hasSize(1511);
    assertThat(data.functions()).isEmpty();
    for (String empty :
        List.of("minecraft:dimension", "minecraft:item_modifier", "minecraft:slot_source")) {
      assertThat(data.elementRegistry(Identifier.parse(empty)).orElseThrow().elements())
          .as(empty)
          .isEmpty();
    }
  }

  @Test
  void everyVanillaTagResolvesWithNoFailure() throws IOException {
    // spec: R31, R33 (883 tag files, 0 missing reference, 0 cycle)
    LoadedData data = vanilla().value().orElseThrow();
    int tags = 0;
    int failed = 0;
    List<Identifier> registries = new ArrayList<>();
    data.staticRegistries().forEach(r -> registries.add(r.registry()));
    data.elementRegistries().forEach(r -> registries.add(r.registry()));
    registries.add(Identifier.parse("minecraft:function"));
    for (Identifier registry : registries) {
      TagResolution resolution = data.tags(registry).orElse(null);
      if (resolution != null) {
        tags += resolution.tags().size();
        failed += resolution.failed().size();
      }
    }
    assertThat(failed).isZero();
    assertThat(tags).isEqualTo(883);
  }

  @Test
  void theDocumentedTagVectorsHoldTheirSizes() throws IOException {
    // spec: R39
    LoadedData data = vanilla().value().orElseThrow();
    assertThat(tagMembers(data, "minecraft:block", "minecraft:logs")).isEqualTo(48);
    assertThat(tagMembers(data, "minecraft:block", "minecraft:wool")).isEqualTo(16);
    assertThat(tagMembers(data, "minecraft:block", "minecraft:mineable/pickaxe")).isEqualTo(542);
    assertThat(tagMembers(data, "minecraft:item", "minecraft:planks")).isEqualTo(13);
    assertThat(tagMembers(data, "minecraft:worldgen/biome", "minecraft:is_overworld"))
        .isEqualTo(56);
    assertThat(tagMembers(data, "minecraft:enchantment", "minecraft:non_treasure")).isEqualTo(36);
  }

  @Test
  void theTradeRebalancePackStackedOnVanillaLoadsWithoutErrorAndChangesWhatR43Says()
      throws IOException {
    // spec: R42, R43, R66 (the flag is active, so the pack is stacked)
    Outcome<LoadedData> outcome = withTradeRebalance();
    assertThat(outcome.hasErrors()).isFalse();
    LoadedData data = outcome.value().orElseThrow();
    assertThat(
            data.elementRegistry(Identifier.parse("minecraft:villager_trade"))
                .orElseThrow()
                .elements())
        .hasSize(471);
    assertThat(
            data.elementRegistry(Identifier.parse("minecraft:loot_table")).orElseThrow().elements())
        .hasSize(1447);
    assertThat(data.tags(Identifier.parse("minecraft:enchantment")).orElseThrow().tags())
        .hasSize(29);
    assertThat(tagMembers(data, "minecraft:villager_trade", "minecraft:armorer/level_1"))
        .isEqualTo(2);
    assertThat(tagMembers(data, "minecraft:villager_trade", "minecraft:librarian/level_1"))
        .isEqualTo(9);
    assertThat(data.packReports())
        .extracting(PackReport::pack)
        .containsExactly("vanilla", "trade_rebalance");
  }

  @Test
  void aBuiltInPackWhoseFlagIsNotActiveIsDiscarded() throws IOException {
    // spec: R66 c (VF1 on the real built-in pack)
    Outcome<LoadedData> outcome =
        load(List.of("vanilla", "trade_rebalance"), FeatureFlags.vanilla());
    assertThat(outcome.diagnostics()).hasSize(1);
    LoadedData data = outcome.value().orElseThrow();
    assertThat(
            data.elementRegistry(Identifier.parse("minecraft:villager_trade"))
                .orElseThrow()
                .elements())
        .hasSize(391);
  }
}

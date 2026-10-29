package dev.shoko.minecraft.data.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.catalog.RegistryDescriptor.Kind;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Jx;
import dev.shoko.minecraft.data.testing.MiniData;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R10, R12, R15, R16, R17, R27, R28 (the shape of datapack.json)
class RegistryCatalogTest {

  private static RegistryCatalog catalog() {
    return MiniData.catalog();
  }

  private static RegistryDescriptor find(String name) {
    return catalog().find(Identifier.parse(name)).orElseThrow();
  }

  private static Outcome<RegistryCatalog> read(String json) {
    return RegistryCatalog.read(Jx.parse(json));
  }

  @Test
  void registriesAreListedInCodePointOrderOfTheirNames() {
    // spec: RegistryCatalog.registries Javadoc
    List<String> names = catalog().registries().stream().map(d -> d.name().toString()).toList();
    assertThat(names)
        .containsExactly(
            "minecraft:block",
            "minecraft:dimension",
            "minecraft:function",
            "minecraft:item",
            "minecraft:loot_table",
            "minecraft:structure",
            "minecraft:villager_trade",
            "minecraft:worldgen/biome");
  }

  @Test
  void provisionalARegistryWithoutElementsIsStatic() {
    // spec: R15 (elements false), R27
    RegistryDescriptor block = find("minecraft:block");
    assertThat(block.kind()).isEqualTo(Kind.STATIC);
    assertThat(block.tags()).isTrue();
    assertThat(block.stable()).isTrue();
  }

  @Test
  void provisionalARegistryWithElementsReadsFromJsonFiles() {
    // spec: R8, R12, R15
    RegistryDescriptor loot = find("minecraft:loot_table");
    assertThat(loot.kind()).isEqualTo(Kind.ELEMENTS);
    assertThat(loot.extension()).isEqualTo(".json");
    assertThat(loot.tags()).isTrue();
    assertThat(loot.stable()).isTrue();
  }

  @Test
  void provisionalTheStableFlagIsReadPerRegistry() {
    // spec: R17
    assertThat(find("minecraft:loot_table").stable()).isTrue();
    assertThat(find("minecraft:worldgen/biome").stable()).isFalse();
    assertThat(find("minecraft:villager_trade").stable()).isFalse();
    assertThat(find("minecraft:dimension").stable()).isFalse();
  }

  @Test
  void provisionalAFolderWithSeveralSegmentsKeepsItsSlashesInTheName() {
    // spec: R8, R11
    RegistryDescriptor biome = find("minecraft:worldgen/biome");
    assertThat(biome.name().path()).isEqualTo("worldgen/biome");
    assertThat(biome.kind()).isEqualTo(Kind.ELEMENTS);
  }

  @Test
  void provisionalTheOthersSectionGivesContentRegistriesWithTheirExtensions() {
    // spec: R10, R12 (functions are .mcfunction text, structures .nbt, structures take no tag)
    RegistryDescriptor function = find("minecraft:function");
    assertThat(function.kind()).isEqualTo(Kind.CONTENT);
    assertThat(function.extension()).isEqualTo(".mcfunction");
    assertThat(function.tags()).isTrue();
    RegistryDescriptor structure = find("minecraft:structure");
    assertThat(structure.kind()).isEqualTo(Kind.CONTENT);
    assertThat(structure.extension()).isEqualTo(".nbt");
    assertThat(structure.tags()).isFalse();
  }

  @Test
  void anUnknownRegistryIsAbsent() {
    assertThat(catalog().find(Identifier.parse("minecraft:nothing"))).isEmpty();
    assertThat(catalog().find(Identifier.parse("other:block"))).isEmpty();
  }

  @Test
  void aReadHasNoDiagnosticOnAWellFormedReport() {
    Outcome<RegistryCatalog> outcome = read(MiniData.CATALOG_JSON);
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(outcome.value()).isPresent();
  }

  @Test
  void provisionalAReportThatIsNotAnObjectIsUnusable() {
    // spec: RegistryCatalog.read Javadoc
    Diags.assertSingleError(read("[]"), DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
    Diags.assertSingleError(read("1"), DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
  }

  @Test
  void provisionalARegistryEntryOfTheWrongShapeIsReportedAtItsPath() {
    // spec: RegistryCatalog.read Javadoc ("at the faulty path")
    Outcome<RegistryCatalog> notBoolean =
        read(
            "{\"others\":{},\"registries\":{\"minecraft:block\":{\"elements\":\"no\","
                + "\"stable\":true,\"tags\":true}}}");
    assertThat(notBoolean.value()).isEmpty();
    Diags.assertSummary(notBoolean, "CODEC_TYPE_MISMATCH@$.registries.minecraft:block.elements");
    Outcome<RegistryCatalog> missing =
        read(
            "{\"others\":{},\"registries\":{\"minecraft:block\":{\"stable\":true,\"tags\":true}}}");
    assertThat(missing.value()).isEmpty();
    Diags.assertSummary(missing, "CODEC_MISSING_FIELD@$.registries.minecraft:block.elements");
  }

  @Test
  void provisionalARegistriesSectionThatIsNotAnObjectIsReported() {
    // spec: RegistryCatalog.read Javadoc
    Outcome<RegistryCatalog> outcome = read("{\"others\":{},\"registries\":[]}");
    assertThat(outcome.value()).isEmpty();
    Diags.assertSummary(outcome, "CODEC_TYPE_MISMATCH@$.registries");
  }

  @Test
  void theDescriptorsAreValuesCompared() {
    assertThat(find("minecraft:block")).isEqualTo(find("minecraft:block"));
    assertThat(find("minecraft:block")).isNotEqualTo(find("minecraft:item"));
  }
}

package dev.shoko.minecraft.data.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Jx;
import dev.shoko.minecraft.data.testing.MiniData;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R26, R27 (static registries read from registries.json)
class StaticRegistryDataTest {

  private static Outcome<List<StaticRegistryData>> read(String json) {
    return StaticRegistryData.read(Jx.parse(json));
  }

  private static StaticRegistryData registry(List<StaticRegistryData> all, String name) {
    return all.stream()
        .filter(r -> r.registry().equals(Identifier.parse(name)))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void theMiniatureReportIsReadWithoutDiagnostic() {
    List<StaticRegistryData> all = MiniData.staticRegistries();
    assertThat(all).hasSize(2);
    StaticRegistryData block = registry(all, "minecraft:block");
    assertThat(block.protocolId()).isZero();
    assertThat(block.ids().stream().map(Identifier::toString))
        .containsExactly(
            "minecraft:air",
            "minecraft:stone",
            "minecraft:dirt",
            "minecraft:oak_log",
            "minecraft:birch_log",
            "minecraft:glass");
    assertThat(block.defaultEntry()).hasValue(Identifier.parse("minecraft:air"));
  }

  @Test
  void aRegistryWithoutDefaultHasNone() {
    // spec: R26 (10 of the 95 registries have a default entry)
    assertThat(registry(MiniData.staticRegistries(), "minecraft:item").defaultEntry()).isEmpty();
  }

  @Test
  void registriesAreOrderedByProtocolIdWhateverTheOrderOfTheFile() {
    // spec: R26 ("dans l'ordre des protocol_id")
    Outcome<List<StaticRegistryData>> outcome =
        read(
            "{\"minecraft:z\":{\"protocol_id\":0,\"entries\":{\"minecraft:a\":{\"protocol_id\":0}}},"
                + "\"minecraft:a\":{\"protocol_id\":2,\"entries\":{}},"
                + "\"minecraft:m\":{\"protocol_id\":1,\"entries\":{\"minecraft:b\":{\"protocol_id\":0}}}}");
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(outcome.value().orElseThrow().stream().map(r -> r.registry().toString()))
        .containsExactly("minecraft:z", "minecraft:m", "minecraft:a");
    assertThat(outcome.value().orElseThrow().stream().map(StaticRegistryData::protocolId))
        .containsExactly(0, 1, 2);
  }

  @Test
  void entriesAreOrderedByProtocolIdSoThePositionIsTheId() {
    // spec: R26, StaticRegistryData.ids Javadoc
    Outcome<List<StaticRegistryData>> outcome =
        read(
            "{\"minecraft:r\":{\"protocol_id\":0,\"entries\":{"
                + "\"minecraft:c\":{\"protocol_id\":2},\"minecraft:a\":{\"protocol_id\":0},"
                + "\"minecraft:b\":{\"protocol_id\":1}}}}");
    assertThat(outcome.diagnostics()).isEmpty();
    StaticRegistryData data = outcome.value().orElseThrow().get(0);
    assertThat(data.ids().stream().map(Identifier::toString))
        .containsExactly("minecraft:a", "minecraft:b", "minecraft:c");
    assertThat(data.entryProtocolId(Identifier.parse("c"))).isEqualTo(OptionalInt.of(2));
    assertThat(data.entryProtocolId(Identifier.parse("a"))).isEqualTo(OptionalInt.of(0));
    assertThat(data.entryProtocolId(Identifier.parse("zzz"))).isEqualTo(OptionalInt.empty());
  }

  @Test
  void anEmptyRegistryIsAllowed() {
    Outcome<List<StaticRegistryData>> outcome =
        read("{\"minecraft:r\":{\"protocol_id\":0,\"entries\":{}}}");
    assertThat(outcome.value().orElseThrow().get(0).ids()).isEmpty();
  }

  @Test
  void provisionalAGapInTheEntryIdsDropsTheRegistryWithAnErrorAtTheId() {
    // spec: StaticRegistryData.read Javadoc (ids exactly 0 to n-1, CODEC_NUMBER_RANGE)
    Outcome<List<StaticRegistryData>> outcome =
        read(
            "{\"minecraft:bad\":{\"protocol_id\":0,\"entries\":{\"minecraft:a\":{\"protocol_id\":0},"
                + "\"minecraft:b\":{\"protocol_id\":2}}},"
                + "\"minecraft:good\":{\"protocol_id\":1,\"entries\":{\"minecraft:a\":{\"protocol_id\":0}}}}");
    assertThat(outcome.value().orElseThrow().stream().map(r -> r.registry().toString()))
        .containsExactly("minecraft:good");
    assertThat(Diags.codes(outcome)).containsExactly(DiagnosticCode.CODEC_NUMBER_RANGE);
    assertThat(outcome.hasErrors()).isTrue();
  }

  @Test
  void provisionalADuplicatedEntryIdDropsTheRegistry() {
    // spec: R26 (ids exactly 0 to n-1), StaticRegistryData.read Javadoc
    Outcome<List<StaticRegistryData>> outcome =
        read(
            "{\"minecraft:bad\":{\"protocol_id\":0,\"entries\":{\"minecraft:a\":{\"protocol_id\":0},"
                + "\"minecraft:b\":{\"protocol_id\":0}}}}");
    assertThat(outcome.value().orElseThrow()).isEmpty();
    assertThat(Diags.codes(outcome)).containsExactly(DiagnosticCode.CODEC_NUMBER_RANGE);
  }

  @Test
  void provisionalAReportOfTheWrongShapeIsUnusable() {
    // spec: StaticRegistryData.read Javadoc
    Diags.assertSingleError(read("[]"), DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
    Diags.assertSingleError(read("\"x\""), DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
  }

  @Test
  void provisionalARegistryWithoutProtocolIdIsReported() {
    Outcome<List<StaticRegistryData>> outcome = read("{\"minecraft:r\":{\"entries\":{}}}");
    assertThat(outcome.hasErrors()).isTrue();
    assertThat(Diags.codes(outcome)).contains(DiagnosticCode.CODEC_MISSING_FIELD);
    assertThat(Diags.summary(outcome)).contains("CODEC_MISSING_FIELD@$.minecraft:r.protocol_id");
  }

  @Test
  void provisionalARegistryEntryOfTheWrongTypeIsReportedAtItsPath() {
    Outcome<List<StaticRegistryData>> outcome =
        read("{\"minecraft:r\":{\"protocol_id\":\"0\",\"entries\":{}}}");
    assertThat(outcome.hasErrors()).isTrue();
    assertThat(Diags.summary(outcome)).contains("CODEC_TYPE_MISMATCH@$.minecraft:r.protocol_id");
  }
}

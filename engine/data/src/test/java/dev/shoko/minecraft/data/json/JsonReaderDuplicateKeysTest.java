package dev.shoko.minecraft.data.json;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Jx;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R51, R58, vectors VJ6 to VJ12 (duplicate keys)
class JsonReaderDuplicateKeysTest {

  private static JsonObject object(Outcome<JsonNode> outcome) {
    return (JsonObject) outcome.value().orElseThrow();
  }

  private static long longOf(JsonObject object, String key) {
    return ((JsonNumber) object.get(key).orElseThrow()).longValue();
  }

  @Test
  void provisionalTheLastValueWinsWithOneWarning() {
    // spec: VJ9, R51, R58 c
    Outcome<JsonNode> outcome = Jx.read("{\"a\":1,\"a\":2}");
    assertThat(object(outcome).members().keySet()).containsExactly("a");
    assertThat(longOf(object(outcome), "a")).isEqualTo(2);
    Diags.assertSummary(outcome, "JSON_DUPLICATE_KEY@$.a");
    Diagnostic warning = outcome.diagnostics().get(0);
    assertThat(warning.code().severity()).isEqualTo(Severity.WARNING);
    assertThat(warning.origin()).isEqualTo(Jx.ORIGIN);
    assertThat(outcome.hasErrors()).isFalse();
  }

  @Test
  void provisionalTheMemberKeepsThePlaceOfItsFirstOccurrenceAndTheValueOfItsLast() {
    // spec: VJ6, R58 b
    Outcome<JsonNode> outcome = Jx.read("{\"a\":1,\"b\":2,\"a\":3}");
    JsonObject object = object(outcome);
    assertThat(object.members().keySet()).containsExactly("a", "b");
    assertThat(longOf(object, "a")).isEqualTo(3);
    assertThat(longOf(object, "b")).isEqualTo(2);
    Diags.assertSummary(outcome, "JSON_DUPLICATE_KEY@$.a");
  }

  @Test
  void provisionalThreeOccurrencesGiveTwoWarnings() {
    // spec: VJ7, R58 c
    Outcome<JsonNode> outcome = Jx.read("{\"a\":1,\"a\":2,\"a\":3}");
    assertThat(object(outcome).members()).hasSize(1);
    assertThat(longOf(object(outcome), "a")).isEqualTo(3);
    Diags.assertSummary(outcome, "JSON_DUPLICATE_KEY@$.a", "JSON_DUPLICATE_KEY@$.a");
  }

  @Test
  void provisionalDuplicatesInsideADiscardedValueAreReportedBeforeTheRepeatedKey() {
    // spec: VJ8, R58 c
    Outcome<JsonNode> outcome = Jx.read("{\"a\":{\"x\":1,\"x\":2},\"a\":{\"y\":1}}");
    JsonObject a = (JsonObject) object(outcome).get("a").orElseThrow();
    assertThat(a.members().keySet()).containsExactly("y");
    Diags.assertSummary(outcome, "JSON_DUPLICATE_KEY@$.a.x", "JSON_DUPLICATE_KEY@$.a");
  }

  @Test
  void provisionalKeysDifferingByCaseAreDifferentKeys() {
    // spec: VJ10, R58 a
    Outcome<JsonNode> outcome = Jx.read("{\"a\":1,\"A\":2}");
    assertThat(object(outcome).members().keySet()).containsExactly("a", "A");
    assertThat(outcome.diagnostics()).isEmpty();
  }

  @Test
  void provisionalDuplicatesInDifferentArrayElementsAreReportedInReadingOrder() {
    // spec: VJ11, R58 c
    Outcome<JsonNode> outcome = Jx.read("[{\"k\":1,\"k\":2},{\"k\":3,\"k\":4}]");
    JsonArray array = (JsonArray) outcome.value().orElseThrow();
    assertThat(longOf((JsonObject) array.elements().get(0), "k")).isEqualTo(2);
    assertThat(longOf((JsonObject) array.elements().get(1), "k")).isEqualTo(4);
    Diags.assertSummary(outcome, "JSON_DUPLICATE_KEY@$[0].k", "JSON_DUPLICATE_KEY@$[1].k");
  }

  @Test
  void provisionalKeysAreComparedAfterDecodingEscapes() {
    // spec: R58 a
    Outcome<JsonNode> outcome = Jx.read("{\"a\":1,\"\\u0061\":2}");
    assertThat(object(outcome).members().keySet()).containsExactly("a");
    assertThat(longOf(object(outcome), "a")).isEqualTo(2);
    Diags.assertSummary(outcome, "JSON_DUPLICATE_KEY@$.a");
  }

  @Test
  void provisionalKeysAreComparedUnitByUnitWithoutNormalization() {
    // spec: R58 a: precomposed and decomposed forms of e-acute are different keys
    Outcome<JsonNode> outcome = Jx.read("{\"\u00e9\":1,\"e\u0301\":2}");
    assertThat(object(outcome).members()).hasSize(2);
    assertThat(outcome.diagnostics()).isEmpty();
  }

  @Test
  void provisionalALoneSurrogateKeyCanBeDuplicated() {
    // spec: R58 a, R60 d
    Outcome<JsonNode> outcome = Jx.read("{\"\\ud800\":1,\"\\ud800\":2}");
    assertThat(object(outcome).members()).hasSize(1);
    assertThat(outcome.diagnostics()).hasSize(1);
    assertThat(outcome.diagnostics().get(0).code()).isEqualTo(DiagnosticCode.JSON_DUPLICATE_KEY);
  }

  @Test
  void provisionalTheReplacingNodeCarriesTheMemberPath() {
    // spec: R58 b, "Arbre JSON" (every node knows its path)
    JsonObject object = object(Jx.read("{\"a\":1,\"b\":2,\"a\":[true]}"));
    JsonArray array = (JsonArray) object.get("a").orElseThrow();
    assertThat(array.path().toString()).isEqualTo("$.a");
    assertThat(array.elements().get(0).path().toString()).isEqualTo("$.a[0]");
  }

  @Test
  void provisionalADuplicateAndAnErrorGiveOnlyTheErrorForTrailingComma() {
    // spec: VJ12, R58 e, R61 c
    Outcome<JsonNode> outcome = Jx.read("{\"a\":1,\"a\":2,}");
    Diags.assertSingleError(outcome, DiagnosticCode.JSON_SYNTAX);
  }

  @Test
  void provisionalADuplicateAndALaterErrorOfAnotherKindGiveOnlyTheError() {
    // spec: R58 e, R61 c (single diagnostic whatever the error)
    Diags.assertSingleError(Jx.read("{\"a\":1,\"a\":2} x"), DiagnosticCode.JSON_TRAILING);
    Diags.assertSingleError(Jx.read("[{\"a\":1,\"a\":2},1e999]"), DiagnosticCode.JSON_NUMBER_RANGE);
    Diags.assertSingleError(Jx.read("{\"a\":1,\"a\":2,\"b\":}"), DiagnosticCode.JSON_SYNTAX);
    Diags.assertSingleError(
        Jx.read("{\"a\":1,\"a\":2,\"c\":[" + "[".repeat(300)), DiagnosticCode.JSON_TOO_DEEP);
  }

  @Test
  void provisionalDuplicateWarningsFollowTheReadingOrderOfTheWholeFile() {
    // spec: R58 c
    Outcome<JsonNode> outcome =
        Jx.read(
            "{\"x\":{\"p\":1,\"p\":2},\"y\":[{\"q\":1,\"q\":2}],\"x\":0,\"z\":{\"r\":1,\"r\":2}}");
    List<String> expected =
        List.of(
            "JSON_DUPLICATE_KEY@$.x.p",
            "JSON_DUPLICATE_KEY@$.y[0].q",
            "JSON_DUPLICATE_KEY@$.x",
            "JSON_DUPLICATE_KEY@$.z.r");
    assertThat(Diags.summary(outcome)).containsExactlyElementsOf(expected);
    assertThat(object(outcome).members().keySet()).containsExactly("x", "y", "z");
  }

  @Test
  void provisionalManyDuplicatesAreAllReported() {
    // spec: R58 c: one warning per extra occurrence
    String text = "{" + "\"k\":1,".repeat(1000) + "\"k\":2}";
    Outcome<JsonNode> outcome = Jx.read(text);
    assertThat(outcome.diagnostics()).hasSize(1000);
    assertThat(longOf(object(outcome), "k")).isEqualTo(2);
  }
}

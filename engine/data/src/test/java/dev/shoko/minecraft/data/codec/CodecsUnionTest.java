package dev.shoko.minecraft.data.codec;

import static dev.shoko.minecraft.data.testing.Cx.decode;
import static dev.shoko.minecraft.data.testing.Cx.value;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.testing.Diags;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// spec: data-datapack-loading K1 ("unions par champ type"), R22, Codecs.byType Javadoc
class CodecsUnionTest {

  /** Each variant reads the whole object, the type field included, and tags what it saw. */
  private static Codec<String> variant(String label, String extra) {
    return Codecs.object(
        reader -> {
          Supplier<String> type = reader.required("type", Codecs.string());
          Supplier<Integer> field = reader.required(extra, Codecs.integer());
          return () -> label + ":" + type.get() + ":" + field.get();
        });
  }

  private static Codec<String> union() {
    Map<Identifier, Codec<? extends String>> variants = new LinkedHashMap<>();
    variants.put(Identifier.parse("minecraft:first"), variant("first", "a"));
    variants.put(Identifier.parse("sonde:second"), variant("second", "b"));
    return Codecs.byType("type", variants);
  }

  @Test
  void provisionalTheTypeFieldSelectsTheVariant() {
    // spec: Codecs.byType Javadoc
    assertThat(value(union(), "{\"type\":\"minecraft:first\",\"a\":1}"))
        .isEqualTo("first:minecraft:first:1");
    assertThat(value(union(), "{\"type\":\"sonde:second\",\"b\":2}"))
        .isEqualTo("second:sonde:second:2");
  }

  @Test
  void provisionalTheTypeIsReadLikeAnIdentifier() {
    // spec: R22 (no namespace means minecraft)
    assertThat(value(union(), "{\"type\":\"first\",\"a\":1}")).startsWith("first:");
    assertThat(value(union(), "{\"type\":\":first\",\"a\":1}")).startsWith("first:");
  }

  @Test
  void provisionalTheVariantReceivesTheWholeObjectTheTypeFieldIncluded() {
    // spec: Codecs.byType Javadoc ("the codec receives the whole object, the type field included")
    assertThat(value(union(), "{\"a\":7,\"type\":\"first\"}")).endsWith(":7");
    Outcome<String> noType = decode(union(), "{\"type\":\"first\"}");
    Diags.assertSummary(noType, "CODEC_MISSING_FIELD@$.a");
  }

  @Test
  void provisionalAnAbsentTypeFieldIsAMissingField() {
    // spec: Codecs.byType Javadoc
    Diags.assertSingleError(
        decode(union(), "{\"a\":1}"), DiagnosticCode.CODEC_MISSING_FIELD, "$.type");
  }

  @Test
  void provisionalATypeThatIsNotAValidIdentifierIsInvalid() {
    // spec: Codecs.byType Javadoc, R20
    Diags.assertSingleError(
        decode(union(), "{\"type\":\"Bad:Id\",\"a\":1}"),
        DiagnosticCode.CODEC_INVALID_IDENTIFIER,
        "$.type");
    Diags.assertSingleError(
        decode(union(), "{\"type\":\"#first\",\"a\":1}"),
        DiagnosticCode.CODEC_INVALID_IDENTIFIER,
        "$.type");
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "true", "null", "[]", "{}"})
  void provisionalATypeThatIsNotAStringIsATypeMismatch(String type) {
    // spec: R62 f
    Diags.assertSingleError(
        decode(union(), "{\"type\":" + type + ",\"a\":1}"),
        DiagnosticCode.CODEC_TYPE_MISMATCH,
        "$.type");
  }

  @Test
  void provisionalAnUnknownVariantIsReportedAsAnUnresolvedReferenceAtTheTypeField() {
    // spec: Codecs.byType Javadoc ("provisional": the closed list has no code for it)
    Diags.assertSingleError(
        decode(union(), "{\"type\":\"minecraft:third\",\"a\":1}"),
        DiagnosticCode.CODEC_UNRESOLVED_REFERENCE,
        "$.type");
  }

  @ParameterizedTest
  @ValueSource(strings = {"[]", "1", "\"first\"", "true", "null"})
  void provisionalARootThatIsNotAnObjectIsATypeMismatch(String json) {
    // spec: Codecs.byType Javadoc (the type field is read from an object), R62 f
    Diags.assertSingleError(decode(union(), json), DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
  }

  @Test
  void provisionalTheDiagnosticsOfTheChosenVariantKeepTheirPaths() {
    // spec: R61 a, R62 g
    Outcome<String> outcome = decode(union(), "{\"type\":\"first\",\"a\":\"x\"}");
    Diags.assertSummary(outcome, "CODEC_TYPE_MISMATCH@$.a");
    assertThat(outcome.value()).isEmpty();
  }

  @Test
  void provisionalVariantsMayBeNestedInLists() {
    // spec: K1
    java.util.List<String> values =
        value(
            Codecs.list(union()),
            "[{\"type\":\"first\",\"a\":1},{\"type\":\"sonde:second\",\"b\":2}]");
    assertThat(values).containsExactly("first:first:1", "second:sonde:second:2");
    Diags.assertSummary(
        decode(Codecs.list(union()), "[{\"type\":\"first\",\"a\":1},{\"type\":\"nope\"}]"),
        "CODEC_UNRESOLVED_REFERENCE@$[1].type");
  }
}

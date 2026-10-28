package dev.shoko.minecraft.data.tag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Jx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// spec: data-datapack-loading R22, R23, R31, R32, R38, R63 c, vector VC17 (reading a tag file)
class TagFileReaderTest {

  private static Outcome<TagFile> read(String json) {
    return TagFileReader.read(Jx.parse(json));
  }

  private static TagFile ok(String json) {
    Outcome<TagFile> outcome = read(json);
    assertThat(outcome.diagnostics()).as(json).isEmpty();
    return outcome.value().orElseThrow();
  }

  private static TagEntry element(String id, boolean required) {
    return new TagEntry(Identifier.parse(id), false, required);
  }

  private static TagEntry tag(String id, boolean required) {
    return new TagEntry(Identifier.parse(id), true, required);
  }

  @Test
  void aVanillaShapedFileHasValuesAndNoReplace() {
    // spec: R31 (objects with the single key "values": identifiers and # references)
    TagFile file =
        ok("{\"values\":[\"minecraft:oak_log\",\"#minecraft:logs\",\"minecraft:stone\"]}");
    assertThat(file.replace()).isFalse();
    assertThat(file.entries())
        .containsExactly(
            element("minecraft:oak_log", true),
            tag("minecraft:logs", true),
            element("minecraft:stone", true));
  }

  @Test
  void provisionalAnEmptyValuesListIsAValidEmptyTag() {
    // spec: R31 (10 vanilla tags), R38
    TagFile file = ok("{\"values\":[]}");
    assertThat(file.entries()).isEmpty();
    assertThat(file.replace()).isFalse();
  }

  @Test
  void provisionalReplaceDefaultsToFalseAndIsReadWhenGiven() {
    // spec: R32, R31 (10 trade_rebalance tags have "replace": true)
    assertThat(ok("{\"replace\":true,\"values\":[]}").replace()).isTrue();
    assertThat(ok("{\"replace\":false,\"values\":[]}").replace()).isFalse();
  }

  @Test
  void provisionalEntriesFollowR22AndR23() {
    // spec: R22, R23 (default namespace, leading colon, # prefix)
    TagFile file = ok("{\"values\":[\"stone\",\":dirt\",\"#logs\",\"#:planks\",\"foo:bar/baz\"]}");
    assertThat(file.entries())
        .containsExactly(
            element("minecraft:stone", true),
            element("minecraft:dirt", true),
            tag("minecraft:logs", true),
            tag("minecraft:planks", true),
            element("foo:bar/baz", true));
  }

  @Test
  void provisionalObjectEntriesDefaultToRequired() {
    // spec: R32 ({"id", "required"}, required true by default)
    TagFile file =
        ok(
            "{\"values\":[{\"id\":\"a\"},{\"id\":\"b\",\"required\":false},"
                + "{\"id\":\"c\",\"required\":true}]}");
    assertThat(file.entries())
        .containsExactly(
            element("minecraft:a", true),
            element("minecraft:b", false),
            element("minecraft:c", true));
  }

  @Test
  void provisionalStringAndObjectEntriesMayBeMixed() {
    // spec: R32
    TagFile file =
        ok("{\"replace\":true,\"values\":[\"a\",{\"id\":\"b\",\"required\":false},\"#c\"]}");
    assertThat(file.replace()).isTrue();
    assertThat(file.entries())
        .containsExactly(
            element("minecraft:a", true), element("minecraft:b", false), tag("minecraft:c", true));
  }

  @Test
  void provisionalDuplicateEntriesAreKeptByTheReader() {
    // spec: R32 ("entries in file order"); duplicates are removed at resolution (R35)
    TagFile file = ok("{\"values\":[\"a\",\"a\",\"b\",\"a\"]}");
    assertThat(file.entries()).hasSize(4);
  }

  @Test
  void provisionalUnknownKeysAreIgnoredWithoutDiagnostic() {
    // spec: VC17, R32, R63 c
    assertThat(ok("{\"values\":[],\"zzz\":1}").entries()).isEmpty();
    TagFile file = ok("{\"values\":[{\"id\":\"a\",\"required\":true,\"zzz\":1}]}");
    assertThat(file.entries()).containsExactly(element("minecraft:a", true));
    assertThat(ok("{\"zzz\":{},\"values\":[\"a\"],\"yyy\":[1,2]}").entries()).hasSize(1);
  }

  @Test
  void provisionalAnObjectWithoutValuesIsInvalid() {
    // spec: R32 ("values absent"), R54
    Diags.assertSingleError(read("{}"), DiagnosticCode.CODEC_MISSING_FIELD, "$.values");
    Diags.assertSingleError(
        read("{\"replace\":true}"), DiagnosticCode.CODEC_MISSING_FIELD, "$.values");
  }

  @ParameterizedTest
  @ValueSource(strings = {"[]", "[\"a\"]", "\"a\"", "1", "1.0", "true", "null"})
  void provisionalARootThatIsNotAnObjectIsInvalid(String json) {
    // spec: R32
    Diags.assertSingleError(read(json), DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"a\"", "{}", "1", "true", "null"})
  void provisionalValuesThatIsNotAnArrayIsInvalid(String values) {
    // spec: R32, R62 e (no promotion of a lone value)
    Diags.assertSingleError(
        read("{\"values\":" + values + "}"), DiagnosticCode.CODEC_TYPE_MISMATCH, "$.values");
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "\"true\"", "0", "null", "[]", "{}"})
  void provisionalReplaceThatIsNotABooleanIsInvalid(String replace) {
    // spec: R32, R62 c
    Diags.assertSingleError(
        read("{\"replace\":" + replace + ",\"values\":[]}"),
        DiagnosticCode.CODEC_TYPE_MISMATCH,
        "$.replace");
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "1.5", "true", "null", "[]", "[\"a\"]"})
  void provisionalAnEntryThatIsNeitherStringNorObjectIsInvalid(String entry) {
    // spec: R32
    Diags.assertSingleError(
        read("{\"values\":[\"ok\"," + entry + "]}"),
        DiagnosticCode.CODEC_TYPE_MISMATCH,
        "$.values[1]");
  }

  @Test
  void provisionalAnObjectEntryNeedsAStringId() {
    // spec: R32
    Diags.assertSingleError(
        read("{\"values\":[{\"required\":true}]}"),
        DiagnosticCode.CODEC_MISSING_FIELD,
        "$.values[0].id");
    Diags.assertSingleError(
        read("{\"values\":[{}]}"), DiagnosticCode.CODEC_MISSING_FIELD, "$.values[0].id");
    Diags.assertSingleError(
        read("{\"values\":[{\"id\":5}]}"), DiagnosticCode.CODEC_TYPE_MISMATCH, "$.values[0].id");
    Diags.assertSingleError(
        read("{\"values\":[{\"id\":null}]}"), DiagnosticCode.CODEC_TYPE_MISMATCH, "$.values[0].id");
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "\"false\"", "0", "null", "[]"})
  void provisionalRequiredThatIsNotABooleanIsInvalid(String required) {
    // spec: R32, R62 c
    Diags.assertSingleError(
        read("{\"values\":[{\"id\":\"a\",\"required\":" + required + "}]}"),
        DiagnosticCode.CODEC_TYPE_MISMATCH,
        "$.values[0].required");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Foo:bar",
        "foo:Bar",
        "foo:bar baz",
        "a:b:c",
        "..:x",
        "foo/bar:baz",
        "#Foo",
        "##a"
      })
  void provisionalAnInvalidIdentifierOrReferenceInvalidatesTheFile(String text) {
    // spec: R32, R20, R23
    Diags.assertSingleError(
        read("{\"values\":[\"ok\",\"" + text + "\"]}"),
        DiagnosticCode.CODEC_INVALID_IDENTIFIER,
        "$.values[1]");
    Diags.assertSingleError(
        read("{\"values\":[{\"id\":\"" + text + "\"}]}"),
        DiagnosticCode.CODEC_INVALID_IDENTIFIER,
        "$.values[0].id");
  }

  @Test
  void provisionalEveryErrorOfTheFileIsReportedInTextOrder() {
    // spec: R32, R62 g
    Outcome<TagFile> outcome =
        read(
            "{\"replace\":1,\"values\":[5,\"A:b\",{\"id\":\"c\",\"required\":\"x\"},{\"zzz\":1}]}");
    assertThat(outcome.value()).isEmpty();
    Diags.assertSummary(
        outcome,
        "CODEC_TYPE_MISMATCH@$.replace",
        "CODEC_TYPE_MISMATCH@$.values[0]",
        "CODEC_INVALID_IDENTIFIER@$.values[1]",
        "CODEC_TYPE_MISMATCH@$.values[2].required",
        "CODEC_MISSING_FIELD@$.values[3].id");
  }

  @Test
  void aHandMadeFileOfOneThousandEntriesIsRead() {
    // spec: R31 (largest vanilla tags have hundreds of entries)
    StringBuilder json = new StringBuilder("{\"values\":[");
    for (int i = 0; i < 1000; i++) {
      json.append(i == 0 ? "" : ",").append("\"minecraft:e").append(i).append('"');
    }
    TagFile file = ok(json.append("]}").toString());
    assertThat(file.entries()).hasSize(1000);
    assertThat(file.entries().get(999)).isEqualTo(element("minecraft:e999", true));
  }
}

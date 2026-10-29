package dev.shoko.minecraft.data.codec;

import static dev.shoko.minecraft.data.testing.Cx.decode;
import static dev.shoko.minecraft.data.testing.Cx.value;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.FakeAccess;
import dev.shoko.minecraft.data.testing.Jx;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

// spec: data-datapack-loading R56, R62, vectors VC1 to VC12 (scalar codecs and lists)
class CodecsScalarsTest {

  private static final String[] JSON_OF_EVERY_TYPE = {
    "{}", "[]", "\"s\"", "1", "1.5", "true", "null"
  };

  @ParameterizedTest
  @CsvSource({
    "1, 1.0",
    "1.0, 1.0",
    "1e2, 100.0",
    "-0.5, -0.5",
    "0, 0.0",
    "123456789012, 1.23456789012E11"
  })
  void provisionalDecimalAcceptsIntegerAndDecimalForms(String json, double expected) {
    // spec: VC1, R62 a
    assertThat(value(Codecs.decimal(), json)).isEqualTo(expected);
  }

  @Test
  void provisionalDecimalConvertsALargeIntegerToTheNearestDouble() {
    // spec: VC2, R62 a
    assertThat(value(Codecs.decimal(), "9007199254740993")).isEqualTo(9007199254740992.0);
  }

  @Test
  void provisionalDecimalKeepsTheSignOfZero() {
    // spec: R59 e, R62 a
    assertThat(Double.doubleToRawLongBits(value(Codecs.decimal(), "-0.0")))
        .isEqualTo(Long.MIN_VALUE);
  }

  @Test
  void provisionalFourWrongTypesGiveFourMismatchesInTextOrder() {
    // spec: VC3, R56, R62 f, g
    Codec<List<Double>> fields =
        Codecs.object(
            reader -> {
              var a = reader.required("a", Codecs.decimal());
              var b = reader.required("b", Codecs.decimal());
              var c = reader.required("c", Codecs.decimal());
              var d = reader.required("d", Codecs.decimal());
              return () -> List.of(a.get(), b.get(), c.get(), d.get());
            });
    Outcome<List<Double>> outcome = decode(fields, "{\"a\":\"1\",\"b\":true,\"c\":null,\"d\":[1]}");
    assertThat(outcome.value()).isEmpty();
    Diags.assertSummary(
        outcome,
        "CODEC_TYPE_MISMATCH@$.a",
        "CODEC_TYPE_MISMATCH@$.b",
        "CODEC_TYPE_MISMATCH@$.c",
        "CODEC_TYPE_MISMATCH@$.d");
  }

  @Test
  void provisionalFloatKeepsTheNearestBinary32() {
    // spec: VC4, R62 a (0.1 gives float bits 3dcccccd)
    assertThat(Float.floatToRawIntBits(value(Codecs.floatDecimal(), "0.1"))).isEqualTo(0x3dcccccd);
    assertThat(value(Codecs.floatDecimal(), "1")).isEqualTo(1.0f);
    assertThat(value(Codecs.floatDecimal(), "-2.5")).isEqualTo(-2.5f);
  }

  @Test
  void provisionalFloatOverflowIsOutOfRange() {
    // spec: VC4, R62 a
    Diags.assertSingleError(
        decode(Codecs.floatDecimal(), "1e40"), DiagnosticCode.CODEC_NUMBER_RANGE, "$");
    Diags.assertSingleError(
        decode(Codecs.floatDecimal(), "-1e40"), DiagnosticCode.CODEC_NUMBER_RANGE, "$");
  }

  @ParameterizedTest
  @CsvSource({"1, 1", "-0, 0", "-2147483648, -2147483648", "2147483647, 2147483647", "0, 0"})
  void provisionalIntegerAcceptsTheIntegerFormWithinInt32(String json, int expected) {
    // spec: VC5, R62 b
    assertThat(value(Codecs.integer(), json)).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"2147483648", "-2147483649", "9223372036854775807", "-9223372036854775808"})
  void provisionalIntegerOutsideInt32IsOutOfRange(String json) {
    // spec: VC6, R62 b
    Diags.assertSingleError(decode(Codecs.integer(), json), DiagnosticCode.CODEC_NUMBER_RANGE, "$");
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.0", "1e0", "-0.0", "2.5", "1E+0", "0.0", "100e-2"})
  void provisionalIntegersRefuseEveryDecimalForm(String json) {
    // spec: VC7, R62 b
    Diags.assertSingleError(
        decode(Codecs.integer(), json), DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
    Diags.assertSingleError(
        decode(Codecs.longInteger(), json), DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
  }

  @Test
  void provisionalLongAcceptsTheWhole64BitRange() {
    // spec: VC8, R62 b
    assertThat(value(Codecs.longInteger(), "9223372036854775807")).isEqualTo(Long.MAX_VALUE);
    assertThat(value(Codecs.longInteger(), "-9223372036854775808")).isEqualTo(Long.MIN_VALUE);
    assertThat(value(Codecs.longInteger(), "-0")).isZero();
    assertThat(value(Codecs.longInteger(), "3000000000")).isEqualTo(3_000_000_000L);
  }

  @Test
  void provisionalBooleansAcceptOnlyTrueAndFalse() {
    // spec: VC9
    assertThat(value(Codecs.bool(), "true")).isTrue();
    assertThat(value(Codecs.bool(), "false")).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "1", "\"true\"", "\"false\"", "null", "[]", "{}", "1.0"})
  void provisionalBooleansRefuseEverythingElse(String json) {
    // spec: VC10, R62 c
    Diags.assertSingleError(decode(Codecs.bool(), json), DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
  }

  @Test
  void provisionalStringsAcceptOnlyStrings() {
    // spec: R62 d
    assertThat(value(Codecs.string(), "\"abc\"")).isEqualTo("abc");
    assertThat(value(Codecs.string(), "\"\"")).isEmpty();
    assertThat(value(Codecs.string(), "\"\\ud800\"")).isEqualTo("\ud800");
    for (String json : new String[] {"1", "true", "null", "[]", "{}", "[\"a\"]"}) {
      Diags.assertSingleError(
          decode(Codecs.string(), json), DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
    }
  }

  @Test
  void provisionalEveryCodecRefusesTheTypesItDoesNotExpectAtThePathOfTheValue() {
    // spec: R62 f, R56: each codec accepts some JSON types; every other gives one TYPE_MISMATCH
    record Case(String name, Codec<?> codec, List<String> accepted) {}
    List<Case> cases =
        List.of(
            new Case("string", Codecs.string(), List.of("\"s\"")),
            new Case("bool", Codecs.bool(), List.of("true")),
            new Case("integer", Codecs.integer(), List.of("1")),
            new Case("long", Codecs.longInteger(), List.of("1")),
            new Case("decimal", Codecs.decimal(), List.of("1", "1.5")),
            new Case("float", Codecs.floatDecimal(), List.of("1", "1.5")),
            new Case("identifier", Codecs.identifier(), List.of("\"s\"")),
            new Case("list", Codecs.list(Codecs.integer()), List.of("[]")));
    for (Case c : cases) {
      for (String json : JSON_OF_EVERY_TYPE) {
        Outcome<?> outcome = decode(c.codec(), json);
        if (c.accepted().contains(json)) {
          assertThat(outcome.diagnostics()).as("%s on %s", c.name(), json).isEmpty();
        } else {
          Diags.assertSingleError(outcome, DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
        }
      }
    }
  }

  @Test
  void provisionalListsAcceptArrays() {
    // spec: VC11, R62 e
    assertThat(value(Codecs.list(Codecs.integer()), "[1,2]")).containsExactly(1, 2);
    assertThat(value(Codecs.list(Codecs.integer()), "[]")).isEmpty();
    assertThat(value(Codecs.list(Codecs.string()), "[\"b\",\"a\",\"b\"]"))
        .containsExactly("b", "a", "b");
  }

  @Test
  void provisionalALoneValueIsNotPromotedToAList() {
    // spec: VC12, R62 e
    Diags.assertSingleError(
        decode(Codecs.list(Codecs.integer()), "1"), DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
    Diags.assertSingleError(
        decode(Codecs.list(Codecs.integer()), "{\"0\":1}"),
        DiagnosticCode.CODEC_TYPE_MISMATCH,
        "$");
  }

  @Test
  void provisionalEveryFaultyElementOfAListIsReportedAtItsOwnPath() {
    // spec: Codecs.list Javadoc, R62 g
    Outcome<List<Integer>> outcome =
        decode(Codecs.list(Codecs.integer()), "[1,\"x\",2,true,2.5,3000000000]");
    assertThat(outcome.value()).isEmpty();
    Diags.assertSummary(
        outcome,
        "CODEC_TYPE_MISMATCH@$[1]",
        "CODEC_TYPE_MISMATCH@$[3]",
        "CODEC_TYPE_MISMATCH@$[4]",
        "CODEC_NUMBER_RANGE@$[5]");
  }

  @Test
  void provisionalNestedListsReportNestedPaths() {
    // spec: R61 a, R62 g
    Codec<List<List<Integer>>> codec = Codecs.list(Codecs.list(Codecs.integer()));
    assertThat(value(codec, "[[1],[],[2,3]]"))
        .containsExactly(List.of(1), List.of(), List.of(2, 3));
    Outcome<List<List<Integer>>> outcome = decode(codec, "[[1],[2,\"x\"],3]");
    Diags.assertSummary(outcome, "CODEC_TYPE_MISMATCH@$[1][1]", "CODEC_TYPE_MISMATCH@$[2]");
  }

  @Test
  void provisionalIdentifiersAreReadLikeJsonValues() {
    // spec: R22, Codecs.identifier Javadoc
    assertThat(value(Codecs.identifier(), "\"stone\""))
        .isEqualTo(Identifier.parse("minecraft:stone"));
    assertThat(value(Codecs.identifier(), "\":stone\""))
        .isEqualTo(Identifier.parse("minecraft:stone"));
    assertThat(value(Codecs.identifier(), "\"foo:bar/baz\""))
        .isEqualTo(Identifier.of("foo", "bar/baz"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"Foo:bar", "foo:Bar", "a:b:c", "..:x", "foo bar", "#logs", "#minecraft:logs"})
  void provisionalInvalidIdentifiersAreReportedAtTheValue(String text) {
    // spec: R20, R23, Codecs.identifier Javadoc ("a #tag included")
    Diags.assertSingleError(
        decode(Codecs.identifier(), "\"" + text + "\""),
        DiagnosticCode.CODEC_INVALID_IDENTIFIER,
        "$");
  }

  @Test
  void provisionalRawHandsTheNodeOverUntouched() {
    // spec: Codecs.raw Javadoc
    for (String json : JSON_OF_EVERY_TYPE) {
      JsonNode node = Jx.parse(json);
      Outcome<JsonNode> outcome = Codecs.raw().decode(node, FakeAccess.create());
      assertThat(outcome.diagnostics()).isEmpty();
      assertThat(outcome.value().orElseThrow()).isSameAs(node);
    }
  }

  @Test
  void provisionalMapConvertsTheValueAndKeepsTheDiagnostics() {
    // spec: Codec.map Javadoc
    Codec<String> codec = Codecs.integer().map(i -> "n" + i);
    assertThat(value(codec, "5")).isEqualTo("n5");
    assertThat(value(Codecs.list(Codecs.integer().map(i -> i * 2)), "[1,2]")).containsExactly(2, 4);
    Diags.assertSingleError(decode(codec, "\"x\""), DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
    Diags.assertSingleError(decode(codec, "1.0"), DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
    Diags.assertSingleError(decode(codec, "3000000000"), DiagnosticCode.CODEC_NUMBER_RANGE, "$");
  }

  @Test
  void provisionalMapNeverCallsTheConversionOnAFailure() {
    // spec: Codec.map Javadoc ("applied to the value")
    AtomicInteger calls = new AtomicInteger();
    Codec<Integer> codec =
        Codecs.integer()
            .map(
                i -> {
                  calls.incrementAndGet();
                  return i;
                });
    decode(codec, "\"x\"");
    assertThat(calls).hasValue(0);
    decode(codec, "7");
    assertThat(calls).hasValue(1);
  }

  @Test
  void provisionalACodecIsStatelessBetweenCalls() {
    // spec: Codec Javadoc ("A decoder must not keep state between calls")
    Codec<List<Integer>> codec = Codecs.list(Codecs.integer());
    for (int i = 0; i < 3; i++) {
      assertThat(value(codec, "[1,2]")).containsExactly(1, 2);
      Diags.assertSummary(decode(codec, "[1,\"x\"]"), "CODEC_TYPE_MISMATCH@$[1]");
    }
  }
}

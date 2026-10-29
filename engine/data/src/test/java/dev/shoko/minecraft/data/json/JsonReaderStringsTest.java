package dev.shoko.minecraft.data.json;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Jx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// spec: data-datapack-loading R53, R60, vectors VJ23 to VJ31 (strings, escapes, UTF-8)
class JsonReaderStringsTest {

  private static String stringOf(byte[] json) {
    return ((JsonString) Jx.read(json).value().orElseThrow()).value();
  }

  private static String stringOf(String json) {
    return stringOf(Jx.utf8(json));
  }

  @Test
  void aPlainStringIsRead() {
    assertThat(stringOf("\"hello\"")).isEqualTo("hello");
    assertThat(stringOf("\"\"")).isEmpty();
  }

  @Test
  void provisionalEveryAllowedSimpleEscapeIsDecoded() {
    // spec: R60 a
    assertThat(stringOf("\"\\\" \\\\ \\/ \\b \\f \\n \\r \\t\""))
        .isEqualTo("\" \\ / \b \f \n \r \t");
  }

  @Test
  void provisionalUnicodeEscapesAcceptUpperAndLowerCaseHexadecimal() {
    // spec: R60 a
    assertThat(stringOf("\"\\u0041\\u00e9\\u00E9\\u4E2D\\u4e2d\""))
        .isEqualTo("A\u00e9\u00e9\u4e2d\u4e2d");
  }

  @Test
  void provisionalEscapedSolidusAndNulAreValid() {
    // spec: VJ27, R60 b
    assertThat(stringOf("\"\\u0000\"")).isEqualTo("\u0000");
    assertThat(stringOf("\"\\/\"")).isEqualTo("/");
  }

  @Test
  void provisionalAnEscapedLoneHighSurrogateIsAcceptedAndKept() {
    // spec: VJ23, R60 c
    String value = stringOf("\"\\ud800\"");
    assertThat(value).hasSize(1);
    assertThat(value.charAt(0)).isEqualTo('\ud800');
  }

  @Test
  void provisionalAnEscapedLoneLowSurrogateIsAcceptedAndKept() {
    // spec: R60 c
    String value = stringOf("\"\\udc00\"");
    assertThat(value).hasSize(1);
    assertThat(value.charAt(0)).isEqualTo('\udc00');
  }

  @Test
  void provisionalASurrogatePairInTheWrongOrderIsTwoLoneUnitsInThatOrder() {
    // spec: VJ25, R60 c
    String value = stringOf("\"\\ude00\\ud83d\"");
    assertThat(value).hasSize(2);
    assertThat(value.charAt(0)).isEqualTo('\ude00');
    assertThat(value.charAt(1)).isEqualTo('\ud83d');
  }

  @Test
  void provisionalACorrectEscapedPairFormsOneCodePoint() {
    // spec: R60 c ("une paire correcte forme le point de code U+1F600")
    String value = stringOf("\"\\ud83d\\ude00\"");
    assertThat(value).hasSize(2);
    assertThat(value.codePointAt(0)).isEqualTo(0x1F600);
    assertThat(value.codePointCount(0, value.length())).isEqualTo(1);
  }

  @Test
  void provisionalARawSupplementaryCharacterIsOneCodePoint() {
    // spec: VJ24, VJ30 (bytes F0 9F 98 80)
    String value = stringOf(Jx.bytes(0x22, 0xF0, 0x9F, 0x98, 0x80, 0x22));
    assertThat(value.codePointAt(0)).isEqualTo(0x1F600);
    assertThat(value).hasSize(2);
    assertThat(stringOf("\"\ud83d\ude00\"")).isEqualTo(value);
  }

  @Test
  void provisionalANonCharacterEncodedInBytesIsValid() {
    // spec: VJ30 (EF BF BE is U+FFFE)
    assertThat(stringOf(Jx.bytes(0x22, 0xEF, 0xBF, 0xBE, 0x22))).isEqualTo("\ufffe");
  }

  @Test
  void provisionalMultiByteCharactersDecodeAsUtf8() {
    // spec: R49, R53
    assertThat(stringOf(Jx.bytes(0x22, 0xC3, 0xA9, 0xE4, 0xB8, 0xAD, 0x22)))
        .isEqualTo("\u00e9\u4e2d");
  }

  @Test
  void provisionalAKeyFollowsTheSameStringRules() {
    // spec: VJ26, R60 d
    JsonObject lone = (JsonObject) Jx.parse("{\"\\ud800\":1}");
    assertThat(lone.members().keySet()).containsExactly("\ud800");
    JsonObject escaped = (JsonObject) Jx.parse("{\"a\\u0062\\n\":1}");
    assertThat(escaped.members().keySet()).containsExactly("ab\n");
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"a\nb\"", "\"\\x41\"", "\"\\u12\"", "\"\\u00zz\"", "\"\\q\""})
  void provisionalBadEscapesAndRawNewlinesAreSyntaxErrors(String text) {
    // spec: VJ31, R60 a
    Diags.assertSingleError(Jx.read(text), DiagnosticCode.JSON_SYNTAX);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "\"\\u\"",
        "\"\\u1\"",
        "\"\\u123\"",
        "\"\\u123g\"",
        "\"\\U0041\"",
        "\"\\'\"",
        "\"\\ \"",
        "\"abc\\\"",
        "{\"a\\x\":1}"
      })
  void provisionalMoreMalformedEscapesAreSyntaxErrors(String text) {
    // spec: R60 a, d
    Diags.assertSingleError(Jx.read(text), DiagnosticCode.JSON_SYNTAX);
  }

  @ParameterizedTest
  @ValueSource(ints = {0x00, 0x01, 0x08, 0x09, 0x0A, 0x0D, 0x1F})
  void provisionalARawControlCharacterInAStringIsASyntaxError(int control) {
    // spec: R60 a (U+0000 to U+001F raw), tab included
    Diags.assertSingleError(
        Jx.read(Jx.bytes(0x22, 'a', control, 'b', 0x22)), DiagnosticCode.JSON_SYNTAX);
    Diags.assertSingleError(
        Jx.read(Jx.bytes('{', 0x22, 'k', control, 0x22, ':', '1', '}')),
        DiagnosticCode.JSON_SYNTAX);
  }

  @Test
  void provisionalDeleteAndHigherCharactersAreNotControlCharacters() {
    // spec: R60 a (only U+0000 to U+001F are refused)
    assertThat(stringOf(Jx.bytes(0x22, 0x7F, 0x22))).isEqualTo("\u007f");
  }

  @Test
  void provisionalUnterminatedStringsAreSyntaxErrors() {
    Diags.assertSingleError(Jx.read("\"abc"), DiagnosticCode.JSON_SYNTAX);
    Diags.assertSingleError(Jx.read("{\"a"), DiagnosticCode.JSON_SYNTAX);
    Diags.assertSingleError(Jx.read("[\"a\",\"b"), DiagnosticCode.JSON_SYNTAX);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ED A0 80",
        "ED BF BF",
        "C0 80",
        "C1 BF",
        "E0 80 80",
        "F0 80 80 80",
        "F4 90 80 80",
        "F5 80 80 80",
        "FF",
        "80",
        "BF",
        "C2",
        "E4 B8",
        "F0 9F 98",
        "C3 28",
        "E4 28 AD",
        "ED A0 80 ED B0 80"
      })
  void provisionalInvalidUtf8InsideAStringIsReportedAsUtf8(String hexBytes) {
    // spec: VJ28, VJ29, R53, R60 e (surrogates in bytes, overlong forms, too large, orphans)
    String[] parts = hexBytes.split(" ", -1);
    byte[] body = new byte[parts.length];
    for (int i = 0; i < parts.length; i++) {
      body[i] = (byte) Integer.parseInt(parts[i], 16);
    }
    byte[] json = Jx.concat(Jx.bytes(0x22), body, Jx.bytes(0x22));
    Diags.assertSingleError(Jx.read(json), DiagnosticCode.JSON_UTF8);
  }

  @Test
  void provisionalInvalidUtf8InAKeyOrBetweenTokensIsReportedAsUtf8() {
    // spec: R53, R61 c
    Diags.assertSingleError(
        Jx.read(Jx.bytes('{', 0x22, 0xC0, 0x80, 0x22, ':', '1', '}')), DiagnosticCode.JSON_UTF8);
    Diags.assertSingleError(Jx.read(Jx.bytes('[', 0x80, ']')), DiagnosticCode.JSON_UTF8);
  }

  @Test
  void provisionalEscapedSurrogatesAreNotUtf8Errors() {
    // spec: R60 c versus R60 e: the same units are fine as escapes and refused as bytes
    assertThat(Jx.read("\"\\ud800\"").diagnostics()).isEmpty();
    Diags.assertSingleError(
        Jx.read(Jx.bytes(0x22, 0xED, 0xA0, 0x80, 0x22)), DiagnosticCode.JSON_UTF8);
  }

  @Test
  void aLongStringIsRead() {
    String body = "x".repeat(1_000_000);
    assertThat(stringOf("\"" + body + "\"")).hasSize(1_000_000);
  }
}

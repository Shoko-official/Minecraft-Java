package dev.shoko.minecraft.data.json;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Jx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// spec: data-datapack-loading R52, R53, R54, R61 c, vectors VJ12, VJ32 to VJ36 and the table of
// "Vecteurs de lecture attendus" (invalid files: one diagnostic, precedence)
class JsonReaderErrorsTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"a\":1,}",
        "// x\n{}",
        "/* c */ {}",
        "{\"a\":1 /* c */}",
        "# x\n{}",
        "{'a':1}",
        "['a']",
        "{a:1}",
        "{\"a\":b}",
        "NaN",
        "[NaN]",
        "[Infinity]",
        "[1,]",
        "[,1]",
        "[,]",
        "{,}",
        "{\"a\" 1}",
        "{\"a\":}",
        "{\"a\"}",
        "{1:2}",
        "[1 2]",
        "{\"a\":1 \"b\":2}",
        "tru",
        "True",
        "NULL",
        "nul",
        "]",
        "}",
        "{]",
        "[}",
        "[",
        "{",
        "{\"a\":1",
        "[1,2",
        "{\"a\"",
        "{\"a\":",
        "[\"a\" \"b\"]",
        "{\"a\":1,\"b\"}"
      })
  void provisionalNonStrictOrBrokenSyntaxIsASingleSyntaxError(String text) {
    // spec: R52, R54, R61 c
    Diags.assertSingleError(Jx.read(text), DiagnosticCode.JSON_SYNTAX);
  }

  @Test
  void provisionalABomBeforeAnObjectIsABomError() {
    // spec: VJ32, R53
    Diags.assertSingleError(
        Jx.read(Jx.bytes(0xEF, 0xBB, 0xBF, 0x7B, 0x7D)), DiagnosticCode.JSON_BOM);
  }

  @Test
  void provisionalABomHasPrecedenceOverInvalidBytesAfterIt() {
    // spec: VJ32, R61 c (BOM, then invalid UTF-8, then reading errors)
    Diags.assertSingleError(Jx.read(Jx.bytes(0xEF, 0xBB, 0xBF, 0x80)), DiagnosticCode.JSON_BOM);
  }

  @Test
  void provisionalAFileMadeOfOnlyABomIsABomError() {
    // spec: VJ32, R61 c ("un fichier reduit a un BOM, JSON_BOM")
    Diags.assertSingleError(Jx.read(Jx.bytes(0xEF, 0xBB, 0xBF)), DiagnosticCode.JSON_BOM);
  }

  @Test
  void provisionalABomBeforeAnyRootIsABomError() {
    // spec: R53
    Diags.assertSingleError(
        Jx.read(Jx.concat(Jx.bytes(0xEF, 0xBB, 0xBF), Jx.utf8("[1]"))), DiagnosticCode.JSON_BOM);
    Diags.assertSingleError(
        Jx.read(Jx.concat(Jx.bytes(0xEF, 0xBB, 0xBF), Jx.utf8("   "))), DiagnosticCode.JSON_BOM);
  }

  @Test
  void provisionalAByteOrderMarkInTheMiddleOfTheFileIsNotABomError() {
    // spec: R53 ("BOM UTF-8 en tete"): elsewhere U+FEFF is just not JSON syntax
    Outcome<JsonNode> outcome = Jx.read("[1,\ufeff2]");
    assertThat(outcome.value()).isEmpty();
    assertThat(Diags.codes(outcome)).containsExactly(DiagnosticCode.JSON_SYNTAX);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "   ", "\n", "\r\n", "\t", "   \n", " \t\r\n \t "})
  void provisionalEmptyAndBlankFilesAreEmptyErrors(String text) {
    // spec: VJ34, R53
    Diags.assertSingleError(Jx.read(text), DiagnosticCode.JSON_EMPTY);
  }

  @Test
  void provisionalAnEmptyByteArrayIsAnEmptyError() {
    // spec: VJ34
    Diags.assertSingleError(Jx.read(new byte[0]), DiagnosticCode.JSON_EMPTY);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{} {}",
        "{}x",
        "[] []",
        "1 2",
        "\"a\" \"b\"",
        "{}}",
        "[]]",
        "true false",
        "{}\n,"
      })
  void provisionalContentAfterTheRootValueIsATrailingError(String text) {
    // spec: VJ35, R53
    Diags.assertSingleError(Jx.read(text), DiagnosticCode.JSON_TRAILING);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"{}\n", "{}\r\n", "{} ", "{}\t\n  ", " {} ", "[]\n\n", "0\n", "\"a\"\r\n"})
  void provisionalTrailingWhitespaceIsValid(String text) {
    // spec: VJ35 ("{} LF" is valid)
    Jx.parse(text);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "80 7B",
        "7B 80",
        "7B 22 80 22 7D",
        "7B 7D 80",
        "5B C3 5D",
        "7B 22 61 22 3A 31 7D FF"
      })
  void provisionalInvalidBytesArePreferredToSyntaxErrors(String hexBytes) {
    // spec: VJ33, R61 c (invalid UTF-8 anywhere in the file has precedence over syntax)
    String[] parts = hexBytes.split(" ", -1);
    int[] values = new int[parts.length];
    for (int i = 0; i < parts.length; i++) {
      values[i] = Integer.parseInt(parts[i], 16);
    }
    Diags.assertSingleError(Jx.read(Jx.bytes(values)), DiagnosticCode.JSON_UTF8);
  }

  @Test
  void provisionalInvalidBytesAtTheEndBeatAnEarlierSyntaxError() {
    // spec: R61 c: UTF-8 is checked on the whole file, before any reading error
    byte[] text = Jx.concat(Jx.utf8("{,} "), Jx.bytes(0x80));
    Diags.assertSingleError(Jx.read(text), DiagnosticCode.JSON_UTF8);
  }

  @Test
  void provisionalInvalidBytesAtTheEndBeatAnEarlierNumberRangeError() {
    // spec: R61 c
    byte[] text = Jx.concat(Jx.utf8("[1e999] "), Jx.bytes(0xC3));
    Diags.assertSingleError(Jx.read(text), DiagnosticCode.JSON_UTF8);
  }

  @Test
  void provisionalAFormFeedBeforeTheRootIsASyntaxError() {
    // spec: VJ36, R52 (whitespace is only space, tab, line feed, carriage return)
    Diags.assertSingleError(Jx.read(Jx.bytes(0x0C, 0x7B, 0x7D)), DiagnosticCode.JSON_SYNTAX);
  }

  @Test
  void provisionalANoBreakSpaceBeforeTheRootIsASyntaxError() {
    // spec: VJ36
    Diags.assertSingleError(Jx.read(Jx.bytes(0xC2, 0xA0, 0x7B, 0x7D)), DiagnosticCode.JSON_SYNTAX);
  }

  @ParameterizedTest
  @ValueSource(ints = {0x0B, 0x0C, 0x1C, 0x1F, 0x00, 0x01})
  void provisionalOtherControlCharactersBetweenTokensAreSyntaxErrors(int control) {
    // spec: R52, table JSON_SYNTAX ("espace hors de espace, \t, \n, \r")
    byte[] json = Jx.bytes('[', '1', control, ',', '2', ']');
    Diags.assertSingleError(Jx.read(json), DiagnosticCode.JSON_SYNTAX);
  }

  @Test
  void provisionalUnicodeSpacesBetweenTokensAreSyntaxErrors() {
    // spec: VJ36 (U+00A0), R52
    Diags.assertSingleError(Jx.read("[1,\u00a02]"), DiagnosticCode.JSON_SYNTAX);
    Diags.assertSingleError(Jx.read("[1,\u20282]"), DiagnosticCode.JSON_SYNTAX);
    Diags.assertSingleError(Jx.read("[1,\u30002]"), DiagnosticCode.JSON_SYNTAX);
  }

  @Test
  void provisionalAFileWithSeveralProblemsGivesOnlyTheFirstOneInTextOrder() {
    // spec: R61 c
    Outcome<JsonNode> outcome = Jx.read("[01, +1, 1e999, \"\\q\", {'a':1}]");
    Diags.assertSingleError(outcome, DiagnosticCode.JSON_SYNTAX);
    Diags.assertSingleError(Jx.read("[1e999, 01]"), DiagnosticCode.JSON_NUMBER_RANGE);
    Diags.assertSingleError(
        Jx.read("[99999999999999999999, \"\\q\"]"), DiagnosticCode.JSON_NUMBER_RANGE);
  }

  @Test
  void provisionalAnInvalidFileNeverThrowsAndNeverReturnsAValue() {
    // spec: R54, JsonReader Javadoc ("It never throws")
    for (String text : new String[] {"", "{", "\u0000", "\"", "[[[[", "}{", "1 1", "\ud800"}) {
      Outcome<JsonNode> outcome = JsonReader.read(text, Jx.ORIGIN);
      assertThat(outcome.value()).as(text).isEmpty();
      assertThat(outcome.diagnostics()).as(text).hasSize(1);
    }
  }

  @Test
  void syntaxErrorsCarryTheFileOrigin() {
    // spec: JsonReader Javadoc ("attached to every node and diagnostic")
    Outcome<JsonNode> outcome = Jx.read("{,}");
    assertThat(outcome.diagnostics().get(0).origin()).isEqualTo(Jx.ORIGIN);
  }
}

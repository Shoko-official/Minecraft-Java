package dev.shoko.minecraft.data.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Jx;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// spec: data-datapack-loading R59 f (no limit on the number of digits), R52, R60 and R61 c (the
// first error in the order of the text). Long numeric literals take another path in the reader
// than the short ones (ADR 0006): these vectors cross that boundary in every direction.
class LongNumbersHuntTest {

  private static void assertSyntaxError(String text) {
    Diags.assertSingleError(Jx.read(text), DiagnosticCode.JSON_SYNTAX);
  }

  @Test
  void malformedLongNumbersAreSyntaxErrors() {
    assertSyntaxError("[-12345678901234567890.]");
    assertSyntaxError("[0123456789012345678901]");
    assertSyntaxError("[123456789012345678901x]");
    assertSyntaxError("[1.5e0123456789012345678901x]");
    assertSyntaxError("[-]");
    assertSyntaxError("[-" + "0".repeat(30) + "]");
    assertSyntaxError("[" + "7".repeat(30) + ".]");
    assertSyntaxError("[" + "7".repeat(30) + "e]");
    assertSyntaxError("[" + "7".repeat(30) + "//x\n]");
  }

  // The first deviation in the order of the text is the character after the number, whatever the
  // length of the number and whether its value would be out of range.
  @ParameterizedTest
  @ValueSource(
      strings = {
        "[12345678901234567890123\f]",
        "[1e999\f]",
        "[99999999999999999999\f]",
        "[1\f]",
        "[12345678901234567890123\"a\"]",
        "[1e999\"a\"]"
      })
  void aNumberFollowedByACharacterOutsideJsonIsASyntaxErrorWhateverItsLength(String text) {
    assertSyntaxError(text);
  }

  @Test
  void aLongNumberInsideAStringAfterAnEscapedQuoteIsNotANumber() {
    JsonNode root = Jx.parse("[\"a\\\"-87192254824593325083\",-7]");
    List<JsonNode> elements = ((JsonArray) root).elements();
    assertThat(elements).hasSize(2);
    assertThat(((JsonString) elements.get(0)).value()).isEqualTo("a\"-87192254824593325083");
    assertThat(((JsonNumber) elements.get(1)).longValue()).isEqualTo(-7);
    assertThat(Jx.parse("[\"a\\\"-87192254824593325083\\\\\",-7]")).isNotNull();
    assertThat(Jx.parse("[\"\\\"\\\\\",-7]")).isNotNull();
    assertSyntaxError("[\"\\\"1234567890123456789012\\\",-7]");
  }

  @Test
  void integersAroundTheSixtyFourBitLimitAreClassifiedWhateverTheirLength() {
    for (String valid :
        List.of("9223372036854775807", "-9223372036854775808", "1000000000000000000")) {
      assertThat(
              ((JsonNumber) ((JsonArray) Jx.parse("[" + valid + "]")).elements().get(0))
                  .longValue())
          .isEqualTo(Long.parseLong(valid));
    }
    for (String range :
        List.of(
            "9223372036854775808",
            "-9223372036854775809",
            "1" + "0".repeat(19),
            "1" + "0".repeat(24),
            "1" + "0".repeat(39),
            "1" + "0".repeat(70),
            "-" + "9".repeat(45))) {
      Diags.assertSingleError(Jx.read("[" + range + "]"), DiagnosticCode.JSON_NUMBER_RANGE, "$[0]");
    }
  }

  @Test
  void longDecimalsKeepTheirLiteralAndTheirRank() {
    List<String> literals =
        List.of(
            "100000000000000000000.5",
            "-12345678901234567890.25e3",
            "0." + "1".repeat(30),
            "1" + "0".repeat(30) + ".5",
            "1" + "0".repeat(17),
            "0.1234567890123456",
            "0.12345678901234567",
            "0.123456789012345678",
            "1e0000000000000000001",
            "1".repeat(400) + "e-390");
    String text =
        "{\"12345678901234567890123\":[\"x-12345678901234567890123\",7,"
            + String.join(",", literals)
            + ",\"\\\\\",-3]}";
    Outcome<JsonNode> outcome = Jx.read(text);
    assertThat(outcome.diagnostics()).isEmpty();
    JsonArray array =
        (JsonArray)
            ((dev.shoko.minecraft.data.json.JsonObject) outcome.value().orElseThrow())
                .members()
                .get("12345678901234567890123");
    List<JsonNode> elements = array.elements();
    assertThat(elements).hasSize(literals.size() + 4);
    for (int i = 0; i < literals.size(); i++) {
      JsonNumber number = (JsonNumber) elements.get(i + 2);
      assertThat(number.literal()).as("literal %d", i).isEqualTo(literals.get(i));
      assertThat(number.isInteger())
          .as("integer %d", i)
          .isEqualTo(literals.get(i).matches("-?[0-9]+"));
    }
    assertThat(((JsonNumber) elements.get(1)).longValue()).isEqualTo(7);
    assertThat(((JsonNumber) elements.get(elements.size() - 1)).longValue()).isEqualTo(-3);
    assertThat(((JsonNumber) elements.get(elements.size() - 3)).doubleValue())
        .isEqualTo(Double.parseDouble(literals.get(literals.size() - 1)));
  }

  @Test
  void aRangeErrorIsReportedAtThePathOfTheLongNumber() {
    Diags.assertSingleError(
        Jx.read("{\"a\":[1,2,100000000000000000000000]}"),
        DiagnosticCode.JSON_NUMBER_RANGE,
        "$.a[2]");
    Diags.assertSingleError(
        Jx.read("[100000000000000000000000,1e999]"), DiagnosticCode.JSON_NUMBER_RANGE, "$[0]");
  }
}

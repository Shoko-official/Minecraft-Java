package dev.shoko.minecraft.data.json;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Jx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

// spec: data-datapack-loading R50, R59, vectors VJ13 to VJ22 (numbers)
class JsonReaderNumbersTest {

  private static JsonNumber number(String text) {
    return (JsonNumber) Jx.parse(text);
  }

  private static long bits(String text) {
    JsonNumber number = number(text);
    assertThat(number.isInteger()).as("%s is a decimal", text).isFalse();
    return Double.doubleToRawLongBits(number.doubleValue());
  }

  private static long hex(String digits) {
    return Long.parseUnsignedLong(digits, 16);
  }

  @Test
  void provisionalIntegerFormKeepsTheWrittenLiteralAndTheExactValue() {
    // spec: R50, R59 a
    JsonNumber number = number("20");
    assertThat(number.isInteger()).isTrue();
    assertThat(number.longValue()).isEqualTo(20);
    assertThat(number.literal()).isEqualTo("20");
  }

  @Test
  void provisionalNegativeZeroIntegerIsValidAndIsTheIntegerZero() {
    // spec: VJ13, R59 c
    JsonNumber zero = number("0");
    JsonNumber negativeZero = number("-0");
    assertThat(negativeZero.isInteger()).isTrue();
    assertThat(negativeZero.longValue()).isZero();
    assertThat(negativeZero.sameValueAs(zero)).isTrue();
    assertThat(zero.sameValueAs(negativeZero)).isTrue();
    assertThat(Double.doubleToRawLongBits(negativeZero.doubleValue())).isZero();
  }

  @Test
  void provisionalNegativeZeroDecimalKeepsItsSignAndDiffersFromPositiveZero() {
    // spec: VJ14, R59 e
    JsonNumber negative = number("-0.0");
    JsonNumber positive = number("0.0");
    assertThat(bits("-0.0")).isEqualTo(hex("8000000000000000"));
    assertThat(bits("0.0")).isEqualTo(hex("0000000000000000"));
    assertThat(negative.sameValueAs(positive)).isFalse();
    assertThat(positive.sameValueAs(negative)).isFalse();
  }

  @Test
  void provisionalExponentFormsAreDecimals() {
    // spec: VJ15, R59 a
    assertThat(bits("1e5")).isEqualTo(hex("40f86a0000000000"));
    assertThat(bits("1E+5")).isEqualTo(hex("40f86a0000000000"));
    assertThat(bits("1.0e0")).isEqualTo(hex("3ff0000000000000"));
    assertThat(number("1e5").sameValueAs(number("1E+5"))).isTrue();
    assertThat(number("1e5").literal()).isEqualTo("1e5");
  }

  @Test
  void provisionalOneAndOnePointZeroAreDifferentNodes() {
    // spec: VJ16, R29, R59 e
    JsonNumber integer = number("1");
    JsonNumber decimal = number("1.0");
    assertThat(integer.isInteger()).isTrue();
    assertThat(decimal.isInteger()).isFalse();
    assertThat(integer.sameValueAs(decimal)).isFalse();
    assertThat(decimal.sameValueAs(integer)).isFalse();
  }

  @Test
  void provisionalEachElementOfAnArrayKeepsItsOwnForm() {
    // spec: VJ16 ("dans [1,1.0], forme conservee par element")
    JsonArray array = (JsonArray) Jx.parse("[1,1.0]");
    assertThat(((JsonNumber) array.elements().get(0)).isInteger()).isTrue();
    assertThat(((JsonNumber) array.elements().get(1)).isInteger()).isFalse();
  }

  @Test
  void provisionalIntegersAtThe64BitBoundsAreValid() {
    // spec: VJ17, R59 b
    assertThat(number("9223372036854775807").longValue()).isEqualTo(Long.MAX_VALUE);
    assertThat(number("-9223372036854775808").longValue()).isEqualTo(Long.MIN_VALUE);
    assertThat(number("9223372036854775807").isInteger()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "9223372036854775808",
        "-9223372036854775809",
        "123456789012345678901234567890",
        "-123456789012345678901234567890",
        "100000000000000000000"
      })
  void provisionalIntegersBeyond64BitsAreOutOfRange(String text) {
    // spec: VJ18, R59 b
    Diags.assertSingleError(Jx.read(text), DiagnosticCode.JSON_NUMBER_RANGE, "$");
  }

  @ParameterizedTest
  @ValueSource(strings = {"1e999", "-1e999", "1.7976931348623159E308", "-1.7976931348623159E308"})
  void provisionalDecimalsThatOverflowToInfinityAreOutOfRange(String text) {
    // spec: VJ19, R59 d
    Diags.assertSingleError(Jx.read(text), DiagnosticCode.JSON_NUMBER_RANGE, "$");
  }

  @Test
  void provisionalOutOfRangeNumbersReportThePathOfTheNumber() {
    // spec: R59 b, R61 b (the path is verified for JSON_NUMBER_RANGE)
    Diags.assertSingleError(
        Jx.read("{\"a\":[1,2e999]}"), DiagnosticCode.JSON_NUMBER_RANGE, "$.a[1]");
    Diags.assertSingleError(
        Jx.read("{\"x\":{\"y\":99999999999999999999}}"), DiagnosticCode.JSON_NUMBER_RANGE, "$.x.y");
  }

  @Test
  void provisionalLargestFiniteDoubleIsValid() {
    // spec: VJ20, R50
    assertThat(bits("1.7976931348623158E308")).isEqualTo(hex("7fefffffffffffff"));
    assertThat(bits("1.7976931348623157E308")).isEqualTo(hex("7fefffffffffffff"));
    assertThat(number("1.7976931348623157E308").doubleValue()).isEqualTo(Double.MAX_VALUE);
  }

  @Test
  void provisionalUnderflowGivesTheZeroOfItsSignAndDenormalsAreKept() {
    // spec: VJ21, R59 d
    assertThat(bits("1e-999")).isEqualTo(hex("0000000000000000"));
    assertThat(bits("-1e-999")).isEqualTo(hex("8000000000000000"));
    assertThat(bits("2E-324")).isEqualTo(hex("0000000000000000"));
    assertThat(bits("4.9E-324")).isEqualTo(hex("0000000000000001"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "+1",
        ".5",
        "1.",
        "01",
        "-01",
        "00",
        "-",
        "1e",
        "1e+",
        "1e-",
        "0x10",
        "1_000",
        "Infinity",
        "-Infinity",
        "NaN",
        "--1",
        "1.e5",
        "1..2",
        "- 1",
        "-.5",
        "+.5",
        "1E",
        "0e",
        ".e1"
      })
  void provisionalMalformedNumbersAreSyntaxErrors(String text) {
    // spec: VJ22, R52, R59 a
    Diags.assertSingleError(Jx.read(text), DiagnosticCode.JSON_SYNTAX);
  }

  @ParameterizedTest
  @ValueSource(strings = {"[+1]", "[.5]", "{\"a\":01}", "[1,2,-]", "{\"a\":1e}"})
  void provisionalMalformedNumbersInsideValuesAreSyntaxErrors(String text) {
    // spec: R52, R59 a
    Diags.assertSingleError(Jx.read(text), DiagnosticCode.JSON_SYNTAX);
  }

  @ParameterizedTest
  @CsvSource({
    "0.1, 3fb999999999999a",
    "1.5, 3ff8000000000000",
    "-2.5, c004000000000000",
    "2.9802322E-8, 3e5ffffff904122a",
    "5.9604645E-8, 3e7000000102f4fd",
    "1.0E-4, 3f1a36e2eb1c432d",
    "9.999999747378752E-6, 3ee4f8b580000000",
    "-0.10000000149011612, bfb99999a0000000",
    "1E+22, 4480f0cf064dd592",
    "123456789.123456789, 419d6f34547e6b75"
  })
  void provisionalDecimalsAreConvertedToTheCorrectlyRoundedBinary64(
      String text, String expectedBits) {
    // spec: R50, R59 d (round half to even); bits computed with Python struct.pack, outside the
    // repository
    assertThat(bits(text)).isEqualTo(hex(expectedBits));
  }

  @Test
  void decimalsAreTheNearestDoubleLikeTheStandardLibrary() {
    // spec: R50 ("2.9802322E-8: double le plus proche")
    for (String text :
        new String[] {
          "2.9802322E-8", "5.9604645E-8", "1.0E-4", "9.999999747378752E-6", "-0.10000000149011612"
        }) {
      assertThat(number(text).doubleValue()).as(text).isEqualTo(Double.parseDouble(text));
    }
  }

  @Test
  void provisionalHalfwayCasesRoundToEven() {
    // spec: R59 d: 2^53 + 1 is halfway between 2^53 and 2^53 + 2; 2^53 + 3 between +2 and +4
    assertThat(number("9007199254740993.0").doubleValue()).isEqualTo(9007199254740992.0);
    assertThat(number("9007199254740995.0").doubleValue()).isEqualTo(9007199254740996.0);
    assertThat(number("9007199254740993e0").doubleValue()).isEqualTo(9007199254740992.0);
  }

  @Test
  void provisionalAnIntegerKeepsAll64BitsAndConvertsToTheNearestDouble() {
    // spec: R59 b, JsonNumber.doubleValue, VC2
    JsonNumber number = number("9007199254740993");
    assertThat(number.isInteger()).isTrue();
    assertThat(number.longValue()).isEqualTo(9007199254740993L);
    assertThat(number.doubleValue()).isEqualTo(9007199254740992.0);
  }

  @Test
  void provisionalAVeryLongMantissaIsConvertedWithTheCorrectRounding() {
    // spec: R59 f (no limit on the number of digits)
    String longZeros = "1." + "0".repeat(400);
    assertThat(number(longZeros).doubleValue()).isEqualTo(1.0);
    String shifted = "0." + "0".repeat(399) + "1e400";
    assertThat(number(shifted).doubleValue()).isEqualTo(1.0);
    String justAboveHalfway = "9007199254740993." + "0".repeat(400) + "1";
    assertThat(number(justAboveHalfway).doubleValue()).isEqualTo(9007199254740994.0);
    String manyDigitsInteger = "1" + "0".repeat(400);
    Outcome<JsonNode> outcome = Jx.read(manyDigitsInteger);
    Diags.assertSingleError(outcome, DiagnosticCode.JSON_NUMBER_RANGE);
  }

  @Test
  void provisionalAHugeExponentWithAZeroMantissaIsStillZero() {
    // spec: R59 d (underflow and overflow concern the value, not the exponent text)
    assertThat(bits("0e999999")).isEqualTo(hex("0000000000000000"));
    assertThat(bits("0.0e-999999")).isEqualTo(hex("0000000000000000"));
    assertThat(bits("-0e999999")).isEqualTo(hex("8000000000000000"));
  }

  @Test
  void numbersInsideStructuresAreRead() {
    JsonObject root = (JsonObject) Jx.parse("{\"a\":[-1,0.5,2e2],\"b\":-7}");
    JsonArray a = (JsonArray) root.get("a").orElseThrow();
    assertThat(((JsonNumber) a.elements().get(0)).longValue()).isEqualTo(-1);
    assertThat(((JsonNumber) a.elements().get(1)).doubleValue()).isEqualTo(0.5);
    assertThat(((JsonNumber) a.elements().get(2)).doubleValue()).isEqualTo(200.0);
    assertThat(((JsonNumber) root.get("b").orElseThrow()).longValue()).isEqualTo(-7);
  }
}

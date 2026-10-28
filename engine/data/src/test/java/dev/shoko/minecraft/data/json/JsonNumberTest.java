package dev.shoko.minecraft.data.json;

import static dev.shoko.minecraft.data.testing.Nodes.arr;
import static dev.shoko.minecraft.data.testing.Nodes.bool;
import static dev.shoko.minecraft.data.testing.Nodes.num;
import static dev.shoko.minecraft.data.testing.Nodes.str;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;
import dev.shoko.minecraft.data.testing.Expect;
import dev.shoko.minecraft.data.testing.Jx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// spec: data-datapack-loading R29, R50, R59 a, c, e (the JsonNumber node, built without the reader)
class JsonNumberTest {

  @ParameterizedTest
  @ValueSource(
      strings = {"0", "-0", "1", "-1", "20", "9223372036854775807", "-9223372036854775808"})
  void provisionalIntegerForms(String literal) {
    // spec: R59 a
    assertThat(num(literal).isInteger()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.0", "-0.0", "0.0", "1e5", "1E+5", "2.9802322E-8", "1e0", "0e0", "5.5"})
  void provisionalDecimalForms(String literal) {
    // spec: R59 a ("1.0, 1e5, -0.0 sont decimaux")
    assertThat(num(literal).isInteger()).isFalse();
  }

  @Test
  void provisionalLongValueIsExact() {
    // spec: R50, R59 b
    assertThat(num("20").longValue()).isEqualTo(20);
    assertThat(num("-7").longValue()).isEqualTo(-7);
    assertThat(num("9223372036854775807").longValue()).isEqualTo(Long.MAX_VALUE);
    assertThat(num("-9223372036854775808").longValue()).isEqualTo(Long.MIN_VALUE);
  }

  @Test
  void provisionalNegativeZeroIntegerHasValueZero() {
    // spec: R59 c
    assertThat(num("-0").longValue()).isZero();
  }

  @Test
  void longValueOfADecimalIsAnIllegalState() {
    // spec: JsonNumber.longValue
    Expect.illegalState(() -> num("1.0").longValue());
    Expect.illegalState(() -> num("1e2").longValue());
  }

  @Test
  void provisionalDoubleValueOfADecimalKeepsTheSignOfZero() {
    // spec: R59 e
    assertThat(Double.doubleToRawLongBits(num("-0.0").doubleValue())).isEqualTo(Long.MIN_VALUE);
    assertThat(Double.doubleToRawLongBits(num("0.0").doubleValue())).isZero();
    assertThat(Double.doubleToRawLongBits(num("-0").doubleValue())).isZero();
  }

  @Test
  void provisionalDoubleValueOfAnIntegerIsTheNearestDouble() {
    // spec: JsonNumber.doubleValue, VC2
    assertThat(num("9007199254740993").doubleValue()).isEqualTo(9007199254740992.0);
    assertThat(num("20").doubleValue()).isEqualTo(20.0);
    assertThat(num("-9223372036854775808").doubleValue()).isEqualTo(-9.223372036854776E18);
  }

  @Test
  void provisionalDoubleValueOfDecimals() {
    // spec: R50, R59 d
    assertThat(num("1.7976931348623157E308").doubleValue()).isEqualTo(Double.MAX_VALUE);
    assertThat(num("4.9E-324").doubleValue()).isEqualTo(Double.MIN_VALUE);
    assertThat(num("0.1").doubleValue()).isEqualTo(0.1);
    assertThat(num("1e5").doubleValue()).isEqualTo(100000.0);
  }

  @Test
  void literalIsKeptAsWritten() {
    // spec: JsonNumber Javadoc (record component literal)
    assertThat(num("2.9802322E-8").literal()).isEqualTo("2.9802322E-8");
    assertThat(num("-0").literal()).isEqualTo("-0");
  }

  @Test
  void typeIsNumber() {
    assertThat(num("1").type()).isEqualTo(JsonType.NUMBER);
    assertThat(num("1.0").type()).isEqualTo(JsonType.NUMBER);
  }

  @Test
  void provisionalAnIntegerAndADecimalOfTheSameValueAreDifferent() {
    // spec: R29, R59 e
    assertThat(num("1").sameValueAs(num("1.0"))).isFalse();
    assertThat(num("1.0").sameValueAs(num("1"))).isFalse();
    assertThat(num("0").sameValueAs(num("0.0"))).isFalse();
    assertThat(num("100000").sameValueAs(num("1e5"))).isFalse();
  }

  @Test
  void provisionalIntegersAreEqualWhenTheirValuesAre() {
    // spec: R59 c
    assertThat(num("-0").sameValueAs(num("0"))).isTrue();
    assertThat(num("7").sameValueAs(num("7"))).isTrue();
    assertThat(num("7").sameValueAs(num("8"))).isFalse();
    assertThat(num("-7").sameValueAs(num("7"))).isFalse();
  }

  @Test
  void provisionalDecimalsAreEqualWhenTheirBitPatternsAre() {
    // spec: R59 e
    assertThat(num("1.0").sameValueAs(num("1.00"))).isTrue();
    assertThat(num("1e5").sameValueAs(num("100000.0"))).isTrue();
    assertThat(num("-0.0").sameValueAs(num("0.0"))).isFalse();
    assertThat(num("-0.0").sameValueAs(num("-0e5"))).isTrue();
    assertThat(num("0.1").sameValueAs(num("0.10000000000000000555"))).isTrue();
    assertThat(num("0.1").sameValueAs(num("0.2"))).isFalse();
  }

  @Test
  void aNumberIsNeverTheSameValueAsAnotherType() {
    assertThat(num("1").sameValueAs(str("1"))).isFalse();
    assertThat(num("1").sameValueAs(bool(true))).isFalse();
    assertThat(num("1").sameValueAs(arr(num("1")))).isFalse();
  }

  @Test
  void locationDoesNotMatterForSameValue() {
    // spec: JsonNode.sameValueAs ("ignoring file, path")
    JsonNumber elsewhere =
        new JsonNumber(new FileOrigin("other", "data/y.json"), JsonPath.root().field("a"), "5");
    assertThat(num("5").sameValueAs(elsewhere)).isTrue();
    assertThat(elsewhere.origin()).isNotEqualTo(Jx.ORIGIN);
  }

  @Test
  void recordEqualityComparesTheWrittenLiteral() {
    // spec: JsonNumber Javadoc: "-0" differs from "0" for equals, not for sameValueAs
    assertThat(num("-0")).isNotEqualTo(num("0"));
    assertThat(num("5")).isEqualTo(num("5"));
  }
}

package dev.shoko.minecraft.core.nbt.snbt;

import static dev.shoko.minecraft.core.nbt.Failures.assertFails;
import static dev.shoko.minecraft.core.nbt.Failures.failure;
import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.doubleTag;
import static dev.shoko.minecraft.core.nbt.Tags.floatTag;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.longTag;
import static dev.shoko.minecraft.core.nbt.Tags.shortTag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.TagType;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** SNBT numbers beyond table T (R46), all provisional until probe PS1. */
class SnbtParserNumbersTest {

  static Stream<Arguments> typeSuffixes() {
    return Stream.of(
        arguments("1b", byteTag(1)),
        arguments("1s", shortTag(1)),
        arguments("1S", shortTag(1)),
        arguments("1i", intTag(1)),
        arguments("1I", intTag(1)),
        arguments("1l", longTag(1L)),
        arguments("1L", longTag(1L)),
        arguments("1f", floatTag(1.0f)),
        arguments("1d", doubleTag(1.0)),
        arguments("1D", doubleTag(1.0)),
        arguments("-1", intTag(-1)),
        arguments("0", intTag(0)),
        arguments("1e3f", floatTag(1000.0f)),
        arguments("1E3", doubleTag(1000.0)),
        arguments("1.2E-3", doubleTag(0.0012)),
        arguments("0.0", doubleTag(0.0)),
        arguments("-0.0f", new FloatTag(0x80000000)),
        arguments("1_000_000L", longTag(1000000L)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("typeSuffixes")
  void parsesTypeSuffixCaseInsensitively(String text, NbtTag expected) throws NbtException {
    // spec: core-nbt R46 (suffixes b s i l f d, any case; '.' or exponent makes a Double)
    assertThat(SnbtParser.parse(text)).isEqualTo(expected);
  }

  static Stream<Arguments> bounds() {
    return Stream.of(
        arguments("-128b", byteTag(-128)),
        arguments("-32768s", shortTag(-32768)),
        arguments("-2147483648", intTag(Integer.MIN_VALUE)),
        arguments("9223372036854775807L", longTag(Long.MAX_VALUE)),
        arguments("-9223372036854775808L", longTag(Long.MIN_VALUE)),
        arguments("3.4028235e38f", floatTag(Float.MAX_VALUE)),
        arguments("1.0e308", doubleTag(1.0e308)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("bounds")
  void acceptsValuesAtTypeBounds(String text, NbtTag expected) throws NbtException {
    // spec: core-nbt R46, "Cas limites" (bounds of each type)
    assertThat(SnbtParser.parse(text)).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        "-129b",
        "-32769s",
        "-2147483649",
        "9223372036854775808L",
        "-9223372036854775809L",
        "128sb",
        "253sb",
        "256ub",
        "65536us",
        "3.5e38f",
        "-1.0e400",
        "1.0e400d"
      })
  void rejectsValueOutsideTypeRange(String text) {
    // spec: core-nbt R46 (out of range is an error; overflow to infinity too; 253sb documented)
    assertFails(() -> SnbtParser.parse(text), Reason.SNBT_NUMBER_OUT_OF_RANGE, 0);
  }

  @Test
  void numberOutOfRangeInsideCompoundIsLocatedAtItsFirstCharacter() {
    // spec: core-nbt R46; offset: first character of the number
    assertFails(() -> SnbtParser.parse("{a: 128b}"), Reason.SNBT_NUMBER_OUT_OF_RANGE, 4);
  }

  static Stream<Arguments> signSuffixes() {
    return Stream.of(
        arguments("255ub", byteTag(-1)),
        arguments("127sb", byteTag(127)),
        arguments("-128sb", byteTag(-128)),
        arguments("65535us", shortTag(-1)),
        arguments("4294967295ui", intTag(-1)),
        arguments("18446744073709551615ul", longTag(-1L)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("signSuffixes")
  void unsignedSuffixReinterpretsBits(String text, NbtTag expected) throws NbtException {
    // spec: core-nbt R46 (240ub = Byte -16), provisional extension to every integer type
    assertThat(SnbtParser.parse(text)).isEqualTo(expected);
  }

  static Stream<Arguments> prefixes() {
    return Stream.of(
        arguments("0x7FFFFFFF", intTag(Integer.MAX_VALUE)),
        arguments("0xff", intTag(255)),
        arguments("0x1FL", longTag(31L)),
        arguments("0xFFub", byteTag(-1)),
        arguments("0b1111", intTag(15)),
        arguments("0x11b", intTag(0x11b)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prefixes")
  void parsesHexadecimalAndBinaryIntegers(String text, NbtTag expected) throws NbtException {
    // spec: core-nbt R46 (0x and 0b prefixes); 0x11b is an Int because b is a hexadecimal
    // digit, provisional
    assertThat(SnbtParser.parse(text)).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"30bu", "82u", "0x", "1.5.5", "1e", "1ee3", "1bb", "0x1.5"})
  void rejectsMalformedNumber(String text) {
    // spec: core-nbt R46 (documented refusals 82u and 30bu, '_' between digits only, prefixes
    // for integers only), provisional
    assertFails(() -> SnbtParser.parse(text), Reason.SNBT_SYNTAX);
  }

  @Test
  void rejectsUnsignedSuffixOnNegativeNumber() {
    // spec: core-nbt R46 (documented refusal -87uI); syntax or range is not pinned down
    NbtException thrown = failure(() -> SnbtParser.parse("-87uI"));
    assertThat(thrown.reason()).isIn(Reason.SNBT_SYNTAX, Reason.SNBT_NUMBER_OUT_OF_RANGE);
  }

  @Test
  void decimalWithoutSuffixIsDouble() throws NbtException {
    // spec: core-nbt R46 (with '.' or exponent and no suffix: Double)
    assertThat(SnbtParser.parse("2.0").type()).isEqualTo(TagType.DOUBLE);
    assertThat(SnbtParser.parse("2e0").type()).isEqualTo(TagType.DOUBLE);
  }
}

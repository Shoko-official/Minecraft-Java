package dev.shoko.minecraft.core.nbt.snbt;

import static dev.shoko.minecraft.core.nbt.Failures.assertFails;
import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static dev.shoko.minecraft.core.nbt.Tags.text;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** SNBT blanks (R41), unquoted words (R43, R44) and quoted strings (R45), provisional. */
class SnbtParserStringsTest {

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"abc", "A1", "_", "a-b.c+d_e", "Infinity", "NaN", "nul"})
  void unquotedWordIsString(String text) throws NbtException {
    // spec: core-nbt R43 (0-9 A-Z a-z _ - . +), R44 (other words than true and false)
    assertThat(SnbtParser.parse(text)).isEqualTo(stringTag(text));
  }

  @Test
  void unquotedBooleansAreBytes() throws NbtException {
    // spec: core-nbt R44 (true and false give Byte 1 and Byte 0)
    assertThat(SnbtParser.parse("true")).isEqualTo(byteTag(1));
    assertThat(SnbtParser.parse("false")).isEqualTo(byteTag(0));
    assertThat(SnbtParser.parse("'false'")).isEqualTo(stringTag("false"));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"-a", ".a", "+a", "1a", "a:b", "minecraft:stone", "a b", "a@b", "a,b"})
  void rejectsInvalidUnquotedValue(String text) {
    // spec: core-nbt R43 (no digit, '-', '.' or '+' first; ':' needs quotes), R41, R51
    assertFails(() -> SnbtParser.parse(text), Reason.SNBT_SYNTAX);
  }

  @Test
  void colonIsTheFaultyCharacter() {
    // spec: core-nbt R43, T28; offset: the unquoted ':'
    assertFails(() -> SnbtParser.parse("a:b"), Reason.SNBT_SYNTAX, 1);
  }

  @Test
  void textAfterValueIsLocated() {
    // spec: core-nbt R41 (the whole text is one value); offset: first character after it
    assertFails(() -> SnbtParser.parse("1 2"), Reason.SNBT_SYNTAX, 2);
    assertFails(() -> SnbtParser.parse("{a:1}}"), Reason.SNBT_SYNTAX, 5);
    assertFails(() -> SnbtParser.parse("\"a\"b"), Reason.SNBT_SYNTAX, 3);
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"", " ", "\t\r\n"})
  void rejectsTextWithoutValue(String text) {
    // spec: core-nbt R41, R51
    assertFails(() -> SnbtParser.parse(text), Reason.SNBT_SYNTAX);
  }

  @Test
  void blanksAroundValueAreIgnored() throws NbtException {
    // spec: core-nbt R41, provisional: space, tab, carriage return and line feed
    assertThat(SnbtParser.parse(" 1 ")).isEqualTo(intTag(1));
    assertThat(SnbtParser.parse("\t1\r\n")).isEqualTo(intTag(1));
    assertThat(SnbtParser.parse("\n\"a\"\t")).isEqualTo(stringTag("a"));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(
      delimiter = '|',
      quoteCharacter = '`',
      value = {
        "\"\" | ``",
        "'' | ``",
        "\"a'b\" | a'b",
        "'a\"b' | a\"b",
        "\"a b:c,{}[]\" | a b:c,{}[]",
        "\"\\\\\" | \\",
        "\"\\\"\" | \"",
        "'\\'' | '",
        "\"\\s\" | ` `",
        "\"\\x41\" | A"
      })
  void parsesQuotedString(String text, String expected) throws NbtException {
    // spec: core-nbt R45 (quotes " or ', escapes \\ \" \' \s \xhh)
    assertThat(SnbtParser.parse(text)).isEqualTo(stringTag(expected));
  }

  @Test
  void parsesControlCharacterEscapes() throws NbtException {
    // spec: core-nbt R45 (\n \t \b \f \r)
    assertThat(SnbtParser.parse("\"\\n\\t\\b\\f\\r\"")).isEqualTo(stringTag("\n\t\b\f\r"));
  }

  @Test
  void parsesUnicodeEscapes() throws NbtException {
    // spec: core-nbt R45 (\\uhhhh, \\Uhhhhhhhh up to U+10FFFF, \\N{name}), T33
    assertThat(SnbtParser.parse("\"\\u00e9\\u20AC\"")).isEqualTo(stringTag(text(0xE9, 0x20AC)));
    assertThat(SnbtParser.parse("\"\\U0001F600\"")).isEqualTo(stringTag(text(0x1F600)));
    assertThat(SnbtParser.parse("\"\\U0010FFFF\"")).isEqualTo(stringTag(text(0x10FFFF)));
    assertThat(SnbtParser.parse("\"\\N{LATIN SMALL LETTER E WITH ACUTE}\""))
        .isEqualTo(stringTag(text(0xE9)));
  }

  @Test
  void keepsNonAsciiCharactersAsTyped() throws NbtException {
    // spec: core-nbt R45 (only escapes are decoded)
    String typed = text(0xE9, 0x2603);
    assertThat(SnbtParser.parse("\"" + typed + "\"")).isEqualTo(stringTag(typed));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(
      delimiter = '|',
      quoteCharacter = '`',
      value = {
        "\"\\q\" | 1",
        "'\\z' | 1",
        "\"ab\\q\" | 3",
        "\"\\U00110000\" | 1",
        "\"\\N{NO SUCH CHARACTER NAME}\" | 1"
      })
  void rejectsInvalidEscape(String text, int offset) {
    // spec: core-nbt R45 (unknown escape, code point above U+10FFFF); offset: the backslash
    assertFails(() -> SnbtParser.parse(text), Reason.SNBT_INVALID_ESCAPE, offset);
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"\"abc", "'abc", "\"abc'", "\"a\\\""})
  void rejectsUnterminatedString(String text) {
    // spec: core-nbt R45, R51
    assertFails(() -> SnbtParser.parse(text), Reason.SNBT_SYNTAX);
  }
}

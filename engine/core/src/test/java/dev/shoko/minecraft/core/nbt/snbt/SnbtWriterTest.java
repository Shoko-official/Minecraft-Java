package dev.shoko.minecraft.core.nbt.snbt;

import static dev.shoko.minecraft.core.nbt.Failures.assertFailsWithoutOffset;
import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.doubleTag;
import static dev.shoko.minecraft.core.nbt.Tags.floatTag;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.longTag;
import static dev.shoko.minecraft.core.nbt.Tags.nestedLists;
import static dev.shoko.minecraft.core.nbt.Tags.shortTag;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static dev.shoko.minecraft.core.nbt.Tags.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.snbt.SnbtVectors.Accepted;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Text shown by {@code /data get} (R52 to R56), provisional until probe PW1. */
class SnbtWriterTest {

  static List<Accepted> accepted() {
    return SnbtVectors.accepted();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("accepted")
  void writesTableEntryText(Accepted vector) throws NbtException {
    // spec: core-nbt R52 to R56, core-nbt-vectors table T ("Texte attendu")
    assertThat(SnbtWriter.write(vector.value().get())).isEqualTo(vector.text());
  }

  static Stream<Arguments> integers() {
    return Stream.of(
        arguments(byteTag(1), "1b"),
        arguments(byteTag(0), "0b"),
        arguments(byteTag(-128), "-128b"),
        arguments(shortTag(-32768), "-32768s"),
        arguments(intTag(Integer.MIN_VALUE), "-2147483648"),
        arguments(longTag(1L), "1L"),
        arguments(longTag(Long.MIN_VALUE), "-9223372036854775808L"));
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("integers")
  void writesIntegerWithTypeSuffix(NbtTag tag, String text) throws NbtException {
    // spec: core-nbt R52 (lower-case suffixes except L, never true or false)
    assertThat(SnbtWriter.write(tag)).isEqualTo(text);
  }

  static Stream<Arguments> decimals() {
    return Stream.of(
        arguments(doubleTag(1000.0), "1000.0d"),
        arguments(doubleTag(0.5), "0.5d"),
        arguments(floatTag(5.0f), "5.0f"),
        arguments(floatTag(1.0e10f), "1.0E10f"),
        arguments(doubleTag(1.0e-4), "1.0E-4d"),
        arguments(doubleTag(1.2345678e7), "1.2345678E7d"),
        arguments(doubleTag(1.0e7), "1.0E7d"),
        arguments(doubleTag(9999999.0), "9999999.0d"),
        arguments(doubleTag(0.001), "0.001d"),
        arguments(doubleTag(9.0e-4), "9.0E-4d"),
        arguments(floatTag(0.1f), "0.1f"),
        arguments(floatTag(7.9f), "7.9f"),
        arguments(doubleTag(7.9f), "7.900000095367432d"),
        arguments(new DoubleTag(0x8000000000000000L), "-0.0d"),
        arguments(new FloatTag(0x80000000), "-0.0f"));
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("decimals")
  void writesDecimalAsShortestRepresentation(NbtTag tag, String text) throws NbtException {
    // spec: core-nbt R53 (Float.toString / Double.toString digits, then suffix), provisional
    assertThat(SnbtWriter.write(tag)).isEqualTo(text);
  }

  static Stream<Arguments> specialDecimals() {
    return Stream.of(
        arguments(new DoubleTag(0x7ff8000000000000L), "NaNd"),
        arguments(new DoubleTag(0x7ff0000000000001L), "NaNd"),
        arguments(new DoubleTag(0x7ff0000000000000L), "Infinityd"),
        arguments(new DoubleTag(0xfff0000000000000L), "-Infinityd"),
        arguments(new FloatTag(0x7fc00000), "NaNf"),
        arguments(new FloatTag(0x7f800000), "Infinityf"),
        arguments(new FloatTag(0xff800000), "-Infinityf"));
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("specialDecimals")
  void writesNanAndInfinities(NbtTag tag, String text) throws NbtException {
    // spec: core-nbt R53, probe PW2 (text of a NaN), provisional
    assertThat(SnbtWriter.write(tag)).isEqualTo(text);
  }

  static Stream<Arguments> strings() {
    return Stream.of(
        arguments("", "\"\""),
        arguments("plain", "\"plain\""),
        arguments("a\\b", "\"a\\\\b\""),
        arguments("a\"b", "'a\"b'"),
        arguments("a'b", "\"a'b\""),
        arguments("\"a'b", "'\"a\\'b'"),
        arguments("a\\\"", "'a\\\\\"'"),
        arguments(text(0x41, 0xE9, 0x2603), "\"" + text(0x41, 0xE9, 0x2603) + "\""),
        arguments(text(0x1F600), "\"" + text(0x1F600) + "\""),
        arguments("a\nb\tc", "\"a\nb\tc\""));
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("strings")
  void writesQuotedString(String value, String text) throws NbtException {
    // spec: core-nbt R54 (always quoted; other quote than the first found; escape \ and the
    // delimiter; other characters as is, provisional)
    assertThat(SnbtWriter.write(stringTag(value))).isEqualTo(text);
  }

  @Test
  void writesKeyUnquotedOnlyWhenMadeOfAllowedCharacters() throws NbtException {
    // spec: core-nbt R55
    var tag =
        compound(
            "a.b-c+d_E9",
            intTag(1),
            "a b",
            intTag(2),
            "minecraft:x",
            intTag(3),
            "a\"b",
            intTag(4),
            text(0xE9),
            intTag(5));
    assertThat(SnbtWriter.write(tag))
        .isEqualTo(
            "{a.b-c+d_E9: 1, \"a b\": 2, \"minecraft:x\": 3, 'a\"b': 4, \""
                + text(0xE9)
                + "\": 5}");
  }

  @Test
  void writesSeparatorsAndKeysInStorageOrder() throws NbtException {
    // spec: core-nbt R56 ({a: 1b, b: "x"}, storage order)
    assertThat(SnbtWriter.write(compound("a", byteTag(1), "b", stringTag("x"))))
        .isEqualTo("{a: 1b, b: \"x\"}");
    assertThat(SnbtWriter.write(compound("z", byteTag(1), "a", byteTag(2))))
        .isEqualTo("{z: 1b, a: 2b}");
  }

  @Test
  void writesEmptyContainers() throws NbtException {
    // spec: core-nbt R56 ({} and [], provisional [B;] [I;] [L;])
    assertThat(SnbtWriter.write(CompoundTag.empty())).isEqualTo("{}");
    assertThat(SnbtWriter.write(ListTag.empty())).isEqualTo("[]");
    assertThat(SnbtWriter.write(ByteArrayTag.of())).isEqualTo("[B;]");
    assertThat(SnbtWriter.write(IntArrayTag.of())).isEqualTo("[I;]");
    assertThat(SnbtWriter.write(LongArrayTag.of())).isEqualTo("[L;]");
  }

  @Test
  void writesNestedValuesOnOneLine() throws NbtException {
    // spec: core-nbt R56
    var tag =
        compound(
            "a", compound("b", list(intTag(1), intTag(2))),
            "c", IntArrayTag.of(-1),
            "d", list(list(intTag(1)), list(byteTag(2))),
            "e", compound("f", CompoundTag.empty()));
    assertThat(SnbtWriter.write(tag))
        .isEqualTo("{a: {b: [1, 2]}, c: [I; -1], d: [[1], [2b]], e: {f: {}}}");
  }

  @Test
  void writesHeterogeneousListWithoutWrapper() throws NbtException {
    // spec: core-nbt R56, provisional
    assertThat(SnbtWriter.write(list(compound("a", intTag(1)), intTag(2))))
        .isEqualTo("[{a: 1}, 2]");
    assertThat(SnbtWriter.write(list(stringTag("a"), list(byteTag(1))))).isEqualTo("[\"a\", [1b]]");
  }

  @Test
  void writesWrappersOfHeterogeneousListReadBackFromBinary() throws NbtException {
    // spec: core-nbt R56, R63 (after a binary round trip: [{"": 1}, {"": "a"}], empty key quoted)
    var readBack = list(compound("", intTag(1)), compound("", stringTag("a")));
    assertThat(SnbtWriter.write(readBack)).isEqualTo("[{\"\": 1}, {\"\": \"a\"}]");
  }

  @Test
  void writes512NestedLists() throws NbtException {
    // spec: core-nbt R59, W1 written as the text of T53
    assertThat(SnbtWriter.write(nestedLists(512, ListTag.empty())))
        .isEqualTo("[".repeat(512) + "]".repeat(512));
  }

  @Test
  void refuses513NestedListsWithoutText() {
    // spec: core-nbt R59, W2: TOO_DEEP without offset, no text
    assertFailsWithoutOffset(
        () -> SnbtWriter.write(nestedLists(513, ListTag.empty())), Reason.TOO_DEEP);
  }

  @Test
  void typedArrayCountsAsALevel() {
    // spec: core-nbt R59 (a valid binary tree of 512 containers holding an array at the deepest
    // level is 513 levels in SNBT)
    assertFailsWithoutOffset(
        () -> SnbtWriter.write(nestedLists(512, list(IntArrayTag.of(1)))), Reason.TOO_DEEP);
  }

  @Test
  void heterogeneousListWrappersDoNotCount() throws NbtException {
    // spec: core-nbt R59 (levels counted without wrappers): W3 is 512 levels in SNBT
    assertThat(SnbtWriter.write(nestedLists(512, list(byteTag(1), intTag(2)))))
        .isEqualTo("[".repeat(511) + "[1b, 2]" + "]".repeat(511));
  }

  @Test
  void veryDeepTreeIsRefusedWithoutStackOverflow() {
    // spec: core-nbt R59 (the written text is always readable, R58)
    assertFailsWithoutOffset(
        () -> SnbtWriter.write(nestedLists(100_000, ListTag.empty())), Reason.TOO_DEEP);
  }
}

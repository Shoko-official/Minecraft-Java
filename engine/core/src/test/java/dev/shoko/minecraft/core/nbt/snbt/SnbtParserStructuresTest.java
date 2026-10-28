package dev.shoko.minecraft.core.nbt.snbt;

import static dev.shoko.minecraft.core.nbt.Failures.assertFails;
import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import dev.shoko.minecraft.core.nbt.TagType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** SNBT compounds (R40, R42), lists (R47), typed arrays (R48), commas (R49), operations (R50). */
class SnbtParserStructuresTest {

  @Test
  void compoundArgumentAcceptsCompounds() throws NbtException {
    // spec: core-nbt R40 (minecraft:nbt_compound_tag)
    assertThat(SnbtParser.parseCompound("{a:1}")).isEqualTo(compound("a", intTag(1)));
    assertThat(SnbtParser.parseCompound(" {} ")).isEqualTo(CompoundTag.empty());
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(
      delimiter = '|',
      quoteCharacter = '`',
      value = {"1 | 0", "[1] | 0", "\"x\" | 0", "[I;1] | 0", "`  true` | 2"})
  void compoundArgumentRejectsOtherValues(String text, int offset) {
    // spec: core-nbt R40; offset: first character of the value
    assertFails(() -> SnbtParser.parseCompound(text), Reason.SNBT_NOT_A_COMPOUND, offset);
  }

  @Test
  void compoundArgumentReportsSyntaxErrorsAsSuch() {
    // spec: core-nbt R40, R51: an invalid text is a syntax error, not a non-compound value
    assertFails(() -> SnbtParser.parseCompound("{a:"), Reason.SNBT_SYNTAX);
  }

  @Test
  void parsesQuotedAndUnquotedKeys() throws NbtException {
    // spec: core-nbt R42, R43
    assertThat(SnbtParser.parse("{\"minecraft:x\":1,'a\"b':2,a.b-c+d_E9:3,'':4}"))
        .isEqualTo(
            compound(
                "minecraft:x",
                intTag(1),
                "a\"b",
                intTag(2),
                "a.b-c+d_E9",
                intTag(3),
                "",
                intTag(4)));
  }

  @Test
  void keepsKeysInTextOrder() throws NbtException {
    // spec: core-nbt R56 (keys in storage order), T46 (order to be observed), provisional
    var tag = (CompoundTag) SnbtParser.parse("{z:1,a:2,m:3}");
    assertThat(tag.entries().sequencedKeySet()).containsExactly("z", "a", "m");
  }

  @Test
  void duplicateKeyKeepsLastValueOfAnyType() throws NbtException {
    // spec: core-nbt R42 (T47), provisional
    assertThat(SnbtParser.parse("{a:1,a:\"x\"}")).isEqualTo(compound("a", stringTag("x")));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"{a}", "{:1}", "{a:1", "{,}", "{a:1,,}", "{a:}", "{minecraft:stone:1}"})
  void rejectsMalformedCompound(String text) {
    // spec: core-nbt R42, R43, R49, R51
    assertFails(() -> SnbtParser.parse(text), Reason.SNBT_SYNTAX);
  }

  @Test
  void missingCommaIsLocated() {
    // spec: core-nbt R42; offset: the unexpected key
    assertFails(() -> SnbtParser.parse("{a:1 b:2}"), Reason.SNBT_SYNTAX, 5);
  }

  @Test
  void parsesListsOfListsAndCompounds() throws NbtException {
    // spec: core-nbt R7, R47
    var lists = (ListTag) SnbtParser.parse("[[1],[2b],[]]");
    assertThat(lists).isEqualTo(list(list(intTag(1)), list(byteTag(2)), ListTag.empty()));
    assertThat(lists.elementType()).isEqualTo(TagType.LIST);
    assertThat(SnbtParser.parse("[{a:1},{}]"))
        .isEqualTo(list(compound("a", intTag(1)), CompoundTag.empty()));
    assertThat(SnbtParser.parse("[ ]")).isEqualTo(ListTag.empty());
    assertThat(SnbtParser.parse("[true,false]")).isEqualTo(list(byteTag(1), byteTag(0)));
  }

  @Test
  void mixedListKeepsElementsUnwrapped() throws NbtException {
    // spec: core-nbt R47, provisional: [{a:1}, 2] is heterogeneous, unwrapped in memory
    var mixed = (ListTag) SnbtParser.parse("[{a:1},2]");
    assertThat(mixed.elements()).containsExactly(compound("a", intTag(1)), intTag(2));
    assertThat(mixed.elementType()).isEqualTo(TagType.COMPOUND);
    assertThat(SnbtParser.parse("[1,[2]]")).isEqualTo(list(intTag(1), list(intTag(2))));
  }

  @Test
  void singleLetterWithoutSemicolonIsAString() throws NbtException {
    // spec: core-nbt R43, R48, provisional: [I] is a list holding the string "I"
    assertThat(SnbtParser.parse("[I]")).isEqualTo(list(stringTag("I")));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"[1,,2]", "[,]", "[1", "[1 2]", "]"})
  void rejectsMalformedList(String text) {
    // spec: core-nbt R47, R49, R51
    assertFails(() -> SnbtParser.parse(text), Reason.SNBT_SYNTAX);
  }

  @Test
  void parsesTypedArrays() throws NbtException {
    // spec: core-nbt R48 (B: Byte; I: Byte, Short, Int; L: Byte, Short, Int, Long), R49
    assertThat(SnbtParser.parse("[B;127b,-128b]"))
        .isEqualTo(ByteArrayTag.of((byte) 127, (byte) -128));
    assertThat(SnbtParser.parse("[I; 1 , -2 ,]")).isEqualTo(IntArrayTag.of(1, -2));
    assertThat(SnbtParser.parse("[L;1b,2s,3,4L]")).isEqualTo(LongArrayTag.of(1L, 2L, 3L, 4L));
  }

  @Test
  void parsesEmptyTypedArrays() throws NbtException {
    // spec: core-nbt R48 (T44), provisional
    assertThat(SnbtParser.parse("[B;]")).isEqualTo(ByteArrayTag.of());
    assertThat(SnbtParser.parse("[L;]")).isEqualTo(LongArrayTag.of());
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(
      delimiter = '|',
      quoteCharacter = '`',
      value = {
        "[B;1] | 3",
        "[B;1s] | 3",
        "[B;false] | 3",
        "[I;\"a\"] | 3",
        "[I;1.5f] | 3",
        "[I;[1]] | 3",
        "[I;1,1L] | 5",
        "[L;1.5] | 3",
        "[L;2,{}] | 5"
      })
  void rejectsArrayElementOfUnacceptedType(String text, int offset) {
    // spec: core-nbt R48 (T40, T42); offset: first character of the element
    assertFails(() -> SnbtParser.parse(text), Reason.SNBT_INVALID_ARRAY_ELEMENT, offset);
  }

  @Test
  void boolOperationGivesZeroOrOne() throws NbtException {
    // spec: core-nbt R50 (number or boolean to Byte 0 or 1)
    assertThat(SnbtParser.parse("bool(0)")).isEqualTo(byteTag(0));
    assertThat(SnbtParser.parse("bool(5)")).isEqualTo(byteTag(1));
    assertThat(SnbtParser.parse("bool(true)")).isEqualTo(byteTag(1));
    assertThat(SnbtParser.parse("bool(false)")).isEqualTo(byteTag(0));
    assertThat(SnbtParser.parse("{a:bool(1)}")).isEqualTo(compound("a", byteTag(1)));
  }

  @Test
  void uuidOperationGivesFourIntsMostSignificantFirst() throws NbtException {
    // spec: core-nbt R50, R8 (UUID of F3, computed from its 16 bytes)
    assertThat(SnbtParser.parse("uuid(\"35592e68-c4a6-428e-b15f-be52b701a331\")"))
        .isEqualTo(IntArrayTag.of(895037032, -995736946, -1319125422, -1224629455));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"uuid(\"x\")", "uuid(1)", "bool(\"x\")", "bool([1])"})
  void rejectsOperationArgumentOfWrongKind(String text) {
    // spec: core-nbt R50 (T49); offset: first character of the argument
    assertFails(() -> SnbtParser.parse(text), Reason.SNBT_INVALID_OPERATION, 5);
  }
}

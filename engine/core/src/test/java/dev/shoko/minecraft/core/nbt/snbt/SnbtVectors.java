package dev.shoko.minecraft.core.nbt.snbt;

import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.doubleTag;
import static dev.shoko.minecraft.core.nbt.Tags.floatTag;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.longTag;
import static dev.shoko.minecraft.core.nbt.Tags.shortTag;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static dev.shoko.minecraft.core.nbt.Tags.text;

import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import dev.shoko.minecraft.core.nbt.NbtTag;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Table T (T1 to T52, T60 to T62; T53 to T59 are in SnbtParserDepthTest) of {@code
 * core-nbt-vectors.md}, probe PS1 for reading and PW1 for the text. Every expected result is
 * provisional (S) until the probes run. Inputs are the exact SNBT text; Java escapes only encode
 * it. Expected values are built lazily, one failure per case.
 */
final class SnbtVectors {

  /** Offset value meaning that the spec does not pin the offset down. */
  static final int ANY_OFFSET = -1;

  /** An accepted input, its value and the text {@code /data get} shows for that value. */
  record Accepted(String id, String input, Supplier<NbtTag> value, String text) {
    @Override
    public String toString() {
      return id + " " + input;
    }
  }

  /** A refused input and the reason (and offset, when determined) of the failure. */
  record Rejected(String id, String input, Reason reason, int offset) {
    @Override
    public String toString() {
      return id + " " + input;
    }
  }

  private SnbtVectors() {}

  static List<Accepted> accepted() {
    var all = new ArrayList<Accepted>();
    all.addAll(numbers());
    all.addAll(strings());
    all.addAll(structures());
    return all;
  }

  // spec: core-nbt R35, R46, R52, R53
  static List<Accepted> numbers() {
    return List.of(
        new Accepted("T1", "1", () -> intTag(1), "1"),
        new Accepted("T2", "1B", () -> byteTag(1), "1b"),
        new Accepted("T3", "127b", () -> byteTag(127), "127b"),
        new Accepted("T5", "-128b", () -> byteTag(-128), "-128b"),
        new Accepted("T6", "240ub", () -> byteTag(-16), "-16b"),
        new Accepted("T8", "32767s", () -> shortTag(32767), "32767s"),
        new Accepted("T10", "2147483647", () -> intTag(2147483647), "2147483647"),
        new Accepted("T12", "2147483648l", () -> longTag(2147483648L), "2147483648L"),
        new Accepted("T13", "1.5", () -> doubleTag(1.5), "1.5d"),
        new Accepted("T14", "1.5F", () -> floatTag(1.5f), "1.5f"),
        new Accepted("T15", ".5", () -> doubleTag(0.5), "0.5d"),
        new Accepted("T16", "5.", () -> doubleTag(5.0), "5.0d"),
        new Accepted("T17", "1e3", () -> doubleTag(1000.0), "1000.0d"),
        new Accepted("T18", "-0.0d", () -> new DoubleTag(0x8000000000000000L), "-0.0d"),
        new Accepted("T19", "0x1F", () -> intTag(31), "31"),
        new Accepted("T20", "0b101", () -> intTag(5), "5"),
        new Accepted("T21", "0x11sb", () -> byteTag(17), "17b"),
        new Accepted("T22", "1_000", () -> intTag(1000), "1000"));
  }

  // spec: core-nbt R43, R44, R45, R54
  static List<Accepted> strings() {
    return List.of(
        new Accepted("T24", "_1", () -> stringTag("_1"), "\"_1\""),
        new Accepted("T25", "true", () -> byteTag(1), "1b"),
        new Accepted("T26", "\"true\"", () -> stringTag("true"), "\"true\""),
        new Accepted("T27", "NaN", () -> stringTag("NaN"), "\"NaN\""),
        new Accepted("T29", "\"a\\\"b\"", () -> stringTag("a\"b"), "'a\"b'"),
        new Accepted("T30", "'it\\'s'", () -> stringTag("it's"), "\"it's\""),
        new Accepted("T31", "\"a'b\\\"c\"", () -> stringTag("a'b\"c"), "\"a'b\\\"c\""),
        new Accepted("T32", "'a\"b\\'c'", () -> stringTag("a\"b'c"), "'a\"b\\'c'"),
        new Accepted(
            "T33",
            "\"\\x41\\u00e9\\N{Snowman}\"",
            () -> stringTag(text(0x41, 0xE9, 0x2603)),
            "\"" + text(0x41, 0xE9, 0x2603) + "\""));
  }

  // spec: core-nbt R8, R36, R41, R42, R47 to R50, R55, R56
  static List<Accepted> structures() {
    return List.of(
        new Accepted("T35", "[]", ListTag::empty, "[]"),
        new Accepted("T36", "[1,2,]", () -> list(intTag(1), intTag(2)), "[1, 2]"),
        new Accepted("T37", "[1,\"a\"]", () -> list(intTag(1), stringTag("a")), "[1, \"a\"]"),
        new Accepted("T38", "[1b,2]", () -> list(byteTag(1), intTag(2)), "[1b, 2]"),
        new Accepted(
            "T39", "[B;1b,-1b]", () -> ByteArrayTag.of((byte) 1, (byte) -1), "[B; 1b, -1b]"),
        new Accepted("T41", "[I;1b,2s,3]", () -> IntArrayTag.of(1, 2, 3), "[I; 1, 2, 3]"),
        new Accepted("T43", "[L;1,2L]", () -> LongArrayTag.of(1L, 2L), "[L; 1L, 2L]"),
        new Accepted("T44", "[I;]", IntArrayTag::of, "[I;]"),
        new Accepted("T45", "{a:1,}", () -> compound("a", intTag(1)), "{a: 1}"),
        new Accepted(
            "T46",
            "{\"a b\":1,'k':2b}",
            () -> compound("a b", intTag(1), "k", byteTag(2)),
            "{\"a b\": 1, k: 2b}"),
        new Accepted("T47", "{a:1,a:2}", () -> compound("a", intTag(2)), "{a: 2}"),
        new Accepted("T48", "bool(5)", () -> byteTag(1), "1b"),
        new Accepted(
            "T50",
            "uuid(\"f81d4fae-7dec-11d0-a765-00a0c91e6bf6\")",
            () -> IntArrayTag.of(-132296786, 2112623056, -1486552928, -920753162),
            "[I; -132296786, 2112623056, -1486552928, -920753162]"),
        new Accepted("T52", "{ a : 1 }", () -> compound("a", intTag(1)), "{a: 1}"),
        new Accepted("T60", "bool(256)", () -> byteTag(1), "1b"),
        new Accepted("T61", "bool(0.5d)", () -> byteTag(1), "1b"),
        new Accepted("T62", "bool(-0.0d)", () -> byteTag(0), "0b"));
  }

  // spec: core-nbt R43, R45, R46, R48, R50, R51
  static List<Rejected> rejected() {
    return List.of(
        new Rejected("T4", "128b", Reason.SNBT_NUMBER_OUT_OF_RANGE, 0),
        new Rejected("T7", "256ub", Reason.SNBT_NUMBER_OUT_OF_RANGE, 0),
        new Rejected("T9", "32768s", Reason.SNBT_NUMBER_OUT_OF_RANGE, 0),
        new Rejected("T11", "2147483648", Reason.SNBT_NUMBER_OUT_OF_RANGE, 0),
        new Rejected("T23", "1_", Reason.SNBT_SYNTAX, ANY_OFFSET),
        new Rejected("T28", "minecraft:stone", Reason.SNBT_SYNTAX, 9),
        new Rejected("T34", "\"\\q\"", Reason.SNBT_INVALID_ESCAPE, 1),
        new Rejected("T40", "[B;true]", Reason.SNBT_INVALID_ARRAY_ELEMENT, 3),
        new Rejected("T42", "[I;1L]", Reason.SNBT_INVALID_ARRAY_ELEMENT, 3),
        new Rejected("T49", "bool(\"x\")", Reason.SNBT_INVALID_OPERATION, 5),
        new Rejected("T51", "1.0e400", Reason.SNBT_NUMBER_OUT_OF_RANGE, 0));
  }
}

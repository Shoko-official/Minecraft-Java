package dev.shoko.minecraft.core.nbt.binary;

import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.longTag;
import static dev.shoko.minecraft.core.nbt.Tags.root;
import static dev.shoko.minecraft.core.nbt.Tags.shortTag;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static dev.shoko.minecraft.core.nbt.Tags.text;

import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.Hex;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.NamedTag;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Binary vectors V02 to V23 of {@code docs/specs/core-nbt-vectors.md}, with the value each one
 * decodes to. V01 (absent root), V20 (unnamed root) and V24 (rewritten differently) are tested on
 * their own. Expected values are built lazily so that each parameterized case fails on its own.
 */
final class BinaryVectors {

  /** A valid named root: its exact bytes and the value they decode to. */
  record Vector(String id, String hex, Supplier<NamedTag> value) {

    byte[] bytes() {
      return Hex.bytes(hex);
    }

    @Override
    public String toString() {
      return id;
    }
  }

  private BinaryVectors() {}

  /** Every vector whose reading then rewriting gives the same bytes (R30). */
  static List<Vector> named() {
    var all = new ArrayList<Vector>();
    all.addAll(scalars());
    all.addAll(strings());
    all.addAll(containers());
    return all;
  }

  // spec: core-nbt R1, R3, R4, R5, R35 (V02 to V07, V22, V23)
  static List<Vector> scalars() {
    return List.of(
        new Vector("V02 Byte -1 named b", "01 00 01 62 ff", () -> new NamedTag("b", byteTag(-1))),
        new Vector("V03 Short -2", "02 00 00 ff fe", () -> root(shortTag(-2))),
        new Vector("V04 Int 16909060", "03 00 00 01 02 03 04", () -> root(intTag(16909060))),
        new Vector(
            "V05 Long -2^63",
            "04 00 00 80 00 00 00 00 00 00 00",
            () -> root(longTag(Long.MIN_VALUE))),
        new Vector("V06 Float 1.5", "05 00 00 3f c0 00 00", () -> root(new FloatTag(0x3fc00000))),
        new Vector(
            "V07 Double -0.0",
            "06 00 00 80 00 00 00 00 00 00 00",
            () -> root(new DoubleTag(0x8000000000000000L))),
        new Vector(
            "V22 Float NaN 7fc00000", "05 00 00 7f c0 00 00", () -> root(new FloatTag(0x7fc00000))),
        new Vector(
            "V23 Double +infinity",
            "06 00 00 7f f0 00 00 00 00 00 00",
            () -> root(new DoubleTag(0x7ff0000000000000L))));
  }

  // spec: core-nbt R10, R11 (V09, V15 to V18)
  static List<Vector> strings() {
    return List.of(
        new Vector("V09 String hi", "08 00 00 00 02 68 69", () -> root(stringTag("hi"))),
        new Vector("V15 String U+0000", "08 00 00 00 02 c0 80", () -> root(stringTag("\0"))),
        new Vector("V16 String U+00E9", "08 00 00 00 02 c3 a9", () -> root(stringTag(text(0xE9)))),
        new Vector(
            "V17 String U+20AC", "08 00 00 00 03 e2 82 ac", () -> root(stringTag(text(0x20AC)))),
        new Vector(
            "V18 String U+1F600",
            "08 00 00 00 06 ed a0 bd ed b8 80",
            () -> root(stringTag(text(0x1F600)))));
  }

  // spec: core-nbt R6, R7, R8, R9, R22, R30 (V08, V10 to V14, V19, V21)
  static List<Vector> containers() {
    return List.of(
        new Vector(
            "V08 Byte_Array [1, -1]",
            "07 00 00 00 00 00 02 01 ff",
            () -> root(ByteArrayTag.of((byte) 1, (byte) -1))),
        new Vector("V10 empty List of End", "09 00 00 00 00 00 00 00", () -> root(ListTag.empty())),
        new Vector(
            "V11 List of Int [7, 8]",
            "09 00 00 03 00 00 00 02 00 00 00 07 00 00 00 08",
            () -> root(list(intTag(7), intTag(8)))),
        new Vector(
            "V12 Compound {a: 1b}",
            "0a 00 00 01 00 01 61 01 00",
            () -> root(compound("a", byteTag(1)))),
        new Vector(
            "V13 Int_Array [1, 2]",
            "0b 00 00 00 00 00 02 00 00 00 01 00 00 00 02",
            () -> root(IntArrayTag.of(1, 2))),
        new Vector(
            "V14 Long_Array [1]",
            "0c 00 00 00 00 00 01 00 00 00 00 00 00 00 01",
            () -> root(LongArrayTag.of(1L))),
        new Vector("V19 empty Compound", "0a 00 00 00", () -> root(CompoundTag.empty())),
        new Vector(
            "V21 Compound {z: 1b, a: 2b}",
            "0a 00 00 01 00 01 7a 01 01 00 01 61 02 00",
            () -> root(compound("z", byteTag(1), "a", byteTag(2)))));
  }

  /**
   * {@code n} nested lists as a named root (vectors E09a and E09b): root header, {@code n - 1}
   * times "List of List, length 1", then the innermost empty list of End.
   */
  static byte[] nestedListsRoot(int n) {
    return Hex.concat(
        Hex.bytes("09 00 00"), Hex.repeat(Hex.bytes("09 00 00 00 01"), n - 1), new byte[5]);
  }

  /** {@code n} nested compounds as a named root, each child under the empty key. */
  static byte[] nestedCompoundsRoot(int n) {
    return Hex.concat(Hex.bytes("0a 00 00"), Hex.repeat(Hex.bytes("0a 00 00"), n - 1), new byte[n]);
  }
}

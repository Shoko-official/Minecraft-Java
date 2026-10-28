package dev.shoko.minecraft.core.nbt;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Short builders for expected values. Scalars use the record constructors only; floats and doubles
 * keep the exact bit pattern of the given Java value, without the canonicalization of {@code of}.
 */
public final class Tags {

  private Tags() {}

  public static ByteTag byteTag(int value) {
    return new ByteTag((byte) value);
  }

  public static ShortTag shortTag(int value) {
    return new ShortTag((short) value);
  }

  public static IntTag intTag(int value) {
    return new IntTag(value);
  }

  public static LongTag longTag(long value) {
    return new LongTag(value);
  }

  public static FloatTag floatTag(float value) {
    return new FloatTag(Float.floatToRawIntBits(value));
  }

  public static DoubleTag doubleTag(double value) {
    return new DoubleTag(Double.doubleToRawLongBits(value));
  }

  public static StringTag stringTag(String value) {
    return new StringTag(value);
  }

  /**
   * Returns the text of these Unicode code points. Sources stay ASCII and the formatter does not
   * turn the characters into literals, as it does with {@code \}{@code u} escapes.
   */
  public static String text(int... codePoints) {
    return new String(codePoints, 0, codePoints.length);
  }

  public static ListTag list(NbtTag... elements) {
    return ListTag.of(List.of(elements));
  }

  /** Builds a compound from alternating keys and values, keeping the given key order. */
  public static CompoundTag compound(Object... keysAndValues) {
    if (keysAndValues.length % 2 != 0) {
      throw new IllegalArgumentException("keys and values must alternate");
    }
    var entries = new LinkedHashMap<String, NbtTag>();
    for (int k = 0; k < keysAndValues.length; k += 2) {
      entries.put((String) keysAndValues[k], (NbtTag) keysAndValues[k + 1]);
    }
    return CompoundTag.of(entries);
  }

  /** Returns {@code levels} nested lists, the deepest one (at level {@code levels}) given. */
  public static ListTag nestedLists(int levels, ListTag innermost) {
    ListTag tree = innermost;
    for (int level = 1; level < levels; level++) {
      tree = ListTag.of(List.of(tree));
    }
    return tree;
  }

  /** Returns the value as a root with the empty name, as in reference files (R14). */
  public static NamedTag root(NbtTag tag) {
    return new NamedTag("", tag);
  }
}

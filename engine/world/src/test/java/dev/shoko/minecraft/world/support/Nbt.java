package dev.shoko.minecraft.world.support;

import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import java.util.LinkedHashMap;
import java.util.List;

/** Short builders for NBT trees in tests. */
public final class Nbt {

  private Nbt() {}

  public static ByteTag b(int value) {
    return new ByteTag((byte) value);
  }

  public static ShortTag s(int value) {
    return new ShortTag((short) value);
  }

  public static IntTag i(int value) {
    return new IntTag(value);
  }

  public static LongTag l(long value) {
    return new LongTag(value);
  }

  public static StringTag str(String value) {
    return new StringTag(value);
  }

  public static LongArrayTag longs(long... values) {
    return LongArrayTag.of(values);
  }

  public static ByteArrayTag bytes(byte... values) {
    return ByteArrayTag.of(values);
  }

  public static IntArrayTag ints(int... values) {
    return IntArrayTag.of(values);
  }

  public static ListTag list(NbtTag... elements) {
    return ListTag.of(List.of(elements));
  }

  public static ListTag listOf(List<? extends NbtTag> elements) {
    return ListTag.of(elements);
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

  /** Returns the compound with one entry added or replaced, other entries kept in order. */
  public static CompoundTag with(CompoundTag base, String key, NbtTag value) {
    var entries = new LinkedHashMap<>(base.entries());
    entries.put(key, value);
    return CompoundTag.of(entries);
  }

  /** Returns the compound without a key. */
  public static CompoundTag without(CompoundTag base, String key) {
    var entries = new LinkedHashMap<>(base.entries());
    entries.remove(key);
    return CompoundTag.of(entries);
  }
}

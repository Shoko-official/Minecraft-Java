package dev.shoko.minecraft.item.testing;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.snbt.SnbtParser;

/** Reads NBT written as SNBT text. */
public final class Nbt {

  private Nbt() {}

  public static NbtTag parse(String snbt) {
    try {
      return SnbtParser.parse(snbt);
    } catch (NbtException e) {
      throw new IllegalArgumentException("bad SNBT in test: " + snbt, e);
    }
  }

  public static CompoundTag compound(String snbt) {
    try {
      return SnbtParser.parseCompound(snbt);
    } catch (NbtException e) {
      throw new IllegalArgumentException("bad SNBT in test: " + snbt, e);
    }
  }
}

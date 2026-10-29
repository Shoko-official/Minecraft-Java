package dev.shoko.minecraft.world.support;

import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.world.palette.PaletteEntryCodec;
import dev.shoko.minecraft.world.palette.PaletteFormatException;

/** Palette entry codecs for tests of containers that know neither blocks nor biomes. */
public final class TestEntries {

  private TestEntries() {}

  /** Entries are Int tags; any other tag cannot be an entry. */
  public static PaletteEntryCodec<Integer> ints() {
    return new PaletteEntryCodec<>() {
      @Override
      public Integer decode(NbtTag entry) throws PaletteFormatException {
        if (entry instanceof IntTag tag) {
          return tag.value();
        }
        throw new PaletteFormatException("entry is not an Int: " + entry.type());
      }

      @Override
      public NbtTag encode(Integer value) {
        return new IntTag(value);
      }
    };
  }

  /** Entries are String tags; any other tag cannot be an entry. */
  public static PaletteEntryCodec<String> strings() {
    return new PaletteEntryCodec<>() {
      @Override
      public String decode(NbtTag entry) throws PaletteFormatException {
        if (entry instanceof StringTag tag) {
          return tag.value();
        }
        throw new PaletteFormatException("entry is not a String: " + entry.type());
      }

      @Override
      public NbtTag encode(String value) {
        return new StringTag(value);
      }
    };
  }
}

package dev.shoko.minecraft.tools.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference state of two small chunks, (2,-1) and (3,-1), each with sections 4 and 5. A section
 * lists its palette, then eight blocks resolved to block names. Tests inject defects into a copy.
 */
final class WorldFixture {

  static final String[] PALETTE = {"minecraft:stone", "minecraft:dirt", "minecraft:air"};

  private WorldFixture() {}

  /** A fresh, mutable list of the reference entries, in canonical order. */
  static List<CanonicalState.Entry> entries() {
    List<CanonicalState.Entry> entries = new ArrayList<>();
    for (int chunkX : new int[] {2, 3}) {
      int chunkZ = -1;
      String chunk = StateKey.segment("chunk", chunkX, chunkZ);
      for (int section : new int[] {4, 5}) {
        String prefix = StateKey.of(chunk, StateKey.segment("section", section));
        for (int i = 0; i < PALETTE.length; i++) {
          entries.add(entry(StateKey.of(prefix, StateKey.segment("palette", i)), PALETTE[i]));
        }
        for (int b = 0; b < 8; b++) {
          int x = chunkX * 16 + (b & 1);
          int y = section * 16 + 6 + ((b >> 1) & 1);
          int z = chunkZ * 16 + 4 + (b >> 2);
          String block = StateKey.segment("block", x, y, z);
          entries.add(entry(StateKey.of(prefix, block), PALETTE[(b + section) % PALETTE.length]));
        }
      }
    }
    return entries;
  }

  static CanonicalState.Entry entry(String key, String value) {
    return new CanonicalState.Entry(key, value);
  }

  static CanonicalState state(List<CanonicalState.Entry> entries) {
    CanonicalState.Builder builder = CanonicalState.builder();
    for (CanonicalState.Entry entry : entries) {
      builder.add(entry.key(), entry.value());
    }
    return builder.build();
  }

  static CanonicalState reference() {
    return state(entries());
  }

  static int indexOf(List<CanonicalState.Entry> entries, String key) {
    for (int i = 0; i < entries.size(); i++) {
      if (entries.get(i).key().equals(key)) {
        return i;
      }
    }
    throw new IllegalArgumentException("no entry " + key);
  }
}

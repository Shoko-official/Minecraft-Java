package dev.shoko.minecraft.world.support;

import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.SplittableRandom;

/**
 * Seeded structural damage to an NBT tree: one random node is replaced, removed, truncated or given
 * an extreme value. Deterministic for a given seed and input.
 */
public final class TreeMutator {

  private static final String[] STRINGS = {
    "",
    "minecraft:stone",
    "minecraft:air",
    "minecraft:oak_log",
    "probe:unknown",
    "full",
    "minecraft:full",
    "minecraft:noise",
    "5023",
    "axis",
    "y",
    "level",
    "Y",
    "palette",
    "data"
  };
  private static final long[] EXTREME_LONGS = {
    0L,
    1L,
    -1L,
    255L,
    256L,
    4095L,
    4096L,
    Integer.MAX_VALUE,
    Integer.MIN_VALUE,
    Long.MAX_VALUE,
    Long.MIN_VALUE,
    0xFFFF_FFFFL,
    5023L,
    36L,
    37L,
    38L
  };

  private final SplittableRandom random;
  private int remaining;

  public TreeMutator(long seed) {
    this.random = new SplittableRandom(seed);
  }

  /** Applies {@code count} independent mutations. */
  public NbtTag mutate(NbtTag root, int count) {
    NbtTag current = root;
    for (int k = 0; k < count; k++) {
      int nodes = countNodes(current);
      remaining = random.nextInt(nodes);
      current = rewrite(current);
    }
    return current;
  }

  public CompoundTag mutate(CompoundTag root, int count) {
    NbtTag result = mutate((NbtTag) root, count);
    return result instanceof CompoundTag compound ? compound : CompoundTag.empty();
  }

  private static int countNodes(NbtTag tag) {
    int count = 1;
    if (tag instanceof CompoundTag compound) {
      for (NbtTag child : compound.entries().values()) {
        count += countNodes(child);
      }
    } else if (tag instanceof ListTag list) {
      for (NbtTag child : list.elements()) {
        count += countNodes(child);
      }
    }
    return count;
  }

  private NbtTag rewrite(NbtTag tag) {
    boolean target = remaining == 0;
    remaining--;
    if (target) {
      return damage(tag);
    }
    if (tag instanceof CompoundTag compound) {
      SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
      compound.entries().forEach((key, value) -> entries.put(key, rewrite(value)));
      return CompoundTag.of(entries);
    }
    if (tag instanceof ListTag list) {
      List<NbtTag> elements = new ArrayList<>();
      for (NbtTag child : list.elements()) {
        elements.add(rewrite(child));
      }
      return ListTag.of(elements);
    }
    return tag;
  }

  private NbtTag damage(NbtTag tag) {
    return switch (random.nextInt(4)) {
      case 0 -> randomTag(2);
      case 1 -> damageSameKind(tag);
      case 2 -> damageSameKind(tag);
      default -> tag instanceof CompoundTag ? damageSameKind(tag) : randomTag(1);
    };
  }

  private NbtTag damageSameKind(NbtTag tag) {
    return switch (tag) {
      case CompoundTag compound -> damageCompound(compound);
      case ListTag list -> damageList(list);
      case LongArrayTag array -> damageLongs(array.toArray());
      case IntArrayTag array -> damageInts(array.toArray());
      case ByteArrayTag array -> damageBytes(array.toArray());
      case ByteTag ignored -> new ByteTag((byte) extreme());
      case ShortTag ignored -> new ShortTag((short) extreme());
      case IntTag ignored -> new IntTag((int) extreme());
      case LongTag ignored -> new LongTag(extreme());
      case StringTag ignored -> new StringTag(STRINGS[random.nextInt(STRINGS.length)]);
      default -> randomTag(1);
    };
  }

  private long extreme() {
    return EXTREME_LONGS[random.nextInt(EXTREME_LONGS.length)];
  }

  private NbtTag damageCompound(CompoundTag compound) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>(compound.entries());
    List<String> keys = new ArrayList<>(entries.keySet());
    if (keys.isEmpty() || random.nextInt(3) == 0) {
      entries.put(STRINGS[random.nextInt(STRINGS.length)], randomTag(2));
    } else if (random.nextBoolean()) {
      entries.remove(keys.get(random.nextInt(keys.size())));
    } else {
      entries.put(keys.get(random.nextInt(keys.size())), randomTag(2));
    }
    return CompoundTag.of(entries);
  }

  private NbtTag damageList(ListTag list) {
    List<NbtTag> elements = new ArrayList<>(list.elements());
    int choice = random.nextInt(4);
    if (elements.isEmpty() || choice == 0) {
      elements.add(randomTag(1));
    } else if (choice == 1) {
      elements.remove(random.nextInt(elements.size()));
    } else if (choice == 2) {
      elements.add(elements.get(random.nextInt(elements.size())));
    } else {
      elements.set(random.nextInt(elements.size()), randomTag(2));
    }
    return ListTag.of(elements);
  }

  private NbtTag damageLongs(long[] data) {
    long[] copy = data;
    int choice = random.nextInt(3);
    if (choice == 0 || copy.length == 0) {
      copy = java.util.Arrays.copyOf(copy, random.nextInt(Math.max(1, copy.length + 3)));
    } else if (choice == 1) {
      copy = copy.clone();
      copy[random.nextInt(copy.length)] = extreme();
    } else {
      copy = java.util.Arrays.copyOf(copy, copy.length + 1 + random.nextInt(3));
    }
    return LongArrayTag.of(copy);
  }

  private NbtTag damageInts(int[] data) {
    int[] copy = java.util.Arrays.copyOf(data, Math.max(0, data.length + random.nextInt(5) - 2));
    if (copy.length > 0) {
      copy[random.nextInt(copy.length)] = (int) extreme();
    }
    return IntArrayTag.of(copy);
  }

  private NbtTag damageBytes(byte[] data) {
    byte[] copy = java.util.Arrays.copyOf(data, Math.max(0, data.length + random.nextInt(7) - 3));
    if (copy.length > 0) {
      copy[random.nextInt(copy.length)] = (byte) extreme();
    }
    return ByteArrayTag.of(copy);
  }

  /** A random tag of any type, nested at most {@code depth} levels. */
  public NbtTag randomTag(int depth) {
    int kinds = depth > 0 ? 13 : 9;
    return switch (random.nextInt(kinds)) {
      case 0 -> new ByteTag((byte) extreme());
      case 1 -> new ShortTag((short) extreme());
      case 2 -> new IntTag((int) extreme());
      case 3 -> new LongTag(extreme());
      case 4 -> new FloatTag(Float.floatToRawIntBits(random.nextInt(100) / 4f));
      case 5 -> new DoubleTag(Double.doubleToRawLongBits(random.nextInt(100) / 4.0));
      case 6 -> new StringTag(STRINGS[random.nextInt(STRINGS.length)]);
      case 7 -> LongArrayTag.of(randomLongs());
      case 8 -> ByteArrayTag.of(new byte[random.nextInt(4) * 1024 / 2]);
      case 9 -> IntArrayTag.of(random.nextInt(5), random.nextInt(5));
      case 10 -> ListTag.empty();
      case 11 -> randomList(depth - 1);
      default -> randomCompound(depth - 1);
    };
  }

  private long[] randomLongs() {
    int[] lengths = {0, 1, 2, 36, 37, 38, 255, 256, 342};
    long[] data = new long[lengths[random.nextInt(lengths.length)]];
    for (int k = 0; k < data.length; k++) {
      data[k] = random.nextBoolean() ? 0L : random.nextLong();
    }
    return data;
  }

  private NbtTag randomList(int depth) {
    List<NbtTag> elements = new ArrayList<>();
    int size = random.nextInt(4);
    for (int k = 0; k < size; k++) {
      elements.add(randomTag(depth));
    }
    return ListTag.of(elements);
  }

  private NbtTag randomCompound(int depth) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    int size = random.nextInt(4);
    for (int k = 0; k < size; k++) {
      entries.put(STRINGS[random.nextInt(STRINGS.length)], randomTag(depth));
    }
    return CompoundTag.of(entries);
  }
}

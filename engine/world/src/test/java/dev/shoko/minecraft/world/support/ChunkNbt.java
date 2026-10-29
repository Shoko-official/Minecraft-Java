package dev.shoko.minecraft.world.support;

import static dev.shoko.minecraft.world.support.Nbt.b;
import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.i;
import static dev.shoko.minecraft.world.support.Nbt.l;
import static dev.shoko.minecraft.world.support.Nbt.longs;
import static dev.shoko.minecraft.world.support.Nbt.s;
import static dev.shoko.minecraft.world.support.Nbt.str;

import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NamedTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.binary.NbtCompression;
import dev.shoko.minecraft.core.nbt.binary.NbtWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.IntUnaryOperator;

/**
 * The synthetic Overworld chunk of vector RG1 and its variants of RG3 and RG4, rebuilt from the
 * description in {@code world-vectors.md}: all air, optionally a bedrock layer at y = -64, status
 * full, 24 sections, four heightmaps. Key order is the one of the reference tool; it is not a rule.
 */
public final class ChunkNbt {

  public static final int OVERWORLD_SECTIONS = 24;
  private static final String PLAINS = "minecraft:plains";
  private static final String[] HEIGHTMAP_KEYS = {
    "MOTION_BLOCKING", "MOTION_BLOCKING_NO_LEAVES", "OCEAN_FLOOR", "WORLD_SURFACE"
  };

  private ChunkNbt() {}

  public static CompoundTag air() {
    return compound("id", str(Blocks.AIR));
  }

  /** Light array of 2048 bytes: every nibble {@code fill}, bottom layer set by {@code layer0}. */
  public static ByteArrayTag lightArray(int fill, IntUnaryOperator layer0) {
    int[] nibbles = new int[4096];
    Arrays.fill(nibbles, fill);
    for (int index = 0; index < 256 && layer0 != null; index++) {
      nibbles[index] = layer0.applyAsInt(index);
    }
    byte[] raw = new byte[2048];
    for (int k = 0; k < 2048; k++) {
      raw[k] = (byte) (nibbles[2 * k] | (nibbles[2 * k + 1] << 4));
    }
    return Nbt.bytes(raw);
  }

  /**
   * A section compound. A null {@code y} leaves the Y key out, a null palette the block states and
   * a null biome the biome container.
   */
  public static CompoundTag section(
      Integer y,
      List<? extends NbtTag> palette,
      long[] data,
      String biome,
      Map<String, ByteArrayTag> light) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    if (y != null) {
      entries.put("Y", b(y));
    }
    if (palette != null) {
      SequencedMap<String, NbtTag> container = new LinkedHashMap<>();
      container.put("palette", Nbt.listOf(palette));
      if (data != null) {
        container.put("data", longs(data));
      }
      entries.put("block_states", CompoundTag.of(container));
    }
    if (biome != null) {
      entries.put("biomes", compound("palette", Nbt.list(str(biome))));
    }
    entries.putAll(light);
    return CompoundTag.of(entries);
  }

  public static CompoundTag airSection(int y) {
    return section(y, List.of(air()), null, PLAINS, Map.of());
  }

  public static CompoundTag lightOnlySection(int y) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    entries.put("Y", b(y));
    entries.put("BlockLight", lightArray(0, k -> k == 0 ? 7 : 0));
    entries.put("SkyLight", lightArray(15, null));
    return CompoundTag.of(entries);
  }

  /** Variant names are those of RG3 and RG4; {@code "bedrock"} is the plain RG1 chunk. */
  public static CompoundTag chunk(int cx, int cz, boolean bedrock, String variant) {
    Map<Integer, CompoundTag> secs = new LinkedHashMap<>();
    for (int sy = -4; sy < 20; sy++) {
      secs.put(sy, sy == -4 && bedrock ? bottomSection(variant) : airSection(sy));
    }
    List<CompoundTag> extra = new ArrayList<>();
    if (variant.equals("skylight_chain")) {
      Map<String, ByteArrayTag> light = Map.of("SkyLight", lightArray(3, k -> k & 15));
      secs.put(19, section(19, List.of(air()), null, PLAINS, light));
    }
    switch (variant) {
      case "light_sections" -> extra = List.of(lightOnlySection(-5), lightOnlySection(20));
      case "far_sections" -> extra = List.of(lightOnlySection(-6), lightOnlySection(21));
      case "missing_section" -> secs.remove(0);
      case "no_section_y" -> secs.put(-3, section(null, List.of(air()), null, PLAINS, Map.of()));
      case "duplicate_section" -> extra = List.of(airSection(-4));
      default -> {}
    }
    List<CompoundTag> ordered = new ArrayList<>(secs.values());
    if (variant.equals("light_sections") || variant.equals("far_sections")) {
      ordered.addFirst(extra.get(0));
      ordered.add(extra.get(1));
    } else {
      ordered.addAll(extra);
    }
    if (variant.equals("unsorted_sections")) {
      Collections.reverse(ordered);
    }
    return root(cx, cz, bedrock, variant, ordered);
  }

  private static CompoundTag bottomSection(String variant) {
    int[] values = new int[4096];
    for (int k = 0; k < 256; k++) {
      values[k] = variant.equals("index_overflow") ? 2 : 1;
    }
    long[] data = RefPacking.pack(values, 4);
    if (variant.equals("bad_length")) {
      data = Arrays.copyOf(data, 255);
    }
    List<NbtTag> palette = List.of(air(), bottomEntry(variant));
    if (variant.equals("string_palette")) {
      palette = List.of(str(Blocks.AIR), str(Blocks.BEDROCK));
    } else if (variant.equals("empty_palette")) {
      palette = List.of();
      data = null;
    }
    return section(
        -4,
        variant.equals("missing_block_states") ? null : palette,
        data,
        variant.equals("missing_biomes")
            ? null
            : variant.equals("unknown_biome") ? "probe:unknown_biome" : PLAINS,
        Map.of());
  }

  private static CompoundTag bottomEntry(String variant) {
    return switch (variant) {
      case "partial_props" -> compound("id", str(Blocks.OAK_LOG));
      case "unknown_block" -> compound("id", str("probe:unknown"));
      case "bad_value" ->
          compound("id", str(Blocks.OAK_LOG), "properties", compound("axis", str("w")));
      default -> compound("id", str(Blocks.BEDROCK));
    };
  }

  private static CompoundTag root(
      int cx, int cz, boolean bedrock, String variant, List<CompoundTag> sections) {
    long[] hm = RefPacking.pack(constant(256, bedrock ? 1 : 0), 9);
    Map<String, long[]> heightmaps = new LinkedHashMap<>();
    for (String key : HEIGHTMAP_KEYS) {
      heightmaps.put(key, hm);
    }
    if (variant.equals("bad_heightmap_length")) {
      heightmaps.put("MOTION_BLOCKING", Arrays.copyOf(hm, 36));
    }
    if (variant.equals("heightmap_overflow")) {
      int[] values = constant(256, 1);
      values[0] = 400;
      heightmaps.put("WORLD_SURFACE", RefPacking.pack(values, 9));
    }
    SequencedMap<String, NbtTag> heightmapTags = new LinkedHashMap<>();
    heightmaps.forEach((key, value) -> heightmapTags.put(key, longs(value)));
    SequencedMap<String, NbtTag> root = new LinkedHashMap<>();
    root.put("DataVersion", i(5023));
    root.put("xPos", i(variant.equals("wrong_pos") ? -2 : cx));
    root.put("zPos", i(cz));
    if (!variant.equals("ypos_absent")) {
      root.put("yPos", i(variant.equals("ypos_wrong") ? 0 : -4));
    }
    root.put("Status", str("minecraft:full"));
    root.put("LastUpdate", l(0));
    root.put("InhabitedTime", l(0));
    if (!variant.equals("no_sections_key")) {
      root.put("sections", Nbt.listOf(sections));
    }
    root.put("Heightmaps", CompoundTag.of(heightmapTags));
    root.put("block_entities", ListTag.empty());
    root.put("block_ticks", ListTag.empty());
    root.put("fluid_ticks", ListTag.empty());
    if (variant.equals("postprocessing")) {
      root.put("PostProcessing", postProcessing());
    }
    return CompoundTag.of(root);
  }

  private static ListTag postProcessing() {
    List<ListTag> lists = new ArrayList<>();
    for (int k = 0; k < OVERWORLD_SECTIONS; k++) {
      lists.add(ListTag.empty());
    }
    lists.set(0, Nbt.list(s(pp(3, 0, 9))));
    lists.set(4, Nbt.list(s(pp(3, 5, 9))));
    lists.set(5, Nbt.list(s(0x1000)));
    return Nbt.listOf(lists);
  }

  /** A full chunk of the Nether (min_y 0, height 256): 16 air sections, heightmaps at 0. */
  public static CompoundTag netherChunk(int cx, int cz) {
    List<CompoundTag> sections = new ArrayList<>();
    for (int y = 0; y < 16; y++) {
      sections.add(airSection(y));
    }
    long[] zeros = RefPacking.pack(constant(256, 0), 9);
    SequencedMap<String, NbtTag> root = new LinkedHashMap<>();
    root.put("DataVersion", i(5023));
    root.put("xPos", i(cx));
    root.put("zPos", i(cz));
    root.put("yPos", i(0));
    root.put("Status", str("minecraft:full"));
    root.put("LastUpdate", l(0));
    root.put("InhabitedTime", l(0));
    root.put("sections", Nbt.listOf(sections));
    root.put(
        "Heightmaps", compound("MOTION_BLOCKING", longs(zeros), "WORLD_SURFACE", longs(zeros)));
    root.put("block_entities", ListTag.empty());
    root.put("block_ticks", ListTag.empty());
    root.put("fluid_ticks", ListTag.empty());
    return CompoundTag.of(root);
  }

  /** The Short of a local position, form 0ZYX (CF-R24). */
  public static int pp(int x, int y, int z) {
    return x | (y << 4) | (z << 8);
  }

  public static int[] constant(int count, int value) {
    int[] values = new int[count];
    Arrays.fill(values, value);
    return values;
  }

  /** The NBT bytes of chunk A (-1, -1) for a variant of RG3 or RG4, damaged ones included. */
  public static byte[] chunkA(String variant) {
    byte[] plain = bytes(chunk(-1, -1, true, "bedrock"));
    return switch (variant) {
      case "root_named" -> bytes(chunk(-1, -1, true, "bedrock"), "chunk");
      case "trailing_bytes" -> Hex.concat(plain, new byte[] {0, 1, 2, 3, 4});
      case "root_not_compound" -> Hex.bytes("09 00 00 01 00 00 00 00");
      case "invalid_nbt" -> Arrays.copyOf(plain, 3000);
      case "bomb" -> new byte[4 << 20];
      case "bedrock", "corrupt_zlib", "compression_127" -> plain;
      default -> bytes(chunk(-1, -1, true, variant));
    };
  }

  /** The NBT file bytes of a root Compound with the given root name. */
  public static byte[] bytes(CompoundTag root, String name) {
    try {
      return NbtWriter.of(NbtCompression.NONE).writeNamed(new NamedTag(name, root));
    } catch (NbtException e) {
      throw new IllegalStateException(e);
    }
  }

  /** The NBT file bytes with the empty root name, as the engine writes them (AR-R24). */
  public static byte[] bytes(CompoundTag root) {
    return bytes(root, "");
  }
}

package dev.shoko.minecraft.world.support;

import dev.shoko.minecraft.world.block.BlockProperty;
import dev.shoko.minecraft.world.block.BlockReportEntry;
import dev.shoko.minecraft.world.block.BlockReportException;
import dev.shoko.minecraft.world.block.BlockReportSource;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.block.ReportedState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * A small made-up block report that has the shapes of the real one (no data of the game): blocks
 * without properties, a default that is not the first state, a report whose property order is not
 * the id order (chest), several value lists for {@code facing}, integers that do not start at 0, a
 * large product. Ids are computed here with the formula of BS-R5 and BS-R6, independently of the
 * code under test.
 *
 * <p>Protocol order and first state ids: air 0, stone 1, grass_block 2 to 3, bedrock 4, water 5 to
 * 20, oak_log 21 to 23, chest 24 to 47, hopper 48 to 57, water_cauldron 58 to 60, acacia_fence 61
 * to 92, note_block 93 to 1442, barrel 1443 to 1454. 1455 states in all.
 */
public final class Blocks {

  public static final int BLOCK_COUNT = 12;
  public static final int STATE_COUNT = 1455;

  public static final String AIR = "minecraft:air";
  public static final String STONE = "minecraft:stone";
  public static final String GRASS_BLOCK = "minecraft:grass_block";
  public static final String BEDROCK = "minecraft:bedrock";
  public static final String WATER = "minecraft:water";
  public static final String OAK_LOG = "minecraft:oak_log";
  public static final String CHEST = "minecraft:chest";
  public static final String HOPPER = "minecraft:hopper";
  public static final String WATER_CAULDRON = "minecraft:water_cauldron";
  public static final String ACACIA_FENCE = "minecraft:acacia_fence";
  public static final String NOTE_BLOCK = "minecraft:note_block";
  public static final String BARREL = "minecraft:barrel";

  private static final List<String> BOOLEANS = List.of("true", "false");

  /** One block of the made-up report. */
  public record Def(String id, List<BlockProperty> reportOrder, Map<String, String> defaults) {}

  private Blocks() {}

  public static BlockProperty prop(String name, String... values) {
    return new BlockProperty(name, List.of(values));
  }

  public static BlockProperty range(String name, int from, int toInclusive) {
    return new BlockProperty(
        name, IntStream.rangeClosed(from, toInclusive).mapToObj(Integer::toString).toList());
  }

  public static List<Def> definitions() {
    List<Def> all = new ArrayList<>(smallBlocks());
    all.addAll(largeBlocks());
    return all;
  }

  private static BlockProperty bool(String name) {
    return new BlockProperty(name, BOOLEANS);
  }

  private static List<Def> smallBlocks() {
    return List.of(
        new Def(AIR, List.of(), Map.of()),
        new Def(STONE, List.of(), Map.of()),
        new Def(GRASS_BLOCK, List.of(bool("snowy")), Map.of("snowy", "false")),
        new Def(BEDROCK, List.of(), Map.of()),
        new Def(WATER, List.of(range("level", 0, 15)), Map.of("level", "0")),
        new Def(OAK_LOG, List.of(prop("axis", "x", "y", "z")), Map.of("axis", "y")),
        new Def(
            CHEST,
            List.of(
                prop("type", "single", "left", "right"),
                prop("facing", "north", "south", "west", "east"),
                bool("waterlogged")),
            Map.of("facing", "north", "type", "single", "waterlogged", "false")),
        new Def(
            HOPPER,
            List.of(bool("enabled"), prop("facing", "down", "north", "south", "west", "east")),
            Map.of("enabled", "true", "facing", "down")),
        new Def(WATER_CAULDRON, List.of(range("level", 1, 3)), Map.of("level", "1")));
  }

  private static List<Def> largeBlocks() {
    List<String> instruments =
        new ArrayList<>(List.of("harp", "basedrum", "snare", "hat", "bass", "flute", "bell"));
    for (int k = instruments.size(); k < 27; k++) {
      instruments.add("instrument_" + k);
    }
    Map<String, String> fenceDefaults = new LinkedHashMap<>();
    for (String name : new String[] {"east", "north", "south", "waterlogged", "west"}) {
      fenceDefaults.put(name, "false");
    }
    return List.of(
        new Def(
            ACACIA_FENCE,
            List.of(bool("east"), bool("north"), bool("south"), bool("waterlogged"), bool("west")),
            fenceDefaults),
        new Def(
            NOTE_BLOCK,
            List.of(
                new BlockProperty("instrument", instruments),
                range("note", 0, 24),
                bool("powered")),
            Map.of("instrument", "harp", "note", "0", "powered", "false")),
        new Def(
            BARREL,
            List.of(prop("facing", "north", "east", "south", "west", "up", "down"), bool("open")),
            Map.of("facing", "north", "open", "false")));
  }

  /** The report entries in protocol order, ids assigned by BS-R5 and BS-R6. */
  public static List<BlockReportEntry> entries() {
    List<BlockReportEntry> entries = new ArrayList<>();
    int next = 0;
    for (Def def : definitions()) {
      BlockReportEntry entry = entry(def, next);
      entries.add(entry);
      next += entry.states().size();
    }
    return entries;
  }

  /** Builds one block whose first state has this id. */
  public static BlockReportEntry entry(Def def, int firstId) {
    List<BlockProperty> sorted =
        def.reportOrder().stream()
            .sorted(java.util.Comparator.comparing(BlockProperty::name))
            .toList();
    int count = sorted.stream().mapToInt(p -> p.values().size()).reduce(1, (a, b) -> a * b);
    List<ReportedState> states = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      Map<String, String> values = new LinkedHashMap<>();
      int rest = index;
      for (int k = sorted.size() - 1; k >= 0; k--) {
        List<String> list = sorted.get(k).values();
        values.put(sorted.get(k).name(), list.get(rest % list.size()));
        rest /= list.size();
      }
      Map<String, String> ordered = new LinkedHashMap<>();
      sorted.forEach(p -> ordered.put(p.name(), values.get(p.name())));
      boolean isDefault = ordered.equals(sortedDefaults(def));
      states.add(new ReportedState(firstId + index, ordered, isDefault));
    }
    return new BlockReportEntry(def.id(), def.reportOrder(), states);
  }

  private static Map<String, String> sortedDefaults(Def def) {
    return new java.util.TreeMap<>(def.defaults());
  }

  public static BlockReportSource sourceOf(List<BlockReportEntry> entries) {
    List<BlockReportEntry> copy = List.copyOf(entries);
    return () -> copy;
  }

  public static BlockReportSource source() {
    return sourceOf(entries());
  }

  /** Loads the standard made-up report. */
  public static BlockStateRegistry registry() {
    try {
      return BlockStateRegistry.load(source());
    } catch (BlockReportException e) {
      throw new IllegalStateException("the made-up report must load", e);
    }
  }

  public static BlockReportEntry entryNamed(List<BlockReportEntry> entries, String id) {
    return entries.stream().filter(e -> e.id().equals(id)).findFirst().orElseThrow();
  }

  /** Replaces the entry with this id by another one. */
  public static List<BlockReportEntry> replacing(
      List<BlockReportEntry> entries, String id, BlockReportEntry replacement) {
    return entries.stream().map(e -> e.id().equals(id) ? replacement : e).toList();
  }
}

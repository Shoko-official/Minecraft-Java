package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.loot.assembly.LootDataLoader;
import dev.shoko.minecraft.loot.assembly.LootLoadResult;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Packs of named predicates and number providers that form a DAG without a cycle: element i
 * references element i + 1 twice, so a walk that does not stop makes 2^levels evaluations. The
 * leaves draw a random number, so that a {@link WatchedRandom} sees the work.
 */
public final class NamedDags {

  /** A condition that is always false and draws one number. */
  public static final String FALSE_LEAF = "{'type':'minecraft:random_chance','chance':0.0}";

  /** A condition that is always true and draws one number. */
  public static final String TRUE_LEAF = "{'type':'minecraft:random_chance','chance':1.0}";

  /** An integer provider that draws one number and gives 0 or 1. */
  public static final String INT_LEAF = "{'type':'minecraft:uniform','min':0,'max':1}";

  /** A float provider that draws one number. */
  public static final String FLOAT_LEAF = "{'type':'minecraft:uniform','min':0.0,'max':1.0}";

  private NamedDags() {}

  /** Loads a pack of the made-up universe plus these files, as the game loads data. */
  public static LootLoadResult load(Map<String, String> files) {
    Map<String, String> all = new LinkedHashMap<>(MiniDatapack.enchantmentFiles());
    all.putAll(files);
    return LootDataLoader.load(
        MiniDatapack.load(all), LootItems.catalog(), RecipeSet.of(new LinkedHashMap<>()));
  }

  /** Predicates {@code prefix}0 to {@code prefix}{levels}: the last one is {@code leaf}. */
  public static Map<String, String> predicateDag(
      int levels, String operator, String leaf, String prefix) {
    Map<String, String> files = new LinkedHashMap<>();
    for (int i = 0; i < levels; i++) {
      String next = "'minecraft:" + prefix + (i + 1) + "'";
      files.put(
          "predicate/" + prefix + i,
          "{'type':'minecraft:" + operator + "','terms':[" + next + "," + next + "]}");
    }
    files.put("predicate/" + prefix + levels, leaf);
    return files;
  }

  /** Providers of a registry such as {@code context_int_provider}, built like the predicates. */
  public static Map<String, String> providerDag(
      int levels, String operator, String leaf, String registry, String prefix) {
    Map<String, String> files = new LinkedHashMap<>();
    for (int i = 0; i < levels; i++) {
      String next = "'minecraft:" + prefix + (i + 1) + "'";
      files.put(
          registry + "/" + prefix + i,
          "{'type':'minecraft:" + operator + "','inputs':[" + next + "," + next + "]}");
    }
    files.put(registry + "/" + prefix + levels, leaf);
    return files;
  }
}

package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.context.ContextKey;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.context.LootContextType;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.number.NumberProviderSet;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.predicate.NamedPredicates;
import dev.shoko.minecraft.loot.predicate.match.ItemMatch;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.table.LootTables;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;

/** Builds {@link LootData} and contexts from the made-up fixtures. */
public final class Ctx {

  /** A context type that allows every parameter and requires none, so that no set is assumed. */
  public static final LootContextType ANY =
      new LootContextType(
          Identifier.minecraft("generic"),
          1,
          Set.of(),
          Set.copyOf(EnumSet.allOf(ContextKey.class)));

  private Ctx() {}

  /** The named predicates {@code tool/can_silk_touch} and {@code tool/can_shear}, from records. */
  public static NamedPredicates standardPredicates() {
    SequencedMap<Identifier, LootCondition> map = new LinkedHashMap<>();
    ItemMatch silk =
        new ItemMatch(
            Sets.anyItem(),
            Sets.anyNumber(),
            new LinkedHashMap<>(
                Map.of(
                    Identifier.minecraft("enchantments"),
                    Json.parse("[{'enchantments':'minecraft:silk_touch','levels':{'min':1}}]"))),
            new LinkedHashMap<>());
    map.put(Identifier.minecraft("tool/can_silk_touch"), new LootCondition.MatchTool(silk));
    ItemMatch shear =
        new ItemMatch(
            Sets.items("shears"), Sets.anyNumber(), new LinkedHashMap<>(), new LinkedHashMap<>());
    map.put(Identifier.minecraft("tool/can_shear"), new LootCondition.MatchTool(shear));
    return new NamedPredicates(map);
  }

  /** Data with these tables (paths of {@code minecraft}), the standard predicates, no providers. */
  public static LootData data(Map<String, LootTable> tables) {
    return data(tables, standardPredicates(), emptyProviders());
  }

  public static LootData data() {
    return data(Map.of());
  }

  public static LootData data(
      Map<String, LootTable> tables, NamedPredicates predicates, NumberProviderSet numbers) {
    SequencedMap<Identifier, LootTable> byId = new LinkedHashMap<>();
    tables.forEach((path, table) -> byId.put(Identifier.minecraft(path), table));
    return new LootData(
        new LootTables(byId),
        predicates,
        numbers,
        EnchantFixtures.catalog(),
        LootItems.catalog(),
        RecipeSet.of(new LinkedHashMap<>()),
        EnchantFixtures.access());
  }

  public static NumberProviderSet emptyProviders() {
    return new NumberProviderSet(new LinkedHashMap<>(), new LinkedHashMap<>());
  }

  /** Providers by path of {@code minecraft}. */
  public static NumberProviderSet providers(
      Map<String, IntProvider> ints, Map<String, FloatProvider> floats) {
    SequencedMap<Identifier, IntProvider> intMap = new LinkedHashMap<>();
    ints.forEach((path, value) -> intMap.put(Identifier.minecraft(path), value));
    SequencedMap<Identifier, FloatProvider> floatMap = new LinkedHashMap<>();
    floats.forEach((path, value) -> floatMap.put(Identifier.minecraft(path), value));
    return new NumberProviderSet(intMap, floatMap);
  }

  /** A builder over {@link #ANY} and the data of these tables. */
  public static LootContext.Builder builder(Map<String, LootTable> tables, RandomSource random) {
    return LootContext.builder(data(tables), ANY, random);
  }

  public static LootContext.Builder builder(RandomSource random) {
    return LootContext.builder(data(), ANY, random);
  }

  public static LootContext context(RandomSource random) {
    return builder(random).build();
  }

  public static List<Identifier> ids(String... paths) {
    return java.util.Arrays.stream(paths).map(Identifier::minecraft).toList();
  }
}

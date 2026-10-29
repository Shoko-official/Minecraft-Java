package dev.shoko.minecraft.loot.assembly;

import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.loader.ElementRegistryData;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.enchant.EnchantmentCatalog;
import dev.shoko.minecraft.loot.enchant.EnchantmentCodec;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import dev.shoko.minecraft.loot.enchant.EnchantmentProvider;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.number.NumberProviderCodec;
import dev.shoko.minecraft.loot.number.NumberProviderSet;
import dev.shoko.minecraft.loot.predicate.ConditionCodec;
import dev.shoko.minecraft.loot.predicate.NamedPredicates;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.table.LootTableCodec;
import dev.shoko.minecraft.loot.table.LootTables;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.BiFunction;

/**
 * Decodes the registries owned by {@code loot} from the datapack loader's output: {@code
 * loot_table}, {@code predicate}, {@code context_int_provider}, {@code context_float_provider},
 * {@code enchantment} and {@code enchantment_provider}. The winner of a path between packs is
 * already chosen by {@code data}; this decodes the winners only.
 */
public final class LootDataLoader {

  private static final Identifier LOOT_TABLE = Identifier.minecraft("loot_table");
  private static final Identifier PREDICATE = Identifier.minecraft("predicate");
  private static final Identifier INT_PROVIDER = Identifier.minecraft("context_int_provider");
  private static final Identifier FLOAT_PROVIDER = Identifier.minecraft("context_float_provider");
  private static final Identifier ENCHANTMENT = Identifier.minecraft("enchantment");
  private static final Identifier ENCHANTMENT_PROVIDER =
      Identifier.minecraft("enchantment_provider");

  private LootDataLoader() {}

  /**
   * Loads the module data, never stopping at the first problem.
   *
   * @param loaded the datapack loader's output
   * @param items the loaded items
   * @param recipes the loaded recipes
   * @return the data and the findings; a table whose named predicate or nested table does not exist
   *     is absent with an {@code UNRESOLVED_REFERENCE} finding (LT-R61b), a table that references
   *     itself is absent with {@code SELF_REFERENCE} (LT-R23)
   */
  public static LootLoadResult load(LoadedData loaded, ItemCatalog items, RecipeSet recipes) {
    RegistryAccess access = loaded.registryAccess();
    List<LootLoadFinding> findings = new ArrayList<>();

    SequencedMap<Identifier, IntProvider> ints =
        decode(loaded, INT_PROVIDER, findings, NumberProviderCodec::decodeInt, access);
    SequencedMap<Identifier, FloatProvider> floats =
        decode(loaded, FLOAT_PROVIDER, findings, NumberProviderCodec::decodeFloat, access);
    NumberProviderSet numbers = new NumberProviderSet(ints, floats);
    for (LootFinding unresolved : NumberProviderCodec.unresolvedReferences(numbers)) {
      String owner = unresolved.message().substring(0, unresolved.message().indexOf(' '));
      findings.add(
          new LootLoadFinding(
              INT_PROVIDER,
              Identifier.tryParse(owner).orElse(Identifier.minecraft("unknown")),
              unresolved));
    }
    NamedPredicates predicates =
        new NamedPredicates(decode(loaded, PREDICATE, findings, ConditionCodec::decode, access));

    SequencedMap<Identifier, EnchantmentDefinition> definitions = new LinkedHashMap<>();
    elements(loaded, ENCHANTMENT)
        .forEach(
            (id, node) -> {
              LootDecoding<EnchantmentDefinition> decoded =
                  EnchantmentCodec.decode(id, node, access);
              relay(findings, ENCHANTMENT, id, decoded.findings());
              if (decoded instanceof LootDecoding.Success<EnchantmentDefinition> ok) {
                definitions.put(id, ok.value());
              }
            });
    SequencedMap<Identifier, EnchantmentProvider> providers =
        decode(loaded, ENCHANTMENT_PROVIDER, findings, EnchantmentCodec::decodeProvider, access);
    EnchantmentCatalog catalog = new EnchantmentCatalog(definitions, providers);

    LootTables tables = loadTables(loaded, items, access, predicates, findings);
    LootData data = new LootData(tables, predicates, numbers, catalog, items, recipes, access);
    return new LootLoadResult(data, List.copyOf(findings));
  }

  private static LootTables loadTables(
      LoadedData loaded,
      ItemCatalog items,
      RegistryAccess access,
      NamedPredicates predicates,
      List<LootLoadFinding> findings) {
    SequencedMap<Identifier, LootTable> decoded = new LinkedHashMap<>();
    Map<Identifier, List<LootFinding>> own = new LinkedHashMap<>();
    elements(loaded, LOOT_TABLE)
        .forEach(
            (id, node) -> {
              LootDecoding<LootTable> result = LootTableCodec.decode(node, access, items);
              own.put(id, new ArrayList<>(result.findings()));
              if (result instanceof LootDecoding.Success<LootTable> ok) {
                decoded.put(id, ok.value());
              }
            });
    SequencedMap<Identifier, List<LootFinding>> checks = new LinkedHashMap<>();
    boolean removed = true;
    while (removed) {
      removed = false;
      checks.clear();
      LootTables known = new LootTables(new LinkedHashMap<>(decoded));
      for (Map.Entry<Identifier, LootTable> entry : List.copyOf(decoded.entrySet())) {
        List<LootFinding> found =
            LootTableCodec.checkReferences(entry.getKey(), entry.getValue(), known, predicates);
        checks.put(entry.getKey(), found);
        if (found.stream().anyMatch(f -> f.code().severity() == Severity.ERROR)) {
          own.get(entry.getKey()).addAll(found);
          decoded.remove(entry.getKey());
          removed = true;
        }
      }
    }
    for (Map.Entry<Identifier, List<LootFinding>> entry : own.entrySet()) {
      relay(findings, LOOT_TABLE, entry.getKey(), entry.getValue());
      if (decoded.containsKey(entry.getKey())) {
        relay(findings, LOOT_TABLE, entry.getKey(), checks.get(entry.getKey()));
      }
    }
    return new LootTables(decoded);
  }

  private static <T> SequencedMap<Identifier, T> decode(
      LoadedData loaded,
      Identifier registry,
      List<LootLoadFinding> findings,
      BiFunction<JsonNode, RegistryAccess, LootDecoding<T>> codec,
      RegistryAccess access) {
    SequencedMap<Identifier, T> values = new LinkedHashMap<>();
    elements(loaded, registry)
        .forEach(
            (id, node) -> {
              LootDecoding<T> decoded = codec.apply(node, access);
              relay(findings, registry, id, decoded.findings());
              if (decoded instanceof LootDecoding.Success<T> ok) {
                values.put(id, ok.value());
              }
            });
    return values;
  }

  private static SequencedMap<Identifier, JsonNode> elements(
      LoadedData loaded, Identifier registry) {
    return loaded
        .elementRegistry(registry)
        .map(ElementRegistryData::elements)
        .orElseGet(LinkedHashMap::new);
  }

  private static void relay(
      List<LootLoadFinding> out, Identifier registry, Identifier element, List<LootFinding> found) {
    found.forEach(f -> out.add(new LootLoadFinding(registry, element, f)));
  }
}

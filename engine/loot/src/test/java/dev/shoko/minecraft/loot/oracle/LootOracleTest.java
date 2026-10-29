package dev.shoko.minecraft.loot.oracle;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.codec.RecipeLoader;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.assembly.LootDataLoader;
import dev.shoko.minecraft.loot.assembly.LootLoadResult;
import dev.shoko.minecraft.loot.context.LootContextTypes;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootPool;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.table.LootTableCodec;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.ReferenceCache;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * The module against the extracted reference data (counts of LT-R1 to LT-R10, V-L7, EN-R1, EN-R34,
 * plan of tests L1). Reads the local cache, copies nothing, and skips itself when the cache is
 * absent. Commands that produce the counts: {@code python -I loot_stats.py "$REF"} and {@code
 * python -I enchant_stats.py "$REF"} (annexes of the specs).
 */
@Tag("oracle")
@Timeout(600)
class LootOracleTest {

  private static LoadedData loaded;
  private static LootLoadResult result;

  private static synchronized LootLoadResult result() {
    if (result == null) {
      loaded = ReferenceCache.vanilla();
      ItemCatalog items = ReferenceCache.items();
      RecipeSet recipes =
          RecipeLoader.load(
                  loaded.elementRegistry(Identifier.minecraft("recipe")).orElseThrow(),
                  loaded.registryAccess(),
                  items)
              .recipes();
      result = LootDataLoader.load(loaded, items, recipes);
    }
    return result;
  }

  private static LootData data() {
    return result().data();
  }

  private static int itemEntries(List<LootEntry> entries) {
    int count = 0;
    for (LootEntry entry : entries) {
      switch (entry.kind()) {
        case LootEntry.Kind.Item item -> count++;
        case LootEntry.Kind.Alternatives alternatives ->
            count += itemEntries(alternatives.children());
        case LootEntry.Kind.Sequence sequence -> count += itemEntries(sequence.children());
        case LootEntry.Kind.Group group -> count += itemEntries(group.children());
        default -> {}
      }
    }
    return count;
  }

  @Test
  void theVanillaDataLoadsWithoutAnyErrorFinding() {
    // spec: LT-R1, V-L1 (loading without error), EN-R1
    long errors =
        result().findings().stream()
            .filter(f -> f.finding().code().severity() == Severity.ERROR)
            .count();
    assertThat(errors).isZero();
  }

  @Test
  void theCountsMatchTheSpecs() {
    // spec: LT-R1 (1447 tables), LT-R61a (3 predicates), LT-R50 (26 + 4 providers), EN-R1 (43),
    // EN-R34 (7)
    LootData data = data();
    assertThat(data.tables().tables()).hasSize(1447);
    assertThat(data.predicates().predicates()).hasSize(3);
    assertThat(data.numbers().ints()).hasSize(26);
    assertThat(data.numbers().floats()).hasSize(4);
    assertThat(data.enchantments().definitions()).hasSize(43);
    assertThat(data.enchantments().providers()).hasSize(7);
  }

  @Test
  void thePoolsAndTheItemEntriesAreCounted() {
    // spec: LT-R5a (1589 pools), LT-R7 (2556 item entries), V-L7
    int pools = 0;
    int items = 0;
    for (LootTable table : data().tables().tables().values()) {
      pools += table.pools().size();
      for (LootPool pool : table.pools()) {
        items += itemEntries(pool.entries());
      }
    }
    assertThat(pools).isEqualTo(1589);
    assertThat(items).isEqualTo(2556);
  }

  @Test
  void theTablesAreSplitByContextTypeAsInTheSpec() {
    // spec: LT-R2 (block 1201, entity 115, chest 68, shearing 22, gift 21, archaeology 6, ...)
    Map<String, Integer> byType = new TreeMap<>();
    data()
        .tables()
        .tables()
        .values()
        .forEach(t -> byType.merge(t.type().id().path(), 1, Integer::sum));
    Map<String, Integer> expected = new TreeMap<>();
    expected.put("block", 1201);
    expected.put("entity", 115);
    expected.put("chest", 68);
    expected.put("shearing", 22);
    expected.put("gift", 21);
    expected.put("archaeology", 6);
    expected.put("block_interact", 5);
    expected.put("fishing", 4);
    expected.put("equipment", 3);
    expected.put("entity_interact", 1);
    expected.put("barter", 1);
    assertThat(byType).isEqualTo(expected);
  }

  @Test
  void everyContextTypeOfATableIsOneOfTheRegistered() {
    // spec: LT-R13
    for (LootTable table : data().tables().tables().values()) {
      assertThat(LootContextTypes.find(table.type().id())).isPresent();
    }
  }

  @Test
  void everyTableEncodesBackToItsFile() {
    // spec: plan of tests L1 (decode, encode, same tree, key order aside)
    result();
    var elements =
        loaded.elementRegistry(Identifier.minecraft("loot_table")).orElseThrow().elements();
    for (Map.Entry<Identifier, LootTable> entry : data().tables().tables().entrySet()) {
      JsonNode original = elements.get(entry.getKey());
      String encoded = LootTableCodec.encode(entry.getValue());
      JsonNode again = Json.parseStrict(encoded);
      assertThat(again.sameValueAs(original)).as(entry.getKey().toString()).isTrue();
    }
  }

  @Test
  void theFortyThreeEnchantmentsKeepTheirParameters() {
    // spec: EN-R3, EN-R4, V-E1 (sharpness: 5 / 10 / 1, min 1 + 11 per level)
    var sharpness = data().enchantments().find(Identifier.minecraft("sharpness")).orElseThrow();
    assertThat(sharpness.maxLevel()).isEqualTo(5);
    assertThat(sharpness.costs().weight()).isEqualTo(10);
    assertThat(sharpness.costs().anvilCost()).isEqualTo(1);
    assertThat(sharpness.minCost(1)).isEqualTo(1);
    assertThat(sharpness.maxCost(5)).isEqualTo(65);
    var mending = data().enchantments().find(Identifier.minecraft("mending")).orElseThrow();
    assertThat(mending.maxLevel()).isEqualTo(1);
    assertThat(mending.minCost(1)).isEqualTo(25);
  }
}

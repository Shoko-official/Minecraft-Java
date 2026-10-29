package dev.shoko.minecraft.loot.assembly;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.table.LootTableCodec;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.MiniDatapack;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: LT-R75 (unknown key: the table that holds it is refused, one UNKNOWN_FIELD finding per key,
// H-unknown-ignore is the rejected alternative), LT-R76 (cycles of named predicates, providers and
// tables are detected at load), LT-R77 (an unresolved or cyclic reference refuses the table that
// holds it, H-ref-a, the other tables load), LT-R23, LT-R61b, probes PL24 and PL25.
class LoadRobustnessTest {

  private static final String HEALTHY =
      "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item',"
          + "'name':'minecraft:stone'}]}]}";

  private static LootDecoding<LootTable> decode(String json) {
    return LootTableCodec.decode(Json.parse(json), EnchantFixtures.access(), LootItems.catalog());
  }

  private static String pool(String extraPoolKeys, String entry) {
    return "{'type':'minecraft:chest','pools':[{'rolls':1,"
        + extraPoolKeys
        + "'entries':["
        + entry
        + "]}]}";
  }

  private static final String STONE = "{'type':'minecraft:item','name':'minecraft:stone'";

  private static void assertRefusedForTheKey(String json, int unknownKeys) {
    LootDecoding<LootTable> decoding = decode(json);
    Decoded.failsWith(decoding, LootFindingCode.UNKNOWN_FIELD);
    List<LootFinding> unknown =
        decoding.findings().stream()
            .filter(f -> f.code() == LootFindingCode.UNKNOWN_FIELD)
            .toList();
    assertThat(unknown).hasSize(unknownKeys);
    assertThat(unknown).allSatisfy(f -> assertThat(f.path()).endsWith("zzz"));
  }

  @Test
  void provisionalAnUnknownKeyOfATableRefusesTheTable() {
    // spec: LT-R75 (table)
    assertRefusedForTheKey("{'type':'minecraft:chest','zzz':1,'pools':[]}", 1);
  }

  @Test
  void provisionalAnUnknownKeyOfAPoolRefusesTheTable() {
    // spec: LT-R75 (pool)
    assertRefusedForTheKey(pool("'zzz':1,", STONE + "}"), 1);
  }

  @Test
  void provisionalAnUnknownKeyOfAnEntryRefusesTheTable() {
    // spec: LT-R75 (entry)
    assertRefusedForTheKey(pool("", STONE + ",'zzz':1}"), 1);
  }

  @Test
  void provisionalAnUnknownKeyOfAConditionRefusesTheTable() {
    // spec: LT-R75 (condition)
    assertRefusedForTheKey(
        pool("", STONE + ",'condition':{'type':'minecraft:killed_by_player','zzz':1}}"), 1);
  }

  @Test
  void provisionalAnUnknownKeyOfAModifierRefusesTheTable() {
    // spec: LT-R75 (modifier)
    assertRefusedForTheKey(
        pool("", STONE + ",'modifier':{'type':'minecraft:set_count','count':2,'zzz':1}}"), 1);
  }

  @Test
  void provisionalAnUnknownKeyOfANumberProviderRefusesTheTable() {
    // spec: LT-R75 (number provider)
    String json =
        "{'type':'minecraft:chest','pools':[{'rolls':{'type':'minecraft:uniform','min':1,'max':2,"
            + "'zzz':1},'entries':[]}]}";
    assertRefusedForTheKey(json, 1);
  }

  @Test
  void provisionalEachUnknownKeyGetsItsOwnFinding() {
    // spec: LT-R75 (un diagnostic par cle)
    assertRefusedForTheKey(
        "{'type':'minecraft:chest','zzz':1,'pools':[{'rolls':1,'zzz':2,'entries':[]}]}", 2);
  }

  @Test
  void provisionalAnUnknownKeyRefusesOnlyTheTableThatHoldsIt() {
    // spec: LT-R75, LootDataLoader contract (the other tables load)
    Map<String, String> files = new LinkedHashMap<>(MiniDatapack.enchantmentFiles());
    files.put("loot_table/blocks/healthy", HEALTHY);
    files.put("loot_table/blocks/odd", "{'type':'minecraft:block','zzz':1,'pools':[]}");
    LootLoadResult result = load(files);
    assertThat(result.data().tables().find(Identifier.parse("minecraft:blocks/healthy")))
        .isPresent();
    assertThat(result.data().tables().find(Identifier.parse("minecraft:blocks/odd"))).isEmpty();
    assertThat(codesFor(result, "blocks/odd")).contains(LootFindingCode.UNKNOWN_FIELD);
  }

  private static LootLoadResult load(Map<String, String> files) {
    LoadedData loaded = MiniDatapack.load(files);
    return LootDataLoader.load(loaded, LootItems.catalog(), RecipeSet.of(new LinkedHashMap<>()));
  }

  private static List<LootFindingCode> codesFor(LootLoadResult result, String element) {
    Identifier id = Identifier.parse("minecraft:" + element);
    return result.findings().stream()
        .filter(f -> f.element().equals(id))
        .map(f -> f.finding().code())
        .toList();
  }

  private static boolean refused(LootLoadResult result, String table) {
    Identifier id = Identifier.parse("minecraft:" + table);
    boolean absent = result.data().tables().find(id).isEmpty();
    boolean error =
        result.findings().stream()
            .anyMatch(
                f -> f.element().equals(id) && f.finding().code().severity() == Severity.ERROR);
    return absent && error;
  }

  private static Map<String, String> base() {
    Map<String, String> files = new LinkedHashMap<>(MiniDatapack.enchantmentFiles());
    files.put("loot_table/blocks/healthy", HEALTHY);
    return files;
  }

  private static String withRolls(String rolls) {
    return "{'type':'minecraft:block','pools':[{'rolls':"
        + rolls
        + ",'entries':[{'type':'minecraft:item','name':'minecraft:stone'}]}]}";
  }

  private static void assertTableRefusedAndHealthyOneLoaded(
      Map<String, String> files, String table) {
    LootLoadResult result = load(files);
    assertThat(refused(result, table))
        .as("%s refused, findings %s", table, result.findings())
        .isTrue();
    assertThat(result.data().tables().find(Identifier.parse("minecraft:blocks/healthy")))
        .isPresent();
  }

  @Test
  void provisionalACycleOfNamedPredicatesRefusesTheTableThatUsesIt() {
    // spec: LT-R76, LT-R77 (A to B to A through all_of and any_of)
    Map<String, String> files = base();
    files.put("predicate/a", "{'type':'minecraft:all_of','terms':['minecraft:b']}");
    files.put("predicate/b", "{'type':'minecraft:any_of','terms':['minecraft:a']}");
    files.put(
        "loot_table/blocks/uses_a",
        "{'type':'minecraft:block','pools':[{'rolls':1,'condition':'minecraft:a','entries':[]}]}");
    assertTableRefusedAndHealthyOneLoaded(files, "blocks/uses_a");
  }

  @Test
  void provisionalAPredicateThatReferencesItselfRefusesTheTableThatUsesIt() {
    // spec: LT-R76 (A to A), LT-R77
    Map<String, String> files = base();
    files.put("predicate/loop", "{'type':'minecraft:inverted','term':'minecraft:loop'}");
    files.put(
        "loot_table/blocks/uses_loop",
        "{'type':'minecraft:block','pools':[{'rolls':1,'condition':'minecraft:loop','entries':[]}]}");
    assertTableRefusedAndHealthyOneLoaded(files, "blocks/uses_loop");
  }

  @Test
  void provisionalACycleOfNamedProvidersRefusesTheTableThatUsesIt() {
    // spec: LT-R76 (context_int_provider A to B to A), LT-R77 (rolls)
    Map<String, String> files = base();
    files.put("context_int_provider/a", "'minecraft:b'");
    files.put("context_int_provider/b", "'minecraft:a'");
    files.put("loot_table/blocks/uses_a", withRolls("'minecraft:a'"));
    assertTableRefusedAndHealthyOneLoaded(files, "blocks/uses_a");
  }

  @Test
  void provisionalACycleOfTablesRefusesBothTables() {
    // spec: LT-R76 (loot_table A to B to A), LT-R77 (the table that holds the reference)
    Map<String, String> files = base();
    files.put(
        "loot_table/blocks/a",
        "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table',"
            + "'value':'minecraft:blocks/b'}]}]}");
    files.put(
        "loot_table/blocks/b",
        "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table',"
            + "'value':'minecraft:blocks/a'}]}]}");
    LootLoadResult result = load(files);
    assertThat(refused(result, "blocks/a")).isTrue();
    assertThat(refused(result, "blocks/b")).isTrue();
    assertThat(result.data().tables().find(Identifier.parse("minecraft:blocks/healthy")))
        .isPresent();
  }

  @Test
  void provisionalAMissingProviderInRollsRefusesTheTable() {
    // spec: LT-R77 (rolls)
    Map<String, String> files = base();
    files.put("loot_table/blocks/bad", withRolls("'minecraft:no_such_provider'"));
    assertTableRefusedAndHealthyOneLoaded(files, "blocks/bad");
    assertThat(codesFor(load(files), "blocks/bad")).contains(LootFindingCode.UNRESOLVED_REFERENCE);
  }

  @Test
  void provisionalAMissingProviderInBonusRollsRefusesTheTable() {
    // spec: LT-R77 (bonus_rolls)
    Map<String, String> files = base();
    files.put(
        "loot_table/blocks/bad",
        "{'type':'minecraft:block','pools':[{'rolls':1,'bonus_rolls':'minecraft:no_such_provider',"
            + "'entries':[]}]}");
    assertTableRefusedAndHealthyOneLoaded(files, "blocks/bad");
  }

  @Test
  void provisionalAMissingProviderInSetCountRefusesTheTable() {
    // spec: LT-R77 (count of set_count)
    Map<String, String> files = base();
    files.put(
        "loot_table/blocks/bad",
        "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item',"
            + "'name':'minecraft:stone','modifier':{'type':'minecraft:set_count',"
            + "'count':'minecraft:no_such_provider'}}]}]}");
    assertTableRefusedAndHealthyOneLoaded(files, "blocks/bad");
  }

  @Test
  void provisionalAMissingProbabilityOfABinomialRefusesTheTableThatUsesIt() {
    // spec: LT-R77 (p of binomial)
    Map<String, String> files = base();
    files.put(
        "context_int_provider/bin",
        "{'type':'minecraft:binomial','n':3,'p':'minecraft:no_such_provider'}");
    files.put("loot_table/blocks/bad", withRolls("'minecraft:bin'"));
    assertTableRefusedAndHealthyOneLoaded(files, "blocks/bad");
  }

  @Test
  void provisionalAMissingInputOfFromFloatRefusesTheTableThatUsesIt() {
    // spec: LT-R77 (input of from_float)
    Map<String, String> files = base();
    files.put(
        "context_int_provider/ff",
        "{'type':'minecraft:from_float','input':'minecraft:no_such_provider'}");
    files.put("loot_table/blocks/bad", withRolls("'minecraft:ff'"));
    assertTableRefusedAndHealthyOneLoaded(files, "blocks/bad");
  }

  @Test
  void provisionalAMissingInputOfFromIntRefusesTheTableThatUsesIt() {
    // spec: LT-R77 (input of from_int)
    Map<String, String> files = base();
    files.put(
        "context_float_provider/fi",
        "{'type':'minecraft:from_int','input':'minecraft:no_such_provider'}");
    files.put(
        "loot_table/blocks/bad",
        "{'type':'minecraft:block','pools':[{'rolls':1,'bonus_rolls':'minecraft:fi',"
            + "'entries':[]}]}");
    assertTableRefusedAndHealthyOneLoaded(files, "blocks/bad");
  }

  @Test
  void provisionalAMissingNamedPredicateRefusesOnlyTheTableThatUsesIt() {
    // spec: LT-R61b, LT-R77
    Map<String, String> files = base();
    files.put(
        "loot_table/blocks/bad",
        "{'type':'minecraft:block','pools':[{'rolls':1,'condition':'minecraft:no_such_predicate',"
            + "'entries':[]}]}");
    assertTableRefusedAndHealthyOneLoaded(files, "blocks/bad");
  }

  @Test
  void provisionalATableThatReferencesItselfIsRefusedAtLevelZero() {
    // spec: LT-R23 (self reference), LT-R76 (A to A)
    Map<String, String> files = base();
    files.put(
        "loot_table/blocks/self",
        "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table',"
            + "'value':'minecraft:blocks/self'}]}]}");
    assertTableRefusedAndHealthyOneLoaded(files, "blocks/self");
    assertThat(codesFor(load(files), "blocks/self")).contains(LootFindingCode.SELF_REFERENCE);
  }
}

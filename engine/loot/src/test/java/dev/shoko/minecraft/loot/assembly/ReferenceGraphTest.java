package dev.shoko.minecraft.loot.assembly;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.MiniDatapack;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: LT-R76 (cycles of named predicates, providers and tables are found by a depth-first walk
// that must not depend on the depth of the call stack), LT-R77, LT-R23 (self reference),
// LT-R76 (the cycle is reported with a finding on the elements that lie on it).
@Timeout(120)
class ReferenceGraphTest {

  private static final int CHAIN = 10_000;

  private static LootLoadResult load(Map<String, String> files) {
    LoadedData loaded = MiniDatapack.load(files);
    return LootDataLoader.load(loaded, LootItems.catalog(), RecipeSet.of(new LinkedHashMap<>()));
  }

  private static Map<String, String> base() {
    return new LinkedHashMap<>(MiniDatapack.enchantmentFiles());
  }

  private static boolean hasFinding(LootLoadResult result, LootFindingCode code) {
    return result.findings().stream().anyMatch(f -> f.finding().code() == code);
  }

  private static List<LootFindingCode> codesFor(
      LootLoadResult result, String registry, String element) {
    Identifier registryId = Identifier.minecraft(registry);
    Identifier id = Identifier.parse("minecraft:" + element);
    return result.findings().stream()
        .filter(f -> f.registry().equals(registryId) && f.element().equals(id))
        .map(f -> f.finding().code())
        .toList();
  }

  private static String useOf(String predicate) {
    return "{'type':'minecraft:block','pools':[{'rolls':1,'condition':'minecraft:"
        + predicate
        + "','entries':[{'type':'minecraft:item','name':'minecraft:stone'}]}]}";
  }

  @Test
  void provisionalAChainOfTenThousandNamedPredicatesIsWalkedWithoutOverflowingTheStack() {
    // spec: LT-R76 (no cycle: nothing reported, whatever the depth)
    Map<String, String> files = base();
    for (int i = 0; i < CHAIN - 1; i++) {
      files.put(
          "predicate/p" + i, "{'type':'minecraft:all_of','terms':['minecraft:p" + (i + 1) + "']}");
    }
    files.put("predicate/p" + (CHAIN - 1), "{'type':'minecraft:all_of','terms':[]}");
    files.put("loot_table/blocks/uses", useOf("p0"));
    LootLoadResult result = load(files);
    assertThat(hasFinding(result, LootFindingCode.REFERENCE_CYCLE)).isFalse();
  }

  @Test
  void provisionalACycleOfTenThousandNamedPredicatesIsReportedWithoutOverflowingTheStack() {
    // spec: LT-R76 (cycle p0 to p9999 back to p0), LT-R77 (the table that uses it is refused)
    Map<String, String> files = base();
    for (int i = 0; i < CHAIN; i++) {
      files.put(
          "predicate/p" + i,
          "{'type':'minecraft:all_of','terms':['minecraft:p" + ((i + 1) % CHAIN) + "']}");
    }
    files.put("loot_table/blocks/uses", useOf("p0"));
    LootLoadResult result = load(files);
    assertThat(hasFinding(result, LootFindingCode.REFERENCE_CYCLE)).isTrue();
    assertThat(result.data().tables().find(Identifier.parse("minecraft:blocks/uses"))).isEmpty();
  }

  @Test
  void provisionalAChainOfTenThousandNamedProvidersIsWalkedWithoutOverflowingTheStack() {
    // spec: LT-R76 (providers that reference each other)
    Map<String, String> files = base();
    for (int i = 0; i < CHAIN - 1; i++) {
      files.put("context_int_provider/i" + i, "'minecraft:i" + (i + 1) + "'");
    }
    files.put("context_int_provider/i" + (CHAIN - 1), "1");
    LootLoadResult result = load(files);
    assertThat(hasFinding(result, LootFindingCode.REFERENCE_CYCLE)).isFalse();
  }

  @Test
  void provisionalACycleOfTenThousandNamedProvidersIsReportedWithoutOverflowingTheStack() {
    // spec: LT-R76
    Map<String, String> files = base();
    for (int i = 0; i < CHAIN; i++) {
      files.put("context_int_provider/i" + i, "'minecraft:i" + ((i + 1) % CHAIN) + "'");
    }
    LootLoadResult result = load(files);
    assertThat(hasFinding(result, LootFindingCode.REFERENCE_CYCLE)).isTrue();
  }

  @Test
  void provisionalAChainOfThreeThousandTablesIsWalkedWithoutOverflowingTheStack() {
    // spec: LT-R76 (loot_table chain), LT-R23
    int tables = 3000;
    Map<String, String> files = base();
    for (int i = 0; i < tables - 1; i++) {
      files.put(
          "loot_table/chain/t" + i,
          "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table',"
              + "'value':'minecraft:chain/t"
              + (i + 1)
              + "'}]}]}");
    }
    files.put("loot_table/chain/t" + (tables - 1), "{'type':'minecraft:block','pools':[]}");
    LootLoadResult result = load(files);
    assertThat(hasFinding(result, LootFindingCode.REFERENCE_CYCLE)).isFalse();
    assertThat(hasFinding(result, LootFindingCode.SELF_REFERENCE)).isFalse();
  }

  @Test
  void provisionalACycleOfThreeThousandTablesIsReportedWithoutOverflowingTheStack() {
    // spec: LT-R76 (loot_table cycle t0 to t2999 back to t0)
    int tables = 3000;
    Map<String, String> files = base();
    for (int i = 0; i < tables; i++) {
      files.put(
          "loot_table/chain/t" + i,
          "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table',"
              + "'value':'minecraft:chain/t"
              + ((i + 1) % tables)
              + "'}]}]}");
    }
    LootLoadResult result = load(files);
    assertThat(result.data().tables().tables()).isEmpty();
  }

  @Test
  void provisionalASelfReferenceOfATableIsReportedAsSelfReference() {
    // spec: LT-R23, LootFindingCode#SELF_REFERENCE
    Map<String, String> files = base();
    files.put(
        "loot_table/blocks/self",
        "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table',"
            + "'value':'minecraft:blocks/self'}]}]}");
    LootLoadResult result = load(files);
    assertThat(codesFor(result, "loot_table", "blocks/self"))
        .contains(LootFindingCode.SELF_REFERENCE);
    assertThat(result.data().tables().find(Identifier.parse("minecraft:blocks/self"))).isEmpty();
  }

  @Test
  void provisionalACycleIsReportedOnTheElementsThatLieOnIt() {
    // spec: LT-R76 (REFERENCE_CYCLE), a neighbour that only reaches the cycle is not on it
    Map<String, String> files = base();
    files.put("predicate/a", "{'type':'minecraft:all_of','terms':['minecraft:b']}");
    files.put("predicate/b", "{'type':'minecraft:any_of','terms':['minecraft:a']}");
    files.put("predicate/fine", "{'type':'minecraft:all_of','terms':[]}");
    LootLoadResult result = load(files);
    assertThat(codesFor(result, "predicate", "a")).contains(LootFindingCode.REFERENCE_CYCLE);
    assertThat(codesFor(result, "predicate", "b")).contains(LootFindingCode.REFERENCE_CYCLE);
    assertThat(codesFor(result, "predicate", "fine")).isEmpty();
  }

  @Test
  void provisionalTheDiagnosticOfACycleNamesAnElementOfTheCycle() {
    // spec: LT-R76 (the cycle diagnostic identifies the element concerned: the identifier of an
    // element that takes part in the cycle; the full chain is not promised)
    Map<String, String> files = base();
    files.put("predicate/a", "{'type':'minecraft:all_of','terms':['minecraft:b']}");
    files.put("predicate/b", "{'type':'minecraft:any_of','terms':['minecraft:a']}");
    LootLoadResult result = load(files);
    List<String> messages =
        result.findings().stream()
            .filter(f -> f.finding().code() == LootFindingCode.REFERENCE_CYCLE)
            .map(f -> f.finding().message())
            .toList();
    assertThat(messages).isNotEmpty();
    assertThat(messages)
        .allSatisfy(message -> assertThat(message).containsAnyOf("minecraft:a", "minecraft:b"));
  }

  @Test
  void provisionalTheDiagnosticOfATableCycleNamesATableOfTheCycle() {
    // spec: LT-R76 (loot_table cycle, same rule)
    Map<String, String> files = base();
    files.put(
        "loot_table/blocks/ta",
        "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table',"
            + "'value':'minecraft:blocks/tb'}]}]}");
    files.put(
        "loot_table/blocks/tb",
        "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table',"
            + "'value':'minecraft:blocks/ta'}]}]}");
    LootLoadResult result = load(files);
    List<String> messages =
        result.findings().stream()
            .filter(f -> f.finding().code() == LootFindingCode.REFERENCE_CYCLE)
            .map(f -> f.finding().message())
            .toList();
    assertThat(messages).isNotEmpty();
    assertThat(messages)
        .allSatisfy(
            message ->
                assertThat(message).containsAnyOf("minecraft:blocks/ta", "minecraft:blocks/tb"));
  }
}

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

// spec: LT-R1 (loot_table registry), LT-R23 (self reference), LT-R50 (named providers), LT-R61a/b
// (named predicates and unresolved references), EN-R1, EN-R34 (enchantments and providers)
class LootDataLoaderTest {

  private static final String STONE =
      "{'type':'minecraft:block','random_sequence':'minecraft:blocks/stone','pools':[{'rolls':1,"
          + "'entries':[{'type':'minecraft:item','name':'minecraft:cobblestone'}]}]}";

  private static final Identifier LOOT_TABLE = Identifier.minecraft("loot_table");
  private static final Identifier ENCHANTMENT = Identifier.minecraft("enchantment");

  private static Map<String, String> baseFiles() {
    Map<String, String> files = new LinkedHashMap<>(MiniDatapack.enchantmentFiles());
    files.put(
        "predicate/tool/can_shear",
        "{'type':'minecraft:match_tool','predicate':{'items':'minecraft:shears'}}");
    files.put(
        "predicate/tool/can_silk_touch",
        "{'type':'minecraft:match_tool','predicate':{'predicates':{'minecraft:enchantments':"
            + "[{'enchantments':'minecraft:silk_touch','levels':{'min':1}}]}}}");
    files.put(
        "enchantment_provider/test_single",
        "{'type':'minecraft:single','enchantment':'minecraft:silk_touch','level':1}");
    files.put("context_int_provider/cooking/time_coal", "1600");
    files.put("context_float_provider/cooking/speed_default", "1.0");
    files.put("loot_table/blocks/stone", STONE);
    return files;
  }

  private static LootLoadResult load(Map<String, String> files) {
    LoadedData loaded = MiniDatapack.load(files);
    return LootDataLoader.load(loaded, LootItems.catalog(), RecipeSet.of(new LinkedHashMap<>()));
  }

  private static List<LootFindingCode> codesFor(LootLoadResult result, Identifier element) {
    return result.findings().stream()
        .filter(f -> f.element().equals(element))
        .map(f -> f.finding().code())
        .toList();
  }

  @Test
  void validElementsOfEveryRegistryLoadWithoutFindings() {
    // spec: LT-R1, LT-R61a, LT-R50, EN-R1, EN-R34
    LootLoadResult result = load(baseFiles());
    assertThat(result.findings()).isEmpty();
    LootData data = result.data();
    assertThat(data.tables().find(Identifier.parse("minecraft:blocks/stone"))).isPresent();
    assertThat(data.predicates().find(Identifier.parse("minecraft:tool/can_shear"))).isPresent();
    assertThat(data.predicates().find(Identifier.parse("minecraft:tool/can_silk_touch")))
        .isPresent();
    assertThat(data.numbers().intProvider(Identifier.parse("minecraft:cooking/time_coal")))
        .isPresent();
    assertThat(data.numbers().floatProvider(Identifier.parse("minecraft:cooking/speed_default")))
        .isPresent();
    assertThat(data.enchantments().find(Identifier.minecraft("sharpness"))).isPresent();
    assertThat(data.enchantments().definitions()).hasSize(MiniDatapack.enchantmentFiles().size());
    assertThat(data.enchantments().provider(Identifier.minecraft("test_single"))).isPresent();
  }

  @Test
  void theTablesAreInIdentifierOrder() {
    // spec: LootTables contract (in identifier order)
    Map<String, String> files = baseFiles();
    files.put("loot_table/blocks/dirt", STONE);
    files.put("loot_table/blocks/apple", STONE);
    LootLoadResult result = load(files);
    assertThat(result.data().tables().tables().keySet())
        .extracting(Identifier::path)
        .containsExactly("blocks/apple", "blocks/dirt", "blocks/stone");
  }

  @Test
  void provisionalATableWithAnUnknownNamedPredicateIsAbsentWithAnUnresolvedReference() {
    // spec: LT-R61b
    Map<String, String> files = baseFiles();
    files.put(
        "loot_table/blocks/bad",
        "{'type':'minecraft:block','pools':[{'rolls':1,'condition':'minecraft:tool/no_such',"
            + "'entries':[]}]}");
    LootLoadResult result = load(files);
    assertThat(result.data().tables().find(Identifier.parse("minecraft:blocks/bad"))).isEmpty();
    assertThat(codesFor(result, Identifier.parse("minecraft:blocks/bad")))
        .contains(LootFindingCode.UNRESOLVED_REFERENCE);
    assertThat(result.data().tables().find(Identifier.parse("minecraft:blocks/stone"))).isPresent();
  }

  @Test
  void provisionalATableThatReferencesItselfIsAbsentWithASelfReference() {
    // spec: LT-R23
    Map<String, String> files = baseFiles();
    files.put(
        "loot_table/blocks/loop",
        "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table',"
            + "'value':'minecraft:blocks/loop'}]}]}");
    LootLoadResult result = load(files);
    assertThat(result.data().tables().find(Identifier.parse("minecraft:blocks/loop"))).isEmpty();
    assertThat(codesFor(result, Identifier.parse("minecraft:blocks/loop")))
        .contains(LootFindingCode.SELF_REFERENCE);
  }

  @Test
  void aTableThatNamesAMissingTableIsAbsentWithAnUnresolvedReference() {
    // spec: LootTableCodec#checkReferences contract
    Map<String, String> files = baseFiles();
    files.put(
        "loot_table/chests/nested",
        "{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table',"
            + "'value':'minecraft:blocks/nowhere'}]}]}");
    LootLoadResult result = load(files);
    assertThat(result.data().tables().find(Identifier.parse("minecraft:chests/nested"))).isEmpty();
    assertThat(codesFor(result, Identifier.parse("minecraft:chests/nested")))
        .contains(LootFindingCode.UNRESOLVED_REFERENCE);
  }

  @Test
  void aTableThatNamesALoadedTableLoads() {
    // spec: LT-R23 (nested tables by identifier)
    Map<String, String> files = baseFiles();
    files.put(
        "loot_table/chests/nested",
        "{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table',"
            + "'value':'minecraft:blocks/stone'}]}]}");
    LootLoadResult result = load(files);
    assertThat(result.data().tables().find(Identifier.parse("minecraft:chests/nested")))
        .isPresent();
    assertThat(result.findings()).isEmpty();
  }

  @Test
  void loadingNeverStopsAtTheFirstProblem() {
    // spec: LootLoadResult contract (an element that fails is absent, the others load)
    Map<String, String> files = baseFiles();
    files.put(
        "loot_table/blocks/bad_type",
        "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:nope'}]}]}");
    files.put("loot_table/blocks/dirt", STONE);
    files.put("enchantment/broken", "{'max_level':1}");
    LootLoadResult result = load(files);
    assertThat(result.data().tables().find(Identifier.parse("minecraft:blocks/bad_type")))
        .isEmpty();
    assertThat(result.data().tables().find(Identifier.parse("minecraft:blocks/dirt"))).isPresent();
    assertThat(result.data().enchantments().find(Identifier.minecraft("broken"))).isEmpty();
    assertThat(result.data().enchantments().find(Identifier.minecraft("sharpness"))).isPresent();
    assertThat(codesFor(result, Identifier.parse("minecraft:blocks/bad_type")))
        .contains(LootFindingCode.UNKNOWN_TYPE);
    assertThat(codesFor(result, Identifier.minecraft("broken")))
        .contains(LootFindingCode.MISSING_FIELD);
  }

  @Test
  void theFindingsNameTheRegistryAndTheElement() {
    // spec: LootLoadFinding contract
    Map<String, String> files = baseFiles();
    files.put(
        "loot_table/blocks/bad_type",
        "{'type':'minecraft:block','pools':[{'rolls':1,"
            + "'entries':[{'type':'minecraft:nope'}]}]}");
    files.put("enchantment/broken", "{'max_level':1}");
    LootLoadResult result = load(files);
    assertThat(result.findings())
        .anySatisfy(
            f -> {
              assertThat(f.registry()).isEqualTo(LOOT_TABLE);
              assertThat(f.element()).isEqualTo(Identifier.parse("minecraft:blocks/bad_type"));
              assertThat(f.finding().path()).startsWith("$");
            })
        .anySatisfy(
            f -> {
              assertThat(f.registry()).isEqualTo(ENCHANTMENT);
              assertThat(f.element()).isEqualTo(Identifier.minecraft("broken"));
            });
  }

  @Test
  void theFindingsFollowTheRegistryOrderThenTheIdentifierOrder() {
    // spec: LootLoadResult contract (providers and predicates, enchantments, then tables)
    Map<String, String> files = baseFiles();
    String bad =
        "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:nope'}]}]}";
    files.put("loot_table/blocks/b_bad", bad);
    files.put("loot_table/blocks/a_bad", bad);
    files.put("enchantment/broken", "{'max_level':1}");
    LootLoadResult result = load(files);
    List<Identifier> elements = result.findings().stream().map(LootLoadFinding::element).toList();
    int enchantment = elements.indexOf(Identifier.minecraft("broken"));
    int first = elements.indexOf(Identifier.parse("minecraft:blocks/a_bad"));
    int second = elements.indexOf(Identifier.parse("minecraft:blocks/b_bad"));
    assertThat(enchantment).isNotNegative().isLessThan(first);
    assertThat(first).isLessThan(second);
  }

  @Test
  void provisionalAModifierWithoutBehaviorLoadsWithAWarning() {
    // spec: LT-R47 (the table loads; NOT_IMPLEMENTED_MODIFIER)
    Map<String, String> files = baseFiles();
    files.put(
        "loot_table/blocks/lore",
        "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item',"
            + "'name':'minecraft:stone','modifier':{'type':'minecraft:set_lore','lore':[]}}]}]}");
    LootLoadResult result = load(files);
    assertThat(result.data().tables().find(Identifier.parse("minecraft:blocks/lore"))).isPresent();
    assertThat(codesFor(result, Identifier.parse("minecraft:blocks/lore")))
        .contains(LootFindingCode.NOT_IMPLEMENTED_MODIFIER);
  }

  @Test
  void anEmptyPackLoadsEmptyData() {
    // spec: LootDataLoader contract
    LootLoadResult result = load(new LinkedHashMap<>());
    assertThat(result.data().tables().tables()).isEmpty();
    assertThat(result.data().predicates().predicates()).isEmpty();
    assertThat(result.data().enchantments().definitions()).isEmpty();
    assertThat(result.findings()).isEmpty();
  }
}

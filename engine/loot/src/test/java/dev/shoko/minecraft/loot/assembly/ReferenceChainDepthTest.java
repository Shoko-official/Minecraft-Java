package dev.shoko.minecraft.loot.assembly;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.eval.LootEvaluator;
import dev.shoko.minecraft.loot.eval.LootResult;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.MiniDatapack;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: LT-R74d (a chain of named references, predicates, integer or float providers, or a mix of
// the families, is limited to 64 links like the evaluation; a longer chain is refused AT LOAD with
// a
// depth diagnostic on the table that carries it: not reported only at evaluation, not passed over
// in silence as a false condition, not signalled by an untyped exception), LT-R77 (the other tables
// load), LT-R76 (a long chain without a cycle is not a cycle).
@Timeout(60)
class ReferenceChainDepthTest {

  private static final Identifier TABLE = Identifier.minecraft("blocks/uses");
  private static final Identifier LOOT_TABLE = Identifier.minecraft("loot_table");

  private static LootLoadResult load(Map<String, String> files) {
    Map<String, String> all = new LinkedHashMap<>(MiniDatapack.enchantmentFiles());
    all.putAll(files);
    all.put(
        "loot_table/blocks/other",
        "{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item',"
            + "'name':'minecraft:stone'}]}]}");
    return LootDataLoader.load(
        MiniDatapack.load(all), LootItems.catalog(), RecipeSet.of(new LinkedHashMap<>()));
  }

  private static LootResult draw(LootLoadResult loaded, Identifier id) {
    LootContext context =
        LootContext.builder(loaded.data(), Ctx.ANY, Lcg48Random.ofSeed(3)).build();
    return LootEvaluator.evaluate(id, context);
  }

  private static String useOf(String poolFields) {
    return "{'type':'minecraft:block','pools':[{"
        + poolFields
        + ",'entries':[{'type':'minecraft:item','name':'minecraft:stone'}]}]}";
  }

  private static Map<String, String> predicateChain(int length) {
    Map<String, String> files = new LinkedHashMap<>();
    for (int i = 0; i < length - 1; i++) {
      files.put(
          "predicate/p" + i, "{'type':'minecraft:all_of','terms':['minecraft:p" + (i + 1) + "']}");
    }
    files.put("predicate/p" + (length - 1), "{'type':'minecraft:all_of','terms':[]}");
    files.put("loot_table/" + TABLE.path(), useOf("'rolls':1,'condition':['minecraft:p0']"));
    return files;
  }

  private static Map<String, String> providerChain(int length) {
    Map<String, String> files = new LinkedHashMap<>();
    for (int i = 0; i < length - 1; i++) {
      files.put("context_int_provider/c" + i, "'minecraft:c" + (i + 1) + "'");
    }
    files.put("context_int_provider/c" + (length - 1), "2");
    files.put("loot_table/" + TABLE.path(), useOf("'rolls':'minecraft:c0'"));
    return files;
  }

  private static Map<String, String> floatProviderChain(int length) {
    Map<String, String> files = new LinkedHashMap<>();
    for (int i = 0; i < length - 1; i++) {
      files.put("context_float_provider/f" + i, "'minecraft:f" + (i + 1) + "'");
    }
    files.put("context_float_provider/f" + (length - 1), "1.0");
    files.put("loot_table/" + TABLE.path(), useOf("'rolls':1,'bonus_rolls':'minecraft:f0'"));
    return files;
  }

  /** Even links are predicates, odd links are integer providers; the last one is a constant. */
  private static Map<String, String> mixedChain(int length) {
    Map<String, String> files = new LinkedHashMap<>();
    for (int i = 0; i < length; i++) {
      boolean predicate = i % 2 == 0;
      boolean last = i == length - 1;
      String next = "minecraft:" + (predicate ? "c" : "p") + (i + 1);
      if (predicate) {
        files.put(
            "predicate/p" + i,
            last
                ? "{'type':'minecraft:all_of','terms':[]}"
                : "{'type':'minecraft:int_value_check','value':'"
                    + next
                    + "','test':{'min':1,'max':1}}");
      } else {
        files.put(
            "context_int_provider/c" + i,
            last
                ? "1"
                : "{'type':'minecraft:conditional','condition':'"
                    + next
                    + "','on_true':1,'on_false':0}");
      }
    }
    files.put("loot_table/" + TABLE.path(), useOf("'rolls':1,'condition':['minecraft:p0']"));
    return files;
  }

  private static void assertAccepted(Map<String, String> files, int expectedStacks) {
    LootLoadResult loaded = load(files);
    assertThat(loaded.data().tables().find(TABLE)).isPresent();
    assertThat(errorsOf(loaded, TABLE)).isEmpty();
    assertThat(draw(loaded, TABLE).stacks()).hasSize(expectedStacks);
  }

  private static void assertRefusedAtLoad(Map<String, String> files) {
    LootLoadResult loaded = load(files);
    assertThat(loaded.data().tables().find(TABLE)).as("the table with the chain").isEmpty();
    assertThat(errorsOf(loaded, TABLE)).as("a diagnostic on the table").isNotEmpty();
    assertThat(errorsOf(loaded, TABLE))
        .doesNotContain(LootFindingCode.REFERENCE_CYCLE, LootFindingCode.SELF_REFERENCE);
    assertThat(loaded.data().tables().find(Identifier.minecraft("blocks/other")))
        .as("the other tables load")
        .isPresent();
    LootResult result = draw(loaded, TABLE);
    assertThat(result.stacks()).isEmpty();
    assertThat(result.findings())
        .extracting(f -> f.code())
        .contains(LootFindingCode.TABLE_NOT_FOUND);
  }

  private static List<LootFindingCode> errorsOf(LootLoadResult loaded, Identifier table) {
    return loaded.findings().stream()
        .filter(f -> f.registry().equals(LOOT_TABLE) && f.element().equals(table))
        .map(f -> f.finding().code())
        .filter(code -> code.severity() == Severity.ERROR)
        .toList();
  }

  @Test
  void provisionalAChainOfSixtyFourNamedProvidersIsAcceptedAndEvaluated() {
    // spec: LT-R74d (64 links are accepted): rolls 2 comes out of c0 to c63
    assertAccepted(providerChain(64), 2);
  }

  @Test
  void provisionalAChainOfSixtyFiveNamedProvidersIsRefusedAtLoad() {
    // spec: LT-R74d (the 65th link is refused, at load)
    assertRefusedAtLoad(providerChain(65));
  }

  @Test
  void provisionalAChainOfAHundredNamedProvidersIsRefusedAtLoad() {
    // spec: LT-R74d
    assertRefusedAtLoad(providerChain(100));
  }

  @Test
  void provisionalAChainOfSixtyFourNamedFloatProvidersIsAcceptedAndEvaluated() {
    // spec: LT-R74d
    assertAccepted(floatProviderChain(64), 1);
  }

  @Test
  void provisionalAChainOfAHundredNamedFloatProvidersIsRefusedAtLoad() {
    // spec: LT-R74d (the float family is counted too)
    assertRefusedAtLoad(floatProviderChain(100));
    assertRefusedAtLoad(floatProviderChain(65));
  }

  @Test
  void provisionalAChainOfSixtyFourNamedPredicatesIsAcceptedAndEvaluated() {
    // spec: LT-R74d: the true condition at the end of the chain keeps the pool
    assertAccepted(predicateChain(64), 1);
  }

  @Test
  void provisionalAChainOfSixtyFiveNamedPredicatesIsRefusedAtLoad() {
    // spec: LT-R74d (no silent false condition at evaluation)
    assertRefusedAtLoad(predicateChain(65));
  }

  @Test
  void provisionalAChainOfAHundredNamedPredicatesIsRefusedAtLoad() {
    // spec: LT-R74d
    assertRefusedAtLoad(predicateChain(100));
  }

  @Test
  void provisionalAMixedChainOfSixtyFourLinksIsAcceptedAndEvaluated() {
    // spec: LT-R74d (chains that pass from one family to the other): p0, c1, p2 ... c63
    assertAccepted(mixedChain(64), 1);
  }

  @Test
  void provisionalAMixedChainOfSixtyFiveLinksIsRefusedAtLoad() {
    // spec: LT-R74d
    assertRefusedAtLoad(mixedChain(65));
  }

  @Test
  void provisionalAMixedChainOfAHundredLinksIsRefusedAtLoad() {
    // spec: LT-R74d
    assertRefusedAtLoad(mixedChain(100));
  }
}

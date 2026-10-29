package dev.shoko.minecraft.loot.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootBudgetException;
import dev.shoko.minecraft.loot.assembly.LootLoadResult;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.NamedDags;
import dev.shoko.minecraft.loot.testing.Tables;
import dev.shoko.minecraft.loot.testing.WatchedRandom;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: LT-R74f (the budget counts every evaluated node: conditions, number providers, also the
// nested ones and the ones reached through named references, pool draws, weighted selections and
// modifiers; provisional cap of 1000000 nodes per root draw), LT-R74g (a typed error), LT-R74e (the
// check happens while computing). A DAG of named references where p_i is any_of of two references
// to p_(i+1) makes 2^n evaluations for n levels. The leaves draw a random number so that the work
// is observable: WatchedRandom ends the test with an Error when the code under test does not stop.
@Timeout(120)
class NodeBudgetTest {

  private static final long WORK_LIMIT = 40_000_000L;
  private static final String TABLE = "blocks/uses";

  private static LootContext context(LootLoadResult loaded, WatchedRandom random) {
    return LootContext.builder(loaded.data(), Ctx.ANY, random).build();
  }

  private static WatchedRandom watched() {
    return WatchedRandom.limited(Lcg48Random.ofSeed(1), WORK_LIMIT);
  }

  private static String tableWith(String poolFields) {
    return "{'type':'minecraft:block','pools':[{"
        + poolFields
        + ",'entries':[{'type':'minecraft:item','name':'minecraft:stone'}]}]}";
  }

  private static LootResult draw(LootLoadResult loaded, WatchedRandom random) {
    return LootEvaluator.evaluate(Identifier.minecraft(TABLE), context(loaded, random));
  }

  @Test
  void provisionalANamedPredicateDagOfFortyLevelsEndsInABudgetError() {
    // spec: LT-R74f (2^40 evaluations of the leaf without the budget), LT-R74g
    Map<String, String> files = NamedDags.predicateDag(40, "any_of", NamedDags.FALSE_LEAF, "p");
    files.put("loot_table/" + TABLE, tableWith("'rolls':1,'condition':['minecraft:p0']"));
    LootLoadResult loaded = NamedDags.load(files);
    assertThat(loaded.data().tables().find(Identifier.minecraft(TABLE))).isPresent();
    assertThatThrownBy(() -> draw(loaded, watched())).isInstanceOf(LootBudgetException.class);
  }

  @Test
  void provisionalANamedIntegerProviderDagOfFortyLevelsEndsInABudgetError() {
    // spec: LT-R74f (providers reached by named references are nodes), LT-R74g
    Map<String, String> files =
        NamedDags.providerDag(
            40, "max", "{'type':'minecraft:uniform','min':0,'max':1}", "context_int_provider", "q");
    files.put("loot_table/" + TABLE, tableWith("'rolls':'minecraft:q0'"));
    LootLoadResult loaded = NamedDags.load(files);
    assertThat(loaded.data().tables().find(Identifier.minecraft(TABLE))).isPresent();
    assertThatThrownBy(() -> draw(loaded, watched())).isInstanceOf(LootBudgetException.class);
  }

  @Test
  void provisionalANamedFloatProviderDagOfFortyLevelsEndsInABudgetError() {
    // spec: LT-R74f, LT-R74g (the float family has its own sampler)
    Map<String, String> files =
        NamedDags.providerDag(
            40,
            "avg",
            "{'type':'minecraft:uniform','min':0.0,'max':1.0}",
            "context_float_provider",
            "f");
    files.put("loot_table/" + TABLE, tableWith("'rolls':1,'bonus_rolls':'minecraft:f0'"));
    LootLoadResult loaded = NamedDags.load(files);
    assertThat(loaded.data().tables().find(Identifier.minecraft(TABLE))).isPresent();
    assertThatThrownBy(() -> draw(loaded, watched())).isInstanceOf(LootBudgetException.class);
  }

  @Test
  void provisionalAWideDagUnderTheBudgetIsEvaluatedWithoutError() {
    // spec: LT-R74f (about 2^16 nodes are far under the 1000000 of the budget): all_of of true
    // leaves keeps the pool, so that the result proves that the condition was fully evaluated
    Map<String, String> files = NamedDags.predicateDag(15, "all_of", NamedDags.TRUE_LEAF, "w");
    files.put("loot_table/" + TABLE, tableWith("'rolls':1,'condition':['minecraft:w0']"));
    LootLoadResult loaded = NamedDags.load(files);
    assertThat(draw(loaded, watched()).stacks()).hasSize(1);
  }

  @Test
  void provisionalAWideIntegerProviderDagUnderTheBudgetIsEvaluatedWithoutError() {
    // spec: LT-R74f
    Map<String, String> files =
        NamedDags.providerDag(
            14, "max", "{'type':'minecraft:uniform','min':1,'max':1}", "context_int_provider", "v");
    files.put("loot_table/" + TABLE, tableWith("'rolls':'minecraft:v0'"));
    LootLoadResult loaded = NamedDags.load(files);
    assertThat(draw(loaded, watched()).stacks()).hasSize(1);
  }

  @Test
  void provisionalNestedNodesCountEvenWhenTheTopLevelDecisionsAreFew() {
    // spec: LT-R74f (nested conditions are nodes): one pool, one condition at the top, about 2^21
    // nodes below it; the leaves draw 2^20 numbers, far under the limit of the watch
    Map<String, String> files = NamedDags.predicateDag(20, "any_of", NamedDags.FALSE_LEAF, "n");
    files.put("loot_table/" + TABLE, tableWith("'rolls':1,'condition':['minecraft:n0']"));
    LootLoadResult loaded = NamedDags.load(files);
    assertThat(loaded.data().tables().find(Identifier.minecraft(TABLE))).isPresent();
    assertThatThrownBy(() -> draw(loaded, watched())).isInstanceOf(LootBudgetException.class);
  }

  @Test
  void provisionalTheBudgetLimitIsOneMillionNodesAndNotOneMoreOrLess() {
    // spec: LT-R74f (provisional cap of 1000000 nodes per root draw, each constant condition is a
    // node): the pool, its rolls and its selection add a handful of nodes, so the largest number of
    // conditions that passes is within a few units of the cap, and one condition more fails
    assertThat(fails(2_000_000)).isTrue();
    assertThat(fails(0)).isFalse();
    int low = 0;
    int high = 2_000_000;
    while (high - low > 1) {
      int middle = (low + high) >>> 1;
      if (fails(middle)) {
        high = middle;
      } else {
        low = middle;
      }
    }
    assertThat(fails(low)).isFalse();
    assertThat(fails(low + 1)).isTrue();
    // 5 nodes besides the conditions: the pool, its rolls, its bonus_rolls, the weighted
    // selection and the table (accounting of the code at the time of writing, provisional)
    assertThat(low).isEqualTo(1_000_000 - 5);
  }

  private static boolean fails(int conditions) {
    LootCondition constant = new LootCondition.AllOf(List.of());
    LootTable table =
        Tables.table(
            Tables.poolWith(
                Collections.nCopies(conditions, constant),
                new IntProvider.Constant(1),
                0f,
                List.of(),
                Tables.item("stone")));
    try {
      LootEvaluator.evaluate(table, Ctx.context(Lcg48Random.ofSeed(1)));
      return false;
    } catch (LootBudgetException e) {
      return true;
    }
  }
}

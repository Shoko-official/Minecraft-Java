package dev.shoko.minecraft.loot.eval;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.random.DrawEvent;
import dev.shoko.minecraft.loot.random.DrawReason;
import dev.shoko.minecraft.loot.table.LootPool;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.DrawLog;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.SequenceStore;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: LT-R66 (order of consumption, hypothesis H-draw), LT-R26 (single candidate, H-single),
// LT-R63 to LT-R65 (source of a top-level draw, nested tables), LT-R67 (determinism)
class LootEvaluatorDrawTest {

  private static List<String> signature(LootResult result) {
    List<String> items = new ArrayList<>();
    for (ItemStack stack : result.stacks()) {
      items.add(stack.count() + "x" + stack.type().id().path());
    }
    return items;
  }

  private static LootTable lottery() {
    return Tables.table(
        Tables.pool(
            Tables.uniform(2, 6),
            Tables.item("stone", 3),
            Tables.item("dirt", 2),
            Tables.item("stick", 1),
            Tables.empty(1)));
  }

  @Test
  void provisionalTheDrawsFollowTheOrderOfTheAlgorithm() {
    // spec: LT-R66
    LootPool pool =
        Tables.poolWith(
            Tables.conditions(Tables.chance(0.5f)),
            Tables.uniform(1, 3),
            0f,
            List.of(Tables.uniformCount(1, 3)),
            Tables.when(Tables.item("stone"), Tables.chance(0.5f)),
            Tables.item("dirt"));
    DrawLog log = new DrawLog();
    ScriptedRandom random = ScriptedRandom.repeating(0.1, 16);
    LootContext context = Ctx.builder(random).observer(log).build();
    LootEvaluator.evaluate(Tables.table(pool), context);
    assertThat(log.consumingReasons())
        .containsExactly(
            DrawReason.POOL_CONDITION,
            DrawReason.POOL_ROLLS,
            DrawReason.ENTRY_CONDITION,
            DrawReason.ENTRY_SELECTION,
            DrawReason.MODIFIER);
  }

  @Test
  void provisionalEveryEventCarriesTheStepsBeforeAndAfterTheDecision() {
    // spec: DrawEvent contract (stepsBefore <= stepsAfter, consecutive decisions chain)
    DrawLog log = new DrawLog();
    ScriptedRandom random = ScriptedRandom.repeating(0.3, 40);
    LootContext context = Ctx.builder(random).observer(log).build();
    LootEvaluator.evaluate(lottery(), context);
    List<DrawEvent> events = log.events();
    assertThat(events).isNotEmpty();
    long previousAfter = 0;
    for (DrawEvent event : events) {
      assertThat(event.stepsBefore()).isGreaterThanOrEqualTo(previousAfter);
      assertThat(event.stepsAfter()).isGreaterThanOrEqualTo(event.stepsBefore());
      previousAfter = event.stepsAfter();
    }
    assertThat(log.totalSteps()).isEqualTo(random.steps());
  }

  @Test
  void provisionalASingleCandidateConsumesNoStepButTheDecisionIsReported() {
    // spec: LT-R26 (hypothese H-single), DrawEvent contract
    DrawLog log = new DrawLog();
    ScriptedRandom random = ScriptedRandom.of();
    LootContext context = Ctx.builder(random).observer(log).build();
    LootEvaluator.evaluate(Tables.table(Tables.pool(1, Tables.item("stone"))), context);
    assertThat(random.steps()).isZero();
    assertThat(log.reasons()).contains(DrawReason.ENTRY_SELECTION);
    assertThat(log.totalSteps()).isZero();
  }

  @Test
  void provisionalNoCandidateConsumesNothing() {
    // spec: LT-R26
    ScriptedRandom random = ScriptedRandom.of();
    LootContext context = Ctx.builder(random).build();
    LootResult result =
        LootEvaluator.evaluate(Tables.table(Tables.pool(2, Tables.empty(0))), context);
    assertThat(result.stacks()).isEmpty();
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalASkippedPoolReportsNoDrawOfItsOwn() {
    // spec: LT-R19a
    DrawLog log = new DrawLog();
    ScriptedRandom random = ScriptedRandom.of(0.9);
    LootPool skipped =
        Tables.poolWith(
            Tables.conditions(Tables.chance(0.5f)),
            new IntProvider.Constant(1),
            0f,
            List.of(),
            Tables.item("stone"));
    LootEvaluator.evaluate(Tables.table(skipped), Ctx.builder(random).observer(log).build());
    assertThat(log.consumingReasons()).containsExactly(DrawReason.POOL_CONDITION);
    assertThat(random.steps()).isEqualTo(1);
  }

  @Test
  void theSameSourceStateGivesTheSameResult() {
    // spec: LT-R67, determinism of the module (package-info)
    LootResult first = LootEvaluator.evaluate(lottery(), Ctx.context(Lcg48Random.ofSeed(2024)));
    LootResult second = LootEvaluator.evaluate(lottery(), Ctx.context(Lcg48Random.ofSeed(2024)));
    assertThat(signature(first)).isEqualTo(signature(second));
    assertThat(first.stacks()).isEqualTo(second.stacks());
  }

  @Test
  void differentSeedsEventuallyGiveDifferentResults() {
    // spec: LT-R67 (deux appels consecutifs different)
    List<List<String>> seen = new ArrayList<>();
    for (long seed = 1; seed <= 12; seed++) {
      seen.add(signature(LootEvaluator.evaluate(lottery(), Ctx.context(Lcg48Random.ofSeed(seed)))));
    }
    assertThat(seen.stream().distinct().count()).isGreaterThan(3);
  }

  @Test
  void provisionalAnExplicitSeedMakesTheResultIndependentOfTheLevelSource() {
    // spec: LT-R63 (1), LT-R68
    LootResult first =
        LootEvaluator.evaluateTopLevel(
            lottery(), Ctx.context(Lcg48Random.ofSeed(1)), 987654321L, new SequenceStore(1));
    LootResult second =
        LootEvaluator.evaluateTopLevel(
            lottery(), Ctx.context(Lcg48Random.ofSeed(777)), 987654321L, new SequenceStore(2));
    assertThat(signature(first)).isEqualTo(signature(second));
  }

  @Test
  void provisionalAnExplicitSeedDoesNotTouchTheLevelNorTheSequences() {
    // spec: LT-R63 (1)
    RandomSource level = Lcg48Random.ofSeed(1);
    SequenceStore store = new SequenceStore(1);
    LootTable named = Tables.named("blocks/lottery", lottery().pools().getFirst());
    LootEvaluator.evaluateTopLevel(named, Ctx.context(level), 42L, store);
    assertThat(level.steps()).isZero();
    assertThat(store.requested()).isEmpty();
  }

  @Test
  void provisionalSeedZeroWithANamedSequenceAdvancesThePersistentSequence() {
    // spec: LT-R63 (2), LT-R65
    RandomSource level = Lcg48Random.ofSeed(1);
    SequenceStore store = new SequenceStore(1);
    LootTable named = Tables.named("blocks/lottery", lottery().pools().getFirst());
    Identifier sequence = Identifier.parse("minecraft:blocks/lottery");
    LootEvaluator.evaluateTopLevel(named, Ctx.context(level), 0L, store);
    long afterFirst = store.peek(sequence).steps();
    assertThat(afterFirst).isPositive();
    assertThat(level.steps()).isZero();
    LootEvaluator.evaluateTopLevel(named, Ctx.context(level), 0L, store);
    assertThat(store.peek(sequence).steps()).isGreaterThan(afterFirst);
  }

  @Test
  void provisionalSeedZeroWithoutSequenceConsumesTheSourceOfTheLevel() {
    // spec: LT-R63 (3)
    RandomSource level = Lcg48Random.ofSeed(1);
    SequenceStore store = new SequenceStore(1);
    LootEvaluator.evaluateTopLevel(lottery(), Ctx.context(level), 0L, store);
    assertThat(level.steps()).isPositive();
    assertThat(store.requested()).isEmpty();
  }

  @Test
  void provisionalANestedTableDoesNotOpenItsOwnSequence() {
    // spec: LT-R23, LT-R64
    LootTable inner =
        Tables.named("gameplay/inner", Tables.pool(1, Tables.item("stone"), Tables.item("dirt")));
    LootTable outer = Tables.named("blocks/outer", Tables.pool(1, Tables.inline(inner)));
    SequenceStore store = new SequenceStore(1);
    LootEvaluator.evaluateTopLevel(outer, Ctx.context(Lcg48Random.ofSeed(1)), 0L, store);
    assertThat(store.requested()).containsExactly(Identifier.parse("minecraft:blocks/outer"));
  }

  @Test
  void provisionalTwoTablesSharingASequenceShareItsState() {
    // spec: LT-R65
    LootTable first = Tables.named("shared/seq", lottery().pools().getFirst());
    LootTable second = Tables.named("shared/seq", lottery().pools().getFirst());
    SequenceStore store = new SequenceStore(5);
    Identifier sequence = Identifier.parse("minecraft:shared/seq");
    LootEvaluator.evaluateTopLevel(first, Ctx.context(Lcg48Random.ofSeed(1)), 0L, store);
    long afterFirst = store.peek(sequence).steps();
    LootEvaluator.evaluateTopLevel(second, Ctx.context(Lcg48Random.ofSeed(1)), 0L, store);
    assertThat(store.peek(sequence).steps()).isGreaterThan(afterFirst);
    assertThat(store.requested()).containsOnly(sequence);
  }

  @Test
  void provisionalItemsOfTheTableAreMadeOfCatalogItems() {
    // spec: LT-R56
    LootResult result = LootEvaluator.evaluate(lottery(), Ctx.context(Lcg48Random.ofSeed(9)));
    for (ItemStack stack : result.stacks()) {
      assertThat(LootItems.catalog().find(stack.type().id())).isPresent();
    }
  }
}

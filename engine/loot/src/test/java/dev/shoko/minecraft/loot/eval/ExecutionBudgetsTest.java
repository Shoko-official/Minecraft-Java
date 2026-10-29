package dev.shoko.minecraft.loot.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.random.DrawEvent;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.table.LootTableCodec;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: LT-R74a (4096 rolls), R74b (100000 binomial trials), R74c (effective weight capped at
// 2^20), R74d (255 nested levels), R74e (1000000 stacks), R74f (global budget), R74g (a constant
// over a cap is a load error, a cap exceeded at evaluation fails the draw, no silent truncation,
// weights are capped without error), probe PL26. The caps are design budgets (S-arch); the type of
// the budget error is not specified, only that it is a runtime exception raised by the module.
@Timeout(60)
class ExecutionBudgetsTest {

  /** Thrown by the observer when a draw keeps working far beyond any sane budget. */
  private static final class TooMuchWork extends Error {
    private static final long serialVersionUID = 1L;

    TooMuchWork(long events) {
      super("the draw made " + events + " decisions without a budget error");
    }
  }

  private static LootContext watched(long maxEvents, long seed) {
    long[] seen = {0};
    return Ctx.builder(Lcg48Random.ofSeed(seed))
        .observer(
            (DrawEvent event) -> {
              if (++seen[0] > maxEvents) {
                throw new TooMuchWork(seen[0]);
              }
            })
        .build();
  }

  private static int total(LootResult result) {
    return result.stacks().stream().mapToInt(ItemStack::count).sum();
  }

  private static LootTable singlePool(int rolls) {
    return Tables.table(Tables.pool(rolls, Tables.item("stick")));
  }

  @Test
  void provisionalAWeightIsCappedAtTwoToTheTwenty() {
    // spec: LT-R74c: weights 2^20 and 2^10 keep their ratio of 1024 to 1 (a cap of 2^8 would not)
    LootTable table =
        Tables.table(Tables.pool(1, Tables.item("stone", 1 << 20), Tables.item("dirt", 1 << 10)));
    LootContext context = Ctx.context(Lcg48Random.ofSeed(6));
    int dirt = 0;
    for (int i = 0; i < 6000; i++) {
      for (ItemStack stack : LootEvaluator.evaluate(table, context).stacks()) {
        if (stack.type().id().path().equals("dirt")) {
          dirt++;
        }
      }
    }
    assertThat(dirt).isLessThanOrEqualTo(25);
  }

  @Test
  void provisionalWeightsAboveTheCapBecomeEqualWithoutError() {
    // spec: LT-R74c, R74g (capped without error): 2^22 and 2^20 both count as 2^20
    LootTable table =
        Tables.table(Tables.pool(1, Tables.item("stone", 1 << 22), Tables.item("dirt", 1 << 20)));
    LootContext context = Ctx.context(Lcg48Random.ofSeed(7));
    int stone = 0;
    int samples = 4000;
    for (int i = 0; i < samples; i++) {
      for (ItemStack stack : LootEvaluator.evaluate(table, context).stacks()) {
        if (stack.type().id().path().equals("stone")) {
          stone++;
        }
      }
    }
    assertThat(stone / (double) samples).isBetween(0.45, 0.55);
  }

  @Test
  void provisionalASumOfCappedWeightsAboveTheIntRangeDoesNotThrow() {
    // spec: LT-R74c (the cap bounds each term, the sum must still not overflow): 2100 x 2^20
    List<LootEntry> entries = new ArrayList<>();
    for (int i = 0; i < 2100; i++) {
      entries.add(Tables.item("stick", 1 << 20));
    }
    LootTable table = Tables.table(Tables.pool(1, entries.toArray(new LootEntry[0])));
    assertThatCode(() -> LootEvaluator.evaluate(table, Ctx.context(Lcg48Random.ofSeed(3))))
        .doesNotThrowAnyException();
  }

  @Test
  void provisionalPoolsDrawEveryRollUpToTheCap() {
    // spec: LT-R74a (4096 rolls per pool)
    assertThat(total(LootEvaluator.evaluate(singlePool(100), Ctx.context(Lcg48Random.ofSeed(1)))))
        .isEqualTo(100);
    assertThat(total(LootEvaluator.evaluate(singlePool(4096), Ctx.context(Lcg48Random.ofSeed(1)))))
        .isEqualTo(4096);
  }

  @Test
  void provisionalMoreRollsThanTheCapFailTheDrawInsteadOfTruncating() {
    // spec: LT-R74a, R74g (an exceeded cap at evaluation fails the draw; no silent truncation)
    LootTable constant = singlePool(4097);
    assertThatThrownBy(() -> LootEvaluator.evaluate(constant, Ctx.context(Lcg48Random.ofSeed(1))))
        .isInstanceOf(RuntimeException.class);
    LootTable dynamic = Tables.table(Tables.pool(Tables.uniform(5000, 6000), Tables.item("stick")));
    assertThatThrownBy(() -> LootEvaluator.evaluate(dynamic, Ctx.context(Lcg48Random.ofSeed(1))))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void provisionalAConstantNumberOfRollsOverTheCapIsALoadError() {
    // spec: LT-R74g (a constant over R74a is a load error of the table)
    String over =
        "{'type':'minecraft:chest','pools':[{'rolls':4097,'entries':[{'type':'minecraft:empty'}]}]}";
    LootDecoding<LootTable> decoding =
        LootTableCodec.decode(Json.parse(over), EnchantFixtures.access(), LootItems.catalog());
    Decoded.fails(decoding);
    String ok =
        "{'type':'minecraft:chest','pools':[{'rolls':4096,'entries':[{'type':'minecraft:empty'}]}]}";
    Decoded.value(
        LootTableCodec.decode(Json.parse(ok), EnchantFixtures.access(), LootItems.catalog()));
  }

  @Test
  void provisionalABinomialAcceptsOneHundredThousandTrialsAndNoMore() {
    // spec: LT-R74b, R74g
    IntProvider atCap =
        new IntProvider.Binomial(new IntProvider.Constant(100_000), new FloatProvider.Constant(1f));
    assertThat(atCap.sample(Ctx.context(Lcg48Random.ofSeed(1)))).isEqualTo(100_000);
    IntProvider over =
        new IntProvider.Binomial(new IntProvider.Constant(100_001), new FloatProvider.Constant(1f));
    assertThatThrownBy(() -> over.sample(Ctx.context(Lcg48Random.ofSeed(1))))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void provisionalABinomialWithAConstantOverTheCapIsALoadError() {
    // spec: LT-R74b, R74g
    String json = "{'type':'minecraft:binomial','n':100001,'p':0.5}";
    Decoded.fails(
        dev.shoko.minecraft.loot.number.NumberProviderCodec.decodeInt(
            Json.parse(json), EnchantFixtures.access()));
  }

  @Test
  void provisionalAWeightOverTheCapGivesAWarningAtLoad() {
    // spec: LT-R74g (avertissement au chargement si le poids est une constante)
    String json =
        "{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[{'type':'minecraft:item',"
            + "'name':'minecraft:stone','weight':33554432}]}]}";
    LootDecoding<LootTable> decoding =
        LootTableCodec.decode(Json.parse(json), EnchantFixtures.access(), LootItems.catalog());
    Decoded.value(decoding);
    assertThat(decoding.findings()).isNotEmpty();
  }

  private static LootTable nested(int levels) {
    LootTable table = Tables.table(Tables.pool(1, Tables.item("stone")));
    for (int level = 0; level < levels; level++) {
      table = Tables.table(Tables.pool(1, Tables.inline(table)));
    }
    return table;
  }

  @Test
  void provisionalTwoHundredFiftyFiveNestedTablesStillDraw() {
    // spec: LT-R74d (255 levels of nested tables)
    assertThat(LootEvaluator.evaluate(nested(255), Ctx.context(Lcg48Random.ofSeed(1))).stacks())
        .hasSize(1);
  }

  @Test
  void provisionalTwoHundredFiftySixNestedTablesGiveNoStack() {
    // spec: LT-R74d (the 256th level is refused: the innermost stack is never reached)
    LootTable table = nested(256);
    LootContext context = Ctx.context(Lcg48Random.ofSeed(1));
    AtomicReference<List<ItemStack>> stacks = new AtomicReference<>(List.of());
    // failing the draw is the other way to refuse (R74g): then no stack was reached either
    Throwable thrown =
        catchThrowable(() -> stacks.set(LootEvaluator.evaluate(table, context).stacks()));
    assertThat(stacks.get()).isEmpty();
    if (thrown != null) {
      assertThat(thrown).isInstanceOf(RuntimeException.class);
    }
  }

  @Test
  void provisionalTwoHundredFiftySixNestedTablesFailTheDraw() {
    // spec: LT-R74d, R74g (a cap exceeded at evaluation fails the draw with an error)
    LootTable table = nested(256);
    assertThatThrownBy(() -> LootEvaluator.evaluate(table, Ctx.context(Lcg48Random.ofSeed(1))))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void provisionalTheGlobalBudgetStopsNestedPoolsWhoseProductIsHuge() {
    // spec: LT-R74f (each cap is respected but their product is not), R74g (a budget error)
    LootTable inner = singlePool(4096);
    LootTable middle = Tables.table(Tables.pool(4096, Tables.inline(inner)));
    LootTable outer = Tables.table(Tables.pool(4096, Tables.inline(middle)));
    assertThatThrownBy(() -> LootEvaluator.evaluate(outer, watched(3_000_000, 5)))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void provisionalMoreThanOneMillionStacksFailTheDraw() {
    // spec: LT-R74e (at most 1000000 stacks after the split), R74g (no partial stack list)
    LootTable table =
        Tables.table(
            Tables.pool(1, Tables.modified(Tables.item("stone"), Tables.setCount(70_000_000))));
    assertThatThrownBy(() -> LootEvaluator.evaluate(table, Ctx.context(ScriptedRandom.of())))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void provisionalTheBudgetDoesNotRefuseAnOrdinaryTable() {
    // spec: LT-R74f: the budget is far above what the game data asks for
    Map<String, Integer> counts = new HashMap<>();
    LootTable table =
        Tables.table(
            Tables.pool(5, Tables.item("stone", 3), Tables.item("dirt", 2), Tables.empty(1)),
            Tables.pool(Tables.uniform(1, 3), Tables.item("stick")));
    LootContext context = watched(1_000_000, 9);
    for (int i = 0; i < 1000; i++) {
      for (ItemStack stack : LootEvaluator.evaluate(table, context).stacks()) {
        counts.merge(stack.type().id().path(), stack.count(), Integer::sum);
      }
    }
    assertThat(counts).containsKeys("stone", "dirt", "stick");
  }
}

package dev.shoko.minecraft.loot.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.loot.LootBudgetException;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.modifier.ModifierAction;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Tables;
import dev.shoko.minecraft.loot.testing.WatchedRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: LT-R74e (the caps are checked while computing, before any allocation and before any loop
// proportional to the size of a stack, so that the work done before the error stays bounded: the
// decay of LT-R41 walks a stack unit by unit), LT-R74g (a typed error, no partial stack list).
// WatchedRandom ends the test with an Error if a draw keeps working past the limit.
@Timeout(60)
class WorkBoundsTest {

  /** Far under the 4096 x 10000000 draws of the table below, far above any sane budget check. */
  private static final long WORK_LIMIT = 400_000_000L;

  private static final long SECONDS_5 = 5_000_000_000L;

  private static LootTable decayedPiles(int rolls, int count) {
    return Tables.table(
        Tables.pool(
            rolls,
            Tables.modified(
                Tables.item("stone"),
                Tables.setCount(count),
                Tables.modifier(new ModifierAction.ExplosionDecay()))));
  }

  @Test
  void provisionalTheDecayOfHugePilesFailsTheDrawQuicklyWithABudgetError() {
    // spec: LT-R74e, LT-R41 (radius 4): 4096 rolls of a pile of 10000000 units would walk
    // 40 billion units; the error must come early and be typed
    WatchedRandom random = WatchedRandom.limited(Lcg48Random.ofSeed(5), WORK_LIMIT);
    LootContext context = Ctx.builder(random).explosionRadius(4f).build();
    long start = System.nanoTime();
    assertThatThrownBy(() -> LootEvaluator.evaluate(decayedPiles(4096, 10_000_000), context))
        .isInstanceOf(LootBudgetException.class);
    assertThat(System.nanoTime() - start).isLessThan(SECONDS_5);
  }

  @Test
  void provisionalTheDecayOfOneHugePileStillRunsUnderTheCap() {
    // spec: LT-R41 (about 1 / radius of the units are kept), LT-R74e (the cap leaves ordinary
    // piles alone): 1000 units at radius 4
    LootContext context = Ctx.builder(Lcg48Random.ofSeed(5)).explosionRadius(4f).build();
    int kept = total(LootEvaluator.evaluate(decayedPiles(1, 1000), context));
    assertThat(kept).isBetween(150, 350);
  }

  @Test
  void provisionalManyFractionedPilesAddUpToMoreThanOneMillionStacksAndFailTheDraw() {
    // spec: LT-R74e (at most 1000000 stacks after the split, counted over the whole draw), LT-R74g:
    // 4096 rolls of 100000 stones, 1563 stacks each, 6.4 million in all
    LootTable table =
        Tables.table(
            Tables.pool(4096, Tables.modified(Tables.item("stone"), Tables.setCount(100_000))));
    assertThatThrownBy(() -> LootEvaluator.evaluate(table, Ctx.context(ScriptedRandom.of())))
        .isInstanceOf(LootBudgetException.class);
  }

  private static int total(LootResult result) {
    return result.stacks().stream().mapToInt(s -> s.count()).sum();
  }
}

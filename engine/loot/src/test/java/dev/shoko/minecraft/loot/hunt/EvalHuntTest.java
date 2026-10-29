package dev.shoko.minecraft.loot.hunt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.eval.LootEvaluator;
import dev.shoko.minecraft.loot.eval.LootResult;
import dev.shoko.minecraft.loot.modifier.BonusFormula;
import dev.shoko.minecraft.loot.modifier.ModifierAction;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.number.NumberProviderCodec;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Tables;
import dev.shoko.minecraft.loot.testing.TestAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// Adversarial tests on the evaluation of tables, providers and modifiers.
@Timeout(120)
class EvalHuntTest {

  private static int total(LootResult result) {
    return result.stacks().stream().mapToInt(ItemStack::count).sum();
  }

  // spec: LT-R26: a table with many entries of large weight must not throw.
  @Test
  void provisionalASumOfEntryWeightsAboveTheIntRangeDoesNotThrow() {
    List<LootEntry> entries = new ArrayList<>();
    for (int i = 0; i < 2100; i++) {
      entries.add(Tables.item("stick", 1 << 20));
    }
    LootTable table = Tables.table(Tables.pool(1, entries.toArray(new LootEntry[0])));
    assertThatCode(() -> LootEvaluator.evaluate(table, Ctx.context(Lcg48Random.ofSeed(3))))
        .doesNotThrowAnyException();
  }

  // spec: LT-R50 (weighted_list), module contract: a decoded provider can be sampled.
  @Test
  void provisionalAWeightedListWhoseWeightsAddAboveTheIntRangeCanBeSampled() {
    var decoded =
        NumberProviderCodec.decodeInt(
            Json.parse(
                "{'type':'minecraft:weighted_list','distribution':["
                    + "{'data':1,'weight':2147483647},{'data':2,'weight':2147483647}]}"),
            TestAccess.standard());
    IntProvider provider =
        ((dev.shoko.minecraft.loot.LootDecoding.Success<IntProvider>) decoded).value();
    LootContext context = Ctx.context(Lcg48Random.ofSeed(1));
    assertThatCode(() -> provider.sample(context)).doesNotThrowAnyException();
  }

  // spec: LT-R22: an expanded tag gives one child per item "de meme poids et de meme condition".
  @Test
  void provisionalAnExpandedTagEvaluatesTheConditionOfTheEntryForEachChild() {
    // test_gems = diamond, emerald, iron_ingot. Script: [chance(child 1)=0.1 pass, 0.9 fail, 0.1
    // pass] then the weighted pick draw. Per child -> 2 candidates, one pick draw.
    LootEntry gems = Tables.when(Tables.tag("test_gems", true, 1), Tables.chance(0.5f));
    LootTable table = Tables.table(Tables.pool(1, gems));
    ScriptedRandom script = ScriptedRandom.of(0.1, 0.9, 0.1, 0.0);
    LootResult result = LootEvaluator.evaluate(table, Ctx.context(script));
    assertThat(script.remaining()).isZero();
    assertThat(result.stacks()).hasSize(1);
  }

  // spec: LT-R42 provisional formula: ore_drops = count x max(1, nextInt(level + 2)): the draw is
  // made at level 0 too (nextInt(2)); an observable shift of the following decisions otherwise.
  @Test
  void provisionalOreDropsAtLevelZeroConsumesItsDraw() {
    ModifierAction.ApplyBonus bonus =
        new ModifierAction.ApplyBonus(
            dev.shoko.minecraft.data.id.Identifier.minecraft("fortune"),
            new BonusFormula.OreDrops());
    LootTable table =
        Tables.table(Tables.pool(1, Tables.modified(Tables.item("stick"), Tables.modifier(bonus))));
    ScriptedRandom script = ScriptedRandom.of(0.0);
    LootEvaluator.evaluate(
        table, Ctx.builder(script).tool(LootItems.stack("iron_pickaxe")).build());
    assertThat(script.remaining()).isZero();
  }

  // spec: LT-R41 H-decay-b: a unit is kept with probability 1/radius, like survives_explosion
  // (R33): a pile of 1000 units at radius 4 keeps about 250.
  @Test
  void provisionalExplosionDecayKeepsAboutOneOverRadiusOfTheUnits() {
    LootTable table =
        Tables.table(
            Tables.pool(
                1,
                Tables.modified(
                    Tables.item("stick"),
                    Tables.setCount(1000),
                    Tables.modifier(new ModifierAction.ExplosionDecay()))));
    LootResult result =
        LootEvaluator.evaluate(
            table, Ctx.builder(Lcg48Random.ofSeed(12345)).explosionRadius(4f).build());
    assertThat(total(result)).isBetween(200, 300);
  }

  // spec: LT-R40 H-limit-a (min > max: raised to min, then brought down to max, so max), V-L16.
  @Test
  void provisionalLimitCountWithMinAboveMaxGivesTheMaximum() {
    LootTable table =
        Tables.table(
            Tables.pool(
                1, Tables.modified(Tables.item("stick"), Tables.setCount(3), Tables.limit(5, 2))));
    LootResult result = LootEvaluator.evaluate(table, Ctx.context(Lcg48Random.ofSeed(1)));
    assertThat(total(result)).isEqualTo(2);
  }

  // spec: LT-R63: named sequences. Two tables sharing a sequence continue the same state.
  @Test
  void provisionalTwoTablesSharingASequenceContinueTheSameState() {
    LootTable a =
        Tables.named("shared", Tables.pool(1, Tables.item("stick", 1), Tables.item("dirt", 1)));
    var store1 = new dev.shoko.minecraft.loot.testing.SequenceStore(9);
    var store2 = new dev.shoko.minecraft.loot.testing.SequenceStore(9);
    LootContext ctx = Ctx.context(Lcg48Random.ofSeed(1));
    List<String> viaA = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      viaA.add(
          LootEvaluator.evaluateTopLevel(a, ctx, 0L, store1)
              .stacks()
              .getFirst()
              .type()
              .id()
              .path());
    }
    LootTable b =
        Tables.named("shared", Tables.pool(1, Tables.item("stick", 1), Tables.item("dirt", 1)));
    List<String> alternating = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      LootTable pick = i % 2 == 0 ? a : b;
      alternating.add(
          LootEvaluator.evaluateTopLevel(pick, ctx, 0L, store2)
              .stacks()
              .getFirst()
              .type()
              .id()
              .path());
    }
    assertThat(alternating).isEqualTo(viaA);
  }

  // spec: LT-R63: seed 0 means absent; Long.MIN_VALUE is a seed; same seed same result.
  @Test
  void provisionalAnExplicitSeedGivesTheSameResultTwiceAndZeroFallsBackToTheSequence() {
    LootTable t =
        Tables.named(
            "s",
            Tables.pool(
                8, Tables.item("stick", 1), Tables.item("dirt", 1), Tables.item("stone", 1)));
    for (long seed : new long[] {Long.MIN_VALUE, Long.MAX_VALUE, -1L, 1L}) {
      var s = new dev.shoko.minecraft.loot.testing.SequenceStore(2);
      LootContext ctx = Ctx.context(Lcg48Random.ofSeed(5));
      var one = LootEvaluator.evaluateTopLevel(t, ctx, seed, s).stacks();
      var two =
          LootEvaluator.evaluateTopLevel(t, Ctx.context(Lcg48Random.ofSeed(99)), seed, s).stacks();
      assertThat(two).isEqualTo(one);
      assertThat(s.requested()).isEmpty();
    }
    var s = new dev.shoko.minecraft.loot.testing.SequenceStore(2);
    LootEvaluator.evaluateTopLevel(t, Ctx.context(Lcg48Random.ofSeed(5)), 0L, s);
    assertThat(s.requested()).hasSize(1);
  }

  @Test
  void provisionalATableReferencingItselfIsDrawnAtMostOnceAtTopLevel() {
    // spec: LT-R23 (self reference is an error): no second pass through the table.
    Map<String, LootTable> tables = new java.util.LinkedHashMap<>();
    LootTable self =
        Tables.table(Tables.pool(1, Tables.ref("self")), Tables.pool(1, Tables.item("stick")));
    tables.put("self", self);
    LootResult result =
        LootEvaluator.evaluateTopLevel(
            self,
            Ctx.builder(tables, Lcg48Random.ofSeed(1)).build(),
            0L,
            new dev.shoko.minecraft.loot.testing.SequenceStore(1));
    assertThat(total(result)).isEqualTo(1);
  }
}

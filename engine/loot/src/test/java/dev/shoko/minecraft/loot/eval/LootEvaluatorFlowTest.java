package dev.shoko.minecraft.loot.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootPool;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: LT-R19a to LT-R19d (flow), LT-R6 (rolls), LT-R23 (nested tables), LT-R24 (dynamic), LT-R48
// (order of the modifiers), LT-R49, LT-R54 (split), LT-R55 (empty tables), V-L9, V-L10
class LootEvaluatorFlowTest {

  private static final LootCondition SILK_TOUCH =
      new LootCondition.Reference(Identifier.parse("minecraft:tool/can_silk_touch"));
  private static final LootCondition SURVIVES = new LootCondition.SurvivesExplosion();

  private static LootContext context(Map<String, LootTable> tables, ScriptedRandom random) {
    return Ctx.builder(tables, random).build();
  }

  private static LootContext plain() {
    return Ctx.context(ScriptedRandom.of());
  }

  private static List<String> paths(LootResult result) {
    return result.stacks().stream().map(s -> s.type().id().path()).toList();
  }

  private static List<Integer> counts(LootResult result) {
    return result.stacks().stream().map(ItemStack::count).toList();
  }

  private static LootTable stoneTable() {
    return Tables.table(
        Tables.pool(
            1,
            Tables.alternatives(
                Tables.when(Tables.item("stone"), SILK_TOUCH),
                Tables.when(Tables.item("cobblestone"), SURVIVES))));
  }

  @Test
  void provisionalStoneDropsCobblestoneWithoutAnyDraw() {
    // spec: V-L9
    ScriptedRandom random = ScriptedRandom.of();
    LootContext context = Ctx.builder(random).build();
    for (int i = 0; i < 1000; i++) {
      LootResult result = LootEvaluator.evaluate(stoneTable(), context);
      assertThat(paths(result)).containsExactly("cobblestone");
      assertThat(counts(result)).containsExactly(1);
    }
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalStoneDropsStoneWithASilkTouchTool() {
    // spec: V-L10
    LootContext context =
        Ctx.builder(ScriptedRandom.of())
            .tool(
                LootItems.enchanted(
                    LootItems.stack("iron_pickaxe"), Map.of("minecraft:silk_touch", 1)))
            .build();
    assertThat(paths(LootEvaluator.evaluate(stoneTable(), context))).containsExactly("stone");
  }

  @Test
  void provisionalATableWithoutPoolGivesNothing() {
    // spec: LT-R55 (44 vanilla tables without pools)
    LootResult result = LootEvaluator.evaluate(Tables.table(), plain());
    assertThat(result.stacks()).isEmpty();
    assertThat(result.findings()).isEmpty();
  }

  @Test
  void provisionalAPoolWithoutEntriesGivesNothing() {
    // spec: LT-R55, cas limite
    assertThat(LootEvaluator.evaluate(Tables.table(Tables.pool(3)), plain()).stacks()).isEmpty();
  }

  @Test
  void provisionalZeroRollsGiveNothing() {
    // spec: LT-R55
    LootTable table = Tables.table(Tables.pool(0, Tables.item("stone")));
    assertThat(LootEvaluator.evaluate(table, plain()).stacks()).isEmpty();
  }

  @Test
  void provisionalANegativeNumberOfRollsGivesNoRoll() {
    // spec: LT-R6
    LootTable table = Tables.table(Tables.pool(-2, Tables.item("stone")));
    assertThat(LootEvaluator.evaluate(table, plain()).stacks()).isEmpty();
  }

  @Test
  void provisionalEachRollDrawsAgainWithReplacement() {
    // spec: LT-R20
    LootTable table = Tables.table(Tables.pool(3, Tables.item("stone")));
    LootResult result = LootEvaluator.evaluate(table, plain());
    assertThat(paths(result)).containsExactly("stone", "stone", "stone");
  }

  @Test
  void provisionalAFailingPoolConditionSkipsThePoolWithoutDrawing() {
    // spec: LT-R19a
    LootPool skipped =
        Tables.poolWith(
            Tables.conditions(new LootCondition.KilledByPlayer()),
            new IntProvider.Constant(1),
            0f,
            List.of(),
            Tables.item("stone"),
            Tables.item("dirt"));
    ScriptedRandom random = ScriptedRandom.of();
    LootResult result =
        LootEvaluator.evaluate(
            Tables.table(skipped, Tables.pool(1, Tables.item("stick"))), Ctx.context(random));
    assertThat(paths(result)).containsExactly("stick");
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalPoolsAreDrawnInOrder() {
    // spec: LT-R19a
    LootTable table =
        Tables.table(
            Tables.pool(1, Tables.item("stone")),
            Tables.pool(1, Tables.item("dirt")),
            Tables.pool(1, Tables.item("stick")));
    assertThat(paths(LootEvaluator.evaluate(table, plain())))
        .containsExactly("stone", "dirt", "stick");
  }

  private static LootPool bonusPool(float bonus) {
    return Tables.poolWith(
        List.of(), new IntProvider.Constant(1), bonus, List.of(), Tables.item("stone"));
  }

  private static LootContext withLuck(float luck) {
    return Ctx.builder(ScriptedRandom.of()).luck(luck).build();
  }

  @Test
  void provisionalBonusRollsAddTheFlooredProductWithTheLuck() {
    // spec: LT-R19b (rolls + floor(bonus_rolls x chance))
    assertThat(LootEvaluator.rolls(bonusPool(0.5f), withLuck(3f))).isEqualTo(2);
    assertThat(LootEvaluator.rolls(bonusPool(0.5f), withLuck(1f))).isEqualTo(1);
    assertThat(LootEvaluator.rolls(bonusPool(1f), withLuck(2.5f))).isEqualTo(3);
    assertThat(LootEvaluator.rolls(bonusPool(0.5f), withLuck(0f))).isEqualTo(1);
  }

  @Test
  void provisionalBonusRollsDriveTheNumberOfStacks() {
    // spec: LT-R19b, probe PL7
    LootTable table = Tables.table(bonusPool(0.5f));
    LootContext context = Ctx.builder(ScriptedRandom.of()).luck(4f).build();
    assertThat(LootEvaluator.evaluate(table, context).stacks()).hasSize(3);
  }

  @Test
  void provisionalANegativeLuckDoesNotReduceTheRolls() {
    // spec: LT-R74i (a negative bonus is brought back to 0 before the sum), LT-R6
    assertThat(LootEvaluator.rolls(bonusPool(1f), withLuck(-5f))).isEqualTo(1);
  }

  @Test
  void provisionalANegativeValueOfRollsItselfIsZero() {
    // spec: LT-R6, LT-R74i (only a negative rolls gives no draw)
    LootPool pool =
        Tables.poolWith(
            List.of(), new IntProvider.Constant(-2), 1f, List.of(), Tables.item("stone"));
    assertThat(LootEvaluator.rolls(pool, withLuck(-5f))).isZero();
    assertThat(LootEvaluator.rolls(pool, withLuck(0f))).isZero();
  }

  @Test
  void provisionalAUniformRollsDrawsOnce() {
    // spec: LT-R6 (rolls uniform), LT-R51
    LootPool pool = Tables.pool(Tables.uniform(1, 3), Tables.item("stone"));
    ScriptedRandom random = ScriptedRandom.of(0.999);
    assertThat(LootEvaluator.rolls(pool, Ctx.context(random))).isEqualTo(3);
    assertThat(random.steps()).isEqualTo(1);
  }

  @Test
  void provisionalTheModifiersApplyEntryThenPoolThenTable() {
    // spec: LT-R48, probe PL6 (entry set_count 2, pool add +3, table limit max 4: result 4)
    LootPool pool =
        Tables.poolWith(
            List.of(),
            new IntProvider.Constant(1),
            0f,
            List.of(Tables.addCount(3)),
            Tables.modified(Tables.item("stone"), Tables.setCount(2)));
    LootTable table = Tables.withModifiers(Tables.table(pool), Tables.limitMax(4));
    LootResult result = LootEvaluator.evaluate(table, plain());
    assertThat(counts(result)).containsExactly(4);
  }

  @Test
  void provisionalTheTableModifiersComeLast() {
    // spec: LT-R19d, LT-R48
    LootPool pool =
        Tables.poolWith(
            List.of(),
            new IntProvider.Constant(1),
            0f,
            List.of(Tables.addCount(3)),
            Tables.modified(Tables.item("stone"), Tables.setCount(2)));
    LootTable table = Tables.withModifiers(Tables.table(pool), Tables.addCount(10));
    assertThat(counts(LootEvaluator.evaluate(table, plain()))).containsExactly(15);
  }

  @Test
  void provisionalThePoolModifiersApplyToEachStackOfThePool() {
    // spec: LT-R19d
    LootPool pool =
        Tables.poolWith(
            List.of(),
            new IntProvider.Constant(2),
            0f,
            List.of(Tables.addCount(1)),
            Tables.item("stone"));
    assertThat(counts(LootEvaluator.evaluate(Tables.table(pool), plain()))).containsExactly(2, 2);
  }

  @Test
  void provisionalADiscardedStackLeavesTheResult() {
    // spec: LT-R49
    LootTable table =
        Tables.table(Tables.pool(1, Tables.modified(Tables.item("stone"), Tables.discard())));
    assertThat(LootEvaluator.evaluate(table, plain()).stacks()).isEmpty();
  }

  @Test
  void provisionalAStackAboveTheMaximumIsSplit() {
    // spec: LT-R54, probe PL14 (70 stone: 64 then 6)
    LootTable table =
        Tables.table(Tables.pool(1, Tables.modified(Tables.item("stone"), Tables.setCount(70))));
    assertThat(counts(LootEvaluator.evaluate(table, plain()))).containsExactly(64, 6);
  }

  @Test
  void provisionalASplitKeepsTheTotalAndRespectsEachMaximum() {
    // spec: LT-R54 (set_count 100)
    LootTable table =
        Tables.table(
            Tables.pool(1, Tables.modified(Tables.item("stone"), Tables.setCount(100))),
            Tables.pool(1, Tables.modified(Tables.item("iron_pickaxe"), Tables.setCount(3))));
    LootResult result = LootEvaluator.evaluate(table, plain());
    assertThat(counts(result)).containsExactly(64, 36, 1, 1, 1);
    for (ItemStack stack : result.stacks()) {
      assertThat(stack.count()).isBetween(1, stack.maxStackSize());
    }
  }

  @Test
  void provisionalEveryOutputStackIsNonEmpty() {
    // spec: LootResult contract (none empty), LT-R49
    LootTable table =
        Tables.table(
            Tables.pool(
                4,
                Tables.modified(Tables.item("stone"), Tables.uniformCount(0, 2)),
                Tables.empty(3)));
    LootContext context = Ctx.context(Lcg48Random.ofSeed(99));
    for (int i = 0; i < 50; i++) {
      for (ItemStack stack : LootEvaluator.evaluate(table, context).stacks()) {
        assertThat(stack.isEmpty()).isFalse();
      }
    }
  }

  @Test
  void provisionalTheResultListIsUnmodifiable() {
    // spec: LootResult contract (unmodifiable)
    LootResult result = LootEvaluator.evaluate(stoneTable(), plain());
    assertThatThrownBy(() -> result.stacks().add(ItemStack.empty()))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(
            () -> result.findings().add(new LootFinding(LootFindingCode.TABLE_NOT_FOUND, "$", "x")))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void provisionalANestedInlineTableUsesTheContextOfItsCaller() {
    // spec: LT-R23
    LootTable inner =
        Tables.table(
            Tables.pool(1, Tables.when(Tables.item("stone"), new LootCondition.KilledByPlayer())));
    LootTable outer = Tables.table(Tables.pool(1, Tables.inline(inner)));
    LootContext killer =
        Ctx.builder(ScriptedRandom.of())
            .entity(EntityTarget.ATTACKING_PLAYER, FakeEntity.of("player"))
            .build();
    assertThat(paths(LootEvaluator.evaluate(outer, killer))).containsExactly("stone");
    assertThat(LootEvaluator.evaluate(outer, plain()).stacks()).isEmpty();
  }

  @Test
  void provisionalANamedNestedTableIsLoadedFromTheData() {
    // spec: LT-R23, LT-R9 (value: identifier)
    LootTable inner = Tables.table(Tables.pool(2, Tables.item("dirt")));
    LootTable outer = Tables.table(Tables.pool(1, Tables.ref("gameplay/inner")));
    LootContext context = context(Map.of("gameplay/inner", inner), ScriptedRandom.of());
    assertThat(paths(LootEvaluator.evaluate(outer, context))).containsExactly("dirt", "dirt");
  }

  @Test
  void provisionalTheNestedTableAppliesItsModifiersBeforeGivingBackItsStacks() {
    // spec: LT-R48
    LootTable inner =
        Tables.withModifiers(
            Tables.table(Tables.pool(1, Tables.item("stone"))), Tables.setCount(7));
    LootPool pool =
        Tables.poolWith(
            List.of(),
            new IntProvider.Constant(1),
            0f,
            List.of(Tables.limitMax(5)),
            Tables.inline(inner));
    assertThat(counts(LootEvaluator.evaluate(Tables.table(pool), plain()))).containsExactly(5);
  }

  @Test
  void provisionalAMissingNestedTableGivesNothingAndAFinding() {
    // spec: LootFindingCode#TABLE_NOT_FOUND (runtime observation)
    LootTable outer = Tables.table(Tables.pool(1, Tables.ref("gameplay/missing")));
    LootResult result = LootEvaluator.evaluate(outer, plain());
    assertThat(result.stacks()).isEmpty();
    assertThat(result.findings().stream().map(LootFinding::code).toList())
        .contains(LootFindingCode.TABLE_NOT_FOUND);
  }

  @Test
  void provisionalEvaluatingByIdentifierDrawsTheLoadedTable() {
    // spec: LootEvaluator#evaluate(Identifier) contract
    LootTable table = Tables.table(Tables.pool(1, Tables.item("diamond")));
    LootContext context = context(Map.of("blocks/diamond", table), ScriptedRandom.of());
    LootResult result =
        LootEvaluator.evaluate(Identifier.parse("minecraft:blocks/diamond"), context);
    assertThat(paths(result)).containsExactly("diamond");
  }

  @Test
  void provisionalEvaluatingAnUnknownIdentifierGivesNothingAndAFinding() {
    // spec: LootEvaluator#evaluate(Identifier) contract (TABLE_NOT_FOUND)
    LootResult result = LootEvaluator.evaluate(Identifier.parse("minecraft:blocks/none"), plain());
    assertThat(result.stacks()).isEmpty();
    assertThat(result.findings().stream().map(LootFinding::code).toList())
        .containsExactly(LootFindingCode.TABLE_NOT_FOUND);
  }

  @Test
  void provisionalADynamicEntryWithoutEmitterGivesNothingAndAnInformation() {
    // spec: LT-R24, LootFindingCode#DYNAMIC_SOURCE_MISSING
    LootTable table = Tables.table(Tables.pool(1, Tables.dynamic("sherds")));
    LootResult result = LootEvaluator.evaluate(table, plain());
    assertThat(result.stacks()).isEmpty();
    assertThat(result.findings().stream().map(LootFinding::code).toList())
        .contains(LootFindingCode.DYNAMIC_SOURCE_MISSING);
  }

  @Test
  void provisionalAnEntryOfTypeEmptyNeverProducesAStack() {
    // spec: LT-R24
    LootTable table = Tables.table(Tables.pool(5, Tables.empty(1)));
    assertThat(LootEvaluator.evaluate(table, Ctx.context(Lcg48Random.ofSeed(3))).stacks())
        .isEmpty();
  }

  @Test
  void provisionalAnItemEntryIsOneStackOfCountOneWithDefaultComponents() {
    // spec: LT-R24, LT-R56
    LootEntry entry = Tables.item("diamond_chestplate");
    LootResult result = LootEvaluator.evaluate(Tables.table(Tables.pool(1, entry)), plain());
    assertThat(result.stacks()).hasSize(1);
    ItemStack stack = result.stacks().getFirst();
    assertThat(stack.count()).isEqualTo(1);
    assertThat(stack.patch().isEmpty()).isTrue();
  }
}

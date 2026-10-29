package dev.shoko.minecraft.loot.eval;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.random.DrawEvent;
import dev.shoko.minecraft.loot.random.DrawObserver;
import dev.shoko.minecraft.loot.table.LootPool;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.table.LootTableCodec;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.DrawLog;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: LT-R74c and R74g (a constant weight above 2^20 counts as 2^20 and gives a warning at load),
// LT-R19b and R74a (bonus_rolls x luck stays within the 4096 rolls without error), LootContext
// contract (withObserver keeps every parameter and changes only the observer).
class RollsAndWeightWarningsTest {

  private static LootDecoding<LootTable> decodeWeight(long weight) {
    String json =
        "{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[{'type':'minecraft:item',"
            + "'name':'minecraft:stone','weight':"
            + weight
            + "}]}]}";
    return LootTableCodec.decode(Json.parse(json), EnchantFixtures.access(), LootItems.catalog());
  }

  private static List<LootFinding> capped(LootDecoding<LootTable> decoding) {
    return decoding.findings().stream()
        .filter(f -> f.code() == LootFindingCode.WEIGHT_CAPPED)
        .toList();
  }

  @Test
  void provisionalAConstantWeightAboveTheCapGivesACappedWarningButLoads() {
    // spec: LT-R74c, LT-R74g (avertissement, pas d'erreur)
    LootDecoding<LootTable> decoding = decodeWeight((1L << 20) + 1);
    Decoded.value(decoding);
    assertThat(capped(decoding)).hasSize(1);
    assertThat(capped(decoding).getFirst().path()).startsWith("$");
    assertThat(LootFindingCode.WEIGHT_CAPPED.severity()).isEqualTo(Severity.WARNING);
  }

  @Test
  void provisionalAConstantWeightAtOrBelowTheCapGivesNoWarning() {
    // spec: LT-R74c (le plafond n'a aucun effet sur les poids vanilla, de 2 a 699)
    assertThat(capped(decodeWeight(1L << 20))).isEmpty();
    assertThat(capped(decodeWeight(699))).isEmpty();
    assertThat(capped(decodeWeight(1))).isEmpty();
  }

  @Test
  void provisionalTheCappedWeightStillLoadsWhateverItsSizeUnderTheIntRange() {
    // spec: LT-R74c, LT-R74g (R74c plafonne sans erreur)
    LootDecoding<LootTable> decoding = decodeWeight(Integer.MAX_VALUE);
    Decoded.value(decoding);
    assertThat(capped(decoding)).hasSize(1);
  }

  private static LootPool bonusPool(float bonus) {
    return Tables.poolWith(
        List.of(), new IntProvider.Constant(1), bonus, List.of(), Tables.empty(1));
  }

  @Test
  void provisionalBonusRollsTimesTheLuckAreBoundedToTheCapWithoutError() {
    // spec: LT-R19b, LT-R74a: 1 + floor(1 x 1e9) is far above 4096
    for (float luck : new float[] {1e9f, Float.MAX_VALUE, Float.POSITIVE_INFINITY}) {
      LootContext context = Ctx.builder(ScriptedRandom.of()).luck(luck).build();
      assertThat(LootEvaluator.rolls(bonusPool(1f), context)).as("luck %s", luck).isEqualTo(4096);
    }
  }

  @Test
  void provisionalBonusRollsNeverGiveMoreThanTheCapNorLessThanZero() {
    // spec: LT-R19b, LT-R6 (0 ou moins: aucun tirage), LT-R74a
    float[] lucks = {Float.NaN, Float.NEGATIVE_INFINITY, -Float.MAX_VALUE, -1e9f, 0f, 3f, 1e9f};
    float[] bonuses = {0f, 0.5f, 1f, 1e9f, -1f, Float.MAX_VALUE};
    for (float luck : lucks) {
      for (float bonus : bonuses) {
        LootContext context = Ctx.builder(ScriptedRandom.of()).luck(luck).build();
        assertThat(LootEvaluator.rolls(bonusPool(bonus), context))
            .as("bonus %s luck %s", bonus, luck)
            .isBetween(0, 4096);
      }
    }
  }

  @Test
  void provisionalAHugeBonusDrawsTheCappedNumberOfRollsAndNothingFails() {
    // spec: LT-R74a: a whole table drawn with luck 1e9 gives 4096 empty rolls, no stack, no error
    LootTable table = Tables.table(bonusPool(1f));
    LootContext context = Ctx.builder(Lcg48Random.ofSeed(1)).luck(1e9f).build();
    assertThat(LootEvaluator.evaluate(table, context).stacks()).isEmpty();
    LootTable stones =
        Tables.table(
            Tables.poolWith(
                List.of(), new IntProvider.Constant(0), 1f, List.of(), Tables.item("stick")));
    List<ItemStack> drawn = LootEvaluator.evaluate(stones, context).stacks();
    assertThat(drawn.stream().mapToInt(ItemStack::count).sum()).isEqualTo(4096);
  }

  @Test
  void withObserverReplacesOnlyTheObserver() {
    // spec: LootContext#withObserver contract
    DrawLog first = new DrawLog();
    DrawLog second = new DrawLog();
    ItemStack tool = LootItems.stack("iron_pickaxe");
    LootContext original =
        Ctx.builder(Lcg48Random.ofSeed(4)).tool(tool).luck(2f).observer(first).build();
    LootContext moved = original.withObserver(second);
    assertThat(moved.observer()).isSameAs(second);
    assertThat(original.observer()).isSameAs(first);
    assertThat(moved.tool()).contains(tool);
    assertThat(moved.luck()).isEqualTo(2f);
    assertThat(moved.random()).isSameAs(original.random());
    assertThat(moved.data()).isSameAs(original.data());
    assertThat(moved.type()).isSameAs(original.type());
  }

  @Test
  void theEventsOfADrawGoToTheObserverOfTheContextItWasGiven() {
    // spec: LootContext#withObserver contract, DrawEvent contract
    LootTable table = Tables.table(Tables.pool(3, Tables.item("stone"), Tables.item("dirt")));
    DrawLog first = new DrawLog();
    List<DrawEvent> counted = new ArrayList<>();
    DrawObserver collecting = counted::add;
    LootContext original = Ctx.builder(Lcg48Random.ofSeed(4)).observer(first).build();
    LootEvaluator.evaluate(table, original.withObserver(collecting));
    assertThat(counted).isNotEmpty();
    assertThat(first.events()).isEmpty();
  }
}

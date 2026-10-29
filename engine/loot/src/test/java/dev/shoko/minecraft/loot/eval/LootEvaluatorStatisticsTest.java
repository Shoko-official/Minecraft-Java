package dev.shoko.minecraft.loot.eval;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.port.TypeSpecific;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.predicate.match.EntityPredicate;
import dev.shoko.minecraft.loot.predicate.match.Tristate;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: LT-R25/LT-R26 (probability proportional to the effective weight), LT-R27 (fishing), LT-R28
// (archaeology), LT-R32 (table_bonus), V-L11, V-L12 and the property tests of plan L1. Fixed seeds,
// wide thresholds (more than five standard deviations).
class LootEvaluatorStatisticsTest {

  private static Map<String, Integer> tally(
      LootTable table, int samples, LootContext.Builder builder) {
    LootContext context = builder.build();
    Map<String, Integer> counts = new HashMap<>();
    for (int i = 0; i < samples; i++) {
      LootResult result = LootEvaluator.evaluate(table, context);
      if (result.stacks().isEmpty()) {
        counts.merge("nothing", 1, Integer::sum);
      }
      for (ItemStack stack : result.stacks()) {
        counts.merge(stack.type().id().path(), 1, Integer::sum);
      }
    }
    return counts;
  }

  private static double share(Map<String, Integer> counts, String key, int samples) {
    return counts.getOrDefault(key, 0) / (double) samples;
  }

  private static LootContext.Builder base(long seed) {
    return Ctx.builder(Lcg48Random.ofSeed(seed));
  }

  private static LootCondition inOpenWater() {
    return new LootCondition.EntityProperties(
        EntityTarget.THIS,
        new EntityPredicate(List.of(new EntityPredicate.Check.FishingHook(Tristate.TRUE))));
  }

  private static LootTable fishing() {
    return Tables.table(
        Tables.pool(
            1,
            Tables.item("dirt", 10, -2),
            Tables.when(Tables.item("diamond", 5, 2), inOpenWater()),
            Tables.item("stick", 85, -1)));
  }

  private static LootContext.Builder fishingContext(long seed, float luck) {
    FakeEntity hook = FakeEntity.of("fishing_bobber").specific(new TypeSpecific.FishingHook(true));
    return base(seed).entity(EntityTarget.THIS, hook).luck(luck);
  }

  @Test
  void provisionalTheChoiceIsProportionalToTheWeights() {
    // spec: LT-R26
    LootTable table = Tables.table(Tables.pool(1, Tables.item("stone", 1), Tables.item("dirt", 3)));
    int samples = 6000;
    Map<String, Integer> counts = tally(table, samples, base(11));
    assertThat(share(counts, "dirt", samples)).isBetween(0.71, 0.79);
    assertThat(share(counts, "stone", samples)).isBetween(0.21, 0.29);
    assertThat(counts.get("dirt") + counts.get("stone")).isEqualTo(samples);
  }

  @Test
  void provisionalThePermutationOfEntriesOfEqualWeightChangesNoFrequency() {
    // spec: plan of tests L1
    LootTable first =
        Tables.table(
            Tables.pool(1, Tables.item("stone"), Tables.item("dirt"), Tables.item("stick")));
    LootTable second =
        Tables.table(
            Tables.pool(1, Tables.item("stick"), Tables.item("stone"), Tables.item("dirt")));
    int samples = 6000;
    Map<String, Integer> a = tally(first, samples, base(21));
    Map<String, Integer> b = tally(second, samples, base(22));
    for (String key : List.of("stone", "dirt", "stick")) {
      assertThat(share(a, key, samples)).as("first %s", key).isBetween(0.30, 0.37);
      assertThat(share(b, key, samples)).as("second %s", key).isBetween(0.30, 0.37);
    }
  }

  @Test
  void provisionalAnEmptyEntryTakesItsShareOfTheTotalWeight() {
    // spec: LT-R24
    LootTable table = Tables.table(Tables.pool(1, Tables.item("stone", 1), Tables.empty(1)));
    int samples = 6000;
    Map<String, Integer> counts = tally(table, samples, base(31));
    assertThat(share(counts, "stone", samples)).isBetween(0.46, 0.54);
    assertThat(share(counts, "nothing", samples)).isBetween(0.46, 0.54);
  }

  @Test
  void provisionalTheProbabilitiesOfAllOutcomesSumToOne() {
    // spec: plan of tests L1
    LootTable table =
        Tables.table(
            Tables.pool(1, Tables.item("stone", 2), Tables.item("dirt", 5), Tables.empty(3)));
    int samples = 5000;
    Map<String, Integer> counts = tally(table, samples, base(41));
    assertThat(counts.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(samples);
  }

  @Test
  void provisionalArchaeologyGivesTwentyFivePercentToAWeightTwoEntry() {
    // spec: LT-R28
    LootTable table =
        Tables.table(
            Tables.pool(
                1,
                Tables.item("stone", 2),
                Tables.item("dirt", 2),
                Tables.item("stick"),
                Tables.item("apple"),
                Tables.item("diamond"),
                Tables.item("emerald")));
    int samples = 16000;
    Map<String, Integer> counts = tally(table, samples, base(45));
    assertThat(share(counts, "stone", samples)).isBetween(0.235, 0.265);
    assertThat(share(counts, "dirt", samples)).isBetween(0.235, 0.265);
    for (String other : List.of("stick", "apple", "diamond", "emerald")) {
      assertThat(share(counts, other, samples)).as(other).isBetween(0.11, 0.14);
    }
  }

  @Test
  void provisionalFishingProportionsFollowTheLuckInOpenWater() {
    // spec: LT-R27, V-L12 (treasure: 5/100, 7/99, 9/98, 11/97)
    int samples = 20000;
    double[] expected = {5 / 100.0, 7 / 99.0, 9 / 98.0, 11 / 97.0};
    for (int luck = 0; luck <= 3; luck++) {
      Map<String, Integer> counts = tally(fishing(), samples, fishingContext(50L + luck, luck));
      assertThat(share(counts, "diamond", samples))
          .as("luck %d", luck)
          .isBetween(expected[luck] - 0.01, expected[luck] + 0.01);
    }
  }

  @Test
  void provisionalFishingJunkShrinksWithTheLuck() {
    // spec: LT-R27 (junk: 10, 8, 6, 4 of 100, 99, 98, 97)
    int samples = 20000;
    Map<String, Integer> none = tally(fishing(), samples, fishingContext(61, 0));
    Map<String, Integer> lucky = tally(fishing(), samples, fishingContext(62, 3));
    assertThat(share(none, "dirt", samples)).isBetween(0.09, 0.11);
    assertThat(share(lucky, "dirt", samples)).isBetween(4 / 97.0 - 0.01, 4 / 97.0 + 0.01);
  }

  @Test
  void provisionalTableBonusGivesTheSaplingChancesOfEachFortuneLevel() {
    // spec: LT-R32, V-L11 (5, 6.25, 8.33 and 10 percent)
    LootTable table =
        Tables.table(
            Tables.pool(
                1,
                Tables.when(
                    Tables.item("oak_sapling"),
                    new LootCondition.TableBonus(
                        Identifier.minecraft("fortune"),
                        List.of(0.05f, 0.0625f, 0.083333336f, 0.1f)))));
    double[] expected = {0.05, 0.0625, 0.0833, 0.10};
    int samples = 30000;
    for (int level = 0; level <= 3; level++) {
      ItemStack pick =
          LootItems.enchanted(
              LootItems.stack("iron_pickaxe"),
              level == 0 ? Map.of() : Map.of("minecraft:fortune", level));
      Map<String, Integer> counts = tally(table, samples, base(70L + level).tool(pick));
      assertThat(share(counts, "oak_sapling", samples))
          .as("fortune %d", level)
          .isBetween(expected[level] - 0.01, expected[level] + 0.01);
    }
  }

  @Test
  void provisionalARandomPoolConditionHoldsAsOftenAsItsChance() {
    // spec: LT-R30, LT-R19a
    LootTable table =
        Tables.table(
            Tables.poolWith(
                Tables.conditions(Tables.chance(0.25f)),
                new IntProvider.Constant(1),
                0f,
                List.of(),
                Tables.item("stone")));
    int samples = 8000;
    Map<String, Integer> counts = tally(table, samples, base(81));
    assertThat(share(counts, "stone", samples)).isBetween(0.22, 0.28);
  }

  @Test
  void provisionalTheNumberOfRollsOfAUniformPoolHasTheExpectedMean() {
    // spec: LT-R6, LT-R51 (uniform 1 to 3: mean 2)
    LootTable table = Tables.table(Tables.pool(Tables.uniform(1, 3), Tables.item("stone")));
    int samples = 6000;
    Map<String, Integer> counts = tally(table, samples, base(91));
    assertThat(counts.get("stone") / (double) samples).isBetween(1.9, 2.1);
  }

  @Test
  void provisionalAnEntryOfEffectiveWeightZeroIsNeverChosen() {
    // spec: LT-R25, LT-R19c
    LootEntry never = Tables.modified(Tables.item("stone", 0), Tables.setCount(9));
    LootTable table = Tables.table(Tables.pool(1, never, Tables.item("dirt")));
    Map<String, Integer> counts = tally(table, 300, base(101));
    assertThat(counts.keySet()).containsExactly("dirt");
  }
}

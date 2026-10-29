package dev.shoko.minecraft.loot.modifier;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.eval.LootEvaluator;
import dev.shoko.minecraft.loot.eval.LootResult;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: LT-R41 (explosion_decay, H-decay-b: a unit is kept when the draw is below 1/radius, like
// survives_explosion), V-L14, LT-R40 (limit_count with min > max, H-limit-a; integer form,
// H-limit-int-a), V-L16, LT-R42 (apply_bonus at level 0, H-bonus0-a), LT-R43 (floor, no draw at
// level 0), probes PL11, PL23, PL3, PL12.
class ModifierBoundariesTest {

  private static final ModifierAction DECAY = new ModifierAction.ExplosionDecay();

  private static LootTable decayed(int count) {
    return Tables.table(
        Tables.pool(
            1,
            Tables.modified(Tables.item("stick"), Tables.setCount(count), Tables.modifier(DECAY))));
  }

  private static int total(LootResult result) {
    return result.stacks().stream().mapToInt(ItemStack::count).sum();
  }

  private static int sumOver(LootTable table, float radius, int samples, long seed) {
    LootContext context = Ctx.builder(Lcg48Random.ofSeed(seed)).explosionRadius(radius).build();
    int sum = 0;
    for (int i = 0; i < samples; i++) {
      sum += total(LootEvaluator.evaluate(table, context));
    }
    return sum;
  }

  @Test
  void provisionalAUnitIsKeptWhenTheDrawIsBelowOneOverTheRadius() {
    // spec: LT-R41 (H-decay-b), LT-R33: radius 4, bound 0.25
    LootTable one = decayed(1);
    LootContext kept = Ctx.builder(ScriptedRandom.of(0.2)).explosionRadius(4f).build();
    LootContext lost = Ctx.builder(ScriptedRandom.of(0.3)).explosionRadius(4f).build();
    LootContext edge = Ctx.builder(ScriptedRandom.of(0.25)).explosionRadius(4f).build();
    assertThat(total(LootEvaluator.evaluate(one, kept))).isEqualTo(1);
    assertThat(total(LootEvaluator.evaluate(one, lost))).isZero();
    assertThat(total(LootEvaluator.evaluate(one, edge))).isZero();
  }

  @Test
  void provisionalAUnitSurvivesWithProbabilityOneOverTheRadius() {
    // spec: V-L14 (one unit: 0.3333, 0.25, 0.1667 for r = 3, 4, 6)
    int samples = 8000;
    double[] expected = {1 / 3.0, 1 / 4.0, 1 / 6.0};
    float[] radii = {3f, 4f, 6f};
    for (int i = 0; i < radii.length; i++) {
      double share = sumOver(decayed(1), radii[i], samples, 100 + i) / (double) samples;
      assertThat(share).as("radius %s", radii[i]).isBetween(expected[i] - 0.03, expected[i] + 0.03);
    }
  }

  @Test
  void provisionalAPileOfFourKeepsFourOverTheRadiusUnitsOnAverage() {
    // spec: V-L14 (mean of a pile of 4: 1.3333, 1.0, 0.6667)
    int samples = 4000;
    double[] expected = {4 / 3.0, 1.0, 4 / 6.0};
    float[] radii = {3f, 4f, 6f};
    for (int i = 0; i < radii.length; i++) {
      double mean = sumOver(decayed(4), radii[i], samples, 200 + i) / (double) samples;
      assertThat(mean).as("radius %s", radii[i]).isBetween(expected[i] - 0.08, expected[i] + 0.08);
    }
  }

  @Test
  void provisionalWithoutRadiusTheDecayLeavesTheWholePile() {
    // spec: LT-R41, H-decay-regle (no radius: no effect, no draw)
    ScriptedRandom random = ScriptedRandom.of();
    LootResult result = LootEvaluator.evaluate(decayed(4), Ctx.context(random));
    assertThat(total(result)).isEqualTo(4);
    assertThat(random.steps()).isZero();
  }

  private static ItemStack limited(int min, int max, int count) {
    ItemModifier limit = Tables.limit(min, max);
    return ModifierApplier.apply(
            limit, LootItems.stack("stone", count), Ctx.context(ScriptedRandom.of()))
        .stack();
  }

  @Test
  void provisionalLimitCountWithMinAboveMaxAlwaysGivesTheMaximum() {
    // spec: LT-R40 (H-limit-a), V-L16: {min 5, max 2} on piles of 1, 3 and 9 gives 2, 2, 2
    assertThat(limited(5, 2, 1).count()).isEqualTo(2);
    assertThat(limited(5, 2, 3).count()).isEqualTo(2);
    assertThat(limited(5, 2, 9).count()).isEqualTo(2);
  }

  @Test
  void provisionalLimitCountWithMinAboveZeroMaxRemovesThePile() {
    // spec: LT-R40 (H-limit-a: the result is max, 0 removes the pile), LT-R49
    assertThat(limited(5, 0, 3).isEmpty()).isTrue();
  }

  @Test
  void provisionalTheIntegerFormOfLimitCountIsAnExactLimit() {
    // spec: LT-R40 (H-limit-int-a: min = max = the integer), PL23
    ItemModifier limit =
        Decoded.value(
            ModifierCodec.decode(
                Json.parse("{'type':'minecraft:limit_count','limit':4}"),
                EnchantFixtures.access(),
                LootItems.catalog()));
    for (int count : new int[] {1, 3, 9}) {
      ItemStack result =
          ModifierApplier.apply(
                  limit, LootItems.stack("stone", count), Ctx.context(ScriptedRandom.of()))
              .stack();
      assertThat(result.count()).as("pile of %d", count).isEqualTo(4);
    }
  }

  private static ModifierAction ore() {
    return new ModifierAction.ApplyBonus(
        Identifier.minecraft("fortune"), new BonusFormula.OreDrops());
  }

  @Test
  void provisionalOreDropsAtLevelZeroStillConsumesOneBoundedDraw() {
    // spec: LT-R42 (H-bonus0-a: nextInt(2) is drawn, max(1, .) = 1, the count is unchanged)
    ScriptedRandom random = ScriptedRandom.of(0.0);
    LootContext context = Ctx.builder(random).tool(LootItems.stack("iron_pickaxe")).build();
    ItemStack result =
        ModifierApplier.apply(Tables.modifier(ore()), LootItems.stack("diamond", 2), context)
            .stack();
    assertThat(result.count()).isEqualTo(2);
    assertThat(random.steps()).isEqualTo(1);
  }

  @Test
  void provisionalOreDropsWithoutAToolReadsLevelZeroAndDrawsOnce() {
    // spec: LT-R42 (H-bonus0-a), LT-R15 (no tool: level 0)
    ScriptedRandom random = ScriptedRandom.of(0.99);
    ItemStack result =
        ModifierApplier.apply(
                Tables.modifier(ore()), LootItems.stack("diamond", 2), Ctx.context(random))
            .stack();
    assertThat(result.count()).isEqualTo(2);
    assertThat(random.steps()).isEqualTo(1);
  }

  @Test
  void provisionalBinomialBonusAtLevelZeroDrawsTheExtraTrials() {
    // spec: LT-R42 (H-bonus0-a: binomial consumes extra float draws at level 0)
    ModifierAction binomial =
        new ModifierAction.ApplyBonus(
            Identifier.minecraft("fortune"), new BonusFormula.BinomialWithBonusCount(3, 0.5f));
    ScriptedRandom random = ScriptedRandom.of(0.1, 0.9, 0.2);
    LootContext context = Ctx.builder(random).tool(LootItems.stack("iron_pickaxe")).build();
    ItemStack result =
        ModifierApplier.apply(Tables.modifier(binomial), LootItems.stack("diamond"), context)
            .stack();
    assertThat(random.steps()).isEqualTo(3);
    assertThat(result.count()).isEqualTo(3);
  }

  private static LootContext withLooting(int level, ScriptedRandom random) {
    FakeEntity attacker =
        FakeEntity.of("player")
            .wearing(
                EquipmentSlot.MAINHAND,
                LootItems.enchanted(
                    LootItems.stack("diamond_sword"), Map.of("minecraft:looting", level)));
    return Ctx.builder(random).entity(EntityTarget.ATTACKER, attacker).build();
  }

  private static ModifierAction looting(float count) {
    return new ModifierAction.EnchantedCountIncrease(
        new FloatProvider.Constant(count), Identifier.minecraft("looting"), 0);
  }

  @Test
  void provisionalTheLootingBonusIsRoundedDown() {
    // spec: LT-R43 (arrondi vers le bas): 0.5 per level
    ItemStack one =
        ModifierApplier.apply(
                Tables.modifier(looting(0.5f)),
                LootItems.stack("beef"),
                withLooting(1, ScriptedRandom.of()))
            .stack();
    assertThat(one.count()).isEqualTo(1);
    ItemStack three =
        ModifierApplier.apply(
                Tables.modifier(looting(0.5f)),
                LootItems.stack("beef"),
                withLooting(3, ScriptedRandom.of()))
            .stack();
    assertThat(three.count()).isEqualTo(2);
  }

  @Test
  void provisionalTheLootingBonusDrawsNothingAtLevelZero() {
    // spec: LT-R43 (level 0: no increase), LT-R42 neighbour: no sampling of the count at level 0
    ModifierAction uniform =
        new ModifierAction.EnchantedCountIncrease(
            new FloatProvider.Uniform(
                new FloatProvider.Constant(0f), new FloatProvider.Constant(1f)),
            Identifier.minecraft("looting"),
            0);
    ScriptedRandom random = ScriptedRandom.of();
    ItemStack result =
        ModifierApplier.apply(
                Tables.modifier(uniform), LootItems.stack("beef", 2), Ctx.context(random))
            .stack();
    assertThat(result.count()).isEqualTo(2);
    assertThat(random.steps()).isZero();
  }
}

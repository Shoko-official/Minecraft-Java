package dev.shoko.minecraft.loot.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: LT-R30 (random_chance), LT-R31 (random_chance_with_enchanted_bonus), LT-R32 (table_bonus),
// LT-R33 (survives_explosion), LT-R15 (absent parameters)
class ConditionRandomTest {

  private static final Identifier FORTUNE = Identifier.minecraft("fortune");

  private static ConditionResult chance(float probability, double draw) {
    ScriptedRandom random = ScriptedRandom.of(draw);
    ConditionResult result = Tables.chance(probability).test(Ctx.context(random));
    assertThat(random.steps()).as("one draw per evaluation").isEqualTo(1);
    return result;
  }

  private static ConditionResult survives(Float radius, double draw) {
    ScriptedRandom random = ScriptedRandom.of(draw);
    LootContext.Builder builder = Ctx.builder(random);
    if (radius != null) {
      builder.explosionRadius(radius);
    }
    return new LootCondition.SurvivesExplosion().test(builder.build());
  }

  private static LootContext withFortune(int level, double draw) {
    return Ctx.builder(ScriptedRandom.of(draw))
        .tool(
            LootItems.enchanted(
                LootItems.stack("iron_pickaxe"), Map.of("minecraft:fortune", level)))
        .build();
  }

  private static LootCondition bonus(List<Float> chances) {
    return new LootCondition.TableBonus(FORTUNE, chances);
  }

  @Test
  void provisionalRandomChancePassesWhenTheDrawIsStrictlyBelow() {
    // spec: LT-R30
    assertThat(chance(0.5f, 0.3).passed()).isTrue();
    assertThat(chance(0.5f, 0.3).cause()).isEqualTo(ConditionCause.MATCHED);
    assertThat(chance(0.5f, 0.5).passed()).isFalse();
    assertThat(chance(0.5f, 0.5).cause()).isEqualTo(ConditionCause.NOT_MATCHED);
    assertThat(chance(0.5f, 0.7).passed()).isFalse();
  }

  @Test
  void provisionalRandomChanceZeroNeverPassesAndOneAlwaysPasses() {
    // spec: LT-R30
    assertThat(chance(0f, 0.0).passed()).isFalse();
    assertThat(chance(1f, 0.9999999).passed()).isTrue();
  }

  @Test
  void provisionalRandomChanceReadsAProviderOfTheEnchantmentLevel() {
    // spec: LT-R30 (chance may be an enchantment_level provider)
    LootCondition condition =
        new LootCondition.RandomChance(
            new FloatProvider.EnchantmentLevel(new LevelBasedValue.Linear(0.1f, 0.1f)));
    LootContext pass = Ctx.builder(ScriptedRandom.of(0.15)).enchantment(2, true).build();
    LootContext fail = Ctx.builder(ScriptedRandom.of(0.25)).enchantment(2, true).build();
    assertThat(condition.test(pass).passed()).isTrue();
    assertThat(condition.test(fail).passed()).isFalse();
  }

  @Test
  void provisionalRandomChanceFrequencyMatchesItsProbability() {
    // spec: LT-R30 (0.125 is seen in the tables)
    RandomSource random = Lcg48Random.ofSeed(17);
    LootContext context = Ctx.context(random);
    LootCondition condition = Tables.chance(0.125f);
    int hits = 0;
    int samples = 16000;
    for (int i = 0; i < samples; i++) {
      if (condition.test(context).passed()) {
        hits++;
      }
    }
    assertThat(hits / (double) samples).isBetween(0.105, 0.145);
  }

  @Test
  void provisionalSurvivesExplosionWithoutRadiusPassesWithoutDrawing() {
    // spec: LT-R33, LT-R15
    ConditionResult result = survives(null, 0.99);
    assertThat(result.passed()).isTrue();
    assertThat(result.cause()).isEqualTo(ConditionCause.PARAMETER_ABSENT);
    ScriptedRandom random = ScriptedRandom.of();
    new LootCondition.SurvivesExplosion().test(Ctx.context(random));
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalSurvivesExplosionWithARadiusDrawsAgainstOneOverTheRadius() {
    // spec: LT-R33
    assertThat(survives(4f, 0.2).passed()).isTrue();
    assertThat(survives(4f, 0.3).passed()).isFalse();
    assertThat(survives(4f, 0.25).passed()).isFalse();
  }

  @Test
  void provisionalSurvivesExplosionWithARadiusOfOneAlwaysPasses() {
    // spec: LT-R33 (1/r = 1)
    assertThat(survives(1f, 0.9999999).passed()).isTrue();
  }

  @Test
  void provisionalTableBonusReadsTheChanceOfTheLevelOnTheTool() {
    // spec: LT-R32
    LootCondition condition = bonus(List.of(0.1f, 0.14285715f, 0.25f, 1.0f));
    assertThat(condition.test(withFortune(3, 0.999)).passed()).isTrue();
    assertThat(condition.test(withFortune(2, 0.3)).passed()).isFalse();
    assertThat(condition.test(withFortune(2, 0.2)).passed()).isTrue();
  }

  @Test
  void provisionalTableBonusWithoutEnchantmentReadsLevelZero() {
    // spec: LT-R32, LT-R15
    LootCondition condition = bonus(List.of(0.1f, 1.0f));
    LootContext plainTool =
        Ctx.builder(ScriptedRandom.of(0.05)).tool(LootItems.stack("iron_pickaxe")).build();
    LootContext noTool = Ctx.builder(ScriptedRandom.of(0.05)).build();
    LootContext noToolMiss = Ctx.builder(ScriptedRandom.of(0.2)).build();
    assertThat(condition.test(plainTool).passed()).isTrue();
    assertThat(condition.test(noTool).passed()).isTrue();
    assertThat(condition.test(noToolMiss).passed()).isFalse();
  }

  @Test
  void provisionalTableBonusClampsALevelBeyondTheListToTheLastCase() {
    // spec: LT-R32, probe PL10
    LootCondition condition = bonus(List.of(0.5f, 0.25f));
    assertThat(condition.test(withFortune(3, 0.2)).passed()).isTrue();
    assertThat(condition.test(withFortune(3, 0.3)).passed()).isFalse();
  }

  private static LootContext withLooting(Integer level, double draw) {
    LootContext.Builder builder = Ctx.builder(ScriptedRandom.of(draw));
    if (level != null) {
      FakeEntity attacker =
          FakeEntity.of("player")
              .wearing(
                  EquipmentSlot.MAINHAND,
                  LootItems.enchanted(
                      LootItems.stack("diamond_sword"), Map.of("minecraft:looting", level)));
      builder.entity(EntityTarget.ATTACKER, attacker);
    }
    return builder.build();
  }

  private static LootCondition lootingChance() {
    return new LootCondition.RandomChanceWithEnchantedBonus(
        Identifier.minecraft("looting"), 0.1f, new LevelBasedValue.Linear(0.13f, 0.02f));
  }

  @Test
  void provisionalEnchantedBonusReadsTheLevelOnTheWeaponOfTheAttacker() {
    // spec: LT-R31 (looting 2: 0.13 + 0.02 per level above the first = 0.15)
    assertThat(lootingChance().test(withLooting(2, 0.14)).passed()).isTrue();
    assertThat(lootingChance().test(withLooting(2, 0.16)).passed()).isFalse();
  }

  @Test
  void provisionalEnchantedBonusWithoutAttackerUsesTheUnenchantedChance() {
    // spec: LT-R31, LT-R15
    assertThat(lootingChance().test(withLooting(null, 0.05)).passed()).isTrue();
    assertThat(lootingChance().test(withLooting(null, 0.12)).passed()).isFalse();
  }

  @Test
  void provisionalEnchantedBonusDrawsOnce() {
    // spec: LT-R31
    ScriptedRandom random = ScriptedRandom.of(0.5);
    lootingChance().test(Ctx.context(random));
    assertThat(random.steps()).isEqualTo(1);
  }
}

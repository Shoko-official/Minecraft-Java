package dev.shoko.minecraft.loot.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.predicate.match.LocationPredicate;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// Adversarial tests on conditions and their random consumption.
class ConditionsHuntTest {

  private static boolean holds(LootCondition condition, LootContext context) {
    return condition.test(context).passed();
  }

  @Test
  void provisionalEmptyCompositesFollowLtR29() {
    // spec: LT-R29
    LootContext context = Ctx.context(ScriptedRandom.of());
    assertThat(holds(new LootCondition.AllOf(List.of()), context)).isTrue();
    assertThat(holds(new LootCondition.AnyOf(List.of()), context)).isFalse();
    assertThat(holds(new LootCondition.Inverted(new LootCondition.AnyOf(List.of())), context))
        .isTrue();
  }

  @Test
  void provisionalShortCircuitDoesNotConsumeTheSkippedTerms() {
    // spec: LT-R29 (no draw for the skipped terms)
    ScriptedRandom script = ScriptedRandom.of(0.1);
    LootCondition any =
        new LootCondition.AnyOf(
            List.of(
                new LootCondition.RandomChance(new FloatProvider.Constant(0.5f)),
                new LootCondition.RandomChance(new FloatProvider.Constant(0.5f))));
    assertThat(holds(any, Ctx.context(script))).isTrue();
    assertThat(script.remaining()).isZero();
  }

  @Test
  void provisionalRandomChanceBoundsAreExact() {
    // spec: LT-R30 (nextFloat in [0, 1[ strictly below chance)
    LootContext zero = Ctx.context(Lcg48Random.ofSeed(1));
    for (int i = 0; i < 20_000; i++) {
      assertThat(holds(new LootCondition.RandomChance(new FloatProvider.Constant(0f)), zero))
          .isFalse();
      assertThat(holds(new LootCondition.RandomChance(new FloatProvider.Constant(1f)), zero))
          .isTrue();
      assertThat(holds(new LootCondition.RandomChance(new FloatProvider.Constant(Float.NaN)), zero))
          .isFalse();
    }
  }

  @Test
  void provisionalTableBonusClampsTheLevelToTheLastChance() {
    // spec: LT-R32 (provisional: index bounded to the last cell), LT-R15 (no tool: level 0)
    LootCondition bonus =
        new LootCondition.TableBonus(Identifier.minecraft("fortune"), List.of(0.0f, 1.0f));
    LootContext deep =
        Ctx.builder(Lcg48Random.ofSeed(1))
            .tool(
                LootItems.enchanted(
                    LootItems.stack("iron_pickaxe"), Map.of("minecraft:fortune", 255)))
            .build();
    assertThat(holds(bonus, deep)).isTrue();
    assertThat(holds(bonus, Ctx.context(Lcg48Random.ofSeed(1)))).isFalse();
    assertThat(
            holds(new LootCondition.TableBonus(Identifier.minecraft("fortune"), List.of()), deep))
        .isFalse();
  }

  @Test
  void provisionalEnchantedBonusWithoutAWeaponReadsLevelZero() {
    // spec: LT-R31, LT-R15 (attacker absent or weaponless: level 0, unenchanted chance)
    LootCondition bonus =
        new LootCondition.RandomChanceWithEnchantedBonus(
            Identifier.minecraft("looting"), 0.0f, new LevelBasedValue.Constant(1f));
    LootContext noAttacker = Ctx.context(Lcg48Random.ofSeed(3));
    LootContext weaponless =
        Ctx.builder(Lcg48Random.ofSeed(3))
            .entity(EntityTarget.ATTACKER, FakeEntity.of("player"))
            .build();
    for (int i = 0; i < 1000; i++) {
      assertThat(holds(bonus, noAttacker)).isFalse();
      assertThat(holds(bonus, weaponless)).isFalse();
    }
    LootContext armed =
        Ctx.builder(Lcg48Random.ofSeed(3))
            .entity(
                EntityTarget.ATTACKER,
                FakeEntity.of("player")
                    .wearing(
                        EquipmentSlot.MAINHAND,
                        LootItems.enchanted(
                            LootItems.stack("diamond_sword"), Map.of("minecraft:looting", 255))))
            .build();
    assertThat(holds(bonus, armed)).isTrue();
  }

  @Test
  void provisionalLocationCheckFloorsNegativeCoordinatesBeforeTheChunkLookup() {
    // spec: LT-R35 / R59: block position by floor, chunk by floor division
    List<ChunkPos> asked = new ArrayList<>();
    LootCondition check =
        new LootCondition.LocationCheck(
            new LocationPredicate(List.of(new LocationPredicate.Requirement.CanSeeSky(true))),
            0,
            0,
            0);
    double[][] origins = {
      {-0.0000001, 70, -16.0}, {-16.0000001, 70, 15.99999}, {-1.0, 70, -1.0}, {16.0, 70, 31.9}
    };
    ChunkPos[] expected = {
      new ChunkPos(-1, -1), new ChunkPos(-2, 0), new ChunkPos(-1, -1), new ChunkPos(1, 1)
    };
    for (int i = 0; i < origins.length; i++) {
      LootContext context =
          Ctx.builder(Lcg48Random.ofSeed(1))
              .origin(new Vec3(origins[i][0], origins[i][1], origins[i][2]))
              .chunks(
                  pos -> {
                    asked.add(pos);
                    return Optional.empty();
                  })
              .build();
      assertThat(holds(check, context)).isFalse();
    }
    assertThat(asked).containsExactly(expected);
  }

  @Test
  void provisionalLocationCheckWithAnOriginBeyondTheIntRangeDoesNotWrapAround() {
    // spec: LT-R35: a position beyond the integer range must not wrap to the other side.
    List<ChunkPos> asked = new ArrayList<>();
    LootCondition check =
        new LootCondition.LocationCheck(
            new LocationPredicate(List.of(new LocationPredicate.Requirement.CanSeeSky(true))),
            5,
            0,
            0);
    LootContext context =
        Ctx.builder(Lcg48Random.ofSeed(1))
            .origin(new Vec3(3.0e9, 70, 0))
            .chunks(
                pos -> {
                  asked.add(pos);
                  return Optional.empty();
                })
            .build();
    holds(check, context);
    assertThat(asked).allSatisfy(pos -> assertThat(pos.x()).isPositive());
  }
}

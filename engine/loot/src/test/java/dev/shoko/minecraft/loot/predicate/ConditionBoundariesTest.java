package dev.shoko.minecraft.loot.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LevelFacts;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.predicate.match.BlockMatch;
import dev.shoko.minecraft.loot.predicate.match.LocationPredicate;
import dev.shoko.minecraft.loot.testing.Chunks;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Sets;
import dev.shoko.minecraft.world.chunk.Chunk;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// spec: LT-R31 (level 0 reads the unenchanted chance), LT-R36 (time_check: the time of day, modulo
// period when present), LT-R35 and LT-R59 (location_check: offsets added to the block of the
// origin,
// chunks by floor division), review mutants M24, M26, M27.
class ConditionBoundariesTest {

  private static final Identifier CLOCK = Identifier.minecraft("overworld");

  private static LootCondition lootingChance() {
    return new LootCondition.RandomChanceWithEnchantedBonus(
        Identifier.minecraft("looting"), 0.1f, new LevelBasedValue.Linear(0.13f, 0.02f));
  }

  private static boolean withLooting(Integer level, double draw) {
    LootContext.Builder builder = Ctx.builder(ScriptedRandom.of(draw));
    if (level != null) {
      builder.entity(
          EntityTarget.ATTACKER,
          FakeEntity.of("player")
              .wearing(
                  EquipmentSlot.MAINHAND,
                  LootItems.enchanted(
                      LootItems.stack("diamond_sword"), Map.of("minecraft:looting", level))));
    }
    return lootingChance().test(builder.build()).passed();
  }

  @Test
  void provisionalLevelZeroReadsTheUnenchantedChanceNotTheCurveAtZero() {
    // spec: LT-R31 (the unenchanted chance at level 0): the curve at level 0 would be 0.11
    assertThat(withLooting(null, 0.105)).isFalse();
    assertThat(withLooting(null, 0.095)).isTrue();
  }

  @Test
  void provisionalALevelOneWeaponReadsTheCurveAtItsLevel() {
    // spec: LT-R31: at level 1 the curve gives 0.13
    assertThat(withLooting(1, 0.125)).isTrue();
    assertThat(withLooting(1, 0.135)).isFalse();
  }

  private static boolean timeCheck(long period, long dayTime) {
    LootCondition check = new LootCondition.TimeCheck(CLOCK, Sets.between(100, 100), period);
    LootContext context =
        Ctx.builder(ScriptedRandom.of()).level(new LevelFacts(false, false, 0, dayTime)).build();
    return check.test(context).passed();
  }

  @Test
  void provisionalTheTimeIsTakenModuloThePeriod() {
    // spec: LT-R36 (hypothesis: the time compared is LevelFacts.dayTime): 1100 mod 1000 = 100
    assertThat(timeCheck(1000, 1100)).isTrue();
    assertThat(timeCheck(1000, 1101)).isFalse();
    assertThat(timeCheck(1000, 99)).isFalse();
  }

  @Test
  void provisionalWithoutPeriodTheTimeIsCompared() {
    // spec: LT-R36 (period absent: no modulo)
    assertThat(timeCheck(0, 100)).isTrue();
    assertThat(timeCheck(0, 1100)).isFalse();
  }

  private static LootCondition stoneAt(int x, int y, int z) {
    return new LootCondition.LocationCheck(
        new LocationPredicate(
            List.of(
                new LocationPredicate.Requirement.Block(
                    new BlockMatch(Sets.blocks("stone"), new LinkedHashMap<>())))),
        x,
        y,
        z);
  }

  private static boolean checkWithChunk(LootCondition check, Vec3 origin) {
    Chunk chunk = Chunks.stoneSection(new ChunkPos(0, 0), 4);
    LootContext context =
        Ctx.builder(ScriptedRandom.of())
            .origin(origin)
            .chunks(pos -> pos.equals(new ChunkPos(0, 0)) ? Optional.of(chunk) : Optional.empty())
            .build();
    return check.test(context).passed();
  }

  @Test
  void provisionalTheVerticalOffsetMovesTheCheckedBlock() {
    // spec: LT-R35 (offsetY seen in the data): the stone section covers y = 64 to 79
    Vec3 origin = new Vec3(0.5, 64.5, 0.5);
    assertThat(checkWithChunk(stoneAt(0, 0, 0), origin)).isTrue();
    assertThat(checkWithChunk(stoneAt(0, -1, 0), origin)).isFalse();
    assertThat(checkWithChunk(stoneAt(0, 15, 0), origin)).isTrue();
    assertThat(checkWithChunk(stoneAt(0, 16, 0), origin)).isFalse();
  }

  @Test
  void provisionalTheHorizontalOffsetsMoveTheCheckedBlockAcrossChunks() {
    // spec: LT-R35, LT-R59: x = 5 stays in chunk (0, 0), x = 16 or z = -1 leaves it (no chunk:
    // fail)
    Vec3 origin = new Vec3(0.5, 64.5, 0.5);
    assertThat(checkWithChunk(stoneAt(5, 0, 5), origin)).isTrue();
    assertThat(checkWithChunk(stoneAt(16, 0, 0), origin)).isFalse();
    assertThat(checkWithChunk(stoneAt(0, 0, -1), origin)).isFalse();
  }

  private static List<ChunkPos> askedFor(Vec3 origin, int offsetX, int offsetZ) {
    List<ChunkPos> asked = new ArrayList<>();
    LootCondition check =
        new LootCondition.LocationCheck(
            new LocationPredicate(List.of(new LocationPredicate.Requirement.CanSeeSky(true))),
            offsetX,
            0,
            offsetZ);
    LootContext context =
        Ctx.builder(ScriptedRandom.of())
            .origin(origin)
            .chunks(
                pos -> {
                  asked.add(pos);
                  return Optional.empty();
                })
            .build();
    assertThat(check.test(context).passed()).isFalse();
    return asked;
  }

  @Test
  void provisionalAnOffsetPastTheIntRangeDoesNotWrapToTheOtherSide() {
    // spec: LT-R35 (offsets are ints): block x = 2147483647 plus 10 must not become a negative x
    List<ChunkPos> east = askedFor(new Vec3(2_147_483_647.0, 70, 0), 10, 0);
    assertThat(east).allSatisfy(pos -> assertThat(pos.x()).isPositive());
    List<ChunkPos> west = askedFor(new Vec3(-2_147_483_648.0, 70, 0), -10, 0);
    assertThat(west).allSatisfy(pos -> assertThat(pos.x()).isNegative());
    List<ChunkPos> south = askedFor(new Vec3(0, 70, 2_147_483_647.0), 0, 10);
    assertThat(south).allSatisfy(pos -> assertThat(pos.z()).isPositive());
  }

  @Test
  void provisionalAnOffsetThatBringsTheBlockBackInsideTheRangeIsHonoured() {
    // spec: LT-R35: block x = 2147483647 plus -16 is inside the range and is looked up
    List<ChunkPos> asked = askedFor(new Vec3(2_147_483_647.0, 70, 0), -16, 0);
    assertThat(asked).containsExactly(new ChunkPos(Math.floorDiv(2_147_483_647 - 16, 16), 0));
  }
}

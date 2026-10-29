package dev.shoko.minecraft.loot.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.ContextKey;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContextType;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.predicate.match.BlockMatch;
import dev.shoko.minecraft.loot.predicate.match.DamageSourcePredicate;
import dev.shoko.minecraft.loot.predicate.match.EntityPredicate;
import dev.shoko.minecraft.loot.predicate.match.ItemMatch;
import dev.shoko.minecraft.loot.predicate.match.LocationPredicate;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.Sets;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

// spec: LT-R14b (parameters each condition reads, provisional), LT-R15 (load-time warning)
class ConditionReadsTest {

  private static final NamedPredicates NAMED = Ctx.standardPredicates();

  private static Set<ContextKey> reads(LootCondition condition) {
    return ConditionReads.of(condition, NAMED);
  }

  private static LootCondition entity(EntityTarget target) {
    return new LootCondition.EntityProperties(target, new EntityPredicate(List.of()));
  }

  private static LootCondition tool() {
    return new LootCondition.MatchTool(
        new ItemMatch(
            Sets.anyItem(), Sets.anyNumber(), new LinkedHashMap<>(), new LinkedHashMap<>()));
  }

  private static LootCondition block() {
    return new LootCondition.MatchBlock(
        new BlockMatch(Sets.blocks("wheat"), new LinkedHashMap<>()));
  }

  private static LootContextType typeAllowing(ContextKey... allowed) {
    return new LootContextType(
        Identifier.minecraft("test"), 99, Set.of(), Set.copyOf(EnumSet.of(allowed[0], allowed)));
  }

  @Test
  void provisionalSurvivesExplosionReadsTheRadius() {
    // spec: LT-R14b
    assertThat(reads(new LootCondition.SurvivesExplosion()))
        .containsExactly(ContextKey.EXPLOSION_RADIUS);
  }

  @Test
  void provisionalToolConditionsReadTheTool() {
    // spec: LT-R14b (table_bonus, match_tool)
    assertThat(reads(tool())).containsExactly(ContextKey.TOOL);
    assertThat(reads(new LootCondition.TableBonus(Identifier.minecraft("fortune"), List.of(0.1f))))
        .containsExactly(ContextKey.TOOL);
  }

  @Test
  void provisionalMatchBlockReadsTheBlockState() {
    // spec: LT-R14b
    assertThat(reads(block())).containsExactly(ContextKey.BLOCK_STATE);
  }

  @Test
  void provisionalKilledByPlayerReadsTheAttackingPlayer() {
    // spec: LT-R14b, LT-R35
    assertThat(reads(new LootCondition.KilledByPlayer()))
        .containsExactly(ContextKey.ATTACKING_PLAYER);
  }

  @Test
  void provisionalEntityPropertiesReadsTheEntityItTargets() {
    // spec: LT-R14b, LT-R12
    assertThat(reads(entity(EntityTarget.THIS))).containsExactly(ContextKey.THIS_ENTITY);
    assertThat(reads(entity(EntityTarget.ATTACKER))).containsExactly(ContextKey.ATTACKER);
    assertThat(reads(entity(EntityTarget.DIRECT_ATTACKER)))
        .containsExactly(ContextKey.DIRECT_ATTACKER);
    assertThat(reads(entity(EntityTarget.ATTACKING_PLAYER)))
        .containsExactly(ContextKey.ATTACKING_PLAYER);
  }

  @Test
  void provisionalLocationCheckReadsTheOrigin() {
    // spec: LT-R14b
    assertThat(reads(new LootCondition.LocationCheck(new LocationPredicate(List.of()), 0, 0, 0)))
        .containsExactly(ContextKey.ORIGIN);
  }

  @Test
  void provisionalDamageSourcePropertiesReadsTheDamageSource() {
    // spec: LT-R14b
    assertThat(
            reads(new LootCondition.DamageSourceProperties(new DamageSourcePredicate(List.of()))))
        .contains(ContextKey.DAMAGE_SOURCE);
  }

  @Test
  void provisionalTheEnchantedBonusReadsTheAttacker() {
    // spec: LT-R31, LT-R15 (the weapon of the attacker)
    LootCondition bonus =
        new LootCondition.RandomChanceWithEnchantedBonus(
            Identifier.minecraft("looting"), 0.1f, new LevelBasedValue.Linear(0.1f, 0.1f));
    assertThat(reads(bonus)).contains(ContextKey.ATTACKER);
  }

  @Test
  void provisionalAPureRandomConditionReadsNothing() {
    // spec: LT-R30
    assertThat(reads(Tables.chance(0.5f))).isEmpty();
  }

  @Test
  void provisionalNestedConditionsReadTheUnionOfTheirTerms() {
    // spec: ConditionReads#of contract (nested terms included)
    LootCondition nested =
        new LootCondition.AllOf(
            List.of(
                new LootCondition.Inverted(tool()),
                new LootCondition.AnyOf(List.of(block(), new LootCondition.KilledByPlayer()))));
    assertThat(reads(nested))
        .containsExactlyInAnyOrder(
            ContextKey.TOOL, ContextKey.BLOCK_STATE, ContextKey.ATTACKING_PLAYER);
  }

  @Test
  void provisionalAReferenceIsFollowedThroughTheNamedPredicates() {
    // spec: ConditionReads contract, LT-R61a (tool/can_silk_touch is a match_tool)
    LootCondition reference =
        new LootCondition.Reference(Identifier.parse("minecraft:tool/can_silk_touch"));
    assertThat(reads(reference)).containsExactly(ContextKey.TOOL);
  }

  @Test
  void provisionalTheWarningNamesEachConditionThatReadsAMissingParameter() {
    // spec: LT-R15 (UNAVAILABLE_PARAMETER, the table still loads)
    LootContextType originOnly = typeAllowing(ContextKey.ORIGIN);
    List<LootFinding> findings =
        ConditionReads.unavailable(
            List.of(new LootCondition.SurvivesExplosion(), tool(), Tables.chance(0.5f)),
            originOnly,
            NAMED);
    assertThat(findings).hasSize(2);
    assertThat(findings.stream().map(LootFinding::code).distinct().toList())
        .containsExactly(LootFindingCode.UNAVAILABLE_PARAMETER);
  }

  @Test
  void provisionalNoWarningWhenTheTypeProvidesEverythingTheConditionsRead() {
    // spec: LT-R15
    assertThat(
            ConditionReads.unavailable(
                List.of(new LootCondition.SurvivesExplosion(), tool(), block()), Ctx.ANY, NAMED))
        .isEmpty();
  }
}

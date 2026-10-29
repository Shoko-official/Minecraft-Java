package dev.shoko.minecraft.loot.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: LT-R29 (all_of, any_of, inverted, short circuit), LT-R4a/LT-R61a (named references)
class ConditionLogicTest {

  private static final LootCondition TRUE = new LootCondition.AllOf(List.of());
  private static final LootCondition FALSE = new LootCondition.AnyOf(List.of());

  private static boolean passes(LootCondition condition, RandomSource random) {
    return condition.test(Ctx.context(random)).passed();
  }

  @Test
  void provisionalAnEmptyAllOfHolds() {
    // spec: LT-R29 (liste vide: vrai)
    assertThat(passes(TRUE, ScriptedRandom.of())).isTrue();
  }

  @Test
  void provisionalAnEmptyAnyOfDoesNotHold() {
    // spec: LT-R29 (liste vide: faux)
    assertThat(passes(FALSE, ScriptedRandom.of())).isFalse();
  }

  @Test
  void provisionalAllOfNeedsEveryTerm() {
    // spec: LT-R29
    assertThat(passes(new LootCondition.AllOf(List.of(TRUE, TRUE)), ScriptedRandom.of())).isTrue();
    assertThat(passes(new LootCondition.AllOf(List.of(TRUE, FALSE)), ScriptedRandom.of()))
        .isFalse();
  }

  @Test
  void provisionalAnyOfNeedsOneTerm() {
    // spec: LT-R29
    assertThat(passes(new LootCondition.AnyOf(List.of(FALSE, TRUE)), ScriptedRandom.of())).isTrue();
    assertThat(passes(new LootCondition.AnyOf(List.of(FALSE, FALSE)), ScriptedRandom.of()))
        .isFalse();
  }

  @Test
  void provisionalInvertedNegatesItsTerm() {
    // spec: LT-R29
    assertThat(passes(new LootCondition.Inverted(TRUE), ScriptedRandom.of())).isFalse();
    assertThat(passes(new LootCondition.Inverted(FALSE), ScriptedRandom.of())).isTrue();
  }

  @Test
  void provisionalAllOfStopsAtTheFirstFailingTermWithoutDrawingForTheRest() {
    // spec: LT-R29, probe PL9 (short circuit, no draw for skipped terms)
    ScriptedRandom random = ScriptedRandom.of();
    LootCondition condition = new LootCondition.AllOf(List.of(FALSE, Tables.chance(0.5f)));
    assertThat(passes(condition, random)).isFalse();
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalAnyOfStopsAtTheFirstHoldingTermWithoutDrawingForTheRest() {
    // spec: LT-R29, probe PL9
    ScriptedRandom random = ScriptedRandom.of();
    LootCondition condition = new LootCondition.AnyOf(List.of(TRUE, Tables.chance(0.5f)));
    assertThat(passes(condition, random)).isTrue();
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalTermsAreEvaluatedInOrderAndEachRandomTermDrawsOnce() {
    // spec: LT-R29, LT-R30 (one draw per evaluation, in the order of the terms)
    ScriptedRandom random = ScriptedRandom.of(0.1, 0.9);
    LootCondition condition =
        new LootCondition.AllOf(List.of(Tables.chance(0.5f), Tables.chance(0.5f)));
    assertThat(passes(condition, random)).isFalse();
    assertThat(random.steps()).isEqualTo(2);
  }

  @Test
  void provisionalAnyOfWithRandomTermsStopsAtTheFirstSuccess() {
    // spec: LT-R29
    ScriptedRandom random = ScriptedRandom.of(0.9, 0.1, 0.1);
    LootCondition condition =
        new LootCondition.AnyOf(
            List.of(Tables.chance(0.5f), Tables.chance(0.5f), Tables.chance(0.5f)));
    assertThat(passes(condition, random)).isTrue();
    assertThat(random.steps()).isEqualTo(2);
    assertThat(random.remaining()).isEqualTo(1);
  }

  @Test
  void provisionalAReferenceEvaluatesTheNamedPredicate() {
    // spec: LT-R61a (tool/can_shear is a match_tool on shears)
    LootCondition reference =
        new LootCondition.Reference(Identifier.parse("minecraft:tool/can_shear"));
    LootContext shears = Ctx.builder(ScriptedRandom.of()).tool(LootItems.stack("shears")).build();
    LootContext pickaxe =
        Ctx.builder(ScriptedRandom.of()).tool(LootItems.stack("iron_pickaxe")).build();
    assertThat(reference.test(shears).passed()).isTrue();
    assertThat(reference.test(pickaxe).passed()).isFalse();
    assertThat(reference.test(Ctx.context(ScriptedRandom.of())).passed()).isFalse();
  }

  @Test
  void provisionalAPassingTermReportsAMatchedCause() {
    // spec: LT-R29, ConditionCause contract
    ConditionResult passed = TRUE.test(Ctx.context(ScriptedRandom.of()));
    ConditionResult failed = FALSE.test(Ctx.context(ScriptedRandom.of()));
    assertThat(passed.cause()).isEqualTo(ConditionCause.MATCHED);
    assertThat(failed.cause()).isEqualTo(ConditionCause.NOT_MATCHED);
  }
}

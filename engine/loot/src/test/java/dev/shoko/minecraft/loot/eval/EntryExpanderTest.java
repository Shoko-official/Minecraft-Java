package dev.shoko.minecraft.loot.eval;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.port.TypeSpecific;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.predicate.match.EntityPredicate;
import dev.shoko.minecraft.loot.predicate.match.Tristate;
import dev.shoko.minecraft.loot.random.DrawReason;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootPool;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.DrawLog;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: LT-R20 (conditions re-evaluated at each roll), LT-R21 (composites), LT-R22 (tag), LT-R24
// (empty), LT-R25 (weights), LT-R27 (fishing weights)
class EntryExpanderTest {

  private static final LootCondition KILLED = new LootCondition.KilledByPlayer();

  private static LootContext plain() {
    return Ctx.context(ScriptedRandom.of());
  }

  private static LootContext killer() {
    return Ctx.builder(ScriptedRandom.of())
        .entity(EntityTarget.ATTACKING_PLAYER, FakeEntity.of("player"))
        .build();
  }

  private static ItemType typeOf(LootEntry entry) {
    return ((LootEntry.Kind.Item) entry.kind()).item();
  }

  private static List<String> itemPaths(List<LootEntry> entries) {
    return entries.stream().map(e -> typeOf(e).id().path()).toList();
  }

  private static List<Integer> weights(List<Candidate> candidates) {
    return candidates.stream().map(Candidate::effectiveWeight).toList();
  }

  private static LootCondition inOpenWater() {
    return new LootCondition.EntityProperties(
        EntityTarget.THIS,
        new EntityPredicate(List.of(new EntityPredicate.Check.FishingHook(Tristate.TRUE))));
  }

  private static LootPool fishingPool() {
    return Tables.pool(
        1,
        Tables.item("dirt", 10, -2),
        Tables.when(Tables.item("diamond", 5, 2), inOpenWater()),
        Tables.item("stick", 85, -1));
  }

  private static LootContext fishing(float luck, boolean openWater) {
    FakeEntity hook =
        FakeEntity.of("fishing_bobber").specific(new TypeSpecific.FishingHook(openWater));
    return Ctx.builder(ScriptedRandom.of()).entity(EntityTarget.THIS, hook).luck(luck).build();
  }

  @Test
  void provisionalCandidatesKeepTheOrderOfThePoolAndTheWeights() {
    // spec: LT-R20, LT-R25
    LootPool pool = Tables.pool(1, Tables.item("stone", 3), Tables.item("dirt", 5));
    List<Candidate> candidates = EntryExpander.candidates(pool, plain());
    assertThat(candidates.stream().map(c -> typeOf(c.entry()).id().path()).toList())
        .containsExactly("stone", "dirt");
    assertThat(weights(candidates)).containsExactly(3, 5);
  }

  @Test
  void provisionalAFailingEntryConditionDropsTheEntry() {
    // spec: LT-R20
    LootPool pool = Tables.pool(1, Tables.when(Tables.item("stone"), KILLED), Tables.item("dirt"));
    assertThat(EntryExpander.candidates(pool, plain())).hasSize(1);
    assertThat(EntryExpander.candidates(pool, killer())).hasSize(2);
  }

  @Test
  void provisionalAnEmptyEntryCountsInTheCandidates() {
    // spec: LT-R24
    LootPool pool = Tables.pool(1, Tables.item("stone"), Tables.empty(4));
    List<Candidate> candidates = EntryExpander.candidates(pool, plain());
    assertThat(candidates).hasSize(2);
    assertThat(candidates.get(1).entry().kind()).isEqualTo(new LootEntry.Kind.Empty());
    assertThat(weights(candidates)).containsExactly(1, 4);
  }

  @Test
  void provisionalLuckAndQualityGiveTheFishingWeights() {
    // spec: LT-R27 (open water, luck 0: 10, 5, 85; luck 2: 6, 9, 83)
    assertThat(weights(EntryExpander.candidates(fishingPool(), fishing(0f, true))))
        .containsExactly(10, 5, 85);
    assertThat(weights(EntryExpander.candidates(fishingPool(), fishing(2f, true))))
        .containsExactly(6, 9, 83);
  }

  @Test
  void provisionalTheTreasureIsOutsideOpenWater() {
    // spec: LT-R27
    assertThat(weights(EntryExpander.candidates(fishingPool(), fishing(0f, false))))
        .containsExactly(10, 85);
  }

  @Test
  void provisionalAnEntryOfEffectiveWeightZeroIsDropped() {
    // spec: LT-R25
    assertThat(weights(EntryExpander.candidates(fishingPool(), fishing(6f, true))))
        .containsExactly(17, 79);
  }

  @Test
  void provisionalEveryCandidateHasAPositiveWeight() {
    // spec: Candidate contract (at least 1)
    for (float luck : new float[] {-5f, 0f, 3f, 9f, 50f}) {
      for (Candidate candidate : EntryExpander.candidates(fishingPool(), fishing(luck, true))) {
        assertThat(candidate.effectiveWeight()).isPositive();
      }
    }
  }

  @Test
  void provisionalAlternativesKeepOnlyTheFirstChildWhoseConditionHolds() {
    // spec: LT-R21
    LootEntry alternatives =
        Tables.alternatives(
            Tables.when(Tables.item("stone"), KILLED), Tables.item("dirt"), Tables.item("stick"));
    assertThat(itemPaths(EntryExpander.expand(alternatives, killer()))).containsExactly("stone");
    assertThat(itemPaths(EntryExpander.expand(alternatives, plain()))).containsExactly("dirt");
  }

  @Test
  void provisionalAlternativesWithNoPassingChildGiveNothing() {
    // spec: LT-R21
    LootEntry alternatives =
        Tables.alternatives(
            Tables.when(Tables.item("stone"), KILLED), Tables.when(Tables.item("dirt"), KILLED));
    assertThat(EntryExpander.expand(alternatives, plain())).isEmpty();
  }

  @Test
  void provisionalASequenceStopsAtTheFirstFailingChild() {
    // spec: LT-R21 (sequence)
    LootEntry sequence =
        Tables.sequence(
            Tables.item("stone"), Tables.when(Tables.item("dirt"), KILLED), Tables.item("stick"));
    assertThat(itemPaths(EntryExpander.expand(sequence, plain()))).containsExactly("stone");
    assertThat(itemPaths(EntryExpander.expand(sequence, killer())))
        .containsExactly("stone", "dirt", "stick");
  }

  @Test
  void provisionalAGroupGivesAllItsChildren() {
    // spec: LT-R21 (group)
    LootEntry group = Tables.group(Tables.item("stone"), Tables.item("dirt"));
    assertThat(itemPaths(EntryExpander.expand(group, plain()))).containsExactly("stone", "dirt");
  }

  @Test
  void provisionalAFailingConditionOnTheCompositeDropsEverything() {
    // spec: LT-R21, LT-R20 (the condition of the entry itself)
    LootEntry group = Tables.when(Tables.group(Tables.item("stone")), KILLED);
    assertThat(EntryExpander.expand(group, plain())).isEmpty();
    assertThat(EntryExpander.expand(group, killer())).hasSize(1);
  }

  @Test
  void provisionalACompositeHasNoWeightOfItsOwn() {
    // spec: LT-R21
    LootEntry heavy =
        new LootEntry(
            List.of(),
            50,
            0,
            List.of(),
            new LootEntry.Kind.Group(List.of(Tables.item("stone", 2))));
    List<Candidate> candidates = EntryExpander.candidates(Tables.pool(1, heavy), plain());
    assertThat(weights(candidates)).containsExactly(2);
  }

  @Test
  void provisionalCompositesNest() {
    // spec: LT-R21
    LootEntry nested =
        Tables.group(
            Tables.alternatives(Tables.when(Tables.item("stone"), KILLED), Tables.item("dirt")),
            Tables.item("stick"));
    assertThat(itemPaths(EntryExpander.expand(nested, plain()))).containsExactly("dirt", "stick");
    assertThat(itemPaths(EntryExpander.expand(nested, killer()))).containsExactly("stone", "stick");
  }

  @Test
  void provisionalAnExpandedTagGivesOneItemPerMemberWithTheWeightOfTheEntry() {
    // spec: LT-R22 (expand: true), probe PL8
    LootEntry tag = Tables.tag("test_gems", true, 7);
    List<LootEntry> expanded = EntryExpander.expand(tag, plain());
    assertThat(itemPaths(expanded)).containsExactlyInAnyOrder("diamond", "emerald", "iron_ingot");
    assertThat(expanded.stream().map(LootEntry::weight).distinct().toList()).containsExactly(7);
  }

  @Test
  void provisionalAnExpandedTagKeepsTheConditionOfTheEntry() {
    // spec: LT-R22
    LootEntry tag = Tables.when(Tables.tag("test_gems", true, 1), KILLED);
    assertThat(EntryExpander.expand(tag, plain())).isEmpty();
    assertThat(EntryExpander.expand(tag, killer())).hasSize(3);
  }

  @Test
  void provisionalAnEmptyTagExpandsToNothing() {
    // spec: LT-R22, cas limite (etiquette vide)
    assertThat(EntryExpander.expand(Tables.tag("empty_tag", true, 1), plain())).isEmpty();
  }

  @Test
  void provisionalANonExpandedTagStaysOneEntry() {
    // spec: LT-R22
    LootEntry tag = Tables.tag("test_gems", false, 3);
    List<LootEntry> expanded = EntryExpander.expand(tag, plain());
    assertThat(expanded).hasSize(1);
    assertThat(expanded.getFirst().kind())
        .isEqualTo(new LootEntry.Kind.Tag(Identifier.minecraft("test_gems"), false));
  }

  @Test
  void provisionalASimpleEntryExpandsToItself() {
    // spec: EntryExpander#expand contract
    LootEntry stone = Tables.item("stone");
    assertThat(EntryExpander.expand(stone, plain())).containsExactly(stone);
  }

  @Test
  void provisionalConditionsAreEvaluatedAgainAtEachCallAndMayDraw() {
    // spec: LT-R20
    ScriptedRandom random = ScriptedRandom.of(0.1, 0.9);
    LootContext context = Ctx.context(random);
    LootPool pool = Tables.pool(1, Tables.when(Tables.item("stone"), Tables.chance(0.5f)));
    assertThat(EntryExpander.candidates(pool, context)).hasSize(1);
    assertThat(EntryExpander.candidates(pool, context)).isEmpty();
    assertThat(random.steps()).isEqualTo(2);
  }

  @Test
  void provisionalARandomEntryConditionIsReportedWithItsReason() {
    // spec: EntryExpander contract (ENTRY_CONDITION), LT-R66
    DrawLog log = new DrawLog();
    LootContext context = Ctx.builder(ScriptedRandom.of(0.1)).observer(log).build();
    LootPool pool = Tables.pool(1, Tables.when(Tables.item("stone"), Tables.chance(0.5f)));
    EntryExpander.candidates(pool, context);
    assertThat(log.consumingReasons()).containsExactly(DrawReason.ENTRY_CONDITION);
  }

  @Test
  void provisionalTheSeededSourceGivesTheSameCandidatesTwice() {
    // spec: LT-R20, determinism
    LootPool pool =
        Tables.pool(
            1,
            Tables.when(Tables.item("stone"), Tables.chance(0.5f)),
            Tables.when(Tables.item("dirt"), Tables.chance(0.5f)));
    LootContext first = Ctx.context(Lcg48Random.ofSeed(5));
    LootContext second = Ctx.context(Lcg48Random.ofSeed(5));
    for (int i = 0; i < 20; i++) {
      assertThat(weights(EntryExpander.candidates(pool, first)))
          .isEqualTo(weights(EntryExpander.candidates(pool, second)));
    }
  }
}

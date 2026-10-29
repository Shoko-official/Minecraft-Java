package dev.shoko.minecraft.loot.table;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.ContextKey;
import dev.shoko.minecraft.loot.context.LootContextType;
import dev.shoko.minecraft.loot.modifier.ItemModifier;
import dev.shoko.minecraft.loot.modifier.ModifierAction;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.random.SequenceRef;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.Set;
import org.junit.jupiter.api.Test;

// spec: LT-R23 (self reference), LT-R61b (unknown named predicate), LT-R15 (warning when a
// condition reads a parameter the type does not provide)
class LootTableReferencesTest {

  private static final Identifier SELF = Identifier.parse("minecraft:blocks/self");

  private static LootTables known(String... paths) {
    SequencedMap<Identifier, LootTable> map = new LinkedHashMap<>();
    for (String path : paths) {
      map.put(Identifier.minecraft(path), Tables.table());
    }
    return new LootTables(map);
  }

  private static List<LootFinding> check(LootTable table, LootTables known) {
    return LootTableCodec.checkReferences(SELF, table, known, Ctx.standardPredicates());
  }

  private static LootCondition ref(String predicate) {
    return new LootCondition.Reference(Identifier.parse(predicate));
  }

  private static LootTable withPoolCondition(LootCondition condition) {
    return Tables.table(
        Tables.poolWith(
            Tables.conditions(condition),
            new IntProvider.Constant(1),
            0f,
            List.of(),
            Tables.item("stone")));
  }

  private static List<LootFindingCode> codes(List<LootFinding> findings) {
    return findings.stream().map(LootFinding::code).toList();
  }

  @Test
  void provisionalATableWithResolvedReferencesHasNoFinding() {
    // spec: LT-R61b
    LootTable table =
        Tables.table(
            Tables.poolWith(
                Tables.conditions(ref("minecraft:tool/can_shear")),
                new IntProvider.Constant(1),
                0f,
                List.of(),
                Tables.ref("gameplay/fish"),
                Tables.item("stone")));
    assertThat(check(table, known("gameplay/fish"))).isEmpty();
  }

  @Test
  void provisionalAnUnknownNamedPredicateIsUnresolved() {
    // spec: LT-R61b
    List<LootFinding> findings = check(withPoolCondition(ref("minecraft:tool/no_such")), known());
    assertThat(codes(findings)).containsExactly(LootFindingCode.UNRESOLVED_REFERENCE);
    assertThat(findings.getFirst().path()).startsWith("$");
  }

  @Test
  void provisionalAnUnknownPredicateNestedInTermsIsUnresolved() {
    // spec: LT-R61b, LT-R4a (terms accept references)
    LootCondition nested =
        new LootCondition.Inverted(
            new LootCondition.AnyOf(
                List.of(ref("minecraft:tool/can_shear"), ref("minecraft:nope"))));
    assertThat(codes(check(withPoolCondition(nested), known())))
        .containsExactly(LootFindingCode.UNRESOLVED_REFERENCE);
  }

  @Test
  void provisionalAnUnknownPredicateInAModifierConditionIsUnresolved() {
    // spec: LT-R61b, LT-R38
    ItemModifier modifier =
        Tables.modifier(new ModifierAction.Discard(), ref("minecraft:tool/no_such"));
    LootTable table = Tables.withModifiers(Tables.table(), modifier);
    assertThat(codes(check(table, known()))).containsExactly(LootFindingCode.UNRESOLVED_REFERENCE);
  }

  @Test
  void provisionalATableThatPointsAtItselfIsASelfReference() {
    // spec: LT-R23
    LootTable table = Tables.table(Tables.pool(1, Tables.ref("blocks/self")));
    assertThat(codes(check(table, known("blocks/self"))))
        .containsExactly(LootFindingCode.SELF_REFERENCE);
  }

  @Test
  void provisionalANamedTableThatIsNotLoadedIsUnresolved() {
    // spec: LootTableCodec#checkReferences contract
    LootTable table = Tables.table(Tables.pool(1, Tables.ref("blocks/other")));
    assertThat(codes(check(table, known()))).containsExactly(LootFindingCode.UNRESOLVED_REFERENCE);
  }

  @Test
  void provisionalAnInlineTableIsCheckedLikeItsParent() {
    // spec: LT-R10 (an inline table is a table), LootTableCodec#checkReferences contract
    LootTable inline = Tables.table(Tables.pool(1, Tables.ref("blocks/other")));
    LootTable table = Tables.table(Tables.pool(1, Tables.inline(inline)));
    assertThat(codes(check(table, known()))).containsExactly(LootFindingCode.UNRESOLVED_REFERENCE);
  }

  @Test
  void provisionalFindingsFollowTheOrderOfTheTable() {
    // spec: LootTableCodec#checkReferences contract (in the order of the table)
    LootTable table =
        Tables.table(
            Tables.pool(1, Tables.ref("blocks/first")),
            Tables.pool(1, Tables.ref("blocks/second")));
    List<LootFinding> findings = check(table, known());
    assertThat(findings).hasSize(2);
    assertThat(findings.get(0).path()).contains("pools[0]");
    assertThat(findings.get(1).path()).contains("pools[1]");
  }

  @Test
  void provisionalAConditionThatReadsAMissingParameterIsOnlyAWarning() {
    // spec: LT-R15 (the table still loads)
    LootContextType originOnly =
        new LootContextType(Identifier.minecraft("test"), 99, Set.of(), Set.of(ContextKey.ORIGIN));
    LootTable table =
        new LootTable(
            originOnly,
            List.of(
                Tables.poolWith(
                    Tables.conditions(new LootCondition.SurvivesExplosion()),
                    new IntProvider.Constant(1),
                    0f,
                    List.of(),
                    Tables.item("stone"))),
            List.of(),
            new SequenceRef.Unnamed());
    List<LootFinding> findings = check(table, known());
    assertThat(codes(findings)).containsExactly(LootFindingCode.UNAVAILABLE_PARAMETER);
    assertThat(findings.getFirst().code().severity())
        .isEqualTo(dev.shoko.minecraft.data.diag.Severity.WARNING);
  }

  @Test
  void provisionalNoWarningWhenTheTypeProvidesTheParameter() {
    // spec: LT-R15
    LootTable table =
        new LootTable(
            Ctx.ANY,
            List.of(withPoolCondition(new LootCondition.SurvivesExplosion()).pools().getFirst()),
            List.of(),
            new SequenceRef.Unnamed());
    assertThat(check(table, known())).isEmpty();
  }
}

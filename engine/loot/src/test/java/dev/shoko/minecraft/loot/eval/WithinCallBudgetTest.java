package dev.shoko.minecraft.loot.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.LootBudgetException;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import dev.shoko.minecraft.loot.enchant.craft.XpRepair;
import dev.shoko.minecraft.loot.enchant.effect.AffectedTarget;
import dev.shoko.minecraft.loot.enchant.effect.ConditionalEntityEffect;
import dev.shoko.minecraft.loot.enchant.effect.EffectComponentValue;
import dev.shoko.minecraft.loot.enchant.effect.EffectComponents;
import dev.shoko.minecraft.loot.enchant.effect.EffectEvaluator;
import dev.shoko.minecraft.loot.enchant.effect.EnchantedTarget;
import dev.shoko.minecraft.loot.enchant.effect.EntityEffect;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.table.LootPool;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.Tables;
import dev.shoko.minecraft.loot.testing.TestEnchantments;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: LT-R74f (S-arch): the calls nested in one call of an entry point share the budget of that
// call: two evaluations of 524000 nodes in the same repair, equipment walk, trigger or candidates
// call add up to more than the cap of 1000000, while the same evaluations in two successive calls
// do not. Without the entry the nested public calls would each start a new budget.
@Timeout(60)
class WithinCallBudgetTest {

  private static final Identifier COMPONENT = Identifier.minecraft("damage");

  private static LootContext context() {
    return Ctx.context(Lcg48Random.ofSeed(1));
  }

  private static EffectComponents repairEffects(LevelBasedValue value) {
    return TestEnchantments.componentEffects(Identifier.minecraft("repair_with_xp"), value);
  }

  private static LootContext repairContext(LevelBasedValue first, LevelBasedValue second) {
    LootData data =
        TestEnchantments.dataWith(
            TestEnchantments.definition("repair_a", repairEffects(first)),
            TestEnchantments.definition("repair_b", repairEffects(second)));
    return LootContext.builder(data, Ctx.ANY, Lcg48Random.ofSeed(1)).build();
  }

  private static ItemStack repairable() {
    return LootItems.damaged(
        LootItems.enchanted(
            LootItems.stack("iron_pickaxe"),
            Map.of("minecraft:repair_a", 1, "minecraft:repair_b", 1)),
        100);
  }

  @Test
  void provisionalTheEffectsOfOneRepairShareTheBudgetOfThatCall() {
    // spec: LT-R74f (XpRepair.repair is an entry point whose nested evaluations share one budget):
    // two enchantments of 2^19 - 1 nodes each add up to more than 1000000 nodes in one call
    LootContext context =
        repairContext(TestEnchantments.binaryDag(18), TestEnchantments.binaryDag(18));
    assertThatThrownBy(() -> XpRepair.repair(repairable(), 10, context))
        .isInstanceOf(LootBudgetException.class);
  }

  @Test
  void provisionalTheEffectsOfOneRepairUnderTheCapPass() {
    // spec: LT-R74f: 2 x (2^18 - 1) nodes, about 524000 in all, are under the cap
    LootContext context =
        repairContext(TestEnchantments.binaryDag(17), TestEnchantments.binaryDag(17));
    assertThat(XpRepair.repair(repairable(), 10, context).stack().damage()).isLessThan(100);
  }

  @Test
  void provisionalTwoSuccessiveRepairsOfOneContextDoNotAddUp() {
    // spec: LT-R74f: each call of 524000 nodes starts a new budget; four calls make 2 million
    LootContext context =
        repairContext(TestEnchantments.binaryDag(17), TestEnchantments.binaryDag(17));
    assertThatCode(
            () -> {
              for (int i = 0; i < 4; i++) {
                XpRepair.repair(repairable(), 10, context);
              }
            })
        .doesNotThrowAnyException();
  }

  @Test
  void provisionalTheEnchantmentsOfOneEquipmentWalkShareTheBudgetOfThatCall() {
    // spec: LT-R74f (EffectEvaluator.valueOverEquipment is an entry point), EN-R31: two
    // enchantments of the same stack of 2^19 - 1 nodes each add up to more than 1000000
    LootData data =
        TestEnchantments.dataWith(
            TestEnchantments.definition(
                "repair_a",
                TestEnchantments.componentEffects(COMPONENT, TestEnchantments.binaryDag(18))),
            TestEnchantments.definition(
                "repair_b",
                TestEnchantments.componentEffects(COMPONENT, TestEnchantments.binaryDag(18))));
    LootContext context = LootContext.builder(data, Ctx.ANY, Lcg48Random.ofSeed(1)).build();
    FakeEntity wearer =
        FakeEntity.of("zombie")
            .wearing(
                EquipmentSlot.MAINHAND,
                LootItems.enchanted(
                    LootItems.stack("diamond_sword"),
                    Map.of("minecraft:repair_a", 1, "minecraft:repair_b", 1)));
    assertThatThrownBy(
            () ->
                EffectEvaluator.valueOverEquipment(
                    wearer, data.enchantments(), COMPONENT, 0f, context))
        .isInstanceOf(LootBudgetException.class);
  }

  @Test
  void provisionalTheEntriesOfOneTriggerShareTheBudgetOfThatCall() {
    // spec: LT-R74f (EffectEvaluator.trigger is an entry point): two entity effects whose
    // requirement walks 600000 conditions add up to more than 1000000 nodes in one call
    LootCondition leaf = new LootCondition.AllOf(List.of());
    LootCondition huge = new LootCondition.AllOf(Collections.nCopies(600_000, leaf));
    EntityEffect ignite =
        new EntityEffect.Typed(
            Identifier.minecraft("ignite"), (JsonObject) Json.parse("{'duration':4}"));
    ConditionalEntityEffect entry =
        new ConditionalEntityEffect(
            ignite, List.of(huge), EnchantedTarget.ATTACKER, AffectedTarget.VICTIM);
    SequencedMap<Identifier, EffectComponentValue> map = new LinkedHashMap<>();
    map.put(
        Identifier.minecraft("post_attack"),
        new EffectComponentValue.EntityEffects(List.of(entry, entry)));
    EnchantmentDefinition twice =
        TestEnchantments.definition("test_twice", new EffectComponents(map));
    LootContext context = context();
    assertThatThrownBy(
            () ->
                EffectEvaluator.trigger(
                    twice, Identifier.minecraft("post_attack"), 1, context, intent -> {}))
        .isInstanceOf(LootBudgetException.class);
  }

  @Test
  void provisionalTheEntriesOfOnePoolShareTheBudgetOfTheCandidatesCall() {
    // spec: LT-R74f (EntryExpander.candidates is an entry point): two entries of 600000 constant
    // conditions each add up to more than 1000000 nodes in one call
    LootCondition leaf = new LootCondition.AllOf(List.of());
    LootCondition[] conditions = Collections.nCopies(600_000, leaf).toArray(new LootCondition[0]);
    LootPool pool =
        Tables.pool(
            1,
            Tables.when(Tables.item("stone"), conditions),
            Tables.when(Tables.item("dirt"), conditions));
    LootContext context = context();
    assertThatThrownBy(() -> EntryExpander.candidates(pool, context))
        .isInstanceOf(LootBudgetException.class);
  }

  @Test
  void provisionalTheEntriesOfOnePoolUnderTheCapPass() {
    // spec: LT-R74f: 2 x 400000 nodes in one call, under the cap
    LootCondition leaf = new LootCondition.AllOf(List.of());
    LootCondition[] conditions = Collections.nCopies(400_000, leaf).toArray(new LootCondition[0]);
    LootPool pool =
        Tables.pool(
            1,
            Tables.when(Tables.item("stone"), conditions),
            Tables.when(Tables.item("dirt"), conditions));
    assertThat(EntryExpander.candidates(pool, context())).hasSize(2);
  }
}

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
import dev.shoko.minecraft.loot.enchant.EnchantmentCatalog;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import dev.shoko.minecraft.loot.enchant.effect.AffectedTarget;
import dev.shoko.minecraft.loot.enchant.effect.ConditionalEntityEffect;
import dev.shoko.minecraft.loot.enchant.effect.ConditionalValueEffect;
import dev.shoko.minecraft.loot.enchant.effect.EffectComponentValue;
import dev.shoko.minecraft.loot.enchant.effect.EffectComponents;
import dev.shoko.minecraft.loot.enchant.effect.EffectEvaluator;
import dev.shoko.minecraft.loot.enchant.effect.EnchantedTarget;
import dev.shoko.minecraft.loot.enchant.effect.EntityEffect;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.enchant.effect.ValueEffect;
import dev.shoko.minecraft.loot.modifier.ItemModifier;
import dev.shoko.minecraft.loot.modifier.ModifierApplier;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootPool;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.Tables;
import dev.shoko.minecraft.loot.testing.TestEnchantments;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: LT-R74f (S-arch): the budget of nodes is PER CALL of an entry point and not per context:
// 1200000 independent light calls with one context built once never add up to the cap of 1000000
// nodes (the counters are reset when a call enters while no other call is in progress), and the
// calls nested in one call share the budget of that call (also for XpRepair, which calls the
// effects several times). Every public entry point that evaluates conditions, providers,
// modifiers, entries, tables or enchantment effects is covered; MachineProviders builds a new
// context at each call, so nothing can add up there.
@Timeout(60)
class IndependentCallsBudgetTest {

  private static final int CALLS = 1_200_000;
  private static final Identifier COMPONENT = Identifier.minecraft("damage");

  private static void repeat(Runnable call) {
    assertThatCode(
            () -> {
              for (int i = 0; i < CALLS; i++) {
                call.run();
              }
            })
        .doesNotThrowAnyException();
  }

  private static LootContext context() {
    return Ctx.context(Lcg48Random.ofSeed(1));
  }

  private static EffectComponents addEffect() {
    SequencedMap<Identifier, EffectComponentValue> map = new LinkedHashMap<>();
    map.put(
        COMPONENT,
        new EffectComponentValue.ValueEffects(
            List.of(
                new ConditionalValueEffect(
                    new ValueEffect.Add(new LevelBasedValue.Constant(1f)), List.of()))));
    return new EffectComponents(map);
  }

  @Test
  void provisionalConditionTestsOfOneContextDoNotAddUp() {
    // spec: LT-R74f
    LootContext context = context();
    LootCondition condition = new LootCondition.AllOf(List.of());
    repeat(() -> condition.test(context));
  }

  @Test
  void provisionalIntegerProviderSamplesOfOneContextDoNotAddUp() {
    // spec: LT-R74f
    LootContext context = context();
    IntProvider provider = new IntProvider.Constant(3);
    repeat(() -> provider.sample(context));
  }

  @Test
  void provisionalFloatProviderSamplesOfOneContextDoNotAddUp() {
    // spec: LT-R74f
    LootContext context = context();
    FloatProvider provider = new FloatProvider.Constant(3f);
    repeat(() -> provider.sample(context));
  }

  @Test
  void provisionalModifierApplicationsOfOneContextDoNotAddUp() {
    // spec: LT-R74f (ModifierApplier.apply)
    LootContext context = context();
    ItemModifier modifier = Tables.setCount(2);
    ItemStack stack = LootItems.stack("stone");
    repeat(() -> ModifierApplier.apply(modifier, stack, context));
  }

  @Test
  void provisionalModifierListsOfOneContextDoNotAddUp() {
    // spec: LT-R74f (ModifierApplier.applyAll)
    LootContext context = context();
    List<ItemModifier> modifiers = List.of(Tables.setCount(2));
    List<ItemStack> stacks = List.of(LootItems.stack("stone"));
    repeat(() -> ModifierApplier.applyAll(modifiers, stacks, context));
  }

  @Test
  void provisionalEntryCandidatesOfOneContextDoNotAddUp() {
    // spec: LT-R74f (EntryExpander.candidates)
    LootContext context = context();
    LootPool pool = Tables.pool(1, Tables.item("stone"));
    repeat(() -> EntryExpander.candidates(pool, context));
  }

  @Test
  void provisionalEntryExpansionsOfOneContextDoNotAddUp() {
    // spec: LT-R74f (EntryExpander.expand)
    LootContext context = context();
    LootEntry entry = Tables.item("stone");
    repeat(() -> EntryExpander.expand(entry, context));
  }

  @Test
  void provisionalTableDrawsOfOneContextDoNotAddUp() {
    // spec: LT-R74f (LootEvaluator.evaluate)
    LootContext context = context();
    LootTable table = Tables.table(Tables.pool(0, Tables.item("stone")));
    repeat(() -> LootEvaluator.evaluate(table, context));
  }

  @Test
  void provisionalEffectValuesOfOneContextDoNotAddUp() {
    // spec: LT-R74f (EffectEvaluator.value)
    LootContext context = context();
    EffectComponents effects = addEffect();
    repeat(() -> EffectEvaluator.value(effects, COMPONENT, 1, 0f, context));
  }

  @Test
  void provisionalEffectValuesOverTheEquipmentOfOneContextDoNotAddUp() {
    // spec: LT-R74f (EffectEvaluator.valueOverEquipment), EN-R31: the effect has no requirement,
    // so no nested call resets the counters by itself
    LootData data =
        TestEnchantments.dataWith(
            TestEnchantments.definition(
                "repair_a",
                TestEnchantments.componentEffects(COMPONENT, new LevelBasedValue.Constant(1f))));
    LootContext context = LootContext.builder(data, Ctx.ANY, Lcg48Random.ofSeed(1)).build();
    FakeEntity wearer =
        FakeEntity.of("zombie")
            .wearing(
                EquipmentSlot.MAINHAND,
                LootItems.enchanted(
                    LootItems.stack("diamond_sword"), Map.of("minecraft:repair_a", 1)));
    EnchantmentCatalog catalog = data.enchantments();
    repeat(() -> EffectEvaluator.valueOverEquipment(wearer, catalog, COMPONENT, 0f, context));
  }

  @Test
  void provisionalEffectTriggersOfOneContextDoNotAddUp() {
    // spec: LT-R74f (EffectEvaluator.trigger)
    LootContext context = context();
    EnchantmentDefinition fire =
        TestEnchantments.definition(
            "test_fire", definitionEffects(new LootCondition.AllOf(List.of())));
    List<Object> sink = new ArrayList<>();
    repeat(
        () -> {
          EffectEvaluator.trigger(fire, Identifier.minecraft("post_attack"), 1, context, sink::add);
          sink.clear();
        });
  }

  @Test
  void provisionalValueEffectApplicationsOfOneContextDoNotAddUp() {
    // spec: LT-R74f (the public ValueEffect.apply is an entry point: the counters are reset at
    // each call, 1200000 calls of 1 node must not pass the cap of 1000000)
    LootContext context = context();
    ValueEffect add = new ValueEffect.Add(new LevelBasedValue.Constant(1f));
    repeat(() -> add.apply(2f, 1, context));
  }

  @Test
  void provisionalEveryKindOfValueEffectApplicationDoesNotAddUp() {
    // spec: LT-R74f
    LootContext context = context();
    LevelBasedValue one = new LevelBasedValue.Constant(1f);
    List<ValueEffect> effects =
        List.of(
            new ValueEffect.Set(one),
            new ValueEffect.Multiply(one),
            new ValueEffect.Exponential(one),
            new ValueEffect.RemoveBinomial(one),
            new ValueEffect.AllOf(List.of(new ValueEffect.Add(one), new ValueEffect.Set(one))));
    for (ValueEffect effect : effects) {
      assertThatCode(
              () -> {
                for (int i = 0; i < CALLS; i++) {
                  effect.apply(2f, 1, context);
                }
              })
          .as(effect.getClass().getSimpleName())
          .doesNotThrowAnyException();
    }
  }

  @Test
  void provisionalAFailedCallLeavesTheContextUsableForTheNextOnes() {
    // spec: LT-R74f: the budget error of one call does not poison the following calls
    LootContext context = context();
    LevelBasedValue value = new LevelBasedValue.Constant(1f);
    for (int level = 0; level < 21; level++) {
      value = new LevelBasedValue.Fraction(value, value);
    }
    EffectComponents hungry =
        new EffectComponents(
            new LinkedHashMap<>(
                Map.of(
                    COMPONENT,
                    new EffectComponentValue.ValueEffects(
                        List.of(
                            new ConditionalValueEffect(new ValueEffect.Add(value), List.of()))))));
    assertThatThrownBy(() -> EffectEvaluator.value(hungry, COMPONENT, 1, 0f, context))
        .isInstanceOf(LootBudgetException.class);
    assertThat(EffectEvaluator.value(addEffect(), COMPONENT, 1, 0f, context)).isEqualTo(1f);
  }

  private static EffectComponents definitionEffects(LootCondition... requirements) {
    SequencedMap<Identifier, EffectComponentValue> map = new LinkedHashMap<>();
    EntityEffect ignite =
        new EntityEffect.Typed(
            Identifier.minecraft("ignite"), (JsonObject) Json.parse("{'duration':4}"));
    map.put(
        Identifier.minecraft("post_attack"),
        new EffectComponentValue.EntityEffects(
            List.of(
                new ConditionalEntityEffect(
                    ignite,
                    List.of(requirements),
                    EnchantedTarget.ATTACKER,
                    AffectedTarget.VICTIM))));
    return new EffectComponents(map);
  }
}

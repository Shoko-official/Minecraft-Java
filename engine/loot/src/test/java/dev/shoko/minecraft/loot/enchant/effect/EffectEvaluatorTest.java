package dev.shoko.minecraft.loot.enchant.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.DamageSourceData;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.EnchantmentCatalog;
import dev.shoko.minecraft.loot.enchant.EnchantmentCosts;
import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;
import dev.shoko.minecraft.loot.enchant.EnchantmentItems;
import dev.shoko.minecraft.loot.enchant.SlotGroup;
import dev.shoko.minecraft.loot.port.EffectIntent;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Sets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

// spec: EN-R13 (entries in order, requirements), EN-R15 (active and level in the context), EN-R18,
// EN-R23 (unbreaking), EN-R31 (evaluation over the equipment), EN-R6 (inactive slots), EN-R16
class EffectEvaluatorTest {

  private static EnchantmentCatalog catalog() {
    return EnchantFixtures.catalog();
  }

  private static final Identifier DAMAGE = Identifier.minecraft("damage");
  private static final Identifier PROTECTION = Identifier.minecraft("damage_protection");
  private static final Identifier ITEM_DAMAGE = Identifier.minecraft("item_damage");
  private static final float EPS = 1e-4f;

  private static EffectComponents effectsOf(String enchantment) {
    return catalog().find(Identifier.minecraft(enchantment)).orElseThrow().effects();
  }

  private static LootContext melee(ScriptedRandom random) {
    return Ctx.builder(random)
        .damageSource(new DamageSourceData(Identifier.minecraft("player_attack"), true))
        .build();
  }

  private static EffectComponents valueEffects(
      Identifier component, ConditionalValueEffect... entries) {
    SequencedMap<Identifier, EffectComponentValue> map = new LinkedHashMap<>();
    map.put(component, new EffectComponentValue.ValueEffects(List.of(entries)));
    return new EffectComponents(map);
  }

  private static ConditionalValueEffect add(float value, LootCondition... requirements) {
    return new ConditionalValueEffect(
        new ValueEffect.Add(new LevelBasedValue.Constant(value)), List.of(requirements));
  }

  private static ConditionalValueEffect multiply(float factor) {
    return new ConditionalValueEffect(
        new ValueEffect.Multiply(new LevelBasedValue.Constant(factor)), List.of());
  }

  private static ItemStack withEnchant(String item, String enchantment, int level) {
    return LootItems.enchanted(LootItems.stack(item), Map.of("minecraft:" + enchantment, level));
  }

  @Test
  void provisionalAnEnchantmentComponentAddsItsValueAtTheLevel() {
    // spec: EN-R18, V-E15 (sharpness III: 1 + 0.5 x 2 = 2)
    float value =
        EffectEvaluator.value(effectsOf("sharpness"), DAMAGE, 3, 0f, melee(ScriptedRandom.of()));
    assertThat(value).isCloseTo(2.0f, within(EPS));
  }

  @Test
  void provisionalTheValueStartsFromTheValueGiven() {
    // spec: EN-R18, EN-R19
    float value =
        EffectEvaluator.value(effectsOf("sharpness"), DAMAGE, 1, 5f, melee(ScriptedRandom.of()));
    assertThat(value).isCloseTo(6.0f, within(EPS));
  }

  @Test
  void provisionalAComponentTheEnchantmentDoesNotHaveReturnsTheStart() {
    // spec: EffectEvaluator#value contract
    float value =
        EffectEvaluator.value(
            effectsOf("sharpness"),
            Identifier.minecraft("tick"),
            3,
            7f,
            melee(ScriptedRandom.of()));
    assertThat(value).isEqualTo(7f);
  }

  @Test
  void provisionalProtectionHoldsAgainstOrdinaryDamage() {
    // spec: V-E4, V-E16 (protection IV: 4; requirement: not a damage that bypasses invulnerability)
    float value =
        EffectEvaluator.value(
            effectsOf("protection"), PROTECTION, 4, 0f, melee(ScriptedRandom.of()));
    assertThat(value).isCloseTo(4.0f, within(EPS));
  }

  @Test
  void provisionalProtectionDoesNotHoldAgainstDamageThatBypassesInvulnerability() {
    // spec: V-E4 (requirement fausse: #minecraft:bypasses_invulnerability)
    LootContext voidDamage =
        Ctx.builder(ScriptedRandom.of())
            .damageSource(new DamageSourceData(Identifier.minecraft("out_of_world"), false))
            .build();
    assertThat(EffectEvaluator.value(effectsOf("protection"), PROTECTION, 4, 0f, voidDamage))
        .isEqualTo(0f);
  }

  @Test
  void provisionalTheEntriesApplyInOrderEachOnTheResultOfThePrevious() {
    // spec: EN-R13
    EffectComponents addThenMultiply = valueEffects(DAMAGE, add(2f), multiply(3f));
    EffectComponents multiplyThenAdd = valueEffects(DAMAGE, multiply(3f), add(2f));
    LootContext context = Ctx.context(ScriptedRandom.of());
    assertThat(EffectEvaluator.value(addThenMultiply, DAMAGE, 1, 4f, context)).isEqualTo(18f);
    assertThat(EffectEvaluator.value(multiplyThenAdd, DAMAGE, 1, 4f, context)).isEqualTo(14f);
  }

  @Test
  void provisionalAnEntryWhoseRequirementsFailIsSkipped() {
    // spec: EN-R13 (requirements)
    EffectComponents effects =
        valueEffects(DAMAGE, add(10f, new LootCondition.KilledByPlayer()), add(1f));
    assertThat(EffectEvaluator.value(effects, DAMAGE, 1, 0f, Ctx.context(ScriptedRandom.of())))
        .isEqualTo(1f);
  }

  @Test
  void provisionalTheRequirementsSeeAnActiveEnchantment() {
    // spec: EN-R15 (enchantment_active is true when a component is evaluated)
    EffectComponents effects =
        valueEffects(DAMAGE, add(5f, new LootCondition.EnchantmentActiveCheck(true)));
    assertThat(EffectEvaluator.value(effects, DAMAGE, 1, 0f, Ctx.context(ScriptedRandom.of())))
        .isEqualTo(5f);
  }

  @Test
  void provisionalUnbreakingOnAnArmorPieceUsesTheArmorProbability() {
    // spec: EN-R23, V-E5 (level 1: 0.2), exclusive entries
    ItemStack armor = withEnchant("diamond_chestplate", "unbreaking", 1);
    ScriptedRandom saves = ScriptedRandom.of(0.1);
    LootContext cancels = Ctx.builder(saves).tool(armor).build();
    assertThat(EffectEvaluator.value(effectsOf("unbreaking"), ITEM_DAMAGE, 1, 1f, cancels))
        .isEqualTo(0f);
    assertThat(saves.steps()).as("the two entries are exclusive: one draw").isEqualTo(1);
    LootContext keeps = Ctx.builder(ScriptedRandom.of(0.5)).tool(armor).build();
    assertThat(EffectEvaluator.value(effectsOf("unbreaking"), ITEM_DAMAGE, 1, 1f, keeps))
        .isEqualTo(1f);
  }

  @Test
  void provisionalUnbreakingOnAToolUsesTheLevelOverLevelPlusOne() {
    // spec: EN-R23, V-E5 (level 2: 2/3)
    ItemStack pick = withEnchant("iron_pickaxe", "unbreaking", 2);
    LootContext cancels = Ctx.builder(ScriptedRandom.of(0.5)).tool(pick).build();
    LootContext keeps = Ctx.builder(ScriptedRandom.of(0.9)).tool(pick).build();
    assertThat(EffectEvaluator.value(effectsOf("unbreaking"), ITEM_DAMAGE, 2, 1f, cancels))
        .isEqualTo(0f);
    assertThat(EffectEvaluator.value(effectsOf("unbreaking"), ITEM_DAMAGE, 2, 1f, keeps))
        .isEqualTo(1f);
  }

  @Test
  void provisionalMendingMultipliesTheRepairByTwo() {
    // spec: EN-R44b, EN-R19c (start 1, multiply 2.0)
    float factor =
        EffectEvaluator.value(
            effectsOf("mending"),
            Identifier.minecraft("repair_with_xp"),
            1,
            1f,
            Ctx.context(ScriptedRandom.of()));
    assertThat(factor).isEqualTo(2f);
  }

  @Test
  void provisionalTheEquipmentOfEverySlotContributesToProtection() {
    // spec: EN-R31
    FakeEntity wearer =
        FakeEntity.of("zombie")
            .wearing(EquipmentSlot.CHEST, withEnchant("diamond_chestplate", "protection", 3))
            .wearing(EquipmentSlot.HEAD, withEnchant("diamond_chestplate", "protection", 1));
    float value =
        EffectEvaluator.valueOverEquipment(
            wearer, catalog(), PROTECTION, 0f, melee(ScriptedRandom.of()));
    assertThat(value).isCloseTo(4.0f, within(EPS));
  }

  @Test
  void provisionalAnEnchantmentOutsideItsActiveSlotsHasNoEffect() {
    // spec: EN-R6, EN-R31
    FakeEntity holder =
        FakeEntity.of("zombie")
            .wearing(EquipmentSlot.MAINHAND, withEnchant("diamond_sword", "protection", 4))
            .wearing(EquipmentSlot.HEAD, withEnchant("diamond_sword", "sharpness", 5));
    assertThat(
            EffectEvaluator.valueOverEquipment(
                holder, catalog(), PROTECTION, 0f, melee(ScriptedRandom.of())))
        .isEqualTo(0f);
    assertThat(
            EffectEvaluator.valueOverEquipment(
                holder, catalog(), DAMAGE, 0f, melee(ScriptedRandom.of())))
        .isEqualTo(0f);
  }

  @Test
  void provisionalAnEnchantmentInItsActiveSlotCounts() {
    // spec: EN-R6 (mainhand), EN-R31
    FakeEntity fighter =
        FakeEntity.of("player")
            .wearing(EquipmentSlot.MAINHAND, withEnchant("diamond_sword", "sharpness", 3))
            .wearing(EquipmentSlot.OFFHAND, withEnchant("diamond_sword", "sharpness", 5));
    float value =
        EffectEvaluator.valueOverEquipment(
            fighter, catalog(), DAMAGE, 0f, melee(ScriptedRandom.of()));
    assertThat(value).isCloseTo(2.0f, within(EPS));
  }

  @Test
  void provisionalAnEnchantmentUnknownToTheCatalogIsIgnored() {
    // spec: EN-R10, cas limite
    FakeEntity wearer =
        FakeEntity.of("zombie")
            .wearing(
                EquipmentSlot.HEAD, withEnchant("diamond_chestplate", "no_such_enchantment", 2));
    assertThat(
            EffectEvaluator.valueOverEquipment(
                wearer, catalog(), PROTECTION, 1f, melee(ScriptedRandom.of())))
        .isEqualTo(1f);
  }

  @Test
  void provisionalAnEntityWithoutEquipmentKeepsTheStart() {
    // spec: EN-R31
    assertThat(
            EffectEvaluator.valueOverEquipment(
                FakeEntity.of("zombie"), catalog(), PROTECTION, 3f, melee(ScriptedRandom.of())))
        .isEqualTo(3f);
  }

  private static EnchantmentDefinition withPostAttack(LootCondition... requirements) {
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
    return new EnchantmentDefinition(
        Identifier.minecraft("test_fire"),
        Json.parse("{'translate':'enchantment.test'}"),
        2,
        new EnchantmentCosts(
            2, 4, new LevelBasedValue.Linear(10f, 20f), new LevelBasedValue.Linear(60f, 20f)),
        List.of(SlotGroup.MAINHAND),
        new EnchantmentItems(
            Sets.itemTag("enchantable/weapon"), Sets.itemTag("enchantable/weapon"), false),
        Sets.enchantments(),
        new EffectComponents(map));
  }

  @Test
  void provisionalTriggerSendsNothingWhenTheRequirementsFail() {
    // spec: EN-R16, EffectEvaluator#trigger contract (nothing for an entry whose requirements fail)
    List<EffectIntent> sink = new ArrayList<>();
    EffectEvaluator.trigger(
        withPostAttack(new LootCondition.KilledByPlayer()),
        Identifier.minecraft("post_attack"),
        1,
        Ctx.context(ScriptedRandom.of()),
        sink::add);
    assertThat(sink).isEmpty();
  }

  @Test
  void provisionalTriggerSendsNothingForAComponentTheEnchantmentDoesNotHave() {
    // spec: EffectEvaluator#trigger contract
    List<EffectIntent> sink = new ArrayList<>();
    EffectEvaluator.trigger(
        withPostAttack(),
        Identifier.minecraft("tick"),
        1,
        Ctx.context(ScriptedRandom.of()),
        sink::add);
    assertThat(sink).isEmpty();
  }
}
